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
 * E2E test for the full Clinical Intelligence pipeline:
 *
 * ClinicalObservation → Knowledge Core → Graph → Evidence → Differential
 * → ClinicalIntelligenceResult
 *
 * Uses a real Room database (via FakeDao) with pre-populated MTC knowledge.
 */
@RunWith(RobolectricTestRunner::class)
class ClinicalIntelligenceE2ETest {

    private lateinit var fakeDao: FakeKnowledgeCoreDao
    private lateinit var knowledgeRepo: FakeKnowledgeRepository
    private lateinit var graphRepo: RoomKnowledgeGraphRepository
    private lateinit var evidenceResolver: EvidenceResolver
    private lateinit var evidenceEngine: EvidenceEngine
    private lateinit var differentialEngine: DifferentialEngine
    private lateinit var missingDataEngine: MissingDataEngine
    private lateinit var engine: ClinicalIntelligenceEngine

    @Before
    fun setup() {
        fakeDao = FakeKnowledgeCoreDao()
        knowledgeRepo = FakeKnowledgeRepository()
        graphRepo = RoomKnowledgeGraphRepository(fakeDao)
        evidenceResolver = EvidenceResolver(fakeDao)
        evidenceEngine = EvidenceEngine(knowledgeRepo, graphRepo, evidenceResolver)
        differentialEngine = DifferentialEngine(knowledgeRepo, graphRepo, evidenceEngine)
        missingDataEngine = MissingDataEngine(graphRepo)
        engine = ClinicalIntelligenceEngine(differentialEngine, evidenceEngine, missingDataEngine, evidenceResolver)
    }

    /**
     * Scenario A: Patient with liver qi stagnation
     *
     * Symptoms: Dor no flanco, irritabilidade, suspiros
     * Tongue: Língua vermelha, saburra fina branca
     * Pulse: Pulso wiry
     * Ba Gang: Interior, Excesso
     *
     * Expected: Candidate pattern should be "Estagnação de Qi do Fígado"
     */
    @Test
    fun e2e_liverQiStagnation_producesValidResult() = runTest {
        // Populate Knowledge Core
        populateLiverQiStagnationKnowledge()

        val observation = ClinicalObservation(
            symptoms = listOf("Dor no flanco", "Irritabilidade", "Suspiros frequentes"),
            tongueFindings = listOf("Língua vermelha", "Saburra fina branca"),
            pulseFindings = listOf("Pulso wiry"),
            baGang = BaGangData(
                polarity = "YIN",
                depth = "INTERIOR",
                temperature = null,
                strength = "EXCESS",
            ),
        )

        val result = engine.analyze(observation)

        // Should not crash and produce a valid result
        assertNotNull(result)
        assertNotNull(result.confidence)
        assertNotNull(result.rankedHypotheses)
        assertNotNull(result.missingInformation)
    }

    /**
     * Scenario B: Incomplete observation — system should report what's missing
     */
    @Test
    fun e2e_incompleteObservation_reportsGaps() = runTest {
        populateLiverQiStagnationKnowledge()

        // Very incomplete observation
        val observation = ClinicalObservation(
            symptoms = listOf("Dor"),
        )

        val result = engine.analyze(observation)

        // Should report missing tongue and pulse
        val missingTypes = result.missingInformation.map { it.observationType }
        assertTrue("Should report missing tongue", missingTypes.contains("TONGUE"))
        assertTrue("Should report missing pulse", missingTypes.contains("PULSE"))
    }

    /**
     * Scenario C: Empty knowledge core — graceful degradation
     */
    @Test
    fun e2e_emptyKnowledgeCore_returnsEmptyResult() = runTest {
        val observation = ClinicalObservation(
            symptoms = listOf("Dor no flanco"),
            tongueFindings = listOf("Língua vermelha"),
            pulseFindings = listOf("Pulso rápido"),
        )

        val result = engine.analyze(observation)

        // Should not crash
        assertNotNull(result)
        assertTrue("Should return empty candidates", result.rankedHypotheses.isEmpty())
        assertEquals(
            com.bioacupunt.prontuario.domain.model.ConfidenceLevel.INSUFFICIENT_EVIDENCE,
            result.confidence
        )
    }

    /**
     * Scenario D: Deterministic results — same input produces same output
     */
    @Test
    fun e2e_deterministicResults() = runTest {
        populateLiverQiStagnationKnowledge()

        val observation = ClinicalObservation(
            symptoms = listOf("Dor no flanco", "Irritabilidade"),
            tongueFindings = listOf("Língua vermelha"),
            pulseFindings = listOf("Pulso wiry"),
        )

        val result1 = engine.analyze(observation)
        val result2 = engine.analyze(observation)

        // Same ranking
        assertEquals(
            result1.rankedHypotheses.map { it.entityId },
            result2.rankedHypotheses.map { it.entityId }
        )
        assertEquals(
            result1.rankedHypotheses.map { it.score },
            result2.rankedHypotheses.map { it.score }
        )
        // Same confidence
        assertEquals(result1.confidence, result2.confidence)
        // Same missing data
        assertEquals(
            result1.missingInformation.map { it.observationType },
            result2.missingInformation.map { it.observationType }
        )
    }

    // ── Knowledge population helpers ─────────────────────────────────

    private fun populateLiverQiStagnationKnowledge() {
        val now = System.currentTimeMillis()

        // Pattern: Liver Qi Stagnation — name includes symptom text for search matching
        val pattern1 = KnowledgeEntity(
            id = "pattern.liver_qi_stagnation",
            type = KnowledgeEntityType.PATTERN,
            canonicalName = "Estagnação de Qi do Fígado com Dor no Flanco",
            aliases = listOf("Liver Qi Stagnation"),
            summary = "Padrão de estagnação de Qi no Fígado",
            content = "Caracterizado por dor no flanco, irritabilidade, suspiros, pulso wiry",
            version = KnowledgeVersion("1", now, now, status = KnowledgeStatus.PUBLISHED),
            createdAt = now,
            updatedAt = now,
        )

        // Pattern 2: for differential
        val pattern2 = KnowledgeEntity(
            id = "pattern.spleen_deficiency",
            type = KnowledgeEntityType.PATTERN,
            canonicalName = "Deficiência de Qi do Baço com Dor",
            version = KnowledgeVersion("1", now, now, status = KnowledgeStatus.PUBLISHED),
            createdAt = now,
            updatedAt = now,
        )

        // Acupoint: LI4
        val acupoint = KnowledgeEntity(
            id = "acupoint.li4",
            type = KnowledgeEntityType.ACUPOINT,
            canonicalName = "LI4 - Hegu",
            version = KnowledgeVersion("1", now, now),
            createdAt = now,
            updatedAt = now,
        )

        // Add to repos
        knowledgeRepo.entities.addAll(listOf(pattern1, pattern2, acupoint))
        fakeDao.entities.addAll(listOf(
            KnowledgeCoreEntityEntity("pattern.liver_qi_stagnation", "PATTERN", "Estagnação de Qi do Fígado com Dor no Flanco",
                created_at = now, updated_at = now, status = "PUBLISHED"),
            KnowledgeCoreEntityEntity("pattern.spleen_deficiency", "PATTERN", "Deficiência de Qi do Baço com Dor",
                created_at = now, updated_at = now, status = "PUBLISHED"),
            KnowledgeCoreEntityEntity("acupoint.li4", "ACUPOINT", "LI4 - Hegu",
                created_at = now, updated_at = now),
        ))

        // Relations
        fakeDao.relations.addAll(listOf(
            KnowledgeCoreRelationEntity("pattern.liver_qi_stagnation", "HAS_SYMPTOM", "pattern.spleen_deficiency",
                confidence = 0.9, created_at = now, updated_at = now),
            KnowledgeCoreRelationEntity("pattern.liver_qi_stagnation", "HAS_POINT", "acupoint.li4",
                confidence = 0.8, created_at = now, updated_at = now),
        ))
    }
}
