package com.bioacupunt.copilot.clinical

import com.bioacupunt.mtc.knowledge.domain.ClinicalIntelligenceResult
import com.bioacupunt.mtc.knowledge.domain.DifferentialCandidate

/**
 * §6 DIFFERENTIAL EXPLANATION USE CASE
 *
 * Answers the question: "Why is candidate A ranked above candidate B?"
 *
 * Input: ClinicalIntelligenceResult + candidate A + candidate B
 * Output: DifferentialExplanation (deterministic — no LLM)
 *
 * The LLM may later transform this into natural language, but the STRUCTURE
 * exists before any LLM call. The LLM CANNOT alter ranking, scores, or evidence.
 */
class ExplainDifferentialUseCase {

    data class DifferentialExplanation(
        val candidateA: String,
        val candidateB: String,
        val rankingDifference: Double,
        val scoreA: Double,
        val scoreB: Double,
        val supportingEvidenceForA: List<EvidenceSummary>,
        val supportingEvidenceForB: List<EvidenceSummary>,
        val contradictionsForA: List<EvidenceSummary>,
        val contradictionsForB: List<EvidenceSummary>,
        val reasoningPathsA: List<String>,
        val reasoningPathsB: List<String>,
        val missingInformationA: List<String>,
        val missingInformationB: List<String>,
        val confidence: String,
    )

    data class EvidenceSummary(
        val entityId: String,
        val entityName: String,
        val evidenceCount: Int,
        val avgConfidence: Double,
        val levels: List<String>,
    )

    /**
     * Explain why candidate A ranks above candidate B.
     * Deterministic: same input → same output, always.
     *
     * @param result The complete clinical intelligence result
     * @param entityA Entity ID of the higher-ranked candidate
     * @param entityB Entity ID of the lower-ranked candidate
     * @return DifferentialExplanation with structured comparison, or null if candidates not found
     */
    fun explain(
        result: ClinicalIntelligenceResult,
        entityA: String,
        entityB: String,
    ): DifferentialExplanation? {
        val candidateA = result.rankedHypotheses.find { it.entityId == entityA }
        val candidateB = result.rankedHypotheses.find { it.entityId == entityB }

        if (candidateA == null || candidateB == null) return null

        val scoreDiff = candidateA.score - candidateB.score

        return DifferentialExplanation(
            candidateA = candidateA.entityName,
            candidateB = candidateB.entityName,
            rankingDifference = scoreDiff,
            scoreA = candidateA.score,
            scoreB = candidateB.score,
            supportingEvidenceForA = summarizeEvidence(candidateA.supportingTraces),
            supportingEvidenceForB = summarizeEvidence(candidateB.supportingTraces),
            contradictionsForA = summarizeEvidence(candidateA.contradictingTraces),
            contradictionsForB = summarizeEvidence(candidateB.contradictingTraces),
            reasoningPathsA = candidateA.reasoningPaths.map { formatPath(it) },
            reasoningPathsB = candidateB.reasoningPaths.map { formatPath(it) },
            missingInformationA = candidateA.missingData.map { it.description },
            missingInformationB = candidateB.missingData.map { it.description },
            confidence = result.confidence.name,
        )
    }

    private fun summarizeEvidence(
        traces: List<com.bioacupunt.mtc.knowledge.domain.EvidenceTrace>,
    ): List<EvidenceSummary> {
        return traces.map { trace ->
            val allEvidence = trace.supportingEvidence + trace.contradictingEvidence
            val avgConf = if (allEvidence.isNotEmpty()) {
                allEvidence.mapNotNull { it.confidence }.average()
            } else 0.0

            EvidenceSummary(
                entityId = trace.entityId,
                entityName = trace.entityName,
                evidenceCount = allEvidence.size,
                avgConfidence = avgConf,
                levels = allEvidence.mapNotNull { it.level }.distinct(),
            )
        }
    }

    private fun formatPath(path: com.bioacupunt.mtc.knowledge.domain.GraphPath): String {
        return path.entityIds.joinToString(" → ")
    }
}
