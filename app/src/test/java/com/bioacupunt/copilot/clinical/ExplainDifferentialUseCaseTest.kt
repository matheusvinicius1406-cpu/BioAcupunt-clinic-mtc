package com.bioacupunt.copilot.clinical

import com.bioacupunt.mtc.knowledge.domain.*
import com.bioacupunt.prontuario.domain.model.ConfidenceLevel
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * §6 DIFFERENTIAL EXPLANATION USE CASE TEST
 *
 * Tests that the use case produces deterministic, structured explanations
 * without any LLM involvement.
 */
class ExplainDifferentialUseCaseTest {

    private lateinit var useCase: ExplainDifferentialUseCase

    @Before
    fun setup() {
        useCase = ExplainDifferentialUseCase()
    }

    // ── Basic explanation ───────────────────────────────────────────

    @Test
    fun explain_withTwoCandidates_returnsExplanation() {
        val result = buildResult(
            candidateA = DifferentialCandidate(
                entityId = "pattern.yin_deficiency",
                entityName = "Deficiência de Yin",
                entityType = KnowledgeEntityType.PATTERN,
                score = 0.8,
                supportingTraces = listOf(
                    EvidenceTrace(
                        entityId = "ev.1",
                        entityName = "Insônia",
                        entityType = KnowledgeEntityType.SYMPTOM,
                        supportingEvidence = listOf(
                            EvidenceItem(id = "ev.1", claim = "Insônia关联Deficiência de Yin", level = "TRADITION", confidence = 0.7),
                        ),
                    ),
                ),
                reasoningPaths = listOf(
                    GraphPath(
                        edges = listOf(GraphEdge("pattern.yin_deficiency", KnowledgeRelationType.HAS_SYMPTOM, "symptom.insomnia")),
                        entityIds = listOf("pattern.yin_deficiency", "symptom.insomnia"),
                    ),
                ),
            ),
            candidateB = DifferentialCandidate(
                entityId = "pattern.yang_excess",
                entityName = "Excesso de Yang",
                entityType = KnowledgeEntityType.PATTERN,
                score = 0.5,
                supportingTraces = emptyList(),
            ),
        )

        val explanation = useCase.explain(result, "pattern.yin_deficiency", "pattern.yang_excess")

        assertNotNull(explanation)
        assertEquals("Deficiência de Yin", explanation!!.candidateA)
        assertEquals("Excesso de Yang", explanation.candidateB)
        assertEquals(0.3, explanation.rankingDifference, 0.01)
        assertEquals(0.8, explanation.scoreA, 0.01)
        assertEquals(0.5, explanation.scoreB, 0.01)
    }

    // ── Evidence summaries ──────────────────────────────────────────

    @Test
    fun explain_withSupportingEvidence_reportsEvidenceSummary() {
        val result = buildResult(
            candidateA = DifferentialCandidate(
                entityId = "p1",
                entityName = "Padrão A",
                entityType = KnowledgeEntityType.PATTERN,
                score = 0.9,
                supportingTraces = listOf(
                    EvidenceTrace(
                        entityId = "ev.1",
                        entityName = "Ev A",
                        entityType = KnowledgeEntityType.SYMPTOM,
                        supportingEvidence = listOf(
                            EvidenceItem(id = "ev.1", claim = "Claim 1", level = "TRADITION", confidence = 0.8),
                            EvidenceItem(id = "ev.2", claim = "Claim 2", level = "MODERN_LITERATURE", confidence = 0.9),
                        ),
                    ),
                ),
            ),
            candidateB = DifferentialCandidate(
                entityId = "p2",
                entityName = "Padrão B",
                entityType = KnowledgeEntityType.PATTERN,
                score = 0.3,
                supportingTraces = emptyList(),
            ),
        )

        val explanation = useCase.explain(result, "p1", "p2")!!

        assertEquals(1, explanation.supportingEvidenceForA.size)
        assertEquals(2, explanation.supportingEvidenceForA[0].evidenceCount)
        assertTrue(explanation.supportingEvidenceForA[0].avgConfidence > 0)
        assertEquals(0, explanation.supportingEvidenceForB.size)
    }

    // ── Contradictions ──────────────────────────────────────────────

    @Test
    fun explain_withContradictions_reportsContradictions() {
        val result = buildResult(
            candidateA = DifferentialCandidate(
                entityId = "p1",
                entityName = "Padrão A",
                entityType = KnowledgeEntityType.PATTERN,
                score = 0.6,
                contradictingTraces = listOf(
                    EvidenceTrace(
                        entityId = "ev.1",
                        entityName = "Ev Contradicting",
                        entityType = KnowledgeEntityType.SYMPTOM,
                        contradictingEvidence = listOf(
                            EvidenceItem(id = "ev.1", claim = "Contradiction", level = "TRADITION", confidence = 0.5),
                        ),
                    ),
                ),
            ),
            candidateB = DifferentialCandidate(
                entityId = "p2",
                entityName = "Padrão B",
                entityType = KnowledgeEntityType.PATTERN,
                score = 0.3,
            ),
        )

        val explanation = useCase.explain(result, "p1", "p2")!!

        assertEquals(1, explanation.contradictionsForA.size)
        assertEquals(1, explanation.contradictionsForA[0].evidenceCount)
    }

    // ── Reasoning paths ─────────────────────────────────────────────

    @Test
    fun explain_withReasoningPaths_formatsPaths() {
        val result = buildResult(
            candidateA = DifferentialCandidate(
                entityId = "p1",
                entityName = "Padrão A",
                entityType = KnowledgeEntityType.PATTERN,
                score = 0.7,
                reasoningPaths = listOf(
                    GraphPath(
                        edges = emptyList(),
                        entityIds = listOf("p1", "symptom.x", "pattern.y"),
                    ),
                ),
            ),
            candidateB = DifferentialCandidate(
                entityId = "p2",
                entityName = "Padrão B",
                entityType = KnowledgeEntityType.PATTERN,
                score = 0.3,
            ),
        )

        val explanation = useCase.explain(result, "p1", "p2")!!

        assertEquals(1, explanation.reasoningPathsA.size)
        assertTrue(explanation.reasoningPathsA[0].contains("→"))
    }

    // ── Missing information ─────────────────────────────────────────

    @Test
    fun explain_withMissingData_reportsMissingInfo() {
        val result = buildResult(
            candidateA = DifferentialCandidate(
                entityId = "p1",
                entityName = "Padrão A",
                entityType = KnowledgeEntityType.PATTERN,
                score = 0.7,
                missingData = listOf(
                    MissingDataItem("TONGUE", "Cor da língua", "Diferenciaria A de B", 1),
                ),
            ),
            candidateB = DifferentialCandidate(
                entityId = "p2",
                entityName = "Padrão B",
                entityType = KnowledgeEntityType.PATTERN,
                score = 0.3,
            ),
        )

        val explanation = useCase.explain(result, "p1", "p2")!!

        assertEquals(1, explanation.missingInformationA.size)
        assertEquals("Cor da língua", explanation.missingInformationA[0])
    }

    // ── Candidate not found ─────────────────────────────────────────

    @Test
    fun explain_candidateNotFound_returnsNull() {
        val result = buildResult(
            candidateA = DifferentialCandidate(
                entityId = "p1", entityName = "A", entityType = KnowledgeEntityType.PATTERN, score = 0.7,
            ),
        )

        val explanation = useCase.explain(result, "p1", "nonexistent")
        assertNull(explanation)
    }

    @Test
    fun explain_bothNotFound_returnsNull() {
        val result = buildResult()
        val explanation = useCase.explain(result, "nonexistent1", "nonexistent2")
        assertNull(explanation)
    }

    // ── Deterministic ───────────────────────────────────────────────

    @Test
    fun explain_deterministic_sameInputSameOutput() {
        val result = buildResult(
            candidateA = DifferentialCandidate(
                entityId = "p1", entityName = "A", entityType = KnowledgeEntityType.PATTERN, score = 0.8,
                supportingTraces = listOf(
                    EvidenceTrace("ev.1", "Ev", KnowledgeEntityType.SYMPTOM,
                        supportingEvidence = listOf(EvidenceItem("ev.1", "Claim", confidence = 0.7))),
                ),
            ),
            candidateB = DifferentialCandidate(
                entityId = "p2", entityName = "B", entityType = KnowledgeEntityType.PATTERN, score = 0.4,
            ),
        )

        val exp1 = useCase.explain(result, "p1", "p2")!!
        val exp2 = useCase.explain(result, "p1", "p2")!!

        assertEquals(exp1.rankingDifference, exp2.rankingDifference, 0.001)
        assertEquals(exp1.scoreA, exp2.scoreA, 0.001)
        assertEquals(exp1.scoreB, exp2.scoreB, 0.001)
        assertEquals(exp1.supportingEvidenceForA.size, exp2.supportingEvidenceForA.size)
        assertEquals(exp1.confidence, exp2.confidence)
    }

    // ── LLM cannot alter ranking ────────────────────────────────────

    @Test
    fun explain_scoresAreFromEngine_notInvented() {
        // The explanation must reflect the scores from the engine,
        // not any modified version
        val result = buildResult(
            candidateA = DifferentialCandidate(
                entityId = "p1", entityName = "A", entityType = KnowledgeEntityType.PATTERN, score = 0.95,
            ),
            candidateB = DifferentialCandidate(
                entityId = "p2", entityName = "B", entityType = KnowledgeEntityType.PATTERN, score = 0.10,
            ),
        )

        val explanation = useCase.explain(result, "p1", "p2")!!

        // Scores must exactly match the engine's scores
        assertEquals(0.95, explanation.scoreA, 0.001)
        assertEquals(0.10, explanation.scoreB, 0.001)
        assertEquals(0.85, explanation.rankingDifference, 0.001)
    }

    // ── Helper ──────────────────────────────────────────────────────

    private fun buildResult(
        candidateA: DifferentialCandidate? = null,
        candidateB: DifferentialCandidate? = null,
        vararg extraCandidates: DifferentialCandidate,
    ): ClinicalIntelligenceResult {
        val allCandidates = mutableListOf<DifferentialCandidate>()
        candidateA?.let { allCandidates.add(it) }
        candidateB?.let { allCandidates.add(it) }
        allCandidates.addAll(extraCandidates)
        val candidates = allCandidates.toList()
        return ClinicalIntelligenceResult(
            rankedHypotheses = candidates,
            supportingEvidence = candidates.flatMap { it.supportingTraces },
            contradictingEvidence = candidates.flatMap { it.contradictingTraces },
            reasoningPaths = candidates.flatMap { it.reasoningPaths },
            missingInformation = candidates.flatMap { it.missingData },
            confidence = ConfidenceLevel.MODERATE,
        )
    }
}
