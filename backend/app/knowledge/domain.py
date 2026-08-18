from enum import Enum
from pydantic import BaseModel, Field


class KnowledgeEntityType(str, Enum):
    SYMPTOM = "SYMPTOM"
    PATTERN = "PATTERN"
    SYNDROME = "SYNDROME"
    ZANG_FU = "ZANG_FU"
    MERIDIAN = "MERIDIAN"
    ACUPOINT = "ACUPOINT"
    FORMULA = "FORMULA"
    HERB = "HERB"
    TECHNIQUE = "TECHNIQUE"
    PROTOCOL = "PROTOCOL"
    THEORY = "THEORY"
    OBSERVATION = "OBSERVATION"
    ANATOMY = "ANATOMY"
    DISEASE = "DISEASE"
    CLINICAL_CASE = "CLINICAL_CASE"
    DOCUMENT = "DOCUMENT"
    UNKNOWN = "UNKNOWN"


class KnowledgeStatus(str, Enum):
    DRAFT = "DRAFT"
    REVIEW = "REVIEW"
    PUBLISHED = "PUBLISHED"
    DEPRECATED = "DEPRECATED"


class KnowledgeVersion(BaseModel):
    """Version metadata — mirrors Android KnowledgeVersion."""
    version: str = "1.0.0"
    created_at: int = 0
    updated_at: int = 0
    reviewed_at: int | None = None
    status: KnowledgeStatus = KnowledgeStatus.DRAFT


class KnowledgeProvenance(BaseModel):
    """Provenance chain — mirrors Android KnowledgeProvenance."""
    original_source: str
    original_id: str
    original_type: str
    source_reference: str | None = None
    migration_version: str
    imported_at: int = 0


class KnowledgeEntity(BaseModel):
    """Canonical knowledge entity — mirrors Android KnowledgeEntity."""
    id: str
    type: KnowledgeEntityType
    canonical_name: str
    aliases: list[str] = Field(default_factory=list)
    summary: str = ""
    content: str = ""
    metadata: dict[str, str] = Field(default_factory=dict)
    source_ids: list[str] = Field(default_factory=list)
    citation_ids: list[str] = Field(default_factory=list)
    evidence_ids: list[str] = Field(default_factory=list)
    version: KnowledgeVersion = Field(default_factory=KnowledgeVersion)
    provenance: list[KnowledgeProvenance] = Field(default_factory=list)
    created_at: int = 0
    updated_at: int = 0
