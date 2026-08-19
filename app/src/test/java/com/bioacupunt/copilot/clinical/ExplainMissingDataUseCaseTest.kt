package com.bioacupunt.copilot.clinical

import com.bioacupunt.mtc.knowledge.domain.*
import com.bioacupunt.prontuario.domain.model.ConfidenceLevel
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * §8 MISSING DATA EXPLANATION USE CASE TEST
 *
 * Tests that the use case produces deterministic explanations of missing data
 * without any LLM involvement.
 */
class ExplainMissingDataUseCaseTest {

    private lateinit var useCase: ExplainMissingDataUseCase

    @Before
    fun setup() {
        useCase = ExplainMissingDataUseCase()
    }

    // ── Basic explanation ───────────────────────────────────────────

    @Test
    fun explain_withMissingData_returnsExplanation() {
        val result = ClinicalIntelligenceResult(
            rankedHypotheses = emptyList(),
            supportingEvidence = emptyList(),
            contradictingEvidence = emptyList(),
            reasoningPaths = emptyList(),
            missingInformation = listOf(
                MissingDataItem("TONGUE", "Cor da língua", "Diferenciaria Calor de Frio", 1),
                MissingDataItem("PULSE", "Qualidades do pulso", "Diferenciaria Interior de Exterior", 2),
            ),
            confidence = ConfidenceLevel.MODERATE,
        )

        val explanation = useCase.explain(result)

        assertEquals(2, explanation.totalMissing)
        assertEquals(1, explanation.highestPriority)
        assertTrue(explanation.summary.contains("2"))
    }

    // ── Empty missing data ──────────────────────────────────────────

    @Test
    fun explain_noMissingData_reportsComplete() {
        val result = ClinicalIntelligenceResult(
            rankedHypotheses = emptyList(),
            supportingEvidence = emptyList(),
            contradictingEvidence = emptyList(),
            reasoningPaths = emptyList(),
            missingInformation = emptyList(),
            confidence = ConfidenceLevel.HIGH,
        )

        val explanation = useCase.explain(result)

        assertEquals(0, explanation.totalMissing)
        assertTrue(explanation.summary.contains("Todos os dados"))
    }

    // ── Priority ordering ───────────────────────────────────────────

    @Test
    fun explain_observationsAreSortedByPriority() {
        val result = ClinicalIntelligenceResult(
            rankedHypotheses = emptyList(),
            supportingEvidence = emptyList(),
            contradictingEvidence = emptyList(),
            reasoningPaths = emptyList(),
            missingInformation = listOf(
                MissingDataItem("HISTORY", "Histórico", "Impacto", 3),
                MissingDataItem("TONGUE", "Língua", "Impacto", 1),
                MissingDataItem("PULSE", "Pulso", "Impacto", 2),
            ),
            confidence = ConfidenceLevel.LOW,
        )

        val explanation = useCase.explain(result)

        // Should be sorted by priority: 1, 2, 3
        assertEquals(1, explanation.missingObservations[0].priority)
        assertEquals(2, explanation.missingObservations[1].priority)
        assertEquals(3, explanation.missingObservations[2].priority)
    }

    // ── Affected candidates ─────────────────────────────────────────

    @Test
    fun explain_withCandidates_reportsAffectedCandidates() {
        val result = ClinicalIntelligenceResult(
            rankedHypotheses = listOf(
                DifferentialCandidate(
                    entityId = "p1",
                    entityName = "Deficiência de Yin",
                    entityType = KnowledgeEntityType.PATTERN,
                    score = 0.8,
                    missingData = listOf(
                        MissingDataItem("TONGUE", "Cor da língua", "Diferencia A de B", 1),
                    ),
                ),
                DifferentialCandidate(
                    entityId = "p2",
                    entityName = "Excesso de Yang",
                    entityType = KnowledgeEntityType.PATTERN,
                    score = 0.5,
                    missingData = listOf(
                        MissingDataItem("TONGUE", "Cor da língua", "Diferencia A de B", 1),
                    ),
                ),
            ),
            supportingEvidence = emptyList(),
            contradictingEvidence = emptyList(),
            reasoningPaths = emptyList(),
            missingInformation = listOf(
                MissingDataItem("TONGUE", "Cor da língua", "Diferencia A de B", 1),
            ),
            confidence = ConfidenceLevel.MODERATE,
        )

        val explanation = useCase.explain(result)

        assertEquals(2, explanation.affectedCandidateCount)
        assertTrue(explanation.missingObservations[0].wouldDifferentiate.contains("Deficiência de Yin"))
        assertTrue(explanation.missingObservations[0].wouldDifferentiate.contains("Excesso de Yang"))
    }

    // ── High priority count ─────────────────────────────────────────

    @Test
    fun explain_highPriorityCount_inSummary() {
        val result = ClinicalIntelligenceResult(
            rankedHypotheses = emptyList(),
            supportingEvidence = emptyList(),
            contradictingEvidence = emptyList(),
            reasoningPaths = emptyList(),
            missingInformation = listOf(
                MissingDataItem("TONGUE", "Língua", "Impacto", 1),
                MissingDataItem("BAGANG", "Ba Gang", "Impacto", 1),
                MissingDataItem("PULSE", "Pulso", "Impacto", 2),
                MissingDataItem("HISTORY", "Histórico", "Impacto", 3),
            ),
            confidence = ConfidenceLevel.LOW,
        )

        val explanation = useCase.explain(result)

        // Priority ≤ 2 = high priority → 3 items
        assertTrue(explanation.summary.contains("3"))
        assertTrue(explanation.summary.contains("alta prioridade"))
    }

    // ── Deterministic ───────────────────────────────────────────────

    @Test
    fun explain_deterministic_sameInputSameOutput() {
        val result = ClinicalIntelligenceResult(
            rankedHypotheses = emptyList(),
            supportingEvidence = emptyList(),
            contradictingEvidence = emptyList(),
            reasoningPaths = emptyList(),
            missingInformation = listOf(
                MissingDataItem("TONGUE", "Cor da língua", "Impacto", 1),
            ),
            confidence = ConfidenceLevel.MODERATE,
        )

        val exp1 = useCase.explain(result)
        val exp2 = useCase.explain(result)

        assertEquals(exp1.totalMissing, exp2.totalMissing)
        assertEquals(exp1.highestPriority, exp2.highestPriority)
        assertEquals(exp1.summary, exp2.summary)
        assertEquals(exp1.missingObservations.size, exp2.missingObservations.size)
    }

    // ── Observation types ───────────────────────────────────────────

    @Test
    fun explain_preservesObservationTypes() {
        val result = ClinicalIntelligenceResult(
            rankedHypotheses = emptyList(),
            supportingEvidence = emptyList(),
            contradictingEvidence = emptyList(),
            reasoningPaths = emptyList(),
            missingInformation = listOf(
                MissingDataItem("TONGUE", "Cor da língua", "Impacto", 1),
                MissingDataItem("PULSE", "Qualidades do pulso", "Impacto", 2),
                MissingDataItem("BAGANG", "Classificação Ba Gang", "Impacto", 1),
            ),
            confidence = ConfidenceLevel.MODERATE,
        )

        val explanation = useCase.explain(result)

        val types = explanation.missingObservations.map { it.observationType }
        assertTrue(types.contains("TONGUE"))
        assertTrue(types.contains("PULSE"))
        assertTrue(types.contains("BAGANG"))
    }

    // ── Summary quality ─────────────────────────────────────────────

    @Test
    fun explain_summaryMentionsTypes() {
        val result = ClinicalIntelligenceResult(
            rankedHypotheses = emptyList(),
            supportingEvidence = emptyList(),
            contradictingEvidence = emptyList(),
            reasoningPaths = emptyList(),
            missingInformation = listOf(
                MissingDataItem("TONGUE", "Cor da língua", "Impacto", 1),
                MissingDataItem("PULSE", "Pulso", "Impacto", 2),
            ),
            confidence = ConfidenceLevel.MODERATE,
        )

        val explanation = useCase.explain(result)

        assertTrue("Summary should mention observation types",
            explanation.summary.contains("TONGUE") || explanation.summary.contains("PULSE"))
    }
}
