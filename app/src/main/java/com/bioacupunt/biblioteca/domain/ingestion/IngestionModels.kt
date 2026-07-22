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
    /**
     * URL do documento-fonte, quando ele é público (PCDT, bulário ANVISA, WHO).
     * Vazio para fonte impressa (livro), que se identifica por [sourceRef].
     */
    val sourceUrl: String = "",
    /**
     * Onde exatamente no documento — "p. 26", "cap. 12, p. 340", "seção 7.2".
     *
     * É isto que transforma a revisão em **conferência**: a médica abre a página e
     * compara, em vez de julgar de memória. Sem localizador, a citação nomeia um
     * documento sem apontar para nada dentro dele.
     */
    val sourceRef: String = "",
)

/**
 * De onde o texto veio, de fato — não o que ele alega.
 *
 * A distinção é a mesma da R3 entre modelo com hash fixado e modelo sem: o que
 * importa não é parecer confiável, é ser conferível. Um item [VERIFICAVEL] pode ser
 * checado contra a fonte em segundos; um [RASCUNHO] exige que a médica saiba a
 * resposta de antemão — que é justamente o que ela está consultando o app para saber.
 */
enum class Provenance {
    /** Extraído de documento identificado, com localizador. Conferível. */
    VERIFICAVEL,

    /** Gerado por IA ou sem localizador. Revisável, mas não conferível. */
    RASCUNHO,
}

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
    val sourceUrl: String = "",
    val sourceRef: String = "",
    /** Default RASCUNHO: itens encenados antes desta mudança não são conferíveis. */
    val provenance: Provenance = Provenance.RASCUNHO,
)
