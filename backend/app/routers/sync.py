from fastapi import APIRouter
from app.models.schemas import SyncRequest
from app.core.supabase_client import supabase

router = APIRouter()

@router.post("/sync")
async def sync(request: SyncRequest):
    # Lógica de UPSERT no Supabase + versionamento
    # supabase.table("knowledge_nodes").upsert(...)
    return {"status": "synced", "conflicts": []}
