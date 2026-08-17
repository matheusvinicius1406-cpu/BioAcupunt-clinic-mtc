from fastapi import APIRouter, Depends, Query
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.deps import get_current_user
from app.db.session import get_db
from app.knowledge.domain import KnowledgeEntity
from app.knowledge.service import search
from app.models.user import User

router = APIRouter(prefix="/api/v1/knowledge", tags=["knowledge-core"])


@router.get("/search", response_model=list[KnowledgeEntity])
async def search_knowledge(
    q: str | None = Query(default=None),
    limit: int = Query(default=50, ge=1, le=200),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> list[KnowledgeEntity]:
    return await search(db, q, limit)
