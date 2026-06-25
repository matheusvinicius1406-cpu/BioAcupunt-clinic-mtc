from fastapi import APIRouter
from app.core.supabase_client import supabase
from app.models.schemas import KnowledgeNode

router = APIRouter()

@router.get("/")
async def get_nodes():
    return supabase.table("knowledge_nodes").select("*").execute()

@router.post("/")
async def create_node(node: KnowledgeNode):
    # Supabase converte automaticamente o Pydantic model se passado como dict
    return supabase.table("knowledge_nodes").insert(node.dict()).execute()
