package com.bioacupunt.mtc.knowledge.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface KnowledgeCoreDao {
    @Query("SELECT * FROM knowledge_core_entities WHERE id = :id") suspend fun getById(id: String): KnowledgeCoreEntityEntity?
    @Query("SELECT * FROM knowledge_core_entities WHERE canonical_name LIKE '%' || :query || '%' OR summary LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' ORDER BY canonical_name LIMIT :limit") suspend fun search(query: String, limit: Int): List<KnowledgeCoreEntityEntity>
    @Query("SELECT * FROM knowledge_core_entities ORDER BY canonical_name") fun observeAll(): Flow<List<KnowledgeCoreEntityEntity>>
    @Query("SELECT * FROM knowledge_core_relations WHERE source_entity_id = :entityId OR target_entity_id = :entityId") suspend fun getRelations(entityId: String): List<KnowledgeCoreRelationEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertEntities(items: List<KnowledgeCoreEntityEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertRelations(items: List<KnowledgeCoreRelationEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertSources(items: List<KnowledgeCoreSourceEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertCitations(items: List<KnowledgeCoreCitationEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertEvidence(items: List<KnowledgeCoreEvidenceEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertProvenance(items: List<KnowledgeCoreProvenanceEntity>)
}
