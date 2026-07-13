package com.bioacupunt.biblioteca.domain.search

import com.bioacupunt.biblioteca.domain.model.MtcArticle
import java.text.Normalizer
import kotlin.math.ln

/**
 * SEARCH ENGINE FOR THE MTC LIBRARY
 *
 * Replaces `WHERE title LIKE '%query%'`, which was broken in three ways that all
 * matter for a Portuguese-speaking clinician typing fast between patients:
 *
 *  1. **Accents.** `LIKE '%pulmao%'` does not match "Pulmão". Nobody types the tilde
 *     on a phone keyboard mid-consultation. Everything is folded to ASCII first.
 *  2. **Ranking.** LIKE returns rows in table order. The article *about* the Lung
 *     meridian and an article that merely mentions it in passing came back equally
 *     ranked. BM25 scores by term rarity and field weight, so the title match wins.
 *  3. **Vocabulary.** TCM is bilingual by nature. A practitioner searching "Baço"
 *     must find "Pi"; searching "deficiência" must find "Xu". A substring match
 *     cannot bridge that, so synonyms are expanded at query time.
 *
 * Pure Kotlin, no Android, no SQL — which is why every claim above is unit-tested
 * rather than asserted.
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

    // -- Indexing -----------------------------------------------------------

    internal data class Doc(
        val article: MtcArticle,
        val titleTokens: List<String>,
        val summaryTokens: List<String>,
        val contentTokens: List<String>,
        val tagTokens: List<String>,
    ) {
        val length: Int = titleTokens.size + summaryTokens.size +
            contentTokens.size + tagTokens.size
    }

    class Index internal constructor(
        private val docs: List<Doc>,
        private val documentFrequency: Map<String, Int>,
        private val averageLength: Double,
    ) {

        /**
         * BM25 with per-field boosts. A hit in the title is worth far more than a
         * hit buried in the body — the doctor searching "Kunlun" wants the article
         * *about* Kunlun, not every article that name-drops it.
         */
        fun search(query: String, limit: Int = 20): List<SearchHit> {
            val terms = expand(tokenize(query))
            if (terms.isEmpty()) return emptyList()

            return docs
                .map { doc -> SearchHit(doc.article, score(doc, terms), matchedTerms(doc, terms)) }
                .filter { it.score > 0.0 }
                .sortedWith(
                    compareByDescending<SearchHit> { it.score }
                        .thenBy { it.article.title },  // stable, so results never jitter
                )
                .take(limit)
        }

        private fun matchedTerms(doc: Doc, terms: List<String>): Set<String> =
            terms.filterTo(mutableSetOf()) { term ->
                term in doc.titleTokens || term in doc.summaryTokens ||
                    term in doc.contentTokens || term in doc.tagTokens
            }

        private fun score(doc: Doc, terms: List<String>): Double =
            terms.sumOf { term -> termScore(doc, term) }

        private fun termScore(doc: Doc, term: String): Double {
            val weighted =
                doc.titleTokens.count { it == term } * TITLE_BOOST +
                    doc.tagTokens.count { it == term } * TAG_BOOST +
                    doc.summaryTokens.count { it == term } * SUMMARY_BOOST +
                    doc.contentTokens.count { it == term } * CONTENT_BOOST

            if (weighted == 0.0) return 0.0

            val df = documentFrequency[term] ?: 0
            if (df == 0) return 0.0

            // Standard BM25 IDF. A term present in every article carries no signal.
            val idf = ln(1.0 + (docs.size - df + 0.5) / (df + 0.5))
            val norm = weighted * (K1 + 1) /
                (weighted + K1 * (1 - B + B * doc.length / averageLength))
            return idf * norm
        }
    }

    fun index(articles: List<MtcArticle>): Index {
        val docs = articles.map { article ->
            Doc(
                article = article,
                titleTokens = tokenize(article.title),
                summaryTokens = tokenize(article.summary),
                contentTokens = tokenize(article.content),
                tagTokens = article.tags.flatMap { tokenize(it) },
            )
        }

        val df = mutableMapOf<String, Int>()
        docs.forEach { doc ->
            val unique = (doc.titleTokens + doc.summaryTokens + doc.contentTokens + doc.tagTokens)
                .toSet()
            unique.forEach { term -> df[term] = (df[term] ?: 0) + 1 }
        }

        val avg = docs.map { it.length }.average().takeIf { !it.isNaN() } ?: 1.0
        return Index(docs, df, avg.coerceAtLeast(1.0))
    }

    private const val K1 = 1.2
    private const val B = 0.75
    private const val TITLE_BOOST = 8.0
    private const val TAG_BOOST = 5.0
    private const val SUMMARY_BOOST = 3.0
    private const val CONTENT_BOOST = 1.0
}

data class SearchHit(
    val article: MtcArticle,
    val score: Double,
    val matchedTerms: Set<String>,
)
