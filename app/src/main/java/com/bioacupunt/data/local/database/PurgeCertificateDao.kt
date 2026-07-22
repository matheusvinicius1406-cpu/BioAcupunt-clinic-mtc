package com.bioacupunt.data.local.database

import androidx.room.*
import com.bioacupunt.data.local.model.PurgeCertificateEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para [PurgeCertificateEntity] — certificados de purge LGPD.
 *
 * Os certificados são imutáveis após conclusão. Apenas operações de leitura
 * e inserção são permitidas para certificados completados.
 */
@Dao
interface PurgeCertificateDao {

    // ======================== CRUD ========================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(certificate: PurgeCertificateEntity)

    @Query("SELECT * FROM purge_certificates WHERE id = :id")
    suspend fun getById(id: String): PurgeCertificateEntity?

    @Query("SELECT * FROM purge_certificates WHERE id = :id")
    fun getByIdFlow(id: String): Flow<PurgeCertificateEntity?>

    @Query("SELECT * FROM purge_certificates WHERE tenant_id = :tenantId ORDER BY created_at DESC")
    fun getByTenant(tenantId: String = "default"): Flow<List<PurgeCertificateEntity>>

    @Query("SELECT * FROM purge_certificates WHERE target_type = :targetType AND target_id = :targetId ORDER BY created_at DESC LIMIT 1")
    suspend fun getByTarget(targetType: String, targetId: String): PurgeCertificateEntity?

    @Query("SELECT * FROM purge_certificates WHERE target_type = :targetType AND target_id = :targetId ORDER BY created_at DESC")
    fun getByTargetFlow(targetType: String, targetId: String): Flow<List<PurgeCertificateEntity>>

    // ======================== Checkpoint Management ========================

    /**
     * Atualiza o checkpoint durante a execução do cascade de purge.
     * Permite retomada em caso de falha do app (interrupção).
     */
    @Query("""
        UPDATE purge_certificates SET
            checkpoint = :checkpoint
        WHERE id = :id AND checkpoint != 'completed'
    """)
    suspend fun updateCheckpoint(id: String, checkpoint: String)

    /**
     * Marca o certificado como concluído.
     */
    @Query("""
        UPDATE purge_certificates SET
            checkpoint = 'completed',
            completed_at = :now
        WHERE id = :id
    """)
    suspend fun complete(id: String, now: Long = System.currentTimeMillis())

    /**
     * Marca o certificado como falho (para retentativa).
     */
    @Query("""
        UPDATE purge_certificates SET
            checkpoint = 'failed',
            notes = :error
        WHERE id = :id
    """)
    suspend fun fail(id: String, error: String)

    /**
     * Adiciona um passo ao steps_log.
     * @param stepJson Ex: '{"step":"nodes_anonymized","affected_rows":1,"duration_ms":15}'
     */
    @Query("""
        UPDATE purge_certificates SET
            steps_log = json_insert(steps_log, '$[#]', :stepJson)
        WHERE id = :id
    """)
    suspend fun appendStep(id: String, stepJson: String)

    // ======================== Queries de Verificação ========================

    /** Certificados pendentes ou em andamento (para retomada). */
    @Query("SELECT * FROM purge_certificates WHERE checkpoint NOT IN ('completed', 'failed') ORDER BY created_at ASC")
    suspend fun getInProgress(): List<PurgeCertificateEntity>

    @Query("SELECT COUNT(*) FROM purge_certificates WHERE tenant_id = :tenantId")
    suspend fun countByTenant(tenantId: String = "default"): Int

    @Query("SELECT COUNT(*) FROM purge_certificates WHERE checkpoint = 'completed' AND completed_at > :since")
    suspend fun countCompletedSince(since: Long): Int

    // ======================== Limpeza ========================

    /** Remove certificados antigos (apenas para testes — produção mantém perpétuo). */
    @Query("DELETE FROM purge_certificates WHERE created_at < :threshold AND checkpoint = 'completed'")
    suspend fun deleteOldCompleted(threshold: Long): Int
}
