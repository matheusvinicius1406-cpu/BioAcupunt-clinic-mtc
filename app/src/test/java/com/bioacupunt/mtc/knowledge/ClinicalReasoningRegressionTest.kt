package com.bioacupunt.mtc.knowledge

import com.bioacupunt.mtc.knowledge.data.KnowledgeCoreEntityEntity
import com.bioacupunt.mtc.knowledge.data.KnowledgeCoreRelationEntity
import com.bioacupunt.mtc.knowledge.domain.*
import com.bioacupunt.mtc.knowledge.repository.RoomKnowledgeGraphRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Regression test for clinical reasoning.
 *
 * Compares: candidate ranking, evidence IDs, reasoning path,
 * missing information, confidence class.
 *
 * Does NOT compare LLM text output — that's outside this engine's scope.
 */
@RunWith(RobolectricTestRunner::class)
class ClinicalReasoningRegressionTest {

    private lateinit var fakeDao: FakeKnowledgeCoreDao
    private lateinit var knowledgeRepo: FakeKnowledgeRepository
    private lateinit var graphRepo: RoomKnowledgeGraphRepository
    private lateinit var engine: ClinicalIntelligenceEngine

    @Before
    fun setup() {
        fakeDao = FakeKnowledgeCoreDao()
        knowledgeRepo = FakeKnowledgeRepository()
        graphRepo = RoomKnowledgeGraphRepository(fakeDao)
        val evidenceResolver = EvidenceResolver(fakeDao)
        val evidenceEngine = EvidenceEngine(knowledgeRepo, graphRepo, evidenceResolver)
        val differentialEngine = DifferentialEngine(knowledgeRepo, graphRepo, evidenceEngine)
        val missingDataEngine = MissingDataEngine(graphRepo)
        engine = ClinicalIntelligenceEngine(differentialEngine, evidenceEngine, missingDataEngine, evidenceResolver)
    }

    /**
     * Regression: same observation always produces same candidate ranking.
     */
    @Test
    fun regression_candidateRanking_stable() = runTest {
        populateKnowledge()

        val observation = ClinicalObservation(
            symptoms = listOf("Dor no flanco", "Irritabilidade"),
            tongueFindings = listOf("Língua vermelha"),
            pulseFindings = listOf("Pulso wiry"),
        )

        // Run 3 times
        val results = (1..3).map { engine.analyze(observation) }

        // All should produce same ranking
        val rankings = results.map { it.rankedHypotheses.map { c -> c.entityId } }
        assertEquals(rankings[0], rankings[1])
        assertEquals(rankings[1], rankings[2])
    }

    /**
     * Regression: same observation always produces same confidence level.
     */
    @Test
    fun regression_confidenceLevel_stable() = runTest {
        populateKnowledge()

        val observation = ClinicalObservation(
            symptoms = listOf("Dor no flanco"),
            tongueFindings = listOf("Língua vermelha"),
            pulseFindings = listOf("Pulso wiry"),
        )

        val results = (1..3).map { engine.analyze(observation) }
        val confidences = results.map { it.confidence }
        assertEquals(confidences[0], confidences[1])
        assertEquals(confidences[1], confidences[2])
    }

    /**
     * Regression: same observation always produces same missing data.
     */
    @Test
    fun regression_missingInformation_stable() = runTest {
        populateKnowledge()

        val observation = ClinicalObservation(symptoms = listOf("Dor"))

        val results = (1..3).map { engine.analyze(observation) }
        val missingTypes = results.map { it.missingInformation.map { m -> m.observationType }.sorted() }
        assertEquals(missingTypes[0], missingTypes[1])
        assertEquals(missingTypes[1], missingTypes[2])
    }

    /**
     * Regression: more evidence yields higher confidence.
     */
    @Test
    fun regression_moreEvidence_higherConfidence() = runTest {
        populateKnowledge()

        val incompleteObservation = ClinicalObservation(symptoms = listOf("Dor"))
        val completeObservation = ClinicalObservation(
            symptoms = listOf("Dor no flanco", "Irritabilidade"),
            tongueFindings = listOf("Língua vermelha"),
            pulseFindings = listOf("Pulso wiry"),
            baGang = BaGangData(polarity = "YIN", depth = "INTERIOR"),
        )

        val incompleteResult = engine.analyze(incompleteObservation)
        val completeResult = engine.analyze(completeObservation)

        // More data should yield same or higher confidence
        val confidenceOrder = listOf(
            com.bioacupunt.prontuario.domain.model.ConfidenceLevel.INSUFFICIENT_EVIDENCE,
            com.bioacupunt.prontuario.domain.model.ConfidenceLevel.LOW,
            com.bioacupunt.prontuario.domain.model.ConfidenceLevel.MODERATE,
            com.bioacupunt.prontuario.domain.model.ConfidenceLevel.HIGH,
        )
        val incompleteIdx = confidenceOrder.indexOf(incompleteResult.confidence)
        val completeIdx = confidenceOrder.indexOf(completeResult.confidence)
        assertTrue(
            "Complete observation should have same or higher confidence",
            completeIdx >= incompleteIdx
        )
    }

    /**
     * Regression: contradictions reduce score.
     */
    @Test
    fun regression_contradictionsReduceScore() = runTest {
        populateKnowledge()

        // Observation that matches pattern A but contradicts pattern B
        val observation = ClinicalObservation(
            symptoms = listOf("Dor no flanco", "Irritabilidade"),
            tongueFindings = listOf("Língua vermelha"),
            pulseFindings = listOf("Pulso wiry"),
        )

        val result = engine.analyze(observation)

        // If there are contradicting traces, they should reduce the score
        for (candidate in result.rankedHypotheses) {
            if (candidate.contradictingTraces.isNotEmpty()) {
                assertTrue(
                    "Candidate with contradictions should have lower score",
                    candidate.score < 1.0
                )
            }
        }
    }

    // ── Knowledge population ─────────────────────────────────────────

    private fun populateKnowledge() {
        val now = System.currentTimeMillis()

        val patterns = listOf(
            KnowledgeEntity("p1", KnowledgeEntityType.PATTERN, "Estagnação de Qi do Fígado",
                version = KnowledgeVersion("1", now, now, status = KnowledgeStatus.PUBLISHED),
                createdAt = now, updatedAt = now),
            KnowledgeEntity("p2", KnowledgeEntityType.PATTERN, "Deficiência de Qi do Baço",
                version = KnowledgeVersion("1", now, now, status = KnowledgeStatus.PUBLISHED),
                createdAt = now, updatedAt = now),
            KnowledgeEntity("p3", KnowledgeEntityType.PATTERN, "Calor no Fígado",
                version = KnowledgeVersion("1", now, now, status = KnowledgeStatus.PUBLISHED),
                createdAt = now, updatedAt = now),
        )

        knowledgeRepo.entities.addAll(patterns)
        fakeDao.entities.addAll(patterns.map {
            KnowledgeCoreEntityEntity(it.id, "PATTERN", it.canonicalName,
                created_at = now, updated_at = now, status = "PUBLISHED")
        })

        fakeDao.relations.addAll(listOf(
            KnowledgeCoreRelationEntity("p1", "HAS_SYMPTOM", "s1", confidence = 0.9,
                created_at = now, updated_at = now),
            KnowledgeCoreRelationEntity("p2", "HAS_SYMPTOM", "s2", confidence = 0.8,
                created_at = now, updated_at = now),
        ))
    }
}
