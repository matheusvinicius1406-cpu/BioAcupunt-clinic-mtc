package com.bioacupunt.copilot.clinical

import com.bioacupunt.mtc.knowledge.domain.ClinicalIntelligenceEngine
import com.bioacupunt.mtc.knowledge.domain.ClinicalIntelligenceResult
import com.bioacupunt.mtc.knowledge.domain.ClinicalObservation
import com.bioacupunt.mtc.knowledge.domain.RunClinicalIntelligenceUseCase
import com.bioacupunt.prontuario.domain.model.MtcAssessment

/**
 * §27 CLINICAL INTELLIGENCE INTEGRATION
 *
 * Connects Phase 3's ClinicalIntelligenceEngine to the RAG pipeline.
 * The clinical intelligence output enriches retrieval context with:
 * - Differential hypotheses
 * - Evidence quality assessment
 * - Missing data gaps
 *
 * Flow:
 * ```text
 * ClinicalObservation
 *     ↓
 * ClinicalIntelligence (differential + evidence + missing data)
 *     ↓
 * Enriched context for HybridRetriever
 *     ↓
 * LLM explanation (using GroundedResponseGenerator)
 *     ↓
 * Validated response
 * ```
 *
 * The LLM CANNOT alter: candidate ranking, scores, evidence, reasoning paths.
 * These come deterministically from Phase 3 engines.
 */
class ClinicalIntelligenceIntegration(
    private val clinicalIntelligenceEngine: ClinicalIntelligenceEngine,
    private val runClinicalIntelligenceUseCase: RunClinicalIntelligenceUseCase,
) {

    data class ClinicalEnrichment(
        val differentialHypotheses: List<String>,
        val evidenceQuality: String,
        val missingData: List<String>,
        val recommendedSearchTerms: List<String>,
        val supportingEvidenceCount: Int = 0,
        val contradictingEvidenceCount: Int = 0,
    )

    /**
     * Run clinical intelligence and produce enrichment for the RAG pipeline.
     * Takes a clinical observation and returns structured enrichment data.
     */
    suspend fun enrich(
        assessment: MtcAssessment,
    ): ClinicalEnrichment {
        return try {
            val result = runClinicalIntelligenceUseCase(assessment)

            ClinicalEnrichment(
                differentialHypotheses = result.rankedHypotheses.map { it.entityName },
                evidenceQuality = result.confidence.name,
                missingData = result.missingInformation.map { it.description },
                recommendedSearchTerms = generateSearchTerms(result),
                supportingEvidenceCount = result.supportingEvidence.size,
                contradictingEvidenceCount = result.contradictingEvidence.size,
            )
        } catch (e: Exception) {
            ClinicalEnrichment(
                differentialHypotheses = emptyList(),
                evidenceQuality = "UNKNOWN",
                missingData = emptyList(),
                recommendedSearchTerms = emptyList(),
            )
        }
    }

    /**
     * Generate search terms from clinical intelligence results
     * to improve retrieval queries.
     */
    private fun generateSearchTerms(
        result: ClinicalIntelligenceResult,
    ): List<String> {
        val terms = mutableListOf<String>()

        // Add differential candidate names as search terms
        for (candidate in result.rankedHypotheses.take(3)) {
            terms.add(candidate.entityName)
        }

        // Add evidence-backed entity names
        for (evidence in result.supportingEvidence.take(3)) {
            terms.add(evidence.entityName)
        }

        return terms.distinct()
    }
}
