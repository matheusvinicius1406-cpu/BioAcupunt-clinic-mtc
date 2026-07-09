"""
MKIS — Segurança e autenticação.
JWT access/refresh, RBAC e helpers de identidade.
"""

from __future__ import annotations

import os
from dataclasses import dataclass
from datetime import datetime, timedelta, timezone
from typing import Optional

from jose import JWTError, jwt
from passlib.context import CryptContext

from mkis_backend.app.core.config import settings

pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")


class AuthError(Exception):
    status_code = 401
    code = "Unauthorized"


class ForbiddenError(Exception):
    status_code = 403
    code = "Forbidden"


def hash_password(password: str) -> str:
    return pwd_context.hash(password)


def verify_password(password: str, hashed: str) -> bool:
    return pwd_context.verify(password, hashed)


def create_access_token(actor_id: str, tenant_id: str, expires_minutes: int | None = None) -> str:
    minutes = expires_minutes or settings.jwt_access_ttl_minutes
    expire = datetime.now(timezone.utc) + timedelta(minutes=minutes)
    payload = {
        "sub": actor_id,
        "tenant_id": tenant_id,
        "type": "access",
        "exp": expire,
        "iat": datetime.now(timezone.utc),
    }
    return jwt.encode(payload, settings.jwt_secret_key, algorithm="HS256")


def create_refresh_token(actor_id: str, tenant_id: str) -> str:
    expire = datetime.now(timezone.utc) + timedelta(days=settings.jwt_refresh_days)
    payload = {
        "sub": actor_id,
        "tenant_id": tenant_id,
        "type": "refresh",
        "exp": expire,
        "iat": datetime.now(timezone.utc),
    }
    return jwt.encode(payload, settings.jwt_secret_key, algorithm="HS256")


def decode_token(token: str) -> dict:
    try:
        payload = jwt.decode(token, settings.jwt_secret_key, algorithms=["HS256"])
    except JWTError as exc:
        raise AuthError("invalid_token") from exc
    return payload
