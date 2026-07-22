package com.bioacupunt.biblioteca.domain.search

import java.text.Normalizer

/**
 * TEXT PROCESSING UTILITIES FOR THE MTC LIBRARY SEARCH.
 *
 * ## O que esta classe faz AGORA
 * Apenas pré-processamento textual: normalização de acentos, tokenização,
 * stopwords e expansão de sinônimos bilíngues (Português ↔ pinyin).
 *
 * A indexação e o ranking BM25 foram movidos para o **SQLite FTS4**
 * ([FtsSearchService]) — que usa comandos SQL nativos do banco para
 * busca e ranqueamento. Isso escala para milhares de artigos sem ocupar RAM.
 *
 * ## Por que manter esta classe?
 * O FTS4 do SQLite não entende sinônimos de MTC. "Baço" e "Pi" são strings
 * diferentes. Então o pré-processamento ainda é necessário: normalizamos e
 * expandimos a query ANTES de enviá-la ao FTS4.
 *
 * ## O que foi removido
 * - A classe `Index` (BM25 em memória)
 * - A função `index()` (construção do índice)
 * - A data class `SearchHit`
 * - As constantes BM25 (K1, B, field boosts)
 * - `MtcArticle` (não mais necessária aqui)
 */
object MtcSearchEngine {

    // -- Text normalisation -------------------------------------------------

    /** Folds accents, lowercases, strips punctuation. "Pulmão" -> "pulmao". */
    fun normalize(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(DIACRITICS, "")
            .lowercase()

    private val DIACRITICS = Regex("\\p{Mn}+")
    private val TOKEN_SPLIT = Regex("[^a-z0-9]+")

    fun tokenize(text: String): List<String> =
        normalize(text)
            .split(TOKEN_SPLIT)
            .filter { it.length > 1 && it !in STOPWORDS }

    /** Portuguese stopwords. Kept small on purpose: over-filtering hurts recall. */
    private val STOPWORDS = setOf(
        "de", "da", "do", "das", "dos", "em", "no", "na", "nos", "nas",
        "um", "uma", "os", "as", "ao", "aos", "com", "por", "para", "que",
        "se", "sua", "seu", "e", "ou",
    )

    /**
     * Bilingual TCM vocabulary. Bidirectional: entering either side finds both.
     *
     * This is the single highest-leverage part of the whole search. A practitioner
     * trained in Portuguese and a textbook written in pinyin do not share a
     * vocabulary, and no amount of string matching fixes that.
     */
    val SYNONYMS: Map<String, Set<String>> = buildSynonyms(
        listOf(
            setOf("baco", "pi"),
            setOf("figado", "gan"),
            setOf("rim", "rins", "shen"),
            setOf("coracao", "xin"),
            setOf("pulmao", "pulmoes", "fei"),
            setOf("estomago", "wei"),
            setOf("vesicula", "dan"),
            setOf("bexiga", "pangguang"),
            setOf("deficiencia", "vazio", "xu"),
            setOf("excesso", "plenitude", "shi"),
            setOf("estagnacao", "stagnacao", "yu"),
            setOf("umidade", "shi-umidade", "dampness"),
            setOf("fleuma", "tan"),
            setOf("sangue", "xue"),
            setOf("energia", "qi", "chi"),
            setOf("essencia", "jing"),
            setOf("mente", "espirito", "shen-mente"),
            setOf("calor", "re"),
            setOf("frio", "han"),
            setOf("vento", "feng"),
            setOf("lingua", "she"),
            setOf("pulso", "mai"),
            setOf("moxa", "moxabustao", "jiu"),
            setOf("ventosa", "ventosaterapia", "cupping"),
            setOf("agulha", "agulhamento", "acupuntura", "zhen"),
        ),
    )

    private fun buildSynonyms(groups: List<Set<String>>): Map<String, Set<String>> {
        val map = mutableMapOf<String, Set<String>>()
        groups.forEach { group -> group.forEach { term -> map[term] = group } }
        return map
    }

    /** Expands each query token into itself plus its synonym group. */
    fun expand(tokens: List<String>): List<String> =
        tokens.flatMap { token -> SYNONYMS[token]?.toList() ?: listOf(token) }.distinct()
}
