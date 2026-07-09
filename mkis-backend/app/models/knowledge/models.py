"""
MKIS — Banco: mapeamento SQLAlchemy dos núcleos do domínio.
"""

from __future__ import annotations

import uuid
from datetime import datetime, timezone
from typing import Optional
from sqlalchemy import (
    Boolean,
    Column,
    DateTime,
    Enum,
    Float,
    ForeignKey,
    Integer,
    LargeBinary,
    String,
    Text,
)
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import DeclarativeBase, relationship

from mkis_backend.app.models.domain.entities import (
    BiasRisk,
    ClinicalEvidence,
    EdgeStatus,
    EvidenceLevel,
    JobStatus,
    KnowledgeType,
    NodeStatus,
    Source,
    Specialty,
)


class Base(DeclarativeBase):
    pass


class KnowledgeArtifactModel(Base):
    __tablename__ = "knowledge_artifacts"
    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    tenant_id = Column(UUID(as_uuid=True), index=True, nullable=False)
    actor_id = Column(UUID(as_uuid=True), nullable=True)
    created_at = Column(DateTime(timezone=True), default=datetime.now(timezone.utc), nullable=False)
    document_hash = Column(String(255), nullable=False, index=True)
    source_filename = Column(String(1024), nullable=True)
    mime_type = Column(String(255), nullable=True)
    size_bytes = Column(Integer, nullable=True)
    checksum = Column(String(255), nullable=False, index=True)


class KnowledgeNodeModel(Base):
    __tablename__ = "knowledge_nodes"
    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    version = Column(String(64), nullable=False)
    status = Column(Enum(NodeStatus), default=NodeStatus.draft, nullable=False)
    created_at = Column(DateTime(timezone=True), default=datetime.now(timezone.utc), nullable=False)
    updated_at = Column(DateTime(timezone=True), default=datetime.now(timezone.utc), onupdate=datetime.now(timezone.utc), nullable=False)
    tenant_id = Column(UUID(as_uuid=True), index=True, nullable=False)
    actor_id = Column(UUID(as_uuid=True), nullable=True)
    checksum = Column(String(255), nullable=False, index=True)
    knowledge_type = Column(Enum(KnowledgeType), nullable=True)
    title = Column(Text, nullable=True)
    abstract = Column(Text, nullable=True)
    body = Column(Text, nullable=True)
    category = Column(Enum(Category), nullable=True)
    specialty = Column(Enum(Specialty), nullable=True)
    source = Column(Enum(Source), nullable=True)
    source_ref = Column(String(255), nullable=True)
    language = Column(String(32), nullable=True)
    publication_date = Column(DateTime(timezone=True), nullable=True)
    review_date = Column(DateTime(timezone=True), nullable=True)
    authors = Column(Text, nullable=True)
    keywords = Column(Text, nullable=True)
    clinical_evidence = Column(Enum(ClinicalEvidence), nullable=True)
    evidence_level = Column(Enum(EvidenceLevel), nullable=True)
    bias_risk = Column(Enum(BiasRisk), nullable=True)
    relevance_score = Column(Float, nullable=True)
    retention_until = Column(DateTime(timezone=True), nullable=True)
    node_metadata = Column(Text, nullable=True)
    embedding_version = Column(String(64), nullable=False, default="v1")
    embedding = Column(LargeBinary, nullable=True)
    embedding_v2 = Column(LargeBinary, nullable=True)


class KnowledgeNodeVersionModel(Base):
    __tablename__ = "knowledge_node_versions"
    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    node_id = Column(UUID(as_uuid=True), ForeignKey("knowledge_nodes.id"), index=True, nullable=False)
    version = Column(String(64), nullable=False)
    created_at = Column(DateTime(timezone=True), default=datetime.now(timezone.utc), nullable=False)
    actor_id = Column(UUID(as_uuid=True), nullable=True)
    snapshot = Column(Text, nullable=True)


class IngestionJobModel(Base):
    __tablename__ = "ingestion_jobs"
    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    tenant_id = Column(UUID(as_uuid=True), index=True, nullable=False)
    status = Column(Enum(JobStatus), default=JobStatus.queued, nullable=False)
    created_at = Column(DateTime(timezone=True), default=datetime.now(timezone.utc), nullable=False)
    updated_at = Column(DateTime(timezone=True), default=datetime.now(timezone.utc), onupdate=datetime.now(timezone.utc), nullable=False)
    actor_id = Column(UUID(as_uuid=True), nullable=True)
    current_step = Column(String(128), nullable=True)
    progress = Column(Integer, nullable=True)
    error_code = Column(String(64), nullable=True)
    error_message = Column(Text, nullable=True)


class KnowledgeGraphEdgeModel(Base):
    __tablename__ = "knowledge_graph_edges"
    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    tenant_id = Column(UUID(as_uuid=True), index=True, nullable=False)
    source_version_id = Column(UUID(as_uuid=True), nullable=True)
    subject_type = Column(String(64), nullable=False)
    subject_id = Column(UUID(as_uuid=True), index=True, nullable=False)
    predicate = Column(String(128), nullable=False, index=True)
    object_type = Column(String(64), nullable=False)
    object_id = Column(UUID(as_uuid=True), index=True, nullable=False)
    status = Column(Enum(EdgeStatus), default=EdgeStatus.active, nullable=False, index=True)
    evidence_refs = Column(Text, nullable=True)
    relation_group = Column(String(255), nullable=True)
    created_at = Column(DateTime(timezone=True), default=datetime.now(timezone.utc), nullable=False)
    created_by = Column(UUID(as_uuid=True), nullable=True)
