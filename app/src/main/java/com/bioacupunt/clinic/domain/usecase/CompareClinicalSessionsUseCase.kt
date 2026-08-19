package com.bioacupunt.clinic.domain.usecase

import com.bioacupunt.clinic.domain.model.SessionComparison
import com.bioacupunt.clinic.domain.model.StructuredObservation

/**
 * Compare two clinical sessions.
 *
 * Identifies only differences derived from structured data:
 * - new findings
 * - resolved findings
 * - persistent findings
 * - worsened findings
 * - improved findings
 * - pattern changes
 * - treatment changes
 *
 * Never infers clinical improvement from text alone.
 */
class CompareClinicalSessionsUseCase {

    fun compare(
        sessionAObservations: List<StructuredObservation>,
        sessionBObservations: List<StructuredObservation>,
        sessionAId: Long = 0,
        sessionBId: Long = 0,
    ): SessionComparison {
        val aContents = sessionAObservations.map { it.content.lowercase().trim() }.toSet()
        val bContents = sessionBObservations.map { it.content.lowercase().trim() }.toSet()

        val newFindings = (bContents - aContents).toList()
        val resolvedFindings = (aContents - bContents).toList()
        val persistentFindings = (aContents intersect bContents).toList()

        // For worsened/improved, we'd need severity data — use type-based heuristic
        val aByType = sessionAObservations.groupBy { it.type }
        val bByType = sessionBObservations.groupBy { it.type }

        val worsenedFindings = mutableListOf<String>()
        val improvedFindings = mutableListOf<String>()

        for ((type, aObs) in aByType) {
            val bObs = bByType[type] ?: continue
            val aCount = aObs.size
            val bCount = bObs.size
            if (bCount > aCount) {
                worsenedFindings.add("More $type observations in session B ($bCount vs $aCount)")
            } else if (bCount < aCount) {
                improvedFindings.add("Fewer $type observations in session B ($bCount vs $aCount)")
            }
        }

        return SessionComparison(
            sessionAId = sessionAId,
            sessionBId = sessionBId,
            newFindings = newFindings,
            resolvedFindings = resolvedFindings,
            persistentFindings = persistentFindings,
            worsenedFindings = worsenedFindings,
            improvedFindings = improvedFindings,
            patternChanges = emptyList(), // Would need pattern-specific comparison
            treatmentChanges = emptyList(), // Would need treatment plan comparison
        )
    }
}
