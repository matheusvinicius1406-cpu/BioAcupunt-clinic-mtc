package com.bioacupunt.copilot.retrieval

import com.bioacupunt.mtc.knowledge.domain.KnowledgeEntityType
import com.bioacupunt.mtc.knowledge.repository.KnowledgeSearchRepository

/**
 * §8 LEXICAL SEARCH BACKEND
 *
 * Wraps [KnowledgeSearchRepository] (FTS5 BM25) as a retrieval backend.
 * The existing FTS5 index already provides BM25 ranking — no new index needed.
 *
 * This is the primary lexical retrieval path for the hybrid retriever.
 */
class LexicalSearchBackend(
    private val searchRepository: KnowledgeSearchRepository,
) {

    /**
     * Execute lexical search with optional type filter.
     * Returns results already ranked by FTS5 BM25.
     */
    suspend fun search(
        query: String,
        limit: Int = 50,
        typeFilter: KnowledgeEntityType? = null,
    ): List<RetrievalHit> {
        if (query.isBlank()) return emptyList()

        val results = if (typeFilter != null) {
            searchRepository.searchByType(query, typeFilter, limit)
        } else {
            searchRepository.search(query, limit)
        }

        return results.map { result ->
            RetrievalHit(
                entityId = result.entity.id,
                entity = result.entity,
                content = result.entity.content,
                score = result.score,
                normalizedScore = normalizeFtsScore(result.score),
                sourceType = RetrievalSource.LEXICAL,
            )
        }
    }

    /**
     * Normalize FTS5 BM25 score to 0.0–1.0 range.
     * FTS5 bm25() returns negative values (closer to 0 = better).
     * We invert and clamp.
     */
    private fun normalizeFtsScore(rawScore: Double): Double {
        // FTS5 bm25() is negative; typical range: -10 to 0
        // Normalize: score=0 → 1.0, score=-10 → 0.0
        return (1.0 + rawScore / 10.0).coerceIn(0.0, 1.0)
    }
}
