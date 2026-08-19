package com.bioacupunt.copilot

import com.bioacupunt.ai.core.AiRepository
import com.bioacupunt.ai.core.AiRequest
import com.bioacupunt.ai.core.AiResult
import com.bioacupunt.copilot.clinical.ClinicalIntelligenceIntegration
import com.bioacupunt.copilot.clinical.ExplainDifferentialUseCase
import com.bioacupunt.copilot.clinical.ExplainMissingDataUseCase
import com.bioacupunt.copilot.patient.PatientContextProvider
import com.bioacupunt.copilot.rag.ContextBuilder
import com.bioacupunt.copilot.rag.EvidenceGate
import com.bioacupunt.copilot.rag.EvidenceResolutionService
import com.bioacupunt.copilot.rag.GroundedResponseGenerator
import com.bioacupunt.copilot.rag.ResponseValidator
import com.bioacupunt.copilot.retrieval.*
import com.bioacupunt.mtc.knowledge.data.*
import com.bioacupunt.mtc.knowledge.domain.*
import com.bioacupunt.mtc.knowledge.repository.KnowledgeGraphRepository
import com.bioacupunt.mtc.knowledge.repository.KnowledgeRepository
import com.bioacupunt.mtc.knowledge.repository.KnowledgeSearchRepository
import com.bioacupunt.mtc.knowledge.repository.KnowledgeSearchResult
import com.bioacupunt.mtc.knowledge.repository.GraphConfig
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

/**
 * §37-40 CLINICAL COPILOT ENGINE INTEGRATION TEST
 *
 * THE CRITICAL TEST: proves that the EvidenceGate blocks the LLM from being
 * called when retrieval returns no evidence.
 *
 * Flow tested:
 * User → Intent → Retrieval → No Evidence → EvidenceGate → BLOCK → LLM NOT CALLED
 */

// ═══════════════════════════════════════════════════════════════════
// FAKES — minimal implementations for integration testing
// ═══════════════════════════════════════════════════════════════════

/** Spy that counts generate() calls — proves LLM was or wasn't called. */
private class SpyAiRepository : AiRepository {
    val generateCalls = AtomicInteger(0)
    override suspend fun generate(request: AiRequest): Result<AiResult> {
        generateCalls.incrementAndGet()
        return Result.success(AiResult(text = """{"answer":"Test","claims":[],"citations":[]}""", providerId = "spy", modelId = "spy"))
    }
    override suspend fun stream(request: AiRequest) = flowOf("test")
}

/** Fake DAO returning empty results for all queries. */
private class FakeDao : KnowledgeCoreDao {
    override suspend fun getById(id: String) = null
    override suspend fun search(query: String, limit: Int) = emptyList<KnowledgeCoreEntityEntity>()
    override fun observeAll() = flowOf(emptyList<KnowledgeCoreEntityEntity>())
    override suspend fun getByType(type: String) = emptyList<KnowledgeCoreEntityEntity>()
    override suspend fun getByStatus(status: String) = emptyList<KnowledgeCoreEntityEntity>()
    override suspend fun getByIds(ids: List<String>) = emptyList<KnowledgeCoreEntityEntity>()
    override suspend fun countAll() = 0
    override suspend fun countByType(type: String) = 0
    override suspend fun getRelations(entityId: String) = emptyList<KnowledgeCoreRelationEntity>()
    override suspend fun getEdgesFrom(entityId: String) = emptyList<KnowledgeCoreRelationEntity>()
    override suspend fun getEdgesTo(entityId: String) = emptyList<KnowledgeCoreRelationEntity>()
    override suspend fun getEdgesBetween(sourceId: String, targetId: String) = emptyList<KnowledgeCoreRelationEntity>()
    override suspend fun getEvidenceById(id: String) = null
    override suspend fun getEvidenceByIds(ids: List<String>) = emptyList<KnowledgeCoreEvidenceEntity>()
    override suspend fun getCitationById(id: String) = null
    override suspend fun getCitationsByIds(ids: List<String>) = emptyList<KnowledgeCoreCitationEntity>()
    override suspend fun getCitationsBySource(sourceId: String) = emptyList<KnowledgeCoreCitationEntity>()
    override suspend fun getSourceById(id: String) = null
    override suspend fun getSourcesByIds(ids: List<String>) = emptyList<KnowledgeCoreSourceEntity>()
    override suspend fun getProvenanceByEntity(entityId: String) = emptyList<KnowledgeCoreProvenanceEntity>()
    override suspend fun insertEntities(items: List<KnowledgeCoreEntityEntity>) {}
    override suspend fun insertRelations(items: List<KnowledgeCoreRelationEntity>) {}
    override suspend fun insertSources(items: List<KnowledgeCoreSourceEntity>) {}
    override suspend fun insertCitations(items: List<KnowledgeCoreCitationEntity>) {}
    override suspend fun insertEvidence(items: List<KnowledgeCoreEvidenceEntity>) {}
    override suspend fun insertProvenance(items: List<KnowledgeCoreProvenanceEntity>) {}
    override suspend fun deleteById(id: String) {}
    override suspend fun deleteRelationsFor(entityId: String) {}
}

/** Fake search repo returning empty results. */
private class FakeSearchRepo : KnowledgeSearchRepository {
    override suspend fun search(query: String, limit: Int) = emptyList<KnowledgeSearchResult>()
    override suspend fun searchByType(query: String, type: com.bioacupunt.mtc.knowledge.domain.KnowledgeEntityType, limit: Int) = emptyList<KnowledgeSearchResult>()
    override suspend fun searchByStatus(query: String, status: com.bioacupunt.mtc.knowledge.domain.KnowledgeStatus, limit: Int) = emptyList<KnowledgeSearchResult>()
    override suspend fun getById(id: String) = null
    override suspend fun getByType(type: com.bioacupunt.mtc.knowledge.domain.KnowledgeEntityType, limit: Int): List<com.bioacupunt.mtc.knowledge.domain.KnowledgeEntity> = emptyList()
    override suspend fun count() = 0
    override suspend fun countByType(type: com.bioacupunt.mtc.knowledge.domain.KnowledgeEntityType) = 0
}

/** Fake graph repo returning empty traversal. */
private class FakeGraphRepo : KnowledgeGraphRepository {
    override suspend fun reachable(entityId: String, config: GraphConfig) = GraphTraversalResult(emptyList(), emptyList())
    override suspend fun neighbors(entityId: String, config: GraphConfig) = GraphTraversalResult(emptyList(), emptyList())
    override suspend fun findPath(fromId: String, toId: String, config: GraphConfig) = emptyList<GraphPath>()
    override suspend fun edgesFrom(entityId: String) = emptyList<GraphEdge>()
    override suspend fun edgesTo(entityId: String) = emptyList<GraphEdge>()
    override suspend fun entitiesNear(entityId: String, targetType: KnowledgeEntityType, config: GraphConfig) = emptyList<KnowledgeEntity>()
}

/** Fake knowledge repo returning empty results. */
private class FakeKnowledgeRepo : KnowledgeRepository {
    override suspend fun search(query: String, limit: Int) = emptyList<KnowledgeEntity>()
    override suspend fun getById(id: String) = null
    override suspend fun getRelations(entityId: String) = emptyList<KnowledgeRelation>()
    override fun observeAll(): kotlinx.coroutines.flow.Flow<List<com.bioacupunt.mtc.knowledge.domain.KnowledgeEntity>> = kotlinx.coroutines.flow.flowOf(emptyList())
}

/** Fake patient context repo returning null. */
private class FakePatientRepo : PatientContextProvider.PatientContextRepository {
    override suspend fun getPatientContext(patientId: Long): com.bioacupunt.copilot.retrieval.PatientContext? = null
    override suspend fun getRecentObservations(patientId: Long, limit: Int): List<String> = emptyList()
    override suspend fun getRelevantHistory(patientId: Long): List<String> = emptyList()
    override suspend fun getCurrentAssessment(patientId: Long): String? = null
}

// ═══════════════════════════════════════════════════════════════════
// TEST CLASS
// ═══════════════════════════════════════════════════════════════════

class ClinicalCopilotEngineTest {

    private lateinit var spyAi: SpyAiRepository
    private lateinit var engine: ClinicalCopilotEngine

    @Before
    fun setup() {
        spyAi = SpyAiRepository()
        val dao = FakeDao()
        val searchRepo = FakeSearchRepo()
        val graphRepo = FakeGraphRepo()
        val knowledgeRepo = FakeKnowledgeRepo()
        val evidenceResolver = EvidenceResolver(dao)
        val evidenceResolutionService = EvidenceResolutionService(evidenceResolver)

        val evidenceEngine = EvidenceEngine(knowledgeRepo, graphRepo, evidenceResolver)
        val missingDataEngine = MissingDataEngine(graphRepo)
        val differentialEngine = DifferentialEngine(knowledgeRepo, graphRepo, evidenceEngine)
        val clinicalIntelligenceEngine = ClinicalIntelligenceEngine(differentialEngine, evidenceEngine, missingDataEngine, evidenceResolver)
        val runClinicalIntelligenceUseCase = RunClinicalIntelligenceUseCase(clinicalIntelligenceEngine)

        engine = ClinicalCopilotEngine(
            intentDetector = IntentDetector(),
            entityRecognizer = EntityRecognizer(searchRepo),
            queryNormalizer = QueryNormalizer(),
            hybridRetriever = HybridRetriever(
                lexicalBackend = LexicalSearchBackend(searchRepo),
                vectorBackend = null,
                graphBackend = GraphRetrievalBackend(graphRepo),
                metadataBackend = MetadataFilterBackend(searchRepo),
            ),
            reranker = RetrievalReranker(),
            contextBuilder = ContextBuilder(),
            evidenceGate = EvidenceGate(),
            evidenceResolutionService = evidenceResolutionService,
            groundedResponseGenerator = GroundedResponseGenerator(spyAi, evidenceResolutionService),
            responseValidator = ResponseValidator(),
            clinicalIntelligenceIntegration = ClinicalIntelligenceIntegration(
                clinicalIntelligenceEngine = clinicalIntelligenceEngine,
                runClinicalIntelligenceUseCase = runClinicalIntelligenceUseCase,
            ),
            patientContextProvider = PatientContextProvider(FakePatientRepo()),
            explainDifferentialUseCase = ExplainDifferentialUseCase(),
            explainMissingDataUseCase = ExplainMissingDataUseCase(),
            copilotRouter = CopilotRouter(),
        )
    }

    // ── R2 CRITICAL TEST: Knowledge search with no evidence ─────────

    @Test
    fun r2_knowledgeSearch_noEvidence_blocksLLM() = runTest {
        val callsBefore = spyAi.generateCalls.get()

        val result = engine.process(query = "pesquise insônia")

        // THE CRITICAL ASSERTION: LLM should NOT have been called
        assertEquals(
            "R2 VIOLATION: LLM was called ${spyAi.generateCalls.get() - callsBefore} time(s) when evidence is empty",
            callsBefore,
            spyAi.generateCalls.get()
        )

        // Gate should have blocked
        assertNotNull(result.gateResult)
        assertNotEquals(
            "Gate must not ALLOW when context is empty",
            EvidenceGate.GateDecision.ALLOW,
            result.gateResult!!.decision
        )

        // Response should indicate insufficient evidence
        assertTrue(
            "Response should indicate no evidence",
            result.response.confidence == "INSUFFICIENT" ||
                result.response.warnings.contains("NO_EVIDENCE") ||
                result.response.warnings.contains("INSUFFICIENT_EVIDENCE") ||
                result.response.warnings.contains("MODEL_NOT_CALLED")
        )
    }

    // ── R2: Evidence lookup with no evidence ────────────────────────

    @Test
    fun r2_evidenceLookup_noEvidence_blocksLLM() = runTest {
        val callsBefore = spyAi.generateCalls.get()
        val result = engine.process(query = "qual a fonte?")
        assertEquals(
            "R2 VIOLATION: LLM called during evidence lookup without evidence",
            callsBefore,
            spyAi.generateCalls.get()
        )
    }

    // ── R2: Point lookup with no evidence ───────────────────────────

    @Test
    fun r2_pointLookup_noEvidence_blocksLLM() = runTest {
        val callsBefore = spyAi.generateCalls.get()
        val result = engine.process(query = "ponto LI4")
        assertEquals(
            "R2 VIOLATION: LLM called during point lookup without evidence",
            callsBefore,
            spyAi.generateCalls.get()
        )
    }

    // ── R2: Formula lookup with no evidence ─────────────────────────

    @Test
    fun r2_formulaLookup_noEvidence_blocksLLM() = runTest {
        val callsBefore = spyAi.generateCalls.get()
        val result = engine.process(query = "fórmula para insônia")
        assertEquals(
            "R2 VIOLATION: LLM called during formula lookup without evidence",
            callsBefore,
            spyAi.generateCalls.get()
        )
    }

    // ── R2: Protocol lookup with no evidence ────────────────────────

    @Test
    fun r2_protocolLookup_noEvidence_blocksLLM() = runTest {
        val callsBefore = spyAi.generateCalls.get()
        val result = engine.process(query = "protocolo de tratamento")
        assertEquals(
            "R2 VIOLATION: LLM called during protocol lookup without evidence",
            callsBefore,
            spyAi.generateCalls.get()
        )
    }

    // ── Differential explanation without clinical intelligence ───────

    @Test
    fun differential_noClinicalIntelligence_returnsWarning() = runTest {
        val result = engine.process(
            query = "por que A está acima de B?",
            clinicalIntelligenceResult = null,
        )
        assertTrue(
            "Should warn about missing clinical intelligence",
            result.response.warnings.contains("NO_CLINICAL_INTELLIGENCE")
        )
    }

    // ── Missing data without clinical intelligence ──────────────────

    @Test
    fun missingData_noClinicalIntelligence_returnsWarning() = runTest {
        val result = engine.process(
            query = "o que falta?",
            clinicalIntelligenceResult = null,
        )
        assertTrue(
            "Should warn about missing clinical intelligence",
            result.response.warnings.contains("NO_CLINICAL_INTELLIGENCE")
        )
    }

    // ── Patient summary without patient context ─────────────────────

    @Test
    fun patientSummary_noContext_returnsUnavailable() = runTest {
        val result = engine.process(query = "resumo do paciente", patientId = null)
        assertTrue(
            "Should indicate patient context unavailable",
            result.response.warnings.contains("PATIENT_CONTEXT_UNAVAILABLE")
        )
    }

    // ── Intent detection works ──────────────────────────────────────

    @Test
    fun knowledgeSearch_intentDetected() = runTest {
        val result = engine.process(query = "o que é insônia?")
        assertEquals(IntentType.KNOWLEDGE_SEARCH, result.intent)
    }

    @Test
    fun pointLookup_intentDetected() = runTest {
        val result = engine.process(query = "ponto LI4")
        assertEquals(IntentType.POINT_LOOKUP, result.intent)
    }

    @Test
    fun differential_intentDetected() = runTest {
        val result = engine.process(query = "por que A está acima de B?")
        assertEquals(IntentType.DIFFERENTIAL_EXPLANATION, result.intent)
    }

    @Test
    fun missingData_intentDetected() = runTest {
        val result = engine.process(query = "o que falta?")
        assertEquals(IntentType.MISSING_DATA, result.intent)
    }

    // ── Latency is recorded ─────────────────────────────────────────

    @Test
    fun process_recordsLatency() = runTest {
        val result = engine.process(query = "pesquise insônia")
        assertTrue("Latency should be non-negative", result.latencyMs >= 0)
    }

    // ── Validation report is produced ───────────────────────────────

    @Test
    fun process_producesValidationReport() = runTest {
        val result = engine.process(query = "pesquise insônia")
        assertNotNull(result.validationReport)
    }
}
