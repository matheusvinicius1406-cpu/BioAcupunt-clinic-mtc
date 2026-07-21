from fastapi import APIRouter, Depends, Request
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.deps import get_current_user
from app.core.limiter import limiter, login_rate_limit
from app.db.session import get_db
from app.models.user import User
from app.schemas.auth import (
    LoginRequest,
    RefreshRequest,
    RegisterRequest,
    TokenPairResponse,
    UserResponse,
)
from app.services import auth_service

router = APIRouter(prefix="/api/v1/auth", tags=["auth"])


@router.post("/register", response_model=TokenPairResponse, status_code=201)
@limiter.limit(login_rate_limit)
async def register(request: Request, payload: RegisterRequest, db: AsyncSession = Depends(get_db)) -> TokenPairResponse:
    return await auth_service.register(
        db,
        email=payload.email,
        password=payload.password,
        full_name=payload.full_name,
        clinic_name=payload.clinic_name,
    )


@router.post("/login", response_model=TokenPairResponse)
@limiter.limit(login_rate_limit)
async def login(request: Request, payload: LoginRequest, db: AsyncSession = Depends(get_db)) -> TokenPairResponse:
    return await auth_service.login(db, email=payload.email, password=payload.password)


@router.post("/refresh", response_model=TokenPairResponse)
async def refresh(payload: RefreshRequest, db: AsyncSession = Depends(get_db)) -> TokenPairResponse:
    return await auth_service.refresh(db, refresh_token=payload.refresh_token)


@router.post("/logout")
async def logout(payload: RefreshRequest, db: AsyncSession = Depends(get_db)) -> dict:
    await auth_service.logout(db, refresh_token=payload.refresh_token)
    return {"status": "ok"}


@router.post("/logout-all")
async def logout_all(db: AsyncSession = Depends(get_db), current_user: User = Depends(get_current_user)) -> dict:
    await auth_service.logout_all(db, user_id=current_user.id)
    return {"status": "ok"}


@router.get("/me", response_model=UserResponse)
async def me(current_user: User = Depends(get_current_user)) -> User:
    return current_user
