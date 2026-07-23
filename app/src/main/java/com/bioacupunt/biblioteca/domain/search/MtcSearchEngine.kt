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

    /**
     * Normaliza texto: folds acentos latinos, mantém caracteres CJK (chinês).
     * "Pulmão" → "pulmao", "中醫" → "中醫" (preservado).
     */
    fun normalize(text: String): String {
        val nfd = Normalizer.normalize(text, Normalizer.Form.NFD)
        // Remove combining diacritical marks (acentos, cedilhas, tils)
        // preserva CJK (chinês) pois CJK não se decompõe em NFD
        val sb = StringBuilder(nfd.length)
        for (ch in nfd) {
            // Character.NON_SPACING_MARK = 6 (Byte) — toda marca diacrítica combinante
            if (Character.getType(ch).toByte() == Character.NON_SPACING_MARK) {
                continue
            }
            sb.append(ch)
        }
        return sb.toString().lowercase()
    }

    /** Tokens são separados por whitespace OU pontuação, mas CJK é preservado. */
    private val TOKEN_SPLIT = Regex("[\\s\\p{P}&&[^一-龯々〆〤]]+")

    /**
     * Tokeniza: respeita caracteres chineses, separa por whitespace/pontuação.
     * Aplica stemming básico em português.
     */
    fun tokenize(text: String): List<String> {
        val tokens = mutableListOf<String>()
        val normalized = normalize(text)

        // 1. Extrair sequências CJK (cada caractere individual é token)
        val cjkPattern = Regex("[一-龯々〆〤〇]")
        val cjkChars = cjkPattern.findAll(normalized).map { it.value }.toList()
        tokens.addAll(cjkChars)

        // 2. Remover CJK do texto e splitar o resto
        val noCjk = normalized.replace(cjkPattern, " ")
        val latinTokens = noCjk
            .split(TOKEN_SPLIT)
            .filter { it.length > 1 && it !in STOPWORDS }
        tokens.addAll(latinTokens)

        return tokens.distinct()
    }

    /** Portuguese stopwords. */
    private val STOPWORDS = setOf(
        "de", "da", "do", "das", "dos", "em", "no", "na", "nos", "nas",
        "um", "uma", "os", "as", "ao", "aos", "com", "por", "para", "que",
        "se", "sua", "seu", "e", "ou", "mais", "mas", "como", "sao",
        "este", "esta", "esse", "essa", "aquele", "aquela",
        "ja", "muito", "pode", "tem", "foram", "ser", "era",
    )

    /**
     * Stemming conservador para português.
     * APENAS sufixos nominais — NÃO verbais ("ia" em "energia" não é sufixo).
     *
     * Exemplos:
     * - "pulmões" → "pulmao" (plural → singular, com/sem acento)
     * - "tratamento" → "trata" (remove -mento)
     * - "estagnação" → "estagna" (remove -ção)
     * - "energia" → "energia" ("ia" é parte da raiz, não sufixo)
     */
    private fun stem(word: String): String {
        if (word.length < 4) return word
        var w = word

        // Plural → singular: texto já normalizado (sem acentos)
        w = w.replace(Regex("(oes|ões)$"), "ao")     // pulmões → pulmao
            .replace(Regex("(aes|ães)$"), "ao")     // cães → cao
            .replace(Regex("ns$"), "m")              // bens → bem

        // Regular plural: remove 's' final
        if (w.endsWith("s") && w.length > 3) {
            val semS = w.dropLast(1)
            if (semS.length >= 3) w = semS
        }

        // Sufixos nominais (conservador: só padrões seguros)
        w = w.replace(Regex("(cao|ção|coes|ções)$"), "")   // estagnação → estagna
            .replace(Regex("(mento|menta)$"), "")           // tratamento → trata
            .replace(Regex("(dade|dades)$"), "")           // saudade → saud
            .replace(Regex("(vel|velmente)$"), "")          // possível → possi

        return w.ifEmpty { word }
    }

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
    /**
     * Expande cada token: sinônimos + stemming português.
     * Ex: "deficiencia" → [deficiencia, vazio, xu, deficienc]
     *     "tratamento" → [tratamento, trata] (stemming)
     */
    fun expand(tokens: List<String>): List<String> {
        val result = mutableListOf<String>()
        for (token in tokens) {
            // 1. Lookup de sinônimos MTC bilíngues
            val group = SYNONYMS[token]
            if (group != null) result.addAll(group)
            else result.add(token)

            // 2. Stemming português para capturar variantes gramaticais
            val stemmed = stem(token)
            if (stemmed != token && stemmed !in result) {
                val stemmedGroup = SYNONYMS[stemmed]
                if (stemmedGroup != null) result.addAll(stemmedGroup)
                else result.add(stemmed)
            }
        }
        return result.distinct()
    }
}
