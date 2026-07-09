"""
MKIS — Serviço de ingestão e atualização de embeddings.
Resume extração, chunking, vetorização e reindexação.
"""

from __future__ import annotations

from datetime import datetime, timezone
from typing import Sequence
from uuid import UUID

from mkis_backend.app.models.domain.entities import JobStatus
from mkis_backend.app.repositories.knowledge import KnowledgeRepository


class PipelineService:
    def __init__(self, repo: KnowledgeRepository):
        self.repo = repo

    async def enqueue_embedding(self, tenant_id: UUID, node_id: UUID) -> IngestionJobModel:
        job = IngestionJobModel(tenant_id=tenant_id, status=JobStatus.embedding_queued)
        return await self.repo.create_job(job)

    async def requeue_for_index(self, tenant_id: UUID, node_id: UUID) -> IngestionJobModel:
        job = IngestionJobModel(tenant_id=tenant_id, status=JobStatus.indexing_queued)
        return await self.repo.create_job(job)

    async def mark_completed(self, job_id: UUID) -> IngestionJobModel | None:
        return await self.repo.update_job_status(job_id, JobStatus.completed)

    async def mark_failed(self, job_id: UUID, error_code: str, error_message: str) -> IngestionJobModel | None:
        return await self.repo.update_job_status(job_id, JobStatus.pipeline_failed, error_code=error_code, error_message=error_message)
