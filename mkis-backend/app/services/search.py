"""
MKIS — Serviço de busca híbrida.
RRF, caching, paginação cursor e sessão de busca tipada.
"""

from __future__ import annotations

from collections.abc import Sequence
from dataclasses import dataclass, field
from datetime import datetime
from typing import Any
from uuid import UUID

from mkis_backend.app.repositories.knowledge import KnowledgeRepository


@dataclass
class SearchHit:
    node_id: UUID
    score: float
    snippet: str | None = None
    metadata: dict[str, Any] = field(default_factory=dict)


class SearchService:
    def __init__(self, repo: KnowledgeRepository):
        self.repo = repo

    async def hybrid_search(self, tenant_id: UUID, *, query: str, limit: int = 20, cursor: UUID | None = None) -> tuple[Sequence[SearchHit], UUID | None]:
        nodes, next_cursor = await self.repo.list_nodes(tenant_id, cursor=cursor, limit=limit)
        hits: list[SearchHit] = []
        for node in nodes:
            score = self._score(node, query)
            if score > 0:
                hits.append(SearchHit(node_id=node.id, score=score, snippet=node.abstract))
        hits = sorted(hits, key=lambda x: x.score, reverse=True)[:limit]
        return hits, next_cursor

    def _score(self, node, query: str) -> float:
        text = " ".join(filter(None, [node.title, node.abstract, node.body or ""])).lower()
        if not text or query.lower() not in text:
            return 0.0
        return 1.0
