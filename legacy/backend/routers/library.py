from fastapi import APIRouter
from app.core.cache import async_cache
from app.core.rate_limiter import rate_limit

router = APIRouter()

@router.get("/search")
@rate_limit("search", 100, 3600)
@async_cache(ttl=86400)
async def search(query: str):
    # Chamadas às APIs externas (Semantic Scholar, Wikimedia) seriam orquestradas aqui
    return {"results": []}

@router.get("/download-pdf")
async def download_pdf(url: str):
    # Validação de domínio contra whitelist deve ser feita aqui antes de baixar
    return {"url": url, "status": "processing"}
