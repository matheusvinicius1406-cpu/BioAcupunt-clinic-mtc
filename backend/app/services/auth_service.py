import hashlib
from datetime import datetime, timedelta, timezone

from sqlalchemy import select, update
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.config import get_settings
from app.core.errors import AppError
from app.core.security import (
    create_access_token,
    create_refresh_token,
    decode_token,
    hash_password,
    new_session_id,
    verify_password,
)
from app.models.auth import RefreshSession
from app.models.user import User
from app.schemas.auth import TokenPairResponse


def _hash_token(token: str) -> str:
    return hashlib.sha256(token.encode("utf-8")).hexdigest()


async def _issue_new_session(db: AsyncSession, user: User) -> tuple[TokenPairResponse, str]:
    settings = get_settings()
    session_id = new_session_id()
    refresh_token = create_refresh_token(subject=str(user.id), session_id=session_id)
    access_token = create_access_token(subject=str(user.id), clinic_id=user.clinic_id, role=user.role)

    db.add(
        RefreshSession(
            id=session_id,
            user_id=user.id,
            token_hash=_hash_token(refresh_token),
            expires_at=datetime.now(timezone.utc) + timedelta(days=settings.refresh_token_expire_days),
        )
    )
    await db.commit()

    return TokenPairResponse(access_token=access_token, refresh_token=refresh_token), session_id


async def register_user(db: AsyncSession, *, clinic_id: int, email: str, password: str, full_name: str, role: str) -> User:
    user = User(
        clinic_id=clinic_id,
        email=email.lower().strip(),
        password_hash=hash_password(password),
        full_name=full_name,
        role=role,
    )
    db.add(user)
    await db.commit()
    await db.refresh(user)
    return user


async def login(db: AsyncSession, *, email: str, password: str) -> TokenPairResponse:
    result = await db.execute(select(User).where(User.email == email.lower().strip()))
    user = result.scalar_one_or_none()
    if user is None or not user.is_active or not verify_password(password, user.password_hash):
        raise AppError("E-mail ou senha inválidos.", status_code=401, code="invalid_credentials")

    pair, _session_id = await _issue_new_session(db, user)
    return pair


async def refresh(db: AsyncSession, *, refresh_token: str) -> TokenPairResponse:
    try:
        payload = decode_token(refresh_token)
    except ValueError as exc:
        raise AppError("Refresh token inválido.", status_code=401, code="invalid_token") from exc

    if payload.get("type") != "refresh":
        raise AppError("Refresh token inválido.", status_code=401, code="invalid_token")

    session_id = payload["sid"]
    user_id = int(payload["sub"])

    result = await db.execute(select(RefreshSession).where(RefreshSession.id == session_id))
    session = result.scalar_one_or_none()

    if session is None:
        raise AppError("Sessão não encontrada.", status_code=401, code="invalid_token")

    if session.revoked:
        # Reuse of an already-rotated (or already-revoked) refresh token: treat as
        # theft and revoke every session for this user.
        await db.execute(
            update(RefreshSession).where(RefreshSession.user_id == user_id).values(revoked=True)
        )
        await db.commit()
        raise AppError(
            "Reutilização de refresh token detectada. Todas as sessões foram revogadas.",
            status_code=401,
            code="token_reuse_detected",
        )

    if session.expires_at.replace(tzinfo=timezone.utc) < datetime.now(timezone.utc):
        raise AppError("Refresh token expirado.", status_code=401, code="token_expired")

    result = await db.execute(select(User).where(User.id == user_id))
    user = result.scalar_one_or_none()
    if user is None or not user.is_active:
        raise AppError("Usuário inválido.", status_code=401, code="invalid_user")

    new_pair, new_session_id_ = await _issue_new_session(db, user)

    session.revoked = True
    session.replaced_by = new_session_id_
    await db.commit()

    return new_pair


async def logout(db: AsyncSession, *, refresh_token: str) -> None:
    try:
        payload = decode_token(refresh_token)
    except ValueError:
        return
    await db.execute(
        update(RefreshSession).where(RefreshSession.id == payload.get("sid")).values(revoked=True)
    )
    await db.commit()


async def logout_all(db: AsyncSession, *, user_id: int) -> None:
    await db.execute(update(RefreshSession).where(RefreshSession.user_id == user_id).values(revoked=True))
    await db.commit()
