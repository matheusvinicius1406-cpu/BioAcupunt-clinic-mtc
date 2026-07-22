package com.bioacupunt.data.local.database

import androidx.room.*
import com.bioacupunt.data.local.model.IngestionJobEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para [IngestionJobEntity] — pipeline de processamento de documentos.
 *
 * Suporta:
 * - CRUD básico
 * - Transições de estado (updateStatus)
 * - Consulta por status, tenant, node_id
 * - Anonimização LGPD (anonymizeForPurge)
 * - Limpeza de jobs antigos
 */
@Dao
interface IngestionJobDao {

    // ======================== CRUD ========================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(job: IngestionJobEntity)

    @Update
    suspend fun update(job: IngestionJobEntity)

    @Query("SELECT * FROM ingestion_jobs WHERE id = :id")
    suspend fun getById(id: String): IngestionJobEntity?

    @Query("SELECT * FROM ingestion_jobs WHERE id = :id")
    fun getByIdFlow(id: String): Flow<IngestionJobEntity?>

    @Query("SELECT * FROM ingestion_jobs ORDER BY created_at DESC")
    fun getAll(): Flow<List<IngestionJobEntity>>

    @Query("SELECT * FROM ingestion_jobs WHERE tenant_id = :tenantId ORDER BY created_at DESC")
    fun getByTenant(tenantId: String = "default"): Flow<List<IngestionJobEntity>>

    // ======================== Status ========================

    @Query("SELECT * FROM ingestion_jobs WHERE status = :status ORDER BY created_at DESC")
    fun getByStatus(status: String): Flow<List<IngestionJobEntity>>

    /** Próximo job pendente na fila (mais antigo primeiro). */
    @Query("SELECT * FROM ingestion_jobs WHERE status = 'na_fila' ORDER BY created_at ASC LIMIT 1")
    suspend fun getNextPendingJob(): IngestionJobEntity?

    @Query("UPDATE ingestion_jobs SET status = :newStatus, updated_at = :now WHERE id = :id")
    suspend fun updateStatus(id: String, newStatus: String, now: Long = System.currentTimeMillis())

    @Query("""
        UPDATE ingestion_jobs SET
            status = :newStatus,
            node_id = :nodeId,
            completed_at = :now,
            updated_at = :now
        WHERE id = :id
    """)
    suspend fun complete(id: String, nodeId: String, newStatus: String = "concluido", now: Long = System.currentTimeMillis())

    @Query("""
        UPDATE ingestion_jobs SET
            status = :newStatus,
            error_code = :errorCode,
            error_message = :errorMessage,
            attempt_count = attempt_count + 1,
            updated_at = :now
        WHERE id = :id
    """)
    suspend fun fail(
        id: String,
        newStatus: String = "falhou",
        errorCode: String? = null,
        errorMessage: String? = null,
        now: Long = System.currentTimeMillis(),
    )

    @Query("""
        UPDATE ingestion_jobs SET
            status = 'em_quarentena',
            quarantine_reason = :reason,
            updated_at = :now
        WHERE id = :id
    """)
    suspend fun quarantine(id: String, reason: String, now: Long = System.currentTimeMillis())

    // ======================== Retry ========================

    @Query("""
        UPDATE ingestion_jobs SET
            status = 'na_fila',
            attempt_id = :newAttemptId,
            error_code = NULL,
            error_message = NULL,
            updated_at = :now
        WHERE id = :id
    """)
    suspend fun resetForRetry(id: String, newAttemptId: String, now: Long = System.currentTimeMillis())

    /** Jobs que podem ser retentados (estados de falha). */
    @Query("SELECT * FROM ingestion_jobs WHERE status IN ('falhou', 'validacao_falhou', 'scan_falhou', 'ocr_falhou', 'parse_falhou', 'chunk_falhou', 'embedding_falhou', 'indexacao_falhou', 'criacao_falhou')")
    suspend fun getRetryable(): List<IngestionJobEntity>

    // ======================== Quarentena ========================

    @Query("SELECT * FROM ingestion_jobs WHERE status = 'em_quarentena' ORDER BY created_at DESC")
    fun getQuarantined(): Flow<List<IngestionJobEntity>>

    /** Quarentenas com mais de 90 dias (prontas para auto-purge). */
    @Query("SELECT * FROM ingestion_jobs WHERE status = 'em_quarentena' AND created_at < :threshold")
    suspend fun getStaleQuarantines(threshold: Long): List<IngestionJobEntity>

    // ======================== Deep Delete LGPD ========================

    /** Anonimiza notas pessoais no deep delete. */
    @Query("""
        UPDATE ingestion_jobs SET
            review_notes = '[redacted for LGPD]',
            error_message = '[redacted for LGPD]',
            quarantine_reason = CASE WHEN quarantine_reason IS NOT NULL THEN '[redacted for LGPD]' ELSE NULL END,
            updated_at = :now
        WHERE node_id = :nodeId OR id = :nodeId
    """)
    suspend fun anonymizeForPurge(nodeId: String, now: Long = System.currentTimeMillis())

    // ======================== Limpeza ========================

    /** Remove jobs concluídos mais antigos que o período de retenção. */
    @Query("DELETE FROM ingestion_jobs WHERE status = 'concluido' AND completed_at IS NOT NULL AND completed_at < :threshold")
    suspend fun deleteCompletedOlderThan(threshold: Long): Int

    /** Remove jobs cancelados ou bloqueados antigos. */
    @Query("DELETE FROM ingestion_jobs WHERE status IN ('cancelado', 'bloqueado_manualmente') AND updated_at < :threshold")
    suspend fun deleteTerminalOlderThan(threshold: Long): Int

    // ======================== Estatísticas ========================

    @Query("SELECT COUNT(*) FROM ingestion_jobs WHERE status = :status")
    suspend fun countByStatus(status: String): Int

    @Query("SELECT COUNT(*) FROM ingestion_jobs")
    suspend fun count(): Int
}
