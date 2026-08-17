package com.bioacupunt.mtc.knowledge.repository

import com.bioacupunt.mtc.knowledge.data.KnowledgeCoreDao
import com.bioacupunt.mtc.knowledge.data.KnowledgeCoreEntityEntity
import com.bioacupunt.mtc.knowledge.domain.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
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
        KnowledgeRelation(row.source_entity_id, KnowledgeRelationType.valueOf(row.relation_type), row.target_entity_id, row.evidence_ids_json.toList(), row.confidence, createdAt = row.created_at, updatedAt = row.updated_at)
    }
    override fun observeAll(): Flow<List<KnowledgeEntity>> = dao.observeAll().map { it.map(KnowledgeCoreEntityEntity::toDomain) }
}

class KnowledgeCoreImporter(private val dao: KnowledgeCoreDao) {
    suspend fun import(imports: List<KnowledgeImport>): KnowledgeMergeResult {
        val result = KnowledgeCanonicalizer.merge(imports)
        dao.insertEntities(result.entities.map { it.toEntity() })
        dao.insertSources(imports.flatMap { it.sources }.distinctBy { it.id }.map { KnowledgeCoreSourceEntity(it.id, it.name, it.locator, it.license, "{}") })
        dao.insertCitations(imports.flatMap { it.citations }.distinctBy { it.id }.map { KnowledgeCoreCitationEntity(it.id, it.sourceId, it.locator, it.excerpt) })
        dao.insertEvidence(imports.flatMap { it.evidence }.distinctBy { it.id }.map { KnowledgeCoreEvidenceEntity(it.id, it.claim, it.citationIds.toJson()) })
        dao.insertProvenance(result.entities.flatMap { entity -> entity.provenance.map { p -> KnowledgeCoreProvenanceEntity(entity.id, p.originalSource, p.originalId, p.originalType, p.sourceReference, p.migrationVersion, p.importedAt) } })
        return result
    }
}

private fun List<String>.toJson(): String = JSONArray().also { array -> forEach(array::put) }.toString()

private fun KnowledgeEntity.toEntity() = KnowledgeCoreEntityEntity(
    id = id, type = type.wireName, canonical_name = canonicalName, aliases_json = aliases.toJson(), summary = summary, content = content,
    metadata_json = "{}", source_ids_json = sourceIds.toJson(), citation_ids_json = citationIds.toJson(), evidence_ids_json = evidenceIds.toJson(),
    version = version.version, status = version.status.name, created_at = createdAt, updated_at = updatedAt, reviewed_at = version.reviewedAt,
)

private fun String.toList(): List<String> = runCatching {
    val array = JSONArray(this)
    List(array.length()) { index -> array.optString(index) }.filter { it.isNotBlank() }
}.getOrDefault(emptyList())

private fun KnowledgeCoreEntityEntity.toDomain(): KnowledgeEntity = KnowledgeEntity(
    id = id, type = KnowledgeEntityType.from(type), canonicalName = canonical_name, aliases = aliases_json.toList(), summary = summary, content = content,
    sourceIds = source_ids_json.toList(), citationIds = citation_ids_json.toList(), evidenceIds = evidence_ids_json.toList(),
    version = KnowledgeVersion(version, created_at, updated_at, reviewed_at, runCatching { KnowledgeStatus.valueOf(status) }.getOrDefault(KnowledgeStatus.DRAFT)),
    createdAt = created_at, updatedAt = updated_at,
)
