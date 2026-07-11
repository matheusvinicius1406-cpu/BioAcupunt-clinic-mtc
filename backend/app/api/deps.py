from collections.abc import Callable

from fastapi import Depends
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.errors import AppError
from app.core.security import decode_token
from app.db.session import get_db
from app.models.user import User

# auto_error=False: FastAPI's HTTPBearer defaults to raising 403 when the
# Authorization header is missing, which is wrong HTTP semantics (403 means
# "authenticated but not allowed"; a missing credential is 401 "not
# authenticated"). Handle the missing case ourselves to get a correct 401.
_bearer_scheme = HTTPBearer(auto_error=False)


async def get_current_user(
    credentials: HTTPAuthorizationCredentials | None = Depends(_bearer_scheme),
    db: AsyncSession = Depends(get_db),
) -> User:
    if credentials is None:
        raise AppError("Não autenticado.", status_code=401, code="not_authenticated")

    try:
        payload = decode_token(credentials.credentials)
    except ValueError as exc:
        raise AppError("Token inválido.", status_code=401, code="invalid_token") from exc

    if payload.get("type") != "access":
        raise AppError("Token inválido.", status_code=401, code="invalid_token")

    result = await db.execute(select(User).where(User.id == int(payload["sub"])))
    user = result.scalar_one_or_none()
    if user is None or not user.is_active:
        raise AppError("Usuário inválido.", status_code=401, code="invalid_user")

    return user


def require_role(*allowed_roles: str) -> Callable:
    async def _check(user: User = Depends(get_current_user)) -> User:
        if user.role not in allowed_roles:
            raise AppError("Sem permissão para este recurso.", status_code=403, code="forbidden")
        return user

    return _check
