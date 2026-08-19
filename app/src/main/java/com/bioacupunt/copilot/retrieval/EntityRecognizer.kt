package com.bioacupunt.copilot.retrieval

import com.bioacupunt.mtc.knowledge.domain.KnowledgeEntityType
import com.bioacupunt.mtc.knowledge.repository.KnowledgeSearchRepository

/**
 * §6 ENTITY RECOGNIZER
 *
 * Recognizes MTC entities in user queries by matching against the Knowledge Core.
 * No LLM — uses exact match + prefix matching against indexed entities.
 *
 * Examples:
 * - "insônia" → SYMPTOM
 * - "LI4" → ACUPOINT
 * - "Fígado Qi" → PATTERN
 * - "Tai Yang" → CHANNEL
 */
class EntityRecognizer(
    private val searchRepository: KnowledgeSearchRepository,
) {

    data class RecognitionResult(
        val entities: List<RecognizedEntity>,
        val unrecognizedTokens: List<String>,
    )

    /**
     * Recognize entities in a query string.
     * Deterministic: same query → same entities, always.
     */
    suspend fun recognize(query: String): RecognitionResult {
        val tokens = tokenize(query)
        val entities = mutableListOf<RecognizedEntity>()
        val recognized = mutableSetOf<String>()

        // 1. Try exact entity lookup for each token
        for (token in tokens) {
            val entity = searchRepository.getById(token.uppercase())
            if (entity != null) {
                entities.add(
                    RecognizedEntity(
                        text = token,
                        entityType = entity.type,
                        entityId = entity.id,
                        confidence = 1.0,
                    )
                )
                recognized.add(token.lowercase())
            }
        }

        // 2. Try multi-word phrases (bigrams, trigrams)
        for (n in 2..minOf(3, tokens.size)) {
            for (i in 0..tokens.size - n) {
                val phrase = tokens.subList(i, i + n).joinToString(" ")
                if (recognized.contains(phrase.lowercase())) continue

                val results = searchRepository.search(phrase, limit = 3)
                for (result in results) {
                    if (result.score > 0.5) {
                        entities.add(
                            RecognizedEntity(
                                text = phrase,
                                entityType = result.entity.type,
                                entityId = result.entity.id,
                                confidence = result.score,
                            )
                        )
                        recognized.add(phrase.lowercase())
                        break
                    }
                }
            }
        }

        // 3. Fallback: pattern-based recognition for common MTC terms
        for (token in tokens) {
            if (recognized.contains(token.lowercase())) continue
            val inferred = inferFromPattern(token)
            if (inferred != null) {
                entities.add(inferred)
                recognized.add(token.lowercase())
            }
        }

        val unrecognized = tokens.filter { !recognized.contains(it.lowercase()) }

        return RecognitionResult(entities = entities, unrecognizedTokens = unrecognized)
    }

    private fun tokenize(query: String): List<String> {
        return query
            .replace(Regex("[,;:.!?()\\[\\]{}\"']"), " ")
            .split(Regex("\\s+"))
            .filter { it.length >= 2 }
    }

    /**
     * Pattern-based inference for common MTC terms not in the Knowledge Core.
     * Conservative: only infers when pattern is unambiguous.
     */
    private fun inferFromPattern(token: String): RecognizedEntity? {
        val upper = token.uppercase()

        // Acupoint codes: LI4, ST36, SP6, etc.
        if (upper.matches(Regex("[A-Z]{1,3}\\d{1,2}"))) {
            return RecognizedEntity(
                text = token,
                entityType = KnowledgeEntityType.ACUPOINT,
                confidence = 0.7,
            )
        }

        // Common TCM patterns (Zang Fu)
        val zangFuPatterns = mapOf(
            "gao" to "fígado", "fei" to "pulmão", "pi" to "baço",
            "shen" to "rim", "xin" to "coração", "xinbao" to "pericárdio",
            "dan" to "vesícula", "we" to "estômago", "xiaochang" to "intestino delgado",
            "dachang" to "intestino grosso", "pangguang" to "bexiga", "sanjiao" to "triplo aquecedor",
        )
        if (zangFuPatterns.containsKey(upper.lowercase())) {
            return RecognizedEntity(
                text = token,
                entityType = KnowledgeEntityType.PATTERN,
                confidence = 0.6,
            )
        }

        return null
    }
}
