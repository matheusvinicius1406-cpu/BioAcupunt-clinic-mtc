from app.core.cache import async_cache

@async_cache(ttl=86400)
async def search_images(query: str, limit: int = 15):
    # Aqui entraria a chamada HTTP para a API da Wikipedia/Wikimedia
    return {"results": []}
