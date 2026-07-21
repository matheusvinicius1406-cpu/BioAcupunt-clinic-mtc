package com.bioacupunt.biblioteca.domain.ingestion

import kotlinx.serialization.Serializable

/**
 * PIPELINE DE INGESTÃO DA BIBLIOTECA — modelos
 *
 * A visão pede centenas de artigos; a biblioteca tem dezenas. A regra R4 do projeto
 * é explícita sobre COMO fechar essa lacuna: **você constrói o pipeline de ingestão,
 * nunca o conteúdo**. Nada de LLM escrevendo artigo de MTC e publicando sozinho —
 * isso seria conteúdo clínico sem revisão numa fonte que a médica trata como confiável.
 *
 * Então o conteúdo entra por **pacotes curados** (JSON versionado, de fonte humana
 * revisada — Maciocia, Deadman, diretrizes), cada item carregando a sua **citação**.
 * O pacote é *encenado* numa fila de revisão; só depois do **aval da médica** o item
 * vira acervo consultável e entra no RAG. O portão é código ([LibraryIngestion]),
 * não um prompt.
 */
@Serializable
data class LibraryContentPack(
    /** Fonte revisada por humano de onde veio este lote (ex.: "Maciocia 2015, cap. 12"). */
    val source: String,
    val items: List<LibraryContentItem>,
)

@Serializable
data class LibraryContentItem(
    val id: String,
    val title: String,
    /** Deve ser o nome de um [com.bioacupunt.biblioteca.domain.model.MtcCategory]. */
    val category: String,
    val summary: String,
    val content: String,
    val tags: List<String> = emptyList(),
    /** Referência bibliográfica específica do item. Sem isto, o item é rejeitado (R4). */
    val citation: String = "",
)

enum class ReviewStatus { PENDING, APPROVED, REJECTED }

/**
 * Estado de revisão + proveniência de um nó encenado. Serializado dentro do campo
 * `metadata` (JSON) de `biblioteca_nodes` — assim o pipeline reaproveita a tabela que
 * já existe, sem migração de schema.
 */
@Serializable
data class ReviewMeta(
    val status: ReviewStatus,
    val source: String,
    val citation: String,
    val stagedAt: Long,
    val reviewedAt: Long? = null,
)
