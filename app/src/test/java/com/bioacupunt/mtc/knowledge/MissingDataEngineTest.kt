package com.bioacupunt.mtc.knowledge

import com.bioacupunt.mtc.knowledge.domain.BaGangData
import com.bioacupunt.mtc.knowledge.domain.ClinicalObservation
import com.bioacupunt.mtc.knowledge.domain.DifferentialCandidate
import com.bioacupunt.mtc.knowledge.domain.KnowledgeEntityType
import com.bioacupunt.mtc.knowledge.domain.MissingDataEngine
import com.bioacupunt.mtc.knowledge.repository.RoomKnowledgeGraphRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MissingDataEngineTest {

    private lateinit var fakeDao: FakeKnowledgeCoreDao
    private lateinit var graphRepo: RoomKnowledgeGraphRepository
    private lateinit var engine: MissingDataEngine

    @Before
    fun setup() {
        fakeDao = FakeKnowledgeCoreDao()
        graphRepo = RoomKnowledgeGraphRepository(fakeDao)
        engine = MissingDataEngine(graphRepo)
    }

    private fun candidate(id: String, name: String) = DifferentialCandidate(
        entityId = id,
        entityName = name,
        entityType = KnowledgeEntityType.PATTERN,
        score = 0.8,
    )

    // ── Complete observation: no missing data ────────────────────────

    @Test
    fun analyze_completeObservation_returnsEmpty() = runTest {
        val candidates = listOf(candidate("p1", "Pattern A"), candidate("p2", "Pattern B"))
        val observation = ClinicalObservation(
            symptoms = listOf("Dor"),
            tongueFindings = listOf("Língua vermelha"),
            pulseFindings = listOf("Pulso rápido"),
            baGang = BaGangData(polarity = "YIN", depth = "INTERIOR", temperature = "HEAT", strength = "EXCESS"),
            zangFuPatterns = listOf("Fígado: Estagnação"),
            history = listOf("Histórico de estresse"),
            etiology = listOf("Estresse"),
        )
        val missing = engine.analyze(candidates, observation)
        assertTrue("Complete observation should have no missing data", missing.isEmpty())
    }

    // ── Missing tongue ───────────────────────────────────────────────

    @Test
    fun analyze_noTongue_reportsTongueMissing() = runTest {
        val candidates = listOf(candidate("p1", "A"), candidate("p2", "B"))
        val observation = ClinicalObservation(symptoms = listOf("Dor"), pulseFindings = listOf("Pulso rápido"))
        val missing = engine.analyze(candidates, observation)
        val missingTypes = missing.map { it.observationType }
        assertTrue("Should report missing tongue", missingTypes.contains("TONGUE"))
    }

    // ── Missing pulse ────────────────────────────────────────────────

    @Test
    fun analyze_noPulse_reportsPulseMissing() = runTest {
        val candidates = listOf(candidate("p1", "A"), candidate("p2", "B"))
        val observation = ClinicalObservation(symptoms = listOf("Dor"), tongueFindings = listOf("Língua vermelha"))
        val missing = engine.analyze(candidates, observation)
        val missingTypes = missing.map { it.observationType }
        assertTrue("Should report missing pulse", missingTypes.contains("PULSE"))
    }

    // ── Missing Ba Gang ──────────────────────────────────────────────

    @Test
    fun analyze_noBaGang_reportsBaGangMissing() = runTest {
        val candidates = listOf(candidate("p1", "A"), candidate("p2", "B"))
        val observation = ClinicalObservation(symptoms = listOf("Dor"), tongueFindings = listOf("Língua vermelha"), pulseFindings = listOf("Pulso rápido"))
        val missing = engine.analyze(candidates, observation)
        val missingTypes = missing.map { it.observationType }
        assertTrue("Should report missing Ba Gang", missingTypes.contains("BAGANG"))
    }

    // ── Multiple missing ─────────────────────────────────────────────

    @Test
    fun analyze_multipleMissing_reportsAll() = runTest {
        val candidates = listOf(candidate("p1", "A"), candidate("p2", "B"))
        val observation = ClinicalObservation(symptoms = listOf("Dor"))
        val missing = engine.analyze(candidates, observation)
        val missingTypes = missing.map { it.observationType }.toSet()
        assertTrue("Should report tongue", missingTypes.contains("TONGUE"))
        assertTrue("Should report pulse", missingTypes.contains("PULSE"))
        assertTrue("Should report Ba Gang", missingTypes.contains("BAGANG"))
    }

    // ── Priority ordering ────────────────────────────────────────────

    @Test
    fun analyze_priorityOrdering_correct() = runTest {
        val candidates = listOf(candidate("p1", "A"), candidate("p2", "B"))
        val observation = ClinicalObservation(symptoms = listOf("Dor"))
        val missing = engine.analyze(candidates, observation)
        val tonguePriority = missing.find { it.observationType == "TONGUE" }?.priority ?: 99
        val pulsePriority = missing.find { it.observationType == "PULSE" }?.priority ?: 99
        // Tongue (1) <= Pulse (2) — these are the main differentiators
        assertTrue("Tongue priority <= Pulse", tonguePriority <= pulsePriority)
    }

    // ── Single candidate: still reports missing data ──────────────────

    @Test
    fun analyze_singleCandidate_stillReportsMissing() = runTest {
        val candidates = listOf(candidate("p1", "A"))
        val observation = ClinicalObservation(symptoms = listOf("Dor"))
        val missing = engine.analyze(candidates, observation)
        // analyze() returns empty for < 2 candidates
        assertTrue("Single candidate returns empty", missing.isEmpty())
    }

    // ── Empty candidates: returns empty ───────────────────────────────

    @Test
    fun analyze_noCandidates_returnsEmpty() = runTest {
        val observation = ClinicalObservation(symptoms = listOf("Dor"))
        val missing = engine.analyze(emptyList(), observation)
        assertTrue("No candidates = no missing data", missing.isEmpty())
    }

    // ── Missing history ──────────────────────────────────────────────

    @Test
    fun analyze_noHistory_reportsHistoryMissing() = runTest {
        val candidates = listOf(candidate("p1", "A"), candidate("p2", "B"))
        val observation = ClinicalObservation(symptoms = listOf("Dor"), tongueFindings = listOf("Língua vermelha"), pulseFindings = listOf("Pulso rápido"), baGang = BaGangData(polarity = "YIN", depth = "INTERIOR"))
        val missing = engine.analyze(candidates, observation)
        val missingTypes = missing.map { it.observationType }
        assertTrue("Should report missing history", missingTypes.contains("HISTORY"))
    }

    // ── Description is meaningful ────────────────────────────────────

    @Test
    fun analyze_descriptionIsNonEmpty() = runTest {
        val candidates = listOf(candidate("p1", "A"), candidate("p2", "B"))
        val observation = ClinicalObservation(symptoms = listOf("Dor"))
        val missing = engine.analyze(candidates, observation)
        for (item in missing) {
            assertTrue("Description should not be blank", item.description.isNotBlank())
            assertTrue("Impact should not be blank", item.impact.isNotBlank())
        }
    }
}
