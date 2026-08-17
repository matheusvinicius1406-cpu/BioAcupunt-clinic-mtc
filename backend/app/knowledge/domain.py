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


class KnowledgeProvenance(BaseModel):
    original_source: str
    original_id: str
    original_type: str
    source_reference: str | None = None
    migration_version: str


class KnowledgeEntity(BaseModel):
    id: str
    type: KnowledgeEntityType
    canonical_name: str
    aliases: list[str] = Field(default_factory=list)
    summary: str = ""
    content: str = ""
    source_ids: list[str] = Field(default_factory=list)
    citation_ids: list[str] = Field(default_factory=list)
    evidence_ids: list[str] = Field(default_factory=list)
    version: str = "1.0.0"
    status: KnowledgeStatus = KnowledgeStatus.DRAFT
    provenance: list[KnowledgeProvenance] = Field(default_factory=list)
