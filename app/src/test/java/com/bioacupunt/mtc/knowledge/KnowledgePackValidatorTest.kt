package com.bioacupunt.mtc.knowledge

import com.bioacupunt.mtc.knowledge.domain.EditorialStatus
import com.bioacupunt.mtc.knowledge.domain.KnowledgeEntity
import com.bioacupunt.mtc.knowledge.domain.KnowledgeEvidence
import com.bioacupunt.mtc.knowledge.domain.KnowledgePack
import com.bioacupunt.mtc.knowledge.domain.KnowledgePackManifest
import com.bioacupunt.mtc.knowledge.domain.KnowledgePackValidator
import com.bioacupunt.mtc.knowledge.domain.KnowledgeRelation
import com.bioacupunt.mtc.knowledge.domain.KnowledgeRelationType
import com.bioacupunt.mtc.knowledge.domain.KnowledgeSource
import com.bioacupunt.mtc.knowledge.domain.KnowledgeCitation
import com.bioacupunt.mtc.knowledge.domain.KnowledgeVersion
import com.bioacupunt.mtc.knowledge.domain.KnowledgeEntityType
import com.bioacupunt.mtc.knowledge.domain.PackValidationErrorType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class KnowledgePackValidatorTest {

    private lateinit var validator: KnowledgePackValidator

    @Before
    fun setUp() {
        validator = KnowledgePackValidator()
    }

    @Test
    fun validPack_passesValidation() {
        val pack = makePack(
            entities = listOf(makeEntity("e1"), makeEntity("e2")),
            relations = listOf(makeRelation("e1", "e2")),
            sources = listOf(makeSource("s1")),
            citations = listOf(makeCitation("c1", "s1")),
            evidence = listOf(makeEvidence("ev1", listOf("c1"))),
        )
        val result = validator.validate(pack)
        assertTrue("Pack should be valid, errors: ${result.errors}", result.isValid)
    }

    @Test
    fun emptyPackId_failsValidation() {
        val pack = makePack(manifest = makeManifest(packId = ""))
        val result = validator.validate(pack)
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.code == PackValidationErrorType.MISSING_REQUIRED_FIELD })
    }

    @Test
    fun emptyVersion_failsValidation() {
        val pack = makePack(manifest = makeManifest(version = ""))
        val result = validator.validate(pack)
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.code == PackValidationErrorType.MISSING_REQUIRED_FIELD })
    }

    @Test
    fun duplicateEntityIds_failsValidation() {
        val pack = makePack(
            entities = listOf(makeEntity("e1"), makeEntity("e1")),
        )
        val result = validator.validate(pack)
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.code == PackValidationErrorType.DUPLICATE_ENTITY })
    }

    @Test
    fun brokenCitationReference_failsValidation() {
        val pack = makePack(
            sources = listOf(makeSource("s1")),
            citations = listOf(makeCitation("c1", "s999")), // references non-existent source
        )
        val result = validator.validate(pack)
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.code == PackValidationErrorType.BROKEN_CITATION })
    }

    @Test
    fun brokenEvidenceReference_failsValidation() {
        val pack = makePack(
            citations = listOf(makeCitation("c1", "s1")),
            evidence = listOf(makeEvidence("ev1", listOf("c999"))), // references non-existent citation
        )
        val result = validator.validate(pack)
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.code == PackValidationErrorType.BROKEN_EVIDENCE })
    }

    @Test
    fun relationMissingSourceEntity_warns() {
        val pack = makePack(
            entities = listOf(makeEntity("e2")),
            relations = listOf(makeRelation("e999", "e2")), // e999 not in entities
        )
        val result = validator.validate(pack)
        assertTrue(result.warnings.any { it.contains("e999") })
    }

    @Test
    fun entityBlankName_warns() {
        val pack = makePack(
            entities = listOf(KnowledgeEntity(
                id = "e1",
                type = KnowledgeEntityType.SYMPTOM,
                canonicalName = "",
                version = KnowledgeVersion(version = "1.0", createdAt = 0, updatedAt = 0),
            )),
        )
        val result = validator.validate(pack)
        assertTrue(result.warnings.any { it.contains("blank canonicalName") })
    }

    @Test
    fun appIncompatible_failsValidation() {
        val pack = makePack(
            manifest = makeManifest(minimumAppVersion = "2.0.0"),
        )
        val result = validator.validate(pack, currentAppVersion = "1.0.0")
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.code == PackValidationErrorType.APP_INCOMPATIBLE })
    }

    @Test
    fun schemaIncompatible_failsValidation() {
        val pack = makePack(
            manifest = makeManifest(minimumSchemaVersion = "3.0.0"),
        )
        val result = validator.validate(pack, currentSchemaVersion = "2.0.0")
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.code == PackValidationErrorType.SCHEMA_INCOMPATIBLE })
    }

    @Test
    fun appCompatible_passesValidation() {
        val pack = makePack(
            manifest = makeManifest(minimumAppVersion = "1.0.0"),
        )
        val result = validator.validate(pack, currentAppVersion = "2.0.0")
        assertTrue(result.isValid)
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun makePack(
        manifest: KnowledgePackManifest = makeManifest(),
        entities: List<KnowledgeEntity> = emptyList(),
        relations: List<KnowledgeRelation> = emptyList(),
        evidence: List<KnowledgeEvidence> = emptyList(),
        sources: List<KnowledgeSource> = emptyList(),
        citations: List<KnowledgeCitation> = emptyList(),
    ) = KnowledgePack(
        manifest = manifest,
        entities = entities,
        relations = relations,
        evidence = evidence,
        sources = sources,
        citations = citations,
    )

    private fun makeManifest(
        packId: String = "test-pack",
        version: String = "1.0.0",
        minimumAppVersion: String? = null,
        minimumSchemaVersion: String? = null,
    ) = KnowledgePackManifest(
        packId = packId,
        version = version,
        schemaVersion = "1.0.0",
        minimumAppVersion = minimumAppVersion,
        minimumSchemaVersion = minimumSchemaVersion,
    )

    private fun makeEntity(id: String) = KnowledgeEntity(
        id = id,
        type = KnowledgeEntityType.SYMPTOM,
        canonicalName = "Entity $id",
        version = KnowledgeVersion(version = "1.0", createdAt = 0, updatedAt = 0),
    )

    private fun makeRelation(sourceId: String, targetId: String) = KnowledgeRelation(
        sourceEntityId = sourceId,
        relationType = KnowledgeRelationType.ASSOCIATED_WITH,
        targetEntityId = targetId,
        createdAt = 0,
        updatedAt = 0,
    )

    private fun makeSource(id: String) = KnowledgeSource(
        id = id,
        name = "Source $id",
    )

    private fun makeCitation(id: String, sourceId: String) = KnowledgeCitation(
        id = id,
        sourceId = sourceId,
    )

    private fun makeEvidence(id: String, citationIds: List<String>) = KnowledgeEvidence(
        id = id,
        claim = "Evidence $id",
        citationIds = citationIds,
    )
}
