"""Tests for Knowledge Core backend — domain models and service."""

import pytest
from app.knowledge.domain import (
    KnowledgeEntity,
    KnowledgeEntityType,
    KnowledgeProvenance,
    KnowledgeStatus,
    KnowledgeVersion,
)


class TestKnowledgeDomain:
    """Verify backend Pydantic models match Android domain contract."""

    def test_entity_has_all_fields(self):
        """KnowledgeEntity must have all fields matching Android KnowledgeEntity."""
        now = 1700000000000
        entity = KnowledgeEntity(
            id="pattern.test",
            type=KnowledgeEntityType.PATTERN,
            canonical_name="Test Pattern",
            aliases=["Alias1", "Alias2"],
            summary="Summary",
            content="Content",
            metadata={"key": "value"},
            source_ids=["s1"],
            citation_ids=["c1"],
            evidence_ids=["e1"],
            version=KnowledgeVersion(
                version="2.0.0",
                created_at=now,
                updated_at=now + 1000,
                reviewed_at=now + 2000,
                status=KnowledgeStatus.PUBLISHED,
            ),
            provenance=[
                KnowledgeProvenance(
                    original_source="library",
                    original_id="art1",
                    original_type="article",
                    source_reference="p. 10",
                    migration_version="v1",
                    imported_at=now,
                )
            ],
            created_at=now,
            updated_at=now + 1000,
        )

        assert entity.id == "pattern.test"
        assert entity.type == KnowledgeEntityType.PATTERN
        assert entity.canonical_name == "Test Pattern"
        assert entity.aliases == ["Alias1", "Alias2"]
        assert entity.summary == "Summary"
        assert entity.content == "Content"
        assert entity.metadata == {"key": "value"}
        assert entity.source_ids == ["s1"]
        assert entity.citation_ids == ["c1"]
        assert entity.evidence_ids == ["e1"]
        assert entity.version.version == "2.0.0"
        assert entity.version.created_at == now
        assert entity.version.reviewed_at == now + 2000
        assert entity.version.status == KnowledgeStatus.PUBLISHED
        assert len(entity.provenance) == 1
        assert entity.provenance[0].imported_at == now
        assert entity.created_at == now

    def test_entity_defaults(self):
        """Verify sensible defaults for all optional fields."""
        entity = KnowledgeEntity(
            id="test",
            type=KnowledgeEntityType.SYMPTOM,
            canonical_name="Test",
        )
        assert entity.aliases == []
        assert entity.summary == ""
        assert entity.content == ""
        assert entity.metadata == {}
        assert entity.source_ids == []
        assert entity.citation_ids == []
        assert entity.evidence_ids == []
        assert entity.version.version == "1.0.0"
        assert entity.provenance == []
        assert entity.created_at == 0

    def test_version_model(self):
        """KnowledgeVersion must have all fields."""
        v = KnowledgeVersion(
            version="1.0.0",
            created_at=1000,
            updated_at=2000,
            reviewed_at=3000,
            status=KnowledgeStatus.REVIEW,
        )
        assert v.version == "1.0.0"
        assert v.status == KnowledgeStatus.REVIEW

    def test_provenance_model(self):
        """KnowledgeProvenance must have imported_at."""
        p = KnowledgeProvenance(
            original_source="library",
            original_id="art1",
            original_type="article",
            source_reference="p. 10",
            migration_version="v1",
            imported_at=1000,
        )
        assert p.imported_at == 1000
        assert p.source_reference == "p. 10"

    def test_entity_serializes_to_json(self):
        """Entity must serialize/deserialize cleanly."""
        entity = KnowledgeEntity(
            id="test",
            type=KnowledgeEntityType.PATTERN,
            canonical_name="Test",
            metadata={"key": "value"},
        )
        json_str = entity.model_dump_json()
        restored = KnowledgeEntity.model_validate_json(json_str)
        assert restored.id == entity.id
        assert restored.metadata == entity.metadata

    def test_all_entity_types_replicated(self):
        """Backend must have same entity types as Android."""
        android_types = {
            "SYMPTOM", "PATTERN", "SYNDROME", "ZANG_FU", "MERIDIAN",
            "ACUPOINT", "FORMULA", "HERB", "TECHNIQUE", "PROTOCOL",
            "THEORY", "OBSERVATION", "ANATOMY", "DISEASE", "CLINICAL_CASE",
            "DOCUMENT", "UNKNOWN",
        }
        backend_types = {t.value for t in KnowledgeEntityType}
        assert android_types == backend_types, f"Mismatch: {android_types.symmetric_difference(backend_types)}"

    def test_all_statuses_replicated(self):
        """Backend must have same statuses as Android."""
        android_statuses = {"DRAFT", "REVIEW", "PUBLISHED", "DEPRECATED"}
        backend_statuses = {s.value for s in KnowledgeStatus}
        assert android_statuses == backend_statuses
