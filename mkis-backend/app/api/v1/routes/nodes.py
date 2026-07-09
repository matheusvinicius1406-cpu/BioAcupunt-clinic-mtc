"""
MKIS — API v1.
Rotas públicas e governança: nodes, upload, graph, evidence, quarantine, purge.
"""

from __future__ import annotations

from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException, Query

from mkis_backend.app.api.v1.routes.search import ApiResponse, PaginatedResponse, apply_pagination
from mkis_backend.app.models.domain.entities import NodeStatus
from mkis_backend.app.repositories.knowledge import KnowledgeRepository
from mkis_backend.app.services.knowledge import KnowledgeService
from mkis_backend.app.services.search import SearchHit, SearchService

router = APIRouter()


# Dependência utilitária para gates iniciais.
# Substituir pelo container de DI em produção.
async def repo_dep() -> KnowledgeRepository:  # noqa: E306
    raise AssertionError("Wire DI container before use")


@router.post("/nodes")
async def create_node(payload: dict, repo: KnowledgeRepository = Depends(repo_dep)):
    service = KnowledgeService(repo)
    tenant_id = payload.get("tenant_id")
    actor_id = payload.get("actor_id")
    node = await service.create_node(tenant_id=tenant_id, actor_id=actor_id, checksum=payload.get("checksum", ""), metadata=payload.get("metadata"))
    return ApiResponse(success=True, data={"id": str(node.id)})


@router.get("/nodes")
async def list_nodes(tenant_id: UUID, cursor: UUID | None = None, limit: int = 20, repo: KnowledgeRepository = Depends(repo_dep)):
    service = KnowledgeService(repo)
    nodes, next_cursor = await service.list_nodes(tenant_id, cursor=cursor, limit=limit)
    response = apply_pagination(nodes, limit=limit, has_next=next_cursor is not None)
    response.page_info.next_cursor = str(next_cursor) if next_cursor else None
    return ApiResponse(success=True, data=response.model_dump())


@router.get("/search")
async def search(tenant_id: UUID, q: str = "", limit: int = 20, repo: KnowledgeRepository = Depends(repo_dep)):
    service = SearchService(repo)
    hits, next_cursor = await service.hybrid_search(tenant_id, query=q, limit=limit)
    return ApiResponse(success=True, data={"items": [{"node_id": str(h.node_id), "score": h.score, "snippet": h.snippet} for h in hits], "page_info": {"limit": limit, "has_next": next_cursor is not None}})
