package com.bioacupunt.mtc.knowledge.domain

/**
 * Clinical Intelligence Engine — orchestrates the full clinical intelligence pipeline.
 *
 * Pipeline:
 * ClinicalObservation → Knowledge Graph → Evidence → Differential → Missing Data
 * → Structured Result → Human Review
 *
 * This is a Clinical Decision Support system — NOT autonomous diagnosis.
 * The LLM may later explain the result, but it never generates the ranking.
 *
 * R1/R2/R4 intact: deterministic Kotlin, no LLM calls in the reasoning path.
 */
class ClinicalIntelligenceEngine(
    private val differentialEngine: DifferentialEngine,
    private val evidenceEngine: EvidenceEngine,
    private val missingDataEngine: MissingDataEngine,
    private val evidenceResolver: EvidenceResolver,
) {

    /**
     * Run the full clinical intelligence pipeline.
     * Returns a structured result for human review.
     */
    suspend fun analyze(observation: ClinicalObservation): ClinicalIntelligenceResult {
        // 1. Run differential analysis (generates candidates, scores, ranks)
        val differential = differentialEngine.analyze(observation)

        // 2. Collect all evidence traces
        val allSupporting = differential.candidates
            .flatMap { it.supportingTraces }
            .distinctBy { it.entityId }
        val allContradicting = differential.candidates
            .flatMap { it.contradictingTraces }
            .distinctBy { it.entityId }

        // 3. Collect reasoning paths
        val allPaths = differential.candidates
            .flatMap { it.reasoningPaths }
            .distinct()

        return ClinicalIntelligenceResult(
            rankedHypotheses = differential.candidates,
            supportingEvidence = allSupporting,
            contradictingEvidence = allContradicting,
            reasoningPaths = allPaths,
            missingInformation = differential.missingInformation,
            confidence = differential.confidence,
            knowledgeVersion = "1.0.0", // Will come from KnowledgeVersion
            timestamp = System.currentTimeMillis(),
        )
    }
}
