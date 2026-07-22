package com.bioacupunt.data.local.database

import androidx.room.*
import com.bioacupunt.data.local.model.AuditTrailEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para [AuditTrailEntity] — trilha de auditoria append-only.
 *
 * Regras:
 * - INSERT apenas (nunca UPDATE, exceto para anonimização LGPD)
 * - Anonimização via [anonymizeForPurge] substitui actor_id por sentinela
 * - Consultas por tenant, ação, recurso e período
 */
@Dao
interface AuditTrailDao {

    // ======================== Insert (append-only) ========================

    @Insert
    suspend fun insert(entry: AuditTrailEntity)

    @Insert
    suspend fun insertAll(entries: List<AuditTrailEntity>)

    // ======================== Queries ========================

    @Query("SELECT * FROM audit_trail WHERE id = :id")
    suspend fun getById(id: String): AuditTrailEntity?

    @Query("SELECT * FROM audit_trail WHERE tenant_id = :tenantId ORDER BY occurred_at DESC")
    fun getByTenant(tenantId: String = "default"): Flow<List<AuditTrailEntity>>

    @Query("SELECT * FROM audit_trail WHERE tenant_id = :tenantId ORDER BY occurred_at DESC LIMIT :limit")
    suspend fun getRecentByTenant(tenantId: String = "default", limit: Int = 50): List<AuditTrailEntity>

    @Query("SELECT * FROM audit_trail WHERE resource_type = :resourceType AND resource_id = :resourceId ORDER BY occurred_at DESC")
    fun getByResource(resourceType: String, resourceId: String): Flow<List<AuditTrailEntity>>

    @Query("SELECT * FROM audit_trail WHERE action = :action ORDER BY occurred_at DESC LIMIT :limit")
    suspend fun getByAction(action: String, limit: Int = 20): List<AuditTrailEntity>

    @Query("SELECT * FROM audit_trail WHERE actor_id = :actorId ORDER BY occurred_at DESC LIMIT :limit")
    suspend fun getByActor(actorId: String, limit: Int = 20): List<AuditTrailEntity>

    @Query("SELECT * FROM audit_trail WHERE occurred_at >= :since ORDER BY occurred_at DESC")
    suspend fun getSince(since: Long): List<AuditTrailEntity>

    @Query("SELECT * FROM audit_trail WHERE tenant_id = :tenantId AND occurred_at >= :since AND occurred_at < :until ORDER BY occurred_at DESC")
    suspend fun getByDateRange(tenantId: String, since: Long, until: Long): List<AuditTrailEntity>

    // ======================== Deep Delete LGPD (Anonimização) ========================

    /**
     * Anonimiza entradas de auditoria relacionadas a um recurso ou ator.
     * Substitui PII (actor_id, ip_address) por valores sentinela.
     */
    @Query("""
        UPDATE audit_trail SET
            actor_id = :sentinel,
            ip_address = NULL,
            metadata = json_set(metadata, '$.lgpd_anonymized', json('true'))
        WHERE (resource_id = :resourceId OR actor_id = :actorId)
          AND (json_extract(metadata, '$.lgpd_anonymized') IS NULL OR json_extract(metadata, '$.lgpd_anonymized') != 1)
    """)
    suspend fun anonymizeForPurge(
        resourceId: String,
        actorId: String? = null,
        sentinel: String = AuditTrailEntity.ANONYMIZED_ACTOR_ID,
    )

    /**
     * Conta entradas não anonimizadas para verificação pós-purge.
     */
    @Query("""
        SELECT COUNT(*) FROM audit_trail
        WHERE resource_id = :resourceId
          AND (json_extract(metadata, '$.lgpd_anonymized') IS NULL OR json_extract(metadata, '$.lgpd_anonymized') != 1)
    """)
    suspend fun countNotAnonymized(resourceId: String): Int

    // ======================== Estatísticas ========================

    @Query("SELECT COUNT(*) FROM audit_trail")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM audit_trail WHERE tenant_id = :tenantId")
    suspend fun countByTenant(tenantId: String = "default"): Int

    @Query("SELECT COUNT(*) FROM audit_trail WHERE action = :action")
    suspend fun countByAction(action: String): Int

    @Query("SELECT COUNT(*) FROM audit_trail WHERE occurred_at >= :since")
    suspend fun countSince(since: Long): Int

    // ======================== Limpeza (retenção 7 anos) ========================

    /**
     * Remove entradas de auditoria mais antigas que o período de retenção.
     * @param threshold Timestamp em ms: 7 anos atrás = System.currentTimeMillis() - 7 * 365 * 24 * 3600 * 1000L
     */
    @Query("DELETE FROM audit_trail WHERE occurred_at < :threshold")
    suspend fun deleteOlderThan(threshold: Long): Int
}
