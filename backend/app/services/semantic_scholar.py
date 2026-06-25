from app.core.cache import async_cache

@async_cache(ttl=86400)
async def search_papers(query: str, limit: int = 10):
    # Aqui entraria a chamada HTTP para api.semanticscholar.org
    return {"data": []}
