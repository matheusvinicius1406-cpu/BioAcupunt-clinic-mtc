package com.bioacupunt.data.local.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * AUDIT TRAIL — trilha de auditoria imutável (append-only).
 *
 * Registra todas as operações relevantes no MKIS para compliance LGPD.
 * A tabela é append-only: linhas nunca são atualizadas ou deletadas.
 * No deep delete, os campos de PII (actor_id, ip_address) são anonimizados
 * via UPDATE (exceção à regra append-only, justificada por exigência legal).
 *
 * ## Eventos registrados:
 * - Ingestão: IngestionJobQueued, IngestionJobCompleted, IngestionJobFailed
 * - KnowledgeNode: KnowledgeNodeCreated, KnowledgeNodeApproved, KnowledgeNodeRejected
 * - LGPD: HardDeleteRequested, HardDeleteCompleted, LegalHoldActivated, LegalHoldDeactivated
 * - Quarentena: QuarantineCreated, QuarantineResolved
 * - Cache: CacheInvalidationRequired
 */
@Entity(
    tableName = "audit_trail",
    indices = [
        Index("tenant_id", "occurred_at"),
        Index("resource_type", "resource_id"),
        Index("action"),
    ]
)
data class AuditTrailEntity(
    @PrimaryKey val id: String,

    // === Identidade ===
    val tenant_id: String = "default",

    // === Quem ===
    val actor_id: String,            // UUID → anonimizado no deep delete
    val actor_role: String? = null,  // system, clinical_validator, clinical_validator_senior, security_reviewer, data_steward

    // === O quê ===
    val action: String,              // KnowledgeNodeApproved, HardDeleteCompleted, etc.
    val resource_type: String,       // knowledge_node, ingestion_job, purge_certificate, tenant
    val resource_id: String?,        // UUID do recurso afetado

    // === Contexto de rede (PII — anonimizado no purge) ===
    val ip_address: String? = null,
    val outcome: String? = null,     // success, failure, blocked

    // === Metadados ===
    val request_id: String = java.util.UUID.randomUUID().toString(),
    val metadata: String = "{}",     // JSON: detalhes específicos do evento

    // === Timestamps ===
    val occurred_at: Long = System.currentTimeMillis(),
    val created_at: Long = System.currentTimeMillis(),
) {
    companion object {
        /** Ações de auditoria canônicas. */
        const val ACTION_NODE_CREATED = "KnowledgeNodeCreated"
        const val ACTION_NODE_APPROVED = "KnowledgeNodeApproved"
        const val ACTION_NODE_REJECTED = "KnowledgeNodeRejected"
        const val ACTION_NODE_DEPRECATED = "KnowledgeNodeDeprecated"
        const val ACTION_NODE_SUBMITTED = "KnowledgeNodeSubmitted"
        const val ACTION_JOB_QUEUED = "IngestionJobQueued"
        const val ACTION_JOB_COMPLETED = "IngestionJobCompleted"
        const val ACTION_JOB_FAILED = "IngestionJobFailed"
        const val ACTION_JOB_STATUS_CHANGED = "IngestionJobStatusChanged"
        const val ACTION_QUARANTINE_CREATED = "QuarantineCreated"
        const val ACTION_QUARANTINE_RESOLVED = "QuarantineResolved"
        const val ACTION_HARD_DELETE_REQUESTED = "HardDeleteRequested"
        const val ACTION_HARD_DELETE_COMPLETED = "HardDeleteCompleted"
        const val ACTION_LEGAL_HOLD_ACTIVATED = "LegalHoldActivated"
        const val ACTION_LEGAL_HOLD_DEACTIVATED = "LegalHoldDeactivated"
        const val ACTION_CACHE_INVALIDATED = "CacheInvalidationRequired"
        const val ACTION_EMBEDDING_GENERATED = "EmbeddingGenerated"
        const val ACTION_GRAPH_EDGE_CREATED = "GraphEdgeCreated"
        const val ACTION_EVIDENCE_SCORED = "EvidenceScoreUpdated"

        /** UUID sentinela para auditoria anonimizada. */
        const val ANONYMIZED_ACTOR_ID = "00000000-0000-0000-0000-0000000000de1"
    }
}
