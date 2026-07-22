package com.bioacupunt.data.local.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * PURGE CERTIFICATE — certificado imutável de exclusão LGPD.
 *
 * Provê evidência de que um deep delete foi executado, conforme exigido
 * pela LGPD (Art. 18, §3º). O certificado é imutável: uma vez criado com
 * checkpoint 'completed', não pode ser alterado.
 *
 * ## Ciclo de vida:
 * pending → nodes_anonymized → versions_anonymized → artifacts_deleted →
 * edges_anonymized → jobs_anonymized → audit_anonymized → cache_invalidated →
 * storage_purged → completed | failed
 *
 * ## Invariantes:
 * - `certificate_hash` é SHA-256 do conteúdo do certificado (tamper-evident)
 * - `legal_hold_verified_at` prova que a verificação de legal hold ocorreu
 * - Se `checkpoint = 'failed'`, o certificado pode ser reexecutado via novo ID
 * - Se `checkpoint = 'completed'`, o certificado é imutável
 */
@Entity(
    tableName = "purge_certificates",
    indices = [
        Index("tenant_id", "created_at"),
        Index("target_type", "target_id"),
    ]
)
data class PurgeCertificateEntity(
    @PrimaryKey val id: String,

    // === Identidade ===
    val tenant_id: String = "default",

    // === Alvo ===
    val target_type: String,           // knowledge_node, knowledge_artifact, user_data, tenant_data
    val target_id: String,
    val cascade_scope: String = "metadata_only", // full, metadata_only, storage_only
    val target_details: String = "{}", // JSON: descrição legível

    // === Execução ===
    val requested_by: String,          // UUID do data_steward
    val started_at: Long = System.currentTimeMillis(),
    val completed_at: Long? = null,

    // === Checkpoint (retomada em caso de falha) ===
    val checkpoint: String = "pending",
    val steps_log: String = "[]",      // JSON array: [{step, affected_rows, duration_ms}]

    // === Integridade ===
    val certificate_hash: String,      // SHA-256 do conteúdo

    // === Legal Hold ===
    val legal_hold_verified_at: Long? = null,
    val notes: String? = null,

    // === Timestamps ===
    val created_at: Long = System.currentTimeMillis()
) {
    // === Transientes (não persistidos) ===
    /** True se o purge foi concluído com sucesso. */
    val isCompleted: Boolean get() = checkpoint == "completed"

    /** True se o purge falhou e pode ser retentado. */
    val isFailed: Boolean get() = checkpoint == "failed"

    companion object {
        /** Calcula o certificate_hash para um novo certificado. */
        fun computeHash(
            targetType: String,
            targetId: String,
            scope: String,
            timestamp: Long,
        ): String {
            val input = "$targetType:$targetId:$scope:$timestamp"
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            return digest.digest(input.toByteArray())
                .joinToString("") { "%02x".format(it) }
        }
    }
}
