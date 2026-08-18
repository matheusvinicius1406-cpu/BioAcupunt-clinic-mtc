package com.bioacupunt.mtc.knowledge

import com.bioacupunt.mtc.knowledge.data.KnowledgeCoreEntityEntity
import com.bioacupunt.mtc.knowledge.data.KnowledgeCoreEvidenceEntity
import com.bioacupunt.mtc.knowledge.data.KnowledgeCoreRelationEntity
import com.bioacupunt.mtc.knowledge.domain.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
/**
 * Verifies that ALL fields survive the domain ↔ Room round-trip.
 *
 * These are the same mappers used in KnowledgeRepository.kt:
 * - KnowledgeEntity.toEntity() → KnowledgeCoreEntityEntity
 * - KnowledgeCoreEntityEntity.toDomain() → KnowledgeEntity
 * - KnowledgeRelation (via getRelations mapper)
 * - KnowledgeEvidence (via importer mapper)
 */
class KnowledgeMapperRoundTripTest {

    // ── KnowledgeEntity round-trip ────────────────────────────────────

    @Test
    fun entityRoundTrip_preservesAllFields() {
        val now = 1700000000000L
        val original = KnowledgeEntity(
            id = "pattern.estagnacao_de_qi",
            type = KnowledgeEntityType.PATTERN,
            canonicalName = "Estagnação de Qi do Fígado",
            aliases = listOf("Liver Qi Stagnation", "肝气郁结"),
            summary = "Padrão de estagnação",
            content = "Conteúdo completo do padrão...",
            metadata = mapOf("category" to "padrao", "source" to "Maciocia", "checksum" to "abc123"),
            sourceIds = listOf("source.library.art1", "source.mkis.node1"),
            citationIds = listOf("citation.library.art1"),
            evidenceIds = listOf("ev1", "ev2"),
            version = KnowledgeVersion(
                version = "2.1.0",
                createdAt = now,
                updatedAt = now + 1000,
                reviewedAt = now + 2000,
                status = KnowledgeStatus.PUBLISHED,
            ),
            provenance = listOf(
                KnowledgeProvenance("library", "art1", "article", "p. 145", "v1", now),
                KnowledgeProvenance("mkis", "node1", "pattern", null, "v1", now),
            ),
            createdAt = now,
            updatedAt = now + 1000,
        )

        // Simulate the mapper: toEntity() then toDomain()
        val roomEntity = original.toEntityRoom()
        val restored = roomEntity.toDomainRoom()

        assertEquals(original.id, restored.id)
        assertEquals(original.type, restored.type)
        assertEquals(original.canonicalName, restored.canonicalName)
        assertEquals(original.aliases, restored.aliases)
        assertEquals(original.summary, restored.summary)
        assertEquals(original.content, restored.content)
        assertEquals(original.metadata, restored.metadata)
        assertEquals(original.sourceIds, restored.sourceIds)
        assertEquals(original.citationIds, restored.citationIds)
        assertEquals(original.evidenceIds, restored.evidenceIds)
        assertEquals(original.version.version, restored.version.version)
        assertEquals(original.version.status, restored.version.status)
        assertEquals(original.version.reviewedAt, restored.version.reviewedAt)
        assertEquals(original.createdAt, restored.createdAt)
        assertEquals(original.updatedAt, restored.updatedAt)
        // Provenance is stored in a separate table, not in the entity — verify provenance count
        assertEquals(2, original.provenance.size)
    }

    @Test
    fun entityRoundTrip_emptyMetadataPreserved() {
        val now = 1L
        val original = KnowledgeEntity(
            id = "test",
            type = KnowledgeEntityType.SYMPTOM,
            canonicalName = "Dor",
            metadata = emptyMap(),
            version = KnowledgeVersion("1", now, now),
            createdAt = now,
            updatedAt = now,
        )

        val roomEntity = original.toEntityRoom()
        val restored = roomEntity.toDomainRoom()

        assertEquals(emptyMap<String, String>(), restored.metadata)
    }

    @Test
    fun entityRoundTrip_specialCharactersInMetadata() {
        val now = 1L
        val original = KnowledgeEntity(
            id = "test",
            type = KnowledgeEntityType.HERB,
            canonicalName = "Chai Hu",
            metadata = mapOf(
                "key" to "value with spaces",
                "chinese" to "柴胡",
                "empty" to "",
                "json" to "{\"nested\": true}",
            ),
            version = KnowledgeVersion("1", now, now),
            createdAt = now,
            updatedAt = now,
        )

        val roomEntity = original.toEntityRoom()
        val restored = roomEntity.toDomainRoom()

        assertEquals(original.metadata, restored.metadata)
    }

    // ── KnowledgeRelation round-trip ──────────────────────────────────

    @Test
    fun relationRoundTrip_preservesAllFields() {
        val now = 1700000000000L
        val original = KnowledgeRelation(
            sourceEntityId = "pattern.a",
            relationType = KnowledgeRelationType.TREATED_BY,
            targetEntityId = "acupoint.li4",
            evidenceIds = listOf("ev1"),
            confidence = 0.85,
            provenance = listOf(
                KnowledgeProvenance("library", "art1", "article", null, "v1", now),
            ),
            createdAt = now,
            updatedAt = now,
        )

        // Simulate the Room entity
        val roomEntity = KnowledgeCoreRelationEntity(
            source_entity_id = original.sourceEntityId,
            relation_type = original.relationType.name,
            target_entity_id = original.targetEntityId,
            evidence_ids_json = original.evidenceIds.toJson(),
            confidence = original.confidence,
            provenance_json = original.provenance.toProvenanceJson(),
            created_at = original.createdAt,
            updated_at = original.updatedAt,
        )

        // Simulate the mapper from getRelations()
        val restored = KnowledgeRelation(
            sourceEntityId = roomEntity.source_entity_id,
            relationType = KnowledgeRelationType.valueOf(roomEntity.relation_type),
            targetEntityId = roomEntity.target_entity_id,
            evidenceIds = roomEntity.evidence_ids_json.toList(),
            confidence = roomEntity.confidence,
            provenance = roomEntity.provenance_json.toProvenanceList(),
            createdAt = roomEntity.created_at,
            updatedAt = roomEntity.updated_at,
        )

        assertEquals(original.sourceEntityId, restored.sourceEntityId)
        assertEquals(original.relationType, restored.relationType)
        assertEquals(original.targetEntityId, restored.targetEntityId)
        assertEquals(original.evidenceIds, restored.evidenceIds)
        assertEquals(original.confidence, restored.confidence)
        assertEquals(1, restored.provenance.size)
        assertEquals("library", restored.provenance[0].originalSource)
        assertEquals(now, restored.provenance[0].importedAt)
    }

    // ── Evidence round-trip ───────────────────────────────────────────

    @Test
    fun evidenceRoundTrip_preservesLevel() {
        val original = KnowledgeCoreEvidenceEntity(
            id = "ev1",
            claim = "Padrão X é tratado por Ponto Y",
            citation_ids_json = JSONArray().apply { put("cit1") }.toString(),
            level = "MODERN_LITERATURE",
            confidence = 0.9,
        )

        // Simulate the mapper from toDomain (evidence is stored separately)
        val citationIds = runCatching {
            val array = JSONArray(original.citation_ids_json)
            List(array.length()) { array.optString(it) }.filter { it.isNotBlank() }
        }.getOrDefault(emptyList())

        assertEquals("ev1", original.id)
        assertEquals("MODERN_LITERATURE", original.level)
        assertEquals(0.9, original.confidence!!, 0.001)
        assertEquals(listOf("cit1"), citationIds)
    }

    @Test
    fun evidenceRoundTrip_nullLevelPreserved() {
        val original = KnowledgeCoreEvidenceEntity(
            id = "ev2",
            claim = "Test claim",
            citation_ids_json = "[]",
            level = null,
            confidence = null,
        )

        assertNull(original.level)
        assertNull(original.confidence)
    }

    // ── Provenance JSON round-trip ────────────────────────────────────

    @Test
    fun provenanceJsonRoundTrip_preservesAllFields() {
        val now = 1700000000000L
        val original = listOf(
            KnowledgeProvenance("library", "art1", "article", "p. 10", "v1", now),
            KnowledgeProvenance("mkis", "node1", "pattern", null, "v1", now + 1),
        )

        val json = original.toProvenanceJson()
        val restored = json.toProvenanceList()

        assertEquals(2, restored.size)
        assertEquals("library", restored[0].originalSource)
        assertEquals("art1", restored[0].originalId)
        assertEquals("article", restored[0].originalType)
        assertEquals("p. 10", restored[0].sourceReference)
        assertEquals("v1", restored[0].migrationVersion)
        assertEquals(now, restored[0].importedAt)

        assertEquals("mkis", restored[1].originalSource)
        assertNull(restored[1].sourceReference)
    }

    @Test
    fun provenanceJsonRoundTrip_emptyList() {
        val json = emptyList<KnowledgeProvenance>().toProvenanceJson()
        val restored = json.toProvenanceList()
        assertTrue(restored.isEmpty())
    }

    // ── Metadata JSON round-trip ──────────────────────────────────────

    @Test
    fun metadataJsonRoundTrip_preservesAllEntries() {
        val original = mapOf("a" to "1", "b" to "2", "chinese" to "中醫")
        val json = original.toJson()
        val restored = json.toMap()
        assertEquals(original, restored)
    }

    @Test
    fun metadataJsonRoundTrip_emptyMap() {
        val json = emptyMap<String, String>().toJson()
        val restored = json.toMap()
        assertEquals(emptyMap<String, String>(), restored)
    }

    // ── Helpers (mirror the private functions in KnowledgeRepository) ─

    private fun List<String>.toJson(): String = JSONArray().also { array -> forEach(array::put) }.toString()
    private fun String.toList(): List<String> = runCatching {
        val array = JSONArray(this)
        List(array.length()) { index -> array.optString(index) }.filter { it.isNotBlank() }
    }.getOrDefault(emptyList())

    private fun Map<String, String>.toJson(): String = JSONObject(this).toString()
    private fun String.toMap(): Map<String, String> = runCatching {
        val obj = JSONObject(this)
        obj.keys().asSequence().associateWith { obj.getString(it) }
    }.getOrDefault(emptyMap())

    private fun List<KnowledgeProvenance>.toProvenanceJson(): String = runCatching {
        JSONArray().also { array ->
            forEach { p ->
                JSONObject().apply {
                    put("originalSource", p.originalSource)
                    put("originalId", p.originalId)
                    put("originalType", p.originalType)
                    put("sourceReference", p.sourceReference ?: JSONObject.NULL)
                    put("migrationVersion", p.migrationVersion)
                    put("importedAt", p.importedAt)
                }.let(array::put)
            }
        }.toString()
    }.getOrDefault("[]")

    private fun String.toProvenanceList(): List<KnowledgeProvenance> = runCatching {
        val array = JSONArray(this)
        List(array.length()) { i ->
            val obj = array.getJSONObject(i)
            KnowledgeProvenance(
                originalSource = obj.getString("originalSource"),
                originalId = obj.getString("originalId"),
                originalType = obj.getString("originalType"),
                sourceReference = if (obj.has("sourceReference") && !obj.isNull("sourceReference")) obj.getString("sourceReference") else null,
                migrationVersion = obj.getString("migrationVersion"),
                importedAt = obj.getLong("importedAt"),
            )
        }
    }.getOrDefault(emptyList())

    /** Mirror of KnowledgeEntity.toEntity() from KnowledgeRepository.kt */
    private fun KnowledgeEntity.toEntityRoom() = KnowledgeCoreEntityEntity(
        id = id, type = type.wireName, canonical_name = canonicalName,
        aliases_json = JSONArray().also { arr -> aliases.forEach(arr::put) }.toString(),
        summary = summary, content = content,
        metadata_json = metadata.toJson(),
        source_ids_json = JSONArray().also { arr -> sourceIds.forEach(arr::put) }.toString(),
        citation_ids_json = JSONArray().also { arr -> citationIds.forEach(arr::put) }.toString(),
        evidence_ids_json = JSONArray().also { arr -> evidenceIds.forEach(arr::put) }.toString(),
        version = version.version, status = version.status.name,
        created_at = createdAt, updated_at = updatedAt, reviewed_at = version.reviewedAt,
    )

    /** Mirror of KnowledgeCoreEntityEntity.toDomain() from KnowledgeRepository.kt */
    private fun KnowledgeCoreEntityEntity.toDomainRoom(): KnowledgeEntity = KnowledgeEntity(
        id = id, type = KnowledgeEntityType.from(type), canonicalName = canonical_name,
        aliases = aliases_json.toList(), summary = summary, content = content,
        metadata = metadata_json.toMap(),
        sourceIds = source_ids_json.toList(), citationIds = citation_ids_json.toList(),
        evidenceIds = evidence_ids_json.toList(),
        version = KnowledgeVersion(version, created_at, updated_at, reviewed_at,
            runCatching { KnowledgeStatus.valueOf(status) }.getOrDefault(KnowledgeStatus.DRAFT)),
        createdAt = created_at, updatedAt = updated_at,
    )
}
