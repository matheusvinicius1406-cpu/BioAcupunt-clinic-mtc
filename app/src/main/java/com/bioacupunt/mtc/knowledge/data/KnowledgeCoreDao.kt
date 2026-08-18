package com.bioacupunt.mtc.knowledge.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface KnowledgeCoreDao {
    // ── Read ─────────────────────────────────────────────────────────
    @Query("SELECT * FROM knowledge_core_entities WHERE id = :id") suspend fun getById(id: String): KnowledgeCoreEntityEntity?
    @Query("SELECT * FROM knowledge_core_entities WHERE canonical_name LIKE '%' || :query || '%' OR summary LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' ORDER BY canonical_name LIMIT :limit") suspend fun search(query: String, limit: Int): List<KnowledgeCoreEntityEntity>
    @Query("SELECT * FROM knowledge_core_entities ORDER BY canonical_name") fun observeAll(): Flow<List<KnowledgeCoreEntityEntity>>
    @Query("SELECT * FROM knowledge_core_entities WHERE type = :type ORDER BY canonical_name") suspend fun getByType(type: String): List<KnowledgeCoreEntityEntity>
    @Query("SELECT * FROM knowledge_core_entities WHERE status = :status ORDER BY canonical_name") suspend fun getByStatus(status: String): List<KnowledgeCoreEntityEntity>
    @Query("SELECT * FROM knowledge_core_entities WHERE id IN (:ids)") suspend fun getByIds(ids: List<String>): List<KnowledgeCoreEntityEntity>

    // ── Count ────────────────────────────────────────────────────────
    @Query("SELECT COUNT(*) FROM knowledge_core_entities") suspend fun countAll(): Int
    @Query("SELECT COUNT(*) FROM knowledge_core_entities WHERE type = :type") suspend fun countByType(type: String): Int

    // ── Relations ─────────────────────────────────────────────────────
    @Query("SELECT * FROM knowledge_core_relations WHERE source_entity_id = :entityId OR target_entity_id = :entityId") suspend fun getRelations(entityId: String): List<KnowledgeCoreRelationEntity>
    @Query("SELECT * FROM knowledge_core_relations WHERE source_entity_id = :entityId") suspend fun getEdgesFrom(entityId: String): List<KnowledgeCoreRelationEntity>
    @Query("SELECT * FROM knowledge_core_relations WHERE target_entity_id = :entityId") suspend fun getEdgesTo(entityId: String): List<KnowledgeCoreRelationEntity>
    @Query("SELECT * FROM knowledge_core_relations WHERE source_entity_id = :sourceId AND target_entity_id = :targetId") suspend fun getEdgesBetween(sourceId: String, targetId: String): List<KnowledgeCoreRelationEntity>

    // ── Evidence Chain ──────────────────────────────────────────────
    @Query("SELECT * FROM knowledge_core_evidence WHERE id = :id") suspend fun getEvidenceById(id: String): KnowledgeCoreEvidenceEntity?
    @Query("SELECT * FROM knowledge_core_evidence WHERE id IN (:ids)") suspend fun getEvidenceByIds(ids: List<String>): List<KnowledgeCoreEvidenceEntity>
    @Query("SELECT * FROM knowledge_core_citations WHERE id = :id") suspend fun getCitationById(id: String): KnowledgeCoreCitationEntity?
    @Query("SELECT * FROM knowledge_core_citations WHERE id IN (:ids)") suspend fun getCitationsByIds(ids: List<String>): List<KnowledgeCoreCitationEntity>
    @Query("SELECT * FROM knowledge_core_citations WHERE source_id = :sourceId") suspend fun getCitationsBySource(sourceId: String): List<KnowledgeCoreCitationEntity>
    @Query("SELECT * FROM knowledge_core_sources WHERE id = :id") suspend fun getSourceById(id: String): KnowledgeCoreSourceEntity?
    @Query("SELECT * FROM knowledge_core_sources WHERE id IN (:ids)") suspend fun getSourcesByIds(ids: List<String>): List<KnowledgeCoreSourceEntity>
    @Query("SELECT * FROM knowledge_core_provenance WHERE entity_id = :entityId") suspend fun getProvenanceByEntity(entityId: String): List<KnowledgeCoreProvenanceEntity>

    // ── Write ─────────────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertEntities(items: List<KnowledgeCoreEntityEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertRelations(items: List<KnowledgeCoreRelationEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertSources(items: List<KnowledgeCoreSourceEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertCitations(items: List<KnowledgeCoreCitationEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertEvidence(items: List<KnowledgeCoreEvidenceEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertProvenance(items: List<KnowledgeCoreProvenanceEntity>)

    // ── Delete ────────────────────────────────────────────────────────
    @Query("DELETE FROM knowledge_core_entities WHERE id = :id") suspend fun deleteById(id: String)
    @Query("DELETE FROM knowledge_core_relations WHERE source_entity_id = :entityId OR target_entity_id = :entityId") suspend fun deleteRelationsFor(entityId: String)
}
