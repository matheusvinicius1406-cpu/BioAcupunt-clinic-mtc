import json
import redis
from functools import wraps
from app.core.config import settings

r = redis.Redis.from_url(settings.REDIS_URL)

def async_cache(ttl: int = 86400):
    """
    Decorator para cachear resultados de funções async no Redis.
    """
    def decorator(func):
        @wraps(func)
        async def wrapper(*args, **kwargs):
            key = f"{func.__name__}:{str(args)}:{str(kwargs)}"
            cached_value = r.get(key)
            if cached_value:
                return json.loads(cached_value)
            
            result = await func(*args, **kwargs)
            r.setex(key, ttl, json.dumps(result))
            return result
        return wrapper
    return decorator
