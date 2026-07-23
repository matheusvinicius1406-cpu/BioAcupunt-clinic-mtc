from fastapi import APIRouter, Depends, Query
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.deps import get_current_user
from app.core.errors import AppError
from app.db.session import get_db
from app.models.user import User
from app.repositories import library_repository
from app.schemas.library import LibraryNodeResponse

router = APIRouter(prefix="/api/v1/library", tags=["library"])


@router.get("", response_model=list[LibraryNodeResponse])
async def list_library(
    q: str | None = Query(default=None, description="Busca em título e resumo."),
    limit: int = Query(default=200, ge=1, le=1000),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> list[LibraryNodeResponse]:
    return await library_repository.list_nodes(db, query=q, limit=limit)


@router.get("/{node_id}", response_model=LibraryNodeResponse)
async def get_library_node(
    node_id: str,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> LibraryNodeResponse:
    node = await library_repository.get_node(db, node_id=node_id)
    if node is None:
        raise AppError("Artigo não encontrado.", status_code=404, code="library_node_not_found")
    return node
