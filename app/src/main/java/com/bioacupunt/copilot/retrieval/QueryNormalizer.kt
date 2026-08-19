package com.bioacupunt.copilot.retrieval

import java.text.Normalizer

/**
 * §7 QUERY NORMALIZER
 *
 * Normalizes user queries for better retrieval matches.
 * Preserves the original query — never destroys it.
 *
 * Operations:
 * - Unicode normalization (NFD → remove accents)
 * - Case normalization
 * - Alias expansion (MTC-specific)
 * - Controlled synonyms
 * - Romanization variants (pinyin)
 * - Term normalization (common MTC abbreviations)
 */
class QueryNormalizer {

    data class NormalizedQuery(
        val originalQuery: String,
        val normalizedQuery: String,
        val expandedTerms: List<String>,
        val recognizedEntities: List<RecognizedEntity>,
    )

    // MTC alias map: common variations → canonical form
    private val aliases = mapOf(
        // Zang Fu variations
        "figado" to "fígado", "figado" to "fígado",
        "pulmao" to "pulmão", "pulmão" to "pulmão",
        "baço" to "baço", "baco" to "baço",
        "rim" to "rim", "rins" to "rim",

        // Yin/Yang variations
        "yin" to "yin", "yin" to "yin",
        "yang" to "yang", "yang" to "yang",

        // Qi variations
        "qi" to "qi", "ch'i" to "qi", "chi" to "qi",
        "ki" to "qi",

        // Blood
        "sangue" to "sangue", "xue" to "sangue",

        // Common symptoms
        "insonia" to "insônia", "insônia" to "insônia",
        "cefaleia" to "cefaleia", "dor de cabeça" to "cefaleia",
        "lombalgia" to "lombalgia", "dor lombar" to "lombalgia",
        "cervicalgia" to "cervicalgia", "dor cervical" to "cervicalgia",
    )

    // Controlled synonyms: query terms → expanded search terms
    private val synonyms = mapOf(
        "insônia" to listOf("distúrbio do sono", "dificuldade para dormir", "insônia"),
        "dor" to listOf("dor", "algia", "ce algia", "dor crônica"),
        "ansiedade" to listOf("ansiedade", "inquietação", "agitacao"),
        "digestão" to listOf("digestão", "dispepsia", "indigestão"),
        "menstruação" to listOf("menstruação", "menstruação", "ciclo menstrual", "dismenorreia"),
        "estresse" to listOf("estresse", "stress", "tensão"),
        "fadiga" to listOf("fadiga", "cansaço", "fatiga", "astenia"),
        "inflamação" to listOf("inflamação", "inflamação", "flogose"),
    )

    // Pinyin → character mappings (common acupoints)
    private val pinyinToCode = mapOf(
        "hegu" to "LI4", "he gu" to "LI4",
        "zusanli" to "ST36", "zu san li" to "ST36",
        "sanyinjiao" to "SP6", "san yin jiao" to "SP6",
        "neiguan" to "PC6", "nei guan" to "PC6",
        "taichong" to "LR3", "tai chong" to "LR3",
        "yongquan" to "KI1", "yong quan" to "KI1",
        "baihui" to "GV20", "bai hui" to "GV20",
        "shenmen" to "HT7", "shen men" to "HT7",
    )

    /**
     * Normalize a user query.
     * Deterministic: same query → same normalization, always.
     */
    fun normalize(query: String): NormalizedQuery {
        val trimmed = query.trim()

        // 1. Unicode normalization + accent removal for matching
        val stripped = Normalizer.normalize(trimmed, Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}"), "")
            .lowercase()

        // 2. Apply aliases (best match from alias map)
        val aliased = aliases[stripped] ?: stripped

        // 3. Expand synonyms
        val expanded = mutableListOf<String>()
        for ((key, syns) in synonyms) {
            if (aliased.contains(key)) {
                expanded.addAll(syns)
            }
        }

        // 4. Detect pinyin acupoint codes
        val withPinyin = pinyinToCode.entries.fold(aliased) { acc, (pinyin, code) ->
            acc.replace(pinyin, code)
        }

        return NormalizedQuery(
            originalQuery = trimmed,
            normalizedQuery = withPinyin,
            expandedTerms = expanded.distinct(),
            recognizedEntities = emptyList(), // populated by EntityRecognizer
        )
    }
}
