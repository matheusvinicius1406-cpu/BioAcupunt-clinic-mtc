package com.bioacupunt.copilot.clinical

import com.bioacupunt.mtc.knowledge.domain.ClinicalIntelligenceResult
import com.bioacupunt.mtc.knowledge.domain.MissingDataItem

/**
 * §8 MISSING DATA EXPLANATION USE CASE
 *
 * Answers: "What information is missing and why does it matter?"
 *
 * Uses MissingDataEngine output deterministically.
 * The LLM may explain this in natural language, but the STRUCTURE
 * comes from deterministic engines — never from LLM inference.
 *
 * Output: MissingDataExplanation with structured missing observations,
 * their impact on differential ranking, and priority for collection.
 */
class ExplainMissingDataUseCase {

    data class MissingDataExplanation(
        val missingObservations: List<MissingObservation>,
        val totalMissing: Int,
        val highestPriority: Int,
        val affectedCandidateCount: Int,
        val summary: String,
    )

    data class MissingObservation(
        val observationType: String,
        val description: String,
        val impact: String,
        val priority: Int,
        val wouldDifferentiate: List<String>, // candidate names that would be differentiated
    )

    /**
     * Explain missing data from clinical intelligence results.
     * Deterministic: same input → same output, always.
     */
    fun explain(result: ClinicalIntelligenceResult): MissingDataExplanation {
        val allMissing = result.missingInformation

        // Group by observation type for better organization
        val grouped = allMissing.groupBy { it.observationType }

        // For each missing observation, find which candidates it would differentiate
        val observations = allMissing.map { item ->
            val affectedCandidates = findAffectedCandidates(item, result)
            MissingObservation(
                observationType = item.observationType,
                description = item.description,
                impact = item.impact,
                priority = item.priority,
                wouldDifferentiate = affectedCandidates,
            )
        }.sortedBy { it.priority }

        val highestPriority = observations.minOfOrNull { it.priority } ?: 0
        val affectedCandidates = observations.flatMap { it.wouldDifferentiate }.distinct()

        val summary = buildSummary(observations, affectedCandidates.size)

        return MissingDataExplanation(
            missingObservations = observations,
            totalMissing = observations.size,
            highestPriority = highestPriority,
            affectedCandidateCount = affectedCandidates.size,
            summary = summary,
        )
    }

    /**
     * Find which candidates would be differentiated by a missing observation.
     * Looks at the missingData field of each candidate.
     */
    private fun findAffectedCandidates(
        item: MissingDataItem,
        result: ClinicalIntelligenceResult,
    ): List<String> {
        return result.rankedHypotheses
            .filter { candidate ->
                candidate.missingData.any { missing ->
                    missing.observationType == item.observationType &&
                        missing.description == item.description
                }
            }
            .map { it.entityName }
            .distinct()
    }

    private fun buildSummary(observations: List<MissingObservation>, affectedCount: Int): String {
        if (observations.isEmpty()) {
            return "Todos os dados clínicos relevantes foram coletados."
        }

        val highPriority = observations.count { it.priority <= 2 }
        val types = observations.map { it.observationType }.distinct().joinToString(", ")

        return buildString {
            append("${observations.size} observação(ões) faltando ($types).")
            if (highPriority > 0) {
                append(" $highPriority de alta prioridade.")
            }
            if (affectedCount > 0) {
                append(" $affectedCount candidato(s) no diferencial poderia(m) ser diferenciado(s).")
            }
        }
    }
}
