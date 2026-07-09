"""
MKIS — Repositórios.
Interfaces e implementações SQLAlchemy para domínio.
"""

from __future__ import annotations

from datetime import datetime
from typing import Sequence
from uuid import UUID

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from mkis_backend.app.models.knowledge.models import (
    IngestionJobModel,
    KnowledgeGraphEdgeModel,
    KnowledgeNodeModel,
    KnowledgeNodeVersionModel,
    NodeStatus,
)
from mkis_backend.app.models.domain.entities import EdgeStatus


class KnowledgeRepository:
    def __init__(self, session: AsyncSession):
        self.session = session

    async def create_node(self, node: KnowledgeNodeModel) -> KnowledgeNodeModel:
        self.session.add(node)
        await self.session.flush()
        return node

    async def get_node(self, tenant_id: UUID, node_id: UUID) -> KnowledgeNodeModel | None:
        stmt = select(KnowledgeNodeModel).where(KnowledgeNodeModel.tenant_id == tenant_id, KnowledgeNodeModel.id == node_id)
        return (await self.session.execute(stmt)).scalar_one_or_none()

    async def list_nodes(self, tenant_id: UUID, *, cursor: UUID | None = None, limit: int = 20) -> tuple[Sequence[KnowledgeNodeModel], UUID | None]:
        stmt = select(KnowledgeNodeModel).where(KnowledgeNodeModel.tenant_id == tenant_id)
        if cursor:
            stmt = stmt.where(KnowledgeNodeModel.id > cursor)
        stmt = stmt.order_by(KnowledgeNodeModel.id.asc()).limit(limit + 1)
        rows = (await self.session.execute(stmt)).scalars().all()
        has_next = len(rows) > limit
        rows = rows[:limit]
        next_cursor = rows[-1].id if has_next and rows else None
        return rows, next_cursor

    async def save_version(self, version: KnowledgeNodeVersionModel) -> KnowledgeNodeVersionModel:
        self.session.add(version)
        await self.session.flush()
        return version

    async def update_status(self, tenant_id: UUID, node_id: UUID, status: NodeStatus) -> KnowledgeNodeModel | None:
        node = await self.get_node(tenant_id, node_id)
        if not node:
            return None
        node.status = status
        node.updated_at = datetime.utcnow()
        await self.session.flush()
        return node

    async def create_edge(self, edge: KnowledgeGraphEdgeModel) -> KnowledgeGraphEdgeModel:
        self.session.add(edge)
        await self.session.flush()
        return edge

    async def get_edge(self, tenant_id: UUID, edge_id: UUID) -> KnowledgeGraphEdgeModel | None:
        stmt = select(KnowledgeGraphEdgeModel).where(KnowledgeGraphEdgeModel.tenant_id == tenant_id, KnowledgeGraphEdgeModel.id == edge_id)
        return (await self.session.execute(stmt)).scalar_one_or_none()

    async def list_edges(self, tenant_id: UUID, subject_id: UUID | None = None, predicate: str | None = None) -> Sequence[KnowledgeGraphEdgeModel]:
        stmt = select(KnowledgeGraphEdgeModel).where(KnowledgeGraphEdgeModel.tenant_id == tenant_id)
        if subject_id:
            stmt = stmt.where(KnowledgeGraphEdgeModel.subject_id == subject_id)
        if predicate:
            stmt = stmt.where(KnowledgeGraphEdgeModel.predicate == predicate)
        return (await self.session.execute(stmt)).scalars().all()

    async def update_edge_status(self, edge_id: UUID, status: EdgeStatus) -> KnowledgeGraphEdgeModel | None:
        edge = await self.session.get(KnowledgeGraphEdgeModel, edge_id)
        if not edge:
            return None
        edge.status = status
        edge.created_at = edge.created_at
        await self.session.flush()
        return edge

    async def create_job(self, job: IngestionJobModel) -> IngestionJobModel:
        self.session.add(job)
        await self.session.flush()
        return job

    async def get_job(self, tenant_id: UUID, job_id: UUID) -> IngestionJobModel | None:
        stmt = select(IngestionJobModel).where(IngestionJobModel.tenant_id == tenant_id, IngestionJobModel.id == job_id)
        return (await self.session.execute(stmt)).scalar_one_or_none()

    async def update_job_status(self, job_id: UUID, status: str, error_code: str | None = None, error_message: str | None = None) -> IngestionJobModel:
        job = await self.session.get(IngestionJobModel, job_id)
        if not job:
            raise ValueError("Job not found")
        job.status = status
        job.error_code = error_code
        job.error_message = error_message
        job.updated_at = datetime.utcnow()
        await self.session.flush()
        return job
