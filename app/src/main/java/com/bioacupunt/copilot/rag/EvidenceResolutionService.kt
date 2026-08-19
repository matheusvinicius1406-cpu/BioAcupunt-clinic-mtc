package com.bioacupunt.copilot.rag

import com.bioacupunt.mtc.knowledge.domain.EvidenceResolver

/**
 * §21 EVIDENCE RESOLUTION SERVICE
 *
 * Reuses the existing [EvidenceResolver] from Phase 3 for the RAG pipeline.
 * Never creates a second evidence resolution mechanism.
 *
 * Flow:
 * ```text
 * Retrieved Result
 *     ↓
 * Evidence IDs
 *     ↓
 * KnowledgeEvidence
 *     ↓
 * Citation
 *     ↓
 * Source
 *     ↓
 * Provenance
 * ```
 */
class EvidenceResolutionService(
    private val evidenceResolver: EvidenceResolver,
) {

    data class ResolvedEvidence(
        val evidenceId: String,
        val claim: String? = null,
        val level: String? = null,
        val confidence: Double? = null,
        val citations: List<String> = emptyList(),
        val sourceCount: Int = 0,
    )

    /**
     * Resolve evidence IDs from retrieval results into full evidence traces.
     * Delegates to the existing EvidenceResolver — no duplicate logic.
     */
    suspend fun resolve(evidenceIds: List<String>): List<ResolvedEvidence> {
        if (evidenceIds.isEmpty()) return emptyList()

        return evidenceIds.mapNotNull { id ->
            try {
                val evidence = evidenceResolver.resolveEvidence(id)
                if (evidence != null) {
                    ResolvedEvidence(
                        evidenceId = id,
                        claim = evidence.claim,
                        level = evidence.level,
                        confidence = evidence.confidence,
                        citations = evidence.citations.map { it.excerpt ?: it.locator ?: it.citationId },
                        sourceCount = evidence.sourceCount,
                    )
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * Resolve evidence for a single retrieval hit.
     */
    suspend fun resolveForHit(hit: com.bioacupunt.copilot.retrieval.RetrievalHit): ResolvedEvidence? {
        if (hit.evidenceIds.isEmpty()) return null
        return resolve(hit.evidenceIds).firstOrNull()
    }
}
