"""
Medical Knowledge Intelligence System (MKIS)
Domain Layer — Entidades, Value Objects, Aggregates e invariantes.
Sem dependências externas ao Python stdlib.
"""

from __future__ import annotations

import hashlib
import uuid
from dataclasses import dataclass, field
from datetime import datetime, timezone
from enum import Enum
from typing import Any

# ---------------------------------------------------------------------------
# Primitivos e value objects
# ---------------------------------------------------------------------------

SHA256 = str
Checksum = str
DocString = str


def utcnow() -> datetime:
    return datetime.now(timezone.utc)


def sha256_hex(value: bytes) -> Checksum:
    return hashlib.sha256(value).hexdigest()


def stable_id() -> uuid.UUID:
    return uuid.uuid7(int(utcnow().timestamp() * 1000))


# ---------------------------------------------------------------------------
# Enums canônicos (fonte: 29-canonical-references.md)
# ---------------------------------------------------------------------------

class KnowledgeType(str, Enum):
    artigo = "artigo"
    guideline = "guideline"
    revisao = "revisao"
    capitulo = "capitulo"
    tese = "tese"
    caso_clinico = "caso_clinico"
    nota = "nota"


class Category(str, Enum):
    MTC = "MTC"
    medicina_ocidental = "medicina_ocidental"
    saude_publica = "saude_publica"
    epidemiologia = "epidemiologia"
    farmacologia = "farmacologia"
    fisiologia = "fisiologia"
    psicologia = "psicologia"


class Specialty(str, Enum):
    acupuntura = "acupuntura"
    auriculoterapia = "auriculoterapia"
    fitoterapia = "fitoterapia"
    ventosas = "ventosas"
    moxabustao = "moxabustao"
    quiropraxia = "quiropraxia"


class Source(str, Enum):
    pubmed = "pubmed"
    europepmc = "europepmc"
    semantic_scholar = "semantic_scholar"
    crossref = "crossref"
    openalex = "openalex"
    clinicaltrials = "clinicaltrials"
    scielo = "scielo"
    bvs = "bvs"
    who = "who"
    doaj = "doaj"


class NodeStatus(str, Enum):
    draft = "draft"
    pending_review = "pending_review"
    approved = "approved"
    rejected = "rejected"
    deprecated = "deprecated"
    superseded = "superseded"


class ClinicalEvidence(str, Enum):
    baixa = "baixa"
    moderada = "moderada"
    alta = "alta"
    muito_alta = "muito_alta"


class EvidenceLevel(str, Enum):
    O_CEBM_G1 = "O:CEBM_G1"
    O_CEBM_G2 = "O:CEBM_G2"
    O_CEBM_G3 = "O:CEBM_G3"
    O_CEBM_G4 = "O:CEBM_G4"
    GRADE_HIGH = "GRADE_HIGH"
    GRADE_MODERATE = "GRADE_MODERATE"
    GRADE_LOW = "GRADE_LOW"
    GRADE_VERY_LOW = "GRADE_VERY_LOW"


class BiasRisk(str, Enum):
    baixo = "baixo"
    moderado = "moderado"
    alto = "alto"
    nao_avaliado = "nao_avaliado"


class ActorType(str, Enum):
    user = "user"
    system = "system"
    admin = "admin"
    health_check = "health_check"


# ---------------------------------------------------------------------------
# Aggregate: KnowledgeNode
# ---------------------------------------------------------------------------

@dataclass
class KnowledgeNode:
    """Agregado raiz: unidade atômica de conhecimento científico."""
    id: uuid.UUID = field(default_factory=stable_id)
    version: str = "v1"
    status: NodeStatus = NodeStatus.draft
    created_at: datetime = field(default_factory=utcnow)
    updated_at: datetime = field(default_factory=utcnow)
    tenant_id: uuid.UUID = field(default_factory=stable_id)
    checksum: Checksum = ""
    embedding_version: str = "v1"
    embedding: bytes | None = None
    metadata: dict[str, Any] = field(default_factory=dict)


# ---------------------------------------------------------------------------
# Aggregate: IngestionJob
# ---------------------------------------------------------------------------

class JobStatus(str, Enum):
    queued = "queued"
    downloading = "downloading"
    validation_failed = "validation_failed"
    scan_failed = "scan_failed"
    scan_quarantined = "scan_quarantined"
    ocr_queued = "ocr_queued"
    ocr_running = "ocr_running"
    ocr_failed = "ocr_failed"
    ocr_needs_human_review = "ocr_needs_human_review"
    parsing_queued = "parsing_queued"
    parsing_running = "parsing_running"
    parsing_failed = "parsing_failed"
    chunking_queued = "chunking_queued"
    chunking_running = "chunking_running"
    chunking_failed = "chunking_failed"
    embedding_queued = "embedding_queued"
    embedding_running = "embedding_running"
    embedding_failed = "embedding_failed"
    indexing_queued = "indexing_queued"
    indexing_running = "indexing_running"
    indexing_failed = "indexing_failed"
    awaiting_approval = "awaiting_approval"
    approved_to_index = "approved_to_index"
    completed = "completed"
    failed = "failed"
    manually_blocked = "manually_blocked"
    cancelled = "cancelled"
    pipeline_failed = "pipeline_failed"


@dataclass
class IngestionJob:
    id: uuid.UUID = field(default_factory=stable_id)
    tenant_id: uuid.UUID = field(default_factory=stable_id)
    status: JobStatus = JobStatus.queued
    created_at: datetime = field(default_factory=utcnow)
    updated_at: datetime = field(default_factory=utcnow)
    actor_id: uuid.UUID | None = None


# ---------------------------------------------------------------------------
# Aggregate: PurgeCertificate — LGPD hard delete imutável
# ---------------------------------------------------------------------------

@dataclass
class PurgeCertificate:
    id: uuid.UUID = field(default_factory=stable_id)
    tenant_id: uuid.UUID = field(default_factory=stable_id)
    target_node_id: uuid.UUID | None = None
    legal_hold: bool = False
    executed_at: datetime | None = None
    approved_by: uuid.UUID | None = None
    requested_by: uuid.UUID | None = None
    requested_at: datetime = field(default_factory=utcnow)


# ---------------------------------------------------------------------------
# Evento de domínio canônico
# ---------------------------------------------------------------------------

class CanonicalEvent(str, Enum):
    knowledge_node_created = "knowledge.node.created.v1"
    knowledge_node_versioned = "knowledge.node.versioned.v1"
    knowledge_node_approved = "knowledge.node.approved.v1"
    knowledge_node_deprecated = "knowledge.node.deprecated.v1"
    knowledge_node_rejected = "knowledge.node.rejected.v1"
    knowledge_artifact_uploaded = "knowledge.artifact.uploaded.v1"
    knowledge_artifact_quarantined = "knowledge.artifact.quarantined.v1"
    ingestion_job_status_changed = "ingestion.job.status_changed.v1"
    ingestion_job_failed = "ingestion.job.failed.v1"
    ingestion_job_completed = "ingestion.job.completed.v1"
    ingestion_job_cancelled = "ingestion.job.cancelled.v1"
    embedding_generated = "embedding.generated.v1"
    embedding_migration_completed = "embedding.migration.completed.v1"
    graph_edge_created = "graph.edge.created.v1"
    evidence_score_updated = "evidence.score.updated.v1"
    hard_delete_completed = "hard_delete.completed.v1"


@dataclass
class DomainEvent:
    event_id: uuid.UUID = field(default_factory=stable_id)
    occurred_at: datetime = field(default_factory=utcnow)
    event_name: CanonicalEvent = CanonicalEvent.knowledge_node_created
    stream_id: uuid.UUID | None = None
    correlation_id: uuid.UUID | None = None
    causation_id: uuid.UUID | None = None
    tenant_id: uuid.UUID = field(default_factory=stable_id)
    request_id: uuid.UUID = field(default_factory=stable_id)
    actor_id: uuid.UUID | None = None
    payload_hash: str = ""
    schema_version: str = "v1"
    payload: dict[str, Any] = field(default_factory=dict)


# ---------------------------------------------------------------------------
# Knowledge Graph edge
# ---------------------------------------------------------------------------

class EdgeStatus(str, Enum):
    active = "active"
    superseded = "superseded"
    deprecated = "deprecated"
    quarantined = "quarantined"


@dataclass
class KnowledgeGraphEdge:
    id: uuid.UUID = field(default_factory=stable_id)
    tenant_id: uuid.UUID = field(default_factory=stable_id)
    source_version_id: uuid.UUID | None = None
    subject_type: str = "knowledge_node"
    subject_id: uuid.UUID = field(default_factory=stable_id)
    predicate: str = "RELATED_TO"
    object_type: str = "knowledge_node"
    object_id: uuid.UUID = field(default_factory=stable_id)
    status: EdgeStatus = EdgeStatus.active
    evidence_refs: list[uuid.UUID] = field(default_factory=list)
    relation_group: str | None = None
