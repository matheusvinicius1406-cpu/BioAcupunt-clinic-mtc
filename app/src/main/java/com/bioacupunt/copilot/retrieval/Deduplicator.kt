package com.bioacupunt.copilot.retrieval

/**
 * §14 DEDUPLICATOR
 *
 * Removes duplicate results from multiple retrieval backends.
 * An entity found by BM25, vector, AND graph should appear ONCE in the final result.
 *
 * Deduplication key: entityId (canonical ID).
 * When duplicates exist, keeps the one with the highest score.
 */
class Deduplicator {

    /**
     * Deduplicate a list of retrieval hits.
     * Same entity from multiple sources → keep highest score, merge source info.
     */
    fun deduplicate(hits: List<RetrievalHit>): List<RetrievalHit> {
        val grouped = mutableMapOf<String, MutableList<RetrievalHit>>()

        for (hit in hits) {
            val key = hit.entityId.ifBlank { hit.documentId ?: hit.content.hashCode().toString() }
            grouped.getOrPut(key) { mutableListOf() }.add(hit)
        }

        return grouped.map { (key, duplicates) ->
            if (duplicates.size == 1) {
                duplicates.first()
            } else {
                mergeDuplicates(key, duplicates)
            }
        }
    }

    /**
     * Merge multiple hits for the same entity.
     * Strategy: keep highest score, combine evidence IDs, prefer entity with content.
     */
    private fun mergeDuplicates(entityId: String, hits: List<RetrievalHit>): RetrievalHit {
        val best = hits.maxByOrNull { it.normalizedScore }!!

        // Combine evidence IDs from all sources
        val allEvidenceIds = hits.flatMap { it.evidenceIds }.distinct()

        // Combine source types
        val sources = hits.map { it.sourceType }.distinct()

        // Prefer entity with content
        val entityWithContent = hits.firstOrNull { it.entity != null }?.entity

        return best.copy(
            entityId = entityId,
            entity = entityWithContent ?: best.entity,
            evidenceIds = allEvidenceIds,
            sourceType = if (sources.size > 1) RetrievalSource.HYBRID else best.sourceType,
            metadata = best.metadata + mapOf(
                "sources" to sources.joinToString(",") { it.name },
                "duplicateCount" to hits.size.toString(),
            ),
        )
    }
}
