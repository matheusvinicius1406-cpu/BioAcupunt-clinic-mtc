package com.bioacupunt.mtc.knowledge.repository

import com.bioacupunt.mtc.knowledge.data.KnowledgeCoreDao
import com.bioacupunt.mtc.knowledge.data.KnowledgeCoreEntityEntity
import com.bioacupunt.mtc.knowledge.domain.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import com.bioacupunt.mtc.knowledge.data.KnowledgeCoreCitationEntity
import com.bioacupunt.mtc.knowledge.data.KnowledgeCoreEvidenceEntity
import com.bioacupunt.mtc.knowledge.data.KnowledgeCoreProvenanceEntity
import com.bioacupunt.mtc.knowledge.data.KnowledgeCoreSourceEntity

interface KnowledgeRepository {
    suspend fun getById(id: String): KnowledgeEntity?
    suspend fun search(query: String, limit: Int = 50): List<KnowledgeEntity>
    suspend fun getRelations(entityId: String): List<KnowledgeRelation>
    fun observeAll(): Flow<List<KnowledgeEntity>>
}

class RoomKnowledgeRepository(private val dao: KnowledgeCoreDao) : KnowledgeRepository {
    override suspend fun getById(id: String) = dao.getById(id)?.toDomain()
    override suspend fun search(query: String, limit: Int) = dao.search(query, limit).map { it.toDomain() }
    override suspend fun getRelations(entityId: String) = dao.getRelations(entityId).map { row ->
        KnowledgeRelation(
            sourceEntityId = row.source_entity_id,
            relationType = KnowledgeRelationType.valueOf(row.relation_type),
            targetEntityId = row.target_entity_id,
            evidenceIds = row.evidence_ids_json.toList(),
            confidence = row.confidence,
            provenance = row.provenance_json.toProvenanceList(),
            createdAt = row.created_at,
            updatedAt = row.updated_at,
        )
    }
    override fun observeAll(): Flow<List<KnowledgeEntity>> = dao.observeAll().map { it.map(KnowledgeCoreEntityEntity::toDomain) }
}

class KnowledgeCoreImporter(private val dao: KnowledgeCoreDao) {
    suspend fun import(imports: List<KnowledgeImport>): KnowledgeMergeResult {
        val result = KnowledgeCanonicalizer.merge(imports)
        dao.insertEntities(result.entities.map { it.toEntity() })
        dao.insertSources(imports.flatMap { it.sources }.distinctBy { it.id }.map { s ->
            KnowledgeCoreSourceEntity(s.id, s.name, s.locator, s.license, s.metadata.toJson())
        })
        dao.insertCitations(imports.flatMap { it.citations }.distinctBy { it.id }.map { c ->
            KnowledgeCoreCitationEntity(c.id, c.sourceId, c.locator, c.excerpt)
        })
        dao.insertEvidence(imports.flatMap { it.evidence }.distinctBy { it.id }.map { e ->
            KnowledgeCoreEvidenceEntity(e.id, e.claim, e.citationIds.toJson(), e.level, e.confidence)
        })
        dao.insertProvenance(result.entities.flatMap { entity ->
            entity.provenance.map { p ->
                KnowledgeCoreProvenanceEntity(entity.id, p.originalSource, p.originalId, p.originalType, p.sourceReference, p.migrationVersion, p.importedAt)
            }
        })
        return result
    }
}

// ── JSON serialization helpers ───────────────────────────────────────

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

// ── Entity ↔ Domain mappers ──────────────────────────────────────────

private fun KnowledgeEntity.toEntity() = KnowledgeCoreEntityEntity(
    id = id,
    type = type.wireName,
    canonical_name = canonicalName,
    aliases_json = aliases.toJson(),
    summary = summary,
    content = content,
    metadata_json = metadata.toJson(),
    source_ids_json = sourceIds.toJson(),
    citation_ids_json = citationIds.toJson(),
    evidence_ids_json = evidenceIds.toJson(),
    version = version.version,
    status = version.status.name,
    created_at = createdAt,
    updated_at = updatedAt,
    reviewed_at = version.reviewedAt,
)

private fun KnowledgeCoreEntityEntity.toDomain(): KnowledgeEntity = KnowledgeEntity(
    id = id,
    type = KnowledgeEntityType.from(type),
    canonicalName = canonical_name,
    aliases = aliases_json.toList(),
    summary = summary,
    content = content,
    metadata = metadata_json.toMap(),
    sourceIds = source_ids_json.toList(),
    citationIds = citation_ids_json.toList(),
    evidenceIds = evidence_ids_json.toList(),
    version = KnowledgeVersion(version, created_at, updated_at, reviewed_at, runCatching { KnowledgeStatus.valueOf(status) }.getOrDefault(KnowledgeStatus.DRAFT)),
    createdAt = created_at,
    updatedAt = updated_at,
)
