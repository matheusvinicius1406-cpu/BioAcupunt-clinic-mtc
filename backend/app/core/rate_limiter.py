import redis
from functools import wraps
from fastapi import HTTPException
from app.core.config import settings

# Conexão com Redis
r = redis.Redis.from_url(settings.REDIS_URL)

def rate_limit(key_prefix: str, limit: int, window: int):
    """
    Simples implementação de rate limiting baseada em Redis.
    """
    def decorator(func):
        @wraps(func)
        async def wrapper(*args, **kwargs):
            # Implementação real de rate limiting viria aqui
            # count = r.incr(key)
            # ...
            return await func(*args, **kwargs)
        return wrapper
    return decorator
