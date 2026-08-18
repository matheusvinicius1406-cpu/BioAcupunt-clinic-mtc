package com.bioacupunt.mtc.knowledge

import com.bioacupunt.mtc.knowledge.domain.*
import com.bioacupunt.mtc.knowledge.repository.GraphConfig
import com.bioacupunt.mtc.knowledge.repository.KnowledgeGraphRepository
import com.bioacupunt.mtc.knowledge.repository.KnowledgeRepository
import com.bioacupunt.mtc.knowledge.repository.RoomKnowledgeGraphRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EvidenceEngineTest {

    private lateinit var fakeDao: FakeKnowledgeCoreDao
    private lateinit var knowledgeRepo: FakeKnowledgeRepository
    private lateinit var graphRepo: RoomKnowledgeGraphRepository
    private lateinit var evidenceResolver: EvidenceResolver
    private lateinit var engine: EvidenceEngine

    @Before
    fun setup() {
        fakeDao = FakeKnowledgeCoreDao()
        knowledgeRepo = FakeKnowledgeRepository()
        graphRepo = RoomKnowledgeGraphRepository(fakeDao)
        evidenceResolver = EvidenceResolver(fakeDao)
        engine = EvidenceEngine(knowledgeRepo, graphRepo, evidenceResolver)
    }

    // ── Supporting evidence ──────────────────────────────────────────

    @Test
    fun calculateConfidence_withSupportingEvidence_returnsPositive() {
        val supporting = listOf(
            EvidenceItem("ev1", "Claim 1", level = "MODERN_LITERATURE", confidence = 0.8),
            EvidenceItem("ev2", "Claim 2", level = "TRADITION", confidence = 0.6),
        )
        val score = engine.calculateConfidence(supporting, emptyList())
        assertTrue("Score should be positive with supporting evidence", score > 0.0)
    }

    @Test
    fun calculateConfidence_moreEvidence_higherScore() {
        val fewEvidence = listOf(
            EvidenceItem("ev1", "Claim 1", confidence = 0.8),
        )
        val manyEvidence = listOf(
            EvidenceItem("ev1", "Claim 1", confidence = 0.8),
            EvidenceItem("ev2", "Claim 2", confidence = 0.7),
            EvidenceItem("ev3", "Claim 3", confidence = 0.6),
        )
        val scoreFew = engine.calculateConfidence(fewEvidence, emptyList())
        val scoreMany = engine.calculateConfidence(manyEvidence, emptyList())
        assertTrue("More evidence should yield higher score", scoreMany > scoreFew)
    }

    // ── Contradicting evidence ───────────────────────────────────────

    @Test
    fun calculateConfidence_withContradictions_reducesScore() {
        val supporting = listOf(
            EvidenceItem("ev1", "Claim 1", confidence = 0.8),
        )
        val withContradictions = listOf(
            EvidenceItem("ev1", "Claim 1", confidence = 0.8),
            EvidenceItem("ev2", "Contradicting", confidence = 0.9),
        )
        val scoreNoContradiction = engine.calculateConfidence(supporting, emptyList())
        val scoreWithContradiction = engine.calculateConfidence(supporting, withContradictions)
        assertTrue("Contradictions should reduce score", scoreWithContradiction < scoreNoContradiction)
    }

    // ── Source quality ───────────────────────────────────────────────

    @Test
    fun calculateConfidence_clinicalEvidence_higherThanTradition() {
        val clinicalEvidence = listOf(
            EvidenceItem("ev1", "Clinical trial", level = "CLINICAL_EVIDENCE", confidence = 0.8),
        )
        val traditionEvidence = listOf(
            EvidenceItem("ev1", "Classical text", level = "TRADITION", confidence = 0.8),
        )
        val scoreClinical = engine.calculateConfidence(clinicalEvidence, emptyList())
        val scoreTradition = engine.calculateConfidence(traditionEvidence, emptyList())
        assertTrue("Clinical evidence should score higher than tradition", scoreClinical > scoreTradition)
    }

    // ── No evidence ──────────────────────────────────────────────────

    @Test
    fun calculateConfidence_noEvidence_returnsZero() {
        val score = engine.calculateConfidence(emptyList(), emptyList())
        assertEquals(0.0, score, 0.001)
    }

    // ── Confidence bounds ────────────────────────────────────────────

    @Test
    fun calculateConfidence_neverExceedsMax() {
        val lotsOfEvidence = (1..20).map { i ->
            EvidenceItem("ev$i", "Claim $i", level = "CLINICAL_EVIDENCE", confidence = 1.0)
        }
        val score = engine.calculateConfidence(lotsOfEvidence, emptyList())
        assertTrue("Score should not exceed 1.0", score <= 1.0)
    }

    @Test
    fun calculateConfidence_neverBelowZero() {
        val lotsOfContradictions = (1..20).map { i ->
            EvidenceItem("ev$i", "Contradiction $i", confidence = 1.0)
        }
        val score = engine.calculateConfidence(emptyList(), lotsOfContradictions)
        assertTrue("Score should not go below 0.0", score >= 0.0)
    }

    // ── EvidenceScoringConfig ────────────────────────────────────────

    @Test
    fun customConfig_affectsScoring() {
        val strictConfig = EvidenceScoringConfig(
            baseSupportPerEvidence = 0.05,
            contradictionPenalty = 0.50,
        )
        val strictEngine = EvidenceEngine(knowledgeRepo, graphRepo, evidenceResolver, strictConfig)

        val evidence = listOf(EvidenceItem("ev1", "Claim", confidence = 0.8))
        val contradiction = listOf(EvidenceItem("ev2", "Contradiction", confidence = 0.9))

        val score = strictEngine.calculateConfidence(evidence, contradiction)
        // With strict config, contradiction penalty is higher
        assertTrue("Strict config should penalize more", score < 0.5)
    }
}

/**
 * Fake KnowledgeRepository for testing EvidenceEngine.
 * Returns entities from a mutable list.
 */
class FakeKnowledgeRepository : KnowledgeRepository {
    val entities = mutableListOf<com.bioacupunt.mtc.knowledge.domain.KnowledgeEntity>()
    val relations = mutableListOf<com.bioacupunt.mtc.knowledge.domain.KnowledgeRelation>()

    override suspend fun getById(id: String) = entities.find { it.id == id }
    override suspend fun search(query: String, limit: Int) = entities
        .filter { it.canonicalName.contains(query, ignoreCase = true) }
        .take(limit)
    override suspend fun getRelations(entityId: String) = relations
        .filter { it.sourceEntityId == entityId || it.targetEntityId == entityId }
    override fun observeAll() = kotlinx.coroutines.flow.flowOf(entities.toList())
}
