import re
import time
import unicodedata

from sqlalchemy.ext.asyncio import AsyncSession

from app.knowledge.domain import (
    KnowledgeEntity,
    KnowledgeEntityType,
    KnowledgeProvenance,
    KnowledgeStatus,
    KnowledgeVersion,
)
from app.repositories import library_repository


def canonical_id(entity_type: KnowledgeEntityType, name: str) -> str:
    normalized = unicodedata.normalize("NFD", name.strip()).encode("ascii", "ignore").decode().lower()
    normalized = re.sub(r"[^a-z0-9]+", "_", normalized).strip("_")
    return f"{entity_type.value.lower()}.{normalized}"


def from_library(node) -> KnowledgeEntity:
    """Convert a library_nodes row to a canonical KnowledgeEntity.

    Mirrors the Android LibraryAdapter.toCanonical() contract.
    """
    entity_type = KnowledgeEntityType.DOCUMENT
    raw_type = (node.type or "").upper().replace("-", "_")
    for candidate in KnowledgeEntityType:
        if candidate.value == raw_type:
            entity_type = candidate
            break

    now = int(time.time() * 1000)
    node_id = str(node.id)

    return KnowledgeEntity(
        id=canonical_id(entity_type, node.title),
        type=entity_type,
        canonical_name=node.title,
        summary=node.summary or "",
        content=node.content or "",
        metadata={},
        source_ids=[f"source.library.{node_id}"],
        citation_ids=[f"citation.library.{node_id}"],
        evidence_ids=[],
        version=KnowledgeVersion(
            version=str(node.version),
            created_at=now,
            updated_at=now,
            status=KnowledgeStatus.PUBLISHED,
        ),
        provenance=[KnowledgeProvenance(
            original_source="library",
            original_id=node_id,
            original_type=node.type or "",
            migration_version="knowledge-core-v1",
            imported_at=now,
        )],
        created_at=now,
        updated_at=now,
    )


async def search(db: AsyncSession, query: str | None, limit: int) -> list[KnowledgeEntity]:
    nodes = await library_repository.list_nodes(db, query=query, limit=limit)
    return [from_library(node) for node in nodes]
