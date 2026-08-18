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

@RunWith(RobolectricTestRunner::class)
class DifferentialEngineTest {

    private lateinit var fakeDao: FakeKnowledgeCoreDao
    private lateinit var knowledgeRepo: FakeKnowledgeRepository
    private lateinit var graphRepo: RoomKnowledgeGraphRepository
    private lateinit var evidenceResolver: EvidenceResolver
    private lateinit var evidenceEngine: EvidenceEngine
    private lateinit var missingDataEngine: MissingDataEngine
    private lateinit var engine: DifferentialEngine

    @Before
    fun setup() {
        fakeDao = FakeKnowledgeCoreDao()
        knowledgeRepo = FakeKnowledgeRepository()
        graphRepo = RoomKnowledgeGraphRepository(fakeDao)
        evidenceResolver = EvidenceResolver(fakeDao)
        evidenceEngine = EvidenceEngine(knowledgeRepo, graphRepo, evidenceResolver)
        missingDataEngine = MissingDataEngine(graphRepo)
        engine = DifferentialEngine(knowledgeRepo, graphRepo, evidenceEngine)
    }

    // ── Candidate generation ─────────────────────────────────────────

    @Test
    fun analyze_withSymptoms_findsPatternCandidates() = runTest {
        // Set up knowledge core with patterns whose names match the symptom search
        val pattern1 = KnowledgeEntity(
            id = "pattern.dor_no_flanco",
            type = KnowledgeEntityType.PATTERN,
            canonicalName = "Estagnação de Qi com Dor no Flanco",
            version = KnowledgeVersion("1", System.currentTimeMillis(), System.currentTimeMillis()),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
        )
        val pattern2 = KnowledgeEntity(
            id = "pattern.dor_abdominal",
            type = KnowledgeEntityType.PATTERN,
            canonicalName = "Dor Abdominal por Frio",
            version = KnowledgeVersion("1", System.currentTimeMillis(), System.currentTimeMillis()),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
        )
        knowledgeRepo.entities.addAll(listOf(pattern1, pattern2))
        fakeDao.entities.addAll(listOf(
            KnowledgeCoreEntityEntity("pattern.dor_no_flanco", "PATTERN", "Estagnação de Qi com Dor no Flanco",
                created_at = System.currentTimeMillis(), updated_at = System.currentTimeMillis()),
            KnowledgeCoreEntityEntity("pattern.dor_abdominal", "PATTERN", "Dor Abdominal por Frio",
                created_at = System.currentTimeMillis(), updated_at = System.currentTimeMillis()),
        ))

        val observation = ClinicalObservation(
            symptoms = listOf("Dor no flanco", "Irritabilidade"),
        )

        val result = engine.analyze(observation)
        // Should find candidates from search (at least one pattern matches "Dor")
        assertTrue("Should find at least one candidate", result.candidates.isNotEmpty())
    }

    // ── Ranking ──────────────────────────────────────────────────────

    @Test
    fun analyze_ranking_isSortedByScore() = runTest {
        val patterns = listOf(
            KnowledgeEntity("p1", KnowledgeEntityType.PATTERN, "Padrão A",
                version = KnowledgeVersion("1", 1L, 1L), createdAt = 1L, updatedAt = 1L),
            KnowledgeEntity("p2", KnowledgeEntityType.PATTERN, "Padrão B",
                version = KnowledgeVersion("1", 1L, 1L), createdAt = 1L, updatedAt = 1L),
            KnowledgeEntity("p3", KnowledgeEntityType.PATTERN, "Padrão C",
                version = KnowledgeVersion("1", 1L, 1L), createdAt = 1L, updatedAt = 1L),
        )
        knowledgeRepo.entities.addAll(patterns)
        fakeDao.entities.addAll(patterns.map {
            KnowledgeCoreEntityEntity(it.id, "PATTERN", it.canonicalName,
                created_at = 1L, updated_at = 1L)
        })

        val observation = ClinicalObservation(symptoms = listOf("Dor"))
        val result = engine.analyze(observation)

        // Candidates should be sorted by score descending
        for (i in 0 until result.candidates.size - 1) {
            assertTrue(
                "Candidates should be sorted by score descending",
                result.candidates[i].score >= result.candidates[i + 1].score
            )
        }
    }

    // ── Missing data ─────────────────────────────────────────────────

    @Test
    fun analyze_incompleteObservation_reportsMissingData() = runTest {
        // Need 2+ candidates for identifyMissingData to run
        val pattern1 = KnowledgeEntity("p1", KnowledgeEntityType.PATTERN, "Dor Crônica por Estagnação",
            version = KnowledgeVersion("1", 1L, 1L), createdAt = 1L, updatedAt = 1L)
        val pattern2 = KnowledgeEntity("p2", KnowledgeEntityType.PATTERN, "Dor Aguda por Frio",
            version = KnowledgeVersion("1", 1L, 1L), createdAt = 1L, updatedAt = 1L)
        knowledgeRepo.entities.addAll(listOf(pattern1, pattern2))
        fakeDao.entities.addAll(listOf(
            KnowledgeCoreEntityEntity("p1", "PATTERN", "Dor Crônica por Estagnação",
                created_at = 1L, updated_at = 1L),
            KnowledgeCoreEntityEntity("p2", "PATTERN", "Dor Aguda por Frio",
                created_at = 1L, updated_at = 1L),
        ))

        // Incomplete observation: no tongue, no pulse
        val observation = ClinicalObservation(symptoms = listOf("Dor"))
        val result = engine.analyze(observation)

        // Should report missing tongue and pulse data
        val missingTypes = result.missingInformation.map { it.observationType }
        assertTrue("Should report missing tongue data", missingTypes.contains("TONGUE"))
        assertTrue("Should report missing pulse data", missingTypes.contains("PULSE"))
    }

    @Test
    fun analyze_completeObservation_lessMissingData() = runTest {
        val pattern = KnowledgeEntity("p1", KnowledgeEntityType.PATTERN, "Padrão",
            version = KnowledgeVersion("1", 1L, 1L), createdAt = 1L, updatedAt = 1L)
        knowledgeRepo.entities.add(pattern)
        fakeDao.entities.add(KnowledgeCoreEntityEntity("p1", "PATTERN", "Padrão",
            created_at = 1L, updated_at = 1L))

        val observation = ClinicalObservation(
            symptoms = listOf("Dor"),
            tongueFindings = listOf("Língua vermelha"),
            pulseFindings = listOf("Pulso rápido"),
            baGang = BaGangData(temperature = "HEAT"),
        )
        val result = engine.analyze(observation)

        // Should have less missing data than incomplete observation
        val missingTypes = result.missingInformation.map { it.observationType }
        assertFalse("Should NOT report missing tongue", missingTypes.contains("TONGUE"))
        assertFalse("Should NOT report missing pulse", missingTypes.contains("PULSE"))
    }

    // ── No candidates ────────────────────────────────────────────────

    @Test
    fun analyze_emptyKnowledgeCore_returnsEmptyResult() = runTest {
        val observation = ClinicalObservation(symptoms = listOf("Dor"))
        val result = engine.analyze(observation)
        assertTrue("Should return empty candidates", result.candidates.isEmpty())
        assertEquals(Insufficient_Evidence, result.confidence)
    }

    // ── Confidence ───────────────────────────────────────────────────

    @Test
    fun analyze_withEvidence_providesConfidenceLevel() = runTest {
        val pattern = KnowledgeEntity("p1", KnowledgeEntityType.PATTERN, "Padrão com evidência",
            version = KnowledgeVersion("1", 1L, 1L), createdAt = 1L, updatedAt = 1L)
        knowledgeRepo.entities.add(pattern)
        fakeDao.entities.add(KnowledgeCoreEntityEntity("p1", "PATTERN", "Padrão com evidência",
            created_at = 1L, updated_at = 1L))

        val observation = ClinicalObservation(symptoms = listOf("Dor"))
        val result = engine.analyze(observation)

        // Confidence should be a valid level
        assertNotNull(result.confidence)
    }

    // ── Deterministic scoring ────────────────────────────────────────

    @Test
    fun analyze_deterministicOrdering() = runTest {
        val pattern = KnowledgeEntity("p1", KnowledgeEntityType.PATTERN, "Padrão",
            version = KnowledgeVersion("1", 1L, 1L), createdAt = 1L, updatedAt = 1L)
        knowledgeRepo.entities.add(pattern)
        fakeDao.entities.add(KnowledgeCoreEntityEntity("p1", "PATTERN", "Padrão",
            created_at = 1L, updated_at = 1L))

        val observation = ClinicalObservation(symptoms = listOf("Dor"))
        val result1 = engine.analyze(observation)
        val result2 = engine.analyze(observation)

        // Same input should produce same ranking
        assertEquals(result1.candidates.map { it.entityId }, result2.candidates.map { it.entityId })
        assertEquals(result1.candidates.map { it.score }, result2.candidates.map { it.score })
    }

    // ── Invalid observation ──────────────────────────────────────────

    @Test
    fun analyze_emptyObservation_handlesGracefully() = runTest {
        val observation = ClinicalObservation()
        val result = engine.analyze(observation)
        // Should not crash, may return empty or low-confidence result
        assertNotNull(result)
    }

    private val Insufficient_Evidence = com.bioacupunt.prontuario.domain.model.ConfidenceLevel.INSUFFICIENT_EVIDENCE
}
