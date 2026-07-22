package com.bioacupunt.data.local.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * INGESTION JOB — pipeline de processamento de documentos.
 *
 * Representa o processamento de um documento (pack, URL, upload) até a criação
 * de um [KnowledgeNodeEntity]. Segue a máquina de estados definida no spec MKIS.
 *
 * ## Estados canônicos (TEXT, validados na app):
 * na_fila, baixando, validacao_falhou, escanando, scan_falhou, em_quarentena,
 * ocr_na_fila, ocr_rodando, ocr_falhou,
 * parse_na_fila, parse_rodando, parse_falhou,
 * chunk_na_fila, chunk_rodando, chunk_falhou,
 * embedding_na_fila, embedding_rodando, embedding_falhou,
 * indexacao_na_fila, indexacao_rodando, indexacao_falhou,
 * criando_no, criacao_falhou,
 * revisao_necessaria,
 * concluido, falhou, bloqueado_manualmente, cancelado
 *
 * ## Ciclo de vida:
 * 1. Job é criado em `na_fila`
 * 2. Pipeline executa stages sequencialmente
 * 3. Ao chegar em `criando_no`, insere o KnowledgeNode
 * 4. Job termina em `concluido`, `falhou`, `cancelado` ou `bloqueado_manualmente`
 */
@Entity(
    tableName = "ingestion_jobs",
    indices = [
        Index("tenant_id", "status"),
        Index("node_id"),
        Index("created_at", "status"),
    ]
)
data class IngestionJobEntity(
    @PrimaryKey val id: String,

    // === Identidade ===
    val tenant_id: String = "default",

    // === Relações ===
    val artifact_id: String? = null,          // FK para knowledge_artifacts (futuro)
    val node_id: String? = null,              // FK para knowledge_nodes (preenchido ao final)

    // === Máquina de estados ===
    val status: String = "na_fila",

    // === Controle de tentativas ===
    val attempt_id: String = java.util.UUID.randomUUID().toString(),
    val attempt_count: Int = 1,
    val max_attempts: Int = 3,

    // === Erro e diagnóstico ===
    val current_stage: String? = null,
    val error_code: String? = null,
    val error_message: String? = null,
    val quarantine_reason: String? = null,
    val review_notes: String? = null,

    // === Origem ===
    val source_url: String? = null,
    val source_package: String? = null,
    val priority: Int = 0,

    // === Timestamps ===
    val started_at: Long? = null,
    val completed_at: Long? = null,
    val created_at: Long = System.currentTimeMillis(),
    val updated_at: Long = System.currentTimeMillis(),
) {
    /** Estados terminais — job não pode mais transicionar. */
    val isTerminal: Boolean get() = status in TERMINAL_STATES

    /** Estados que indicam falha — podem ser retentados. */
    val isRetryable: Boolean get() = status in RETRYABLE_STATES

    companion object {
        val TERMINAL_STATES = setOf("concluido", "falhou", "bloqueado_manualmente", "cancelado")
        val RETRYABLE_STATES = setOf(
            "validacao_falhou", "scan_falhou", "ocr_falhou",
            "parse_falhou", "chunk_falhou", "embedding_falhou",
            "indexacao_falhou", "criacao_falhou", "falhou",
        )

        /** Transições válidas por estado (para validação na app). */
        val VALID_TRANSITIONS: Map<String, Set<String>> = mapOf(
            "na_fila" to setOf("baixando"),
            "baixando" to setOf("escanando", "validacao_falhou"),
            "escanando" to setOf("parse_na_fila", "ocr_na_fila", "em_quarentena", "scan_falhou"),
            "ocr_na_fila" to setOf("ocr_rodando"),
            "ocr_rodando" to setOf("parse_na_fila", "revisao_necessaria", "ocr_falhou"),
            "parse_na_fila" to setOf("parse_rodando"),
            "parse_rodando" to setOf("chunk_na_fila", "revisao_necessaria", "parse_falhou"),
            "chunk_na_fila" to setOf("chunk_rodando"),
            "chunk_rodando" to setOf("embedding_na_fila", "chunk_falhou"),
            "embedding_na_fila" to setOf("embedding_rodando"),
            "embedding_rodando" to setOf("indexacao_na_fila", "embedding_falhou"),
            "indexacao_na_fila" to setOf("indexacao_rodando"),
            "indexacao_rodando" to setOf("criando_no", "indexacao_falhou"),
            "criando_no" to setOf("concluido", "criacao_falhou"),
            "revisao_necessaria" to setOf("baixando", "ocr_rodando", "parse_rodando", "em_quarentena", "cancelado"),
            "em_quarentena" to setOf("revisao_necessaria", "escanando", "cancelado", "bloqueado_manualmente"),
            "validacao_falhou" to setOf("na_fila", "cancelado"),
            "scan_falhou" to setOf("escanando", "cancelado"),
            "ocr_falhou" to setOf("ocr_na_fila", "cancelado"),
            "parse_falhou" to setOf("parse_na_fila", "cancelado"),
            "chunk_falhou" to setOf("chunk_na_fila", "cancelado"),
            "embedding_falhou" to setOf("embedding_na_fila", "cancelado"),
            "indexacao_falhou" to setOf("indexacao_na_fila", "cancelado"),
            "criacao_falhou" to setOf("criando_no", "cancelado"),
            "falhou" to setOf("na_fila"),   // apenas com novo attempt_id
            "concluido" to emptySet(),
            "bloqueado_manualmente" to emptySet(),
            "cancelado" to emptySet(),
        )
    }
}
