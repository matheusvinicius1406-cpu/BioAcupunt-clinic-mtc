package com.bioacupunt.data.local.database

import androidx.room.*
import com.bioacupunt.data.local.model.KnowledgeNodeEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para [KnowledgeNodeEntity] — operações CRUD + queries do MKIS.
 *
 * Inclui métodos específicos para:
 * - Deep delete LGPD (anonymizeForPurge)
 * - Transições de estado (updateStatus)
 * - Busca por tenant e enums
 * - Sincronização com sqlite-vec e FTS
 */
@Dao
interface KnowledgeNodeDao {

    // ======================== CRUD Básico ========================

    @Query("SELECT * FROM knowledge_nodes ORDER BY created_at DESC")
    fun getAllNodes(): Flow<List<KnowledgeNodeEntity>>

    @Query("SELECT * FROM knowledge_nodes WHERE id = :id")
    suspend fun getById(id: String): KnowledgeNodeEntity?

    @Query("SELECT * FROM knowledge_nodes WHERE id = :id")
    fun getByIdFlow(id: String): Flow<KnowledgeNodeEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(node: KnowledgeNodeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(nodes: List<KnowledgeNodeEntity>)

    @Update
    suspend fun update(node: KnowledgeNodeEntity)

    @Query("DELETE FROM knowledge_nodes WHERE id = :id")
    suspend fun deleteById(id: String)

    // ======================== Busca ========================

    @Query("""
        SELECT * FROM knowledge_nodes 
        WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%'
        ORDER BY created_at DESC
    """)
    fun searchNodes(query: String): Flow<List<KnowledgeNodeEntity>>

    @Query("""
        SELECT * FROM knowledge_nodes 
        WHERE status = :status AND tenant_id = :tenantId
        ORDER BY created_at DESC
    """)
    fun getByStatus(status: String, tenantId: String = "default"): Flow<List<KnowledgeNodeEntity>>

    @Query("""
        SELECT * FROM knowledge_nodes 
        WHERE knowledge_type = :type AND tenant_id = :tenantId
        ORDER BY created_at DESC
    """)
    fun getByType(type: String, tenantId: String = "default"): Flow<List<KnowledgeNodeEntity>>

    @Query("""
        SELECT * FROM knowledge_nodes 
        WHERE tenant_id = :tenantId AND deleted_at IS NULL
        ORDER BY created_at DESC
    """)
    fun getActiveByTenant(tenantId: String = "default"): Flow<List<KnowledgeNodeEntity>>

    /** Nós que são indexáveis (status = aprovado ou descontinuado). */
    @Query("""
        SELECT id, content, title, summary FROM knowledge_nodes
        WHERE status IN ('aprovado', 'descontinuado') AND deleted_at IS NULL
    """)
    suspend fun getSearchableNodes(): List<SearchableNodeProjection>

    // ======================== Transições de Estado ========================

    @Query("UPDATE knowledge_nodes SET status = :newStatus, updated_at = :now WHERE id = :id")
    suspend fun updateStatus(id: String, newStatus: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE knowledge_nodes SET status = :newStatus, approved_by = :approvedBy, approved_at = :now, updated_at = :now WHERE id = :id")
    suspend fun approve(id: String, newStatus: String, approvedBy: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE knowledge_nodes SET status = 'rejeitado', updated_at = :now WHERE id = :id")
    suspend fun reject(id: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE knowledge_nodes SET status = 'descontinuado', updated_at = :now WHERE id = :id")
    suspend fun deprecate(id: String, now: Long = System.currentTimeMillis())

    @Query("""
        UPDATE knowledge_nodes 
        SET status = 'substituido', superseded_by = :newNodeId, updated_at = :now 
        WHERE id = :id
    """)
    suspend fun supersede(id: String, newNodeId: String, now: Long = System.currentTimeMillis())

    // ======================== Soft Delete ========================

    @Query("UPDATE knowledge_nodes SET deleted_at = :now, updated_at = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM knowledge_nodes WHERE deleted_at IS NOT NULL AND deleted_at < :threshold")
    suspend fun countSoftDeletedBefore(threshold: Long): Int

    /** Purge físico de soft-deletes expirados (>90 dias). */
    @Query("DELETE FROM knowledge_nodes WHERE deleted_at IS NOT NULL AND deleted_at < :threshold")
    suspend fun purgeExpiredSoftDeletes(threshold: Long): Int

    // ======================== Deep Delete LGPD (Anonimização) ========================

    /**
     * Anonimiza campos de pessoa no deep delete LGPD.
     * Substitui UUIDs de pessoa pelo sentinela e limpa metadados.
     */
    @Query("""
        UPDATE knowledge_nodes SET
            created_by = :sentinel,
            reviewed_by = NULL,
            approved_by = NULL,
            metadata = '{"lgpd_anonymized": true}',
            updated_at = :now
        WHERE id = :id
    """)
    suspend fun anonymizeForPurge(
        id: String,
        sentinel: String = KnowledgeNodeEntity.DELETED_USER_SENTINEL,
        now: Long = System.currentTimeMillis(),
    )

    /**
     * Verifica se um nó foi anonimizado (para confirmação pós-purge).
     */
    @Query("""
        SELECT COUNT(*) FROM knowledge_nodes 
        WHERE id = :id AND created_by != :sentinel
    """)
    suspend fun countNotAnonymized(
        id: String,
        sentinel: String = KnowledgeNodeEntity.DELETED_USER_SENTINEL,
    ): Int

    // ======================== Estatísticas ========================

    @Query("SELECT COUNT(*) FROM knowledge_nodes")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM knowledge_nodes WHERE status = :status")
    suspend fun countByStatus(status: String): Int

    @Query("SELECT COUNT(*) FROM knowledge_nodes WHERE tenant_id = :tenantId AND status = 'aprovado'")
    suspend fun countApprovedByTenant(tenantId: String = "default"): Int

    // ======================== Projeções ========================

    /** Projeção leve para indexação. */
    data class SearchableNodeProjection(
        val id: String,
        val content: String,
        val title: String,
        val summary: String,
    )
}
