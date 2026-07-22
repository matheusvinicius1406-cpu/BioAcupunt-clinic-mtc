package com.bioacupunt.data.local.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * KNOWLEDGE NODE — unidade atômica de conhecimento científico estruturado.
 *
 * Esta entidade substitui a [KnowledgeNode] anterior e adiciona todos os campos
 * necessários para o MKIS on-device: enums canônicos, embeddings (384d),
 * scores de evidência, governança e LGPD.
 *
 * ## Enums
 * Todos os enums são armazenados como TEXT e validados na camada de aplicação.
 * O CHECK constraint é adicionado via migration para defesa em profundidade.
 *
 * ## Embedding
 * O embedding vetorial (384d) é armazenado na tabela virtual sqlite-vec
 * `vec_knowledge_nodes`, não aqui. Esta tabela só tem a referência [id].
 *
 * ## LGPD
 * Campos de pessoa (created_by, reviewed_by, approved_by) são anonimizados
 * durante o deep delete, substituídos pelo UUID sentinela.
 */
@Entity(
    tableName = "knowledge_nodes",
    indices = [
        Index("tenant_id"),
        Index("status"),
        Index("checksum", "tenant_id", unique = true),
        Index("tenant_id", "created_at"),
    ]
)
data class KnowledgeNodeEntity(
    @PrimaryKey val id: String,

    // === Identidade ===
    val tenant_id: String = "default",
    val title: String,
    val summary: String,
    val content: String,

    // === Enums canônicos (TEXT - validados na app) ===
    val knowledge_type: String = "artigo",   // artigo, revisao, guideline, capitulo, livro, tese, caso_clinico, ensaio_clinico, protocolo, nota, relatorio, educacional
    val status: String = "rascunho",         // rascunho, em_revisao, aguardando_aprovacao, aprovado, rejeitado, descontinuado, substituido
    val evidence_level: String? = null,      // cebm_1a..5, grade_alta..muito_baixa
    val bias_risk: String = "nao_avaliado",  // nao_avaliado, baixo, moderado, alto
    val clinical_evidence: String? = null,   // muito_alta, alta, moderada, baixa, insuficiente
    val category: String = "mtc",            // mtc, medicina_ocidental, saude_publica, farmacologia, fisiologia, psicologia, nutricao, biotecnologia, educacao
    val source: String = "manual",           // pubmed, europe_pmc, semantic_scholar, crossref, openalex, scielo, bvs, who_iris, paho_iris, doaj, clinical_trials, wikimedia, manual, pacote
    val specialty: String = "geral",         // acupuntura, auriculoterapia, fitoterapia, moxabustao, ventosaterapia, tui_na, eletroacupuntura, craniopuntura, geral
    val language: String = "pt",             // ISO 639-1 (pt, en, zh, es, fr, de, ja, ko)
    val data_classification: String = "restrito", // publico, interno, restrito, pii

    // === Metadados científicos ===
    val doi: String? = null,
    val pmid: String? = null,
    val source_url: String? = null,
    val authors: String = "[]",              // JSON array: [{name, affiliation, orcid}]
    val citation: String = "",               // Referência bibliográfica formatada

    // === Scores (0.0 a 1.0) ===
    val scientific_score: Double? = null,
    val ai_score: Double? = null,
    val reliability_score: Double? = null,

    // === Governança ===
    val checksum: String = "",
    val created_by: String? = null,           // UUID → anonimizado no purge
    val reviewed_by: String? = null,
    val approved_by: String? = null,
    val approved_at: Long? = null,
    val version: String = "0.1.0",
    val superseded_by: String? = null,        // FK: novo nó que substitui este

    // === Tags e metadados flexíveis ===
    val tags: String = "",                    // Espaço-separadas para FTS
    val metadata: String = "{}",              // JSON livre
    val conflicts: String = "[]",             // JSON: registros de conflitos e resoluções

    // === Timestamps ===
    val created_at: Long = System.currentTimeMillis(),
    val updated_at: Long = System.currentTimeMillis(),
    val deleted_at: Long? = null,             // Soft delete
) {
    /** Retorna true se o nó não foi marcado como deletado. */
    val isActive: Boolean get() = deleted_at == null

    /** Retorna true se o nó pode ser buscado (aprovado ou descontinuado). */
    val isSearchable: Boolean get() = status == "aprovado" || status == "descontinuado"

    companion object {
        /** UUID sentinela usado no deep delete para anonimizar campos de pessoa. */
        const val DELETED_USER_SENTINEL = "00000000-0000-0000-0000-0000000000de1"
    }
}
