from slowapi import Limiter
from slowapi.util import get_remote_address

from app.core.config import get_settings

limiter = Limiter(key_func=get_remote_address)


def login_rate_limit() -> str:
    """Read fresh from settings (not cached at import time) so tests can
    override RATE_LIMIT_LOGIN_PER_MINUTE via env before the app starts."""
    return f"{get_settings().rate_limit_login_per_minute}/minute"
