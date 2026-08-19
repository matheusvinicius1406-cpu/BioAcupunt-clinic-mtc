package com.bioacupunt.copilot.retrieval

/**
 * §15 SCORE NORMALIZER
 *
 * Normalizes scores from different retrieval backends to a comparable 0.0–1.0 range.
 * Each backend has different score semantics:
 * - FTS5 BM25: negative values (closer to 0 = better)
 * - Vector cosine: 0.0–1.0 (1.0 = identical)
 * - Graph proximity: distance-based (closer = higher score)
 * - Metadata: binary match (1.0 if matches filter)
 *
 * Normalization is deterministic: same input → same output, always.
 */
class ScoreNormalizer {

    /**
     * Normalize FTS5 BM25 scores.
     * FTS5 bm25() returns negative values; typical range: -10 to 0.
     */
    fun normalizeLexical(hits: List<RetrievalHit>): List<RetrievalHit> {
        if (hits.isEmpty()) return hits

        val minScore = hits.minOf { it.score }
        val maxScore = hits.maxOf { it.score }
        val range = (maxScore - minScore).coerceAtLeast(0.001)

        return hits.map { hit ->
            val normalized = (hit.score - minScore) / range
            hit.copy(normalizedScore = normalized.coerceIn(0.0, 1.0))
        }
    }

    /**
     * Normalize vector cosine similarity scores.
     * Already in 0.0–1.0 range; just clamp.
     */
    fun normalizeVector(hits: List<RetrievalHit>): List<RetrievalHit> {
        return hits.map { hit ->
            hit.copy(normalizedScore = hit.score.coerceIn(0.0, 1.0))
        }
    }

    /**
     * Normalize graph proximity scores.
     * Graph scores are distance-based (closer = higher).
     */
    fun normalizeGraph(hits: List<RetrievalHit>): List<RetrievalHit> {
        if (hits.isEmpty()) return hits

        val maxDepth = hits.maxOfOrNull { it.graphDepth } ?: 1
        val depthRange = maxOf(maxDepth, 1).toDouble()

        return hits.map { hit ->
            val normalized = 1.0 - (hit.graphDepth.toDouble() / depthRange)
            hit.copy(normalizedScore = normalized.coerceIn(0.0, 1.0))
        }
    }

    /**
     * Normalize metadata filter scores.
     * Metadata hits are binary: 1.0 if they match, 0.0 otherwise.
     */
    fun normalizeMetadata(hits: List<RetrievalHit>): List<RetrievalHit> {
        return hits.map { hit ->
            hit.copy(normalizedScore = hit.score.coerceIn(0.0, 1.0))
        }
    }
}
