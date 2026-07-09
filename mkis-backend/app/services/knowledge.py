"""
MKIS — Serviço de KnowledgeNode.
Aplicação do domínio: casos de uso de criação, versionamento, aprovação e status.
"""

from __future__ import annotations

from datetime import datetime, timezone
from uuid import UUID

from mkis_backend.app.events.outbox import OutboxEvent
from mkis_backend.app.events.publisher import EventPublisher
from mkis_backend.app.events.registry import CANONICAL_EVENTS
from mkis_backend.app.models.domain.entities import NodeStatus, ActorType
from mkis_backend.app.repositories.knowledge import KnowledgeRepository


class KnowledgeService:
    def __init__(self, repo: KnowledgeRepository):
        self.repo = repo

    async def create_node(self, tenant_id: UUID, actor_id: UUID | None, checksum: str, *, metadata: dict | None = None) -> KnowledgeNodeModel:
        node = KnowledgeNodeModel(
            tenant_id=tenant_id,
            actor_id=actor_id,
            checksum=checksum,
            status=NodeStatus.draft,
            version="v1",
            embedding_version="v1",
            node_metadata=str(metadata or {}),
        )
        return await self.repo.create_node(node)

    async def get_node(self, tenant_id: UUID, node_id: UUID) -> KnowledgeNodeModel | None:
        return await self.repo.get_node(tenant_id, node_id)

    async def list_nodes(self, tenant_id: UUID, *, cursor: UUID | None = None, limit: int = 20):
        return await self.repo.list_nodes(tenant_id, cursor=cursor, limit=limit)

    async def version_node(self, tenant_id: UUID, node_id: UUID, actor_id: UUID | None, snapshot: str | None = None) -> KnowledgeNodeModel | None:
        node = await self.repo.get_node(tenant_id, node_id)
        if not node:
            return None
        major, minor = (int(x) for x in node.version.lstrip("v").split("."))
        node.version = f"v{major}.{minor + 1}"
        node.updated_at = datetime.now(timezone.utc)
        await self.repo.save_version(KnowledgeNodeVersionModel(node_id=node.id, version=node.version, actor_id=actor_id, snapshot=snapshot))
        await self.repo.update_status(tenant_id, node_id, node.status)
        return node

    async def approve_node(self, tenant_id: UUID, node_id: UUID, actor_id: UUID | None) -> KnowledgeNodeModel | None:
        node = await self.repo.update_status(tenant_id, node_id, NodeStatus.approved)
        if node:
            node.updated_at = datetime.now(timezone.utc)
        return node

    async def reject_node(self, tenant_id: UUID, node_id: UUID, actor_id: UUID | None) -> KnowledgeNodeModel | None:
        node = await self.repo.update_status(tenant_id, node_id, NodeStatus.rejected)
        if node:
            node.updated_at = datetime.now(timezone.utc)
        return node
