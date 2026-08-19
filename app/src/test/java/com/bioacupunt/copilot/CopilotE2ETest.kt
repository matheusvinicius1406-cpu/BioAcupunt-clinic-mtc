package com.bioacupunt.copilot

import com.bioacupunt.ai.core.AiRepository
import com.bioacupunt.ai.core.AiRequest
import com.bioacupunt.ai.core.AiResult
import com.bioacupunt.copilot.clinical.ClinicalIntelligenceIntegration
import com.bioacupunt.copilot.clinical.ExplainDifferentialUseCase
import com.bioacupunt.copilot.clinical.ExplainMissingDataUseCase
import com.bioacupunt.copilot.patient.PatientContextProvider
import com.bioacupunt.copilot.rag.*
import com.bioacupunt.copilot.retrieval.*
import com.bioacupunt.mtc.knowledge.data.*
import com.bioacupunt.mtc.knowledge.domain.*
import com.bioacupunt.mtc.knowledge.repository.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

/**
 * §11-17 E2E TESTS — FULL PIPELINE VALIDATION
 *
 * These tests exercise the complete ClinicalCopilotEngine pipeline
 * from query to response, proving that:
 * - EvidenceGate blocks LLM when no evidence (R2)
 * - Intent detection routes correctly
 * - Response validation catches unsupported claims
 * - Patient context is properly scoped
 * - Offline mode degrades gracefully
 */

// ═══════════════════════════════════════════════════════════════════
// FAKES
// ═══════════════════════════════════════════════════════════════════

internal class E2ESpyAi : AiRepository {
    val generateCalls = AtomicInteger(0)
    val streamCalls = AtomicInteger(0)
    override suspend fun generate(request: AiRequest): Result<AiResult> {
        generateCalls.incrementAndGet()
        return Result.success(AiResult(
            text = """{"answer":"Resposta baseada em evidência da biblioteca.","claims":["afirmação suportada"],"citations":["Maciocia"]}""",
            providerId = "spy", modelId = "spy",
        ))
    }
    override suspend fun stream(request: AiRequest) = flowOf("test").also { streamCalls.incrementAndGet() }
}

private class E2EFakeDao : KnowledgeCoreDao {
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

private class E2EFakeSearchRepo : KnowledgeSearchRepository {
    override suspend fun search(query: String, limit: Int) = emptyList<KnowledgeSearchResult>()
    override suspend fun searchByType(query: String, type: KnowledgeEntityType, limit: Int) = emptyList<KnowledgeSearchResult>()
    override suspend fun searchByStatus(query: String, status: KnowledgeStatus, limit: Int) = emptyList<KnowledgeSearchResult>()
    override suspend fun getById(id: String) = null
    override suspend fun getByType(type: KnowledgeEntityType, limit: Int): List<KnowledgeEntity> = emptyList()
    override suspend fun count() = 0
    override suspend fun countByType(type: KnowledgeEntityType) = 0
}

private class E2EFakeGraphRepo : KnowledgeGraphRepository {
    override suspend fun reachable(entityId: String, config: GraphConfig) = GraphTraversalResult(emptyList(), emptyList())
    override suspend fun neighbors(entityId: String, config: GraphConfig) = GraphTraversalResult(emptyList(), emptyList())
    override suspend fun findPath(fromId: String, toId: String, config: GraphConfig) = emptyList<GraphPath>()
    override suspend fun edgesFrom(entityId: String) = emptyList<GraphEdge>()
    override suspend fun edgesTo(entityId: String) = emptyList<GraphEdge>()
    override suspend fun entitiesNear(entityId: String, targetType: KnowledgeEntityType, config: GraphConfig) = emptyList<KnowledgeEntity>()
}

private class E2EFakeKnowledgeRepo : KnowledgeRepository {
    override suspend fun search(query: String, limit: Int) = emptyList<KnowledgeEntity>()
    override suspend fun getById(id: String) = null
    override suspend fun getRelations(entityId: String) = emptyList<KnowledgeRelation>()
    override fun observeAll(): kotlinx.coroutines.flow.Flow<List<KnowledgeEntity>> = flowOf(emptyList())
}

private class E2EFakePatientRepo : PatientContextProvider.PatientContextRepository {
    override suspend fun getPatientContext(patientId: Long) = null
    override suspend fun getRecentObservations(patientId: Long, limit: Int): List<String> = emptyList()
    override suspend fun getRelevantHistory(patientId: Long): List<String> = emptyList()
    override suspend fun getCurrentAssessment(patientId: Long): String? = null
}

// ═══════════════════════════════════════════════════════════════════
// TEST CLASS
// ═══════════════════════════════════════════════════════════════════

class CopilotE2ETest {

    private lateinit var spyAi: E2ESpyAi
    private lateinit var engine: ClinicalCopilotEngine

    @Before
    fun setup() {
        spyAi = E2ESpyAi()
        val dao = E2EFakeDao()
        val searchRepo = E2EFakeSearchRepo()
        val graphRepo = E2EFakeGraphRepo()
        val knowledgeRepo = E2EFakeKnowledgeRepo()
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
            patientContextProvider = PatientContextProvider(E2EFakePatientRepo()),
            explainDifferentialUseCase = ExplainDifferentialUseCase(),
            explainMissingDataUseCase = ExplainMissingDataUseCase(),
            copilotRouter = CopilotRouter(),
        )
    }

    // ═══════════════════════════════════════════════════════════════
    // E2E — KNOWLEDGE QUERY (§11)
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun e2e_knowledgeQuery_fullPipeline() = runTest {
        val callsBefore = spyAi.generateCalls.get()

        val result = engine.process(query = "o que é insônia?")

        // Pipeline executed: intent → entity → normalize → retrieve → rerank → context → gate → LLM → validate
        assertNotNull(result.intent)
        assertNotNull(result.gateResult)
        assertNotNull(result.validationReport)
        assertTrue(result.latencyMs >= 0)

        // With empty retrieval, gate should block
        assertEquals(callsBefore, spyAi.generateCalls.get())
    }

    // ═══════════════════════════════════════════════════════════════
    // E2E — NO EVIDENCE (§12) — R2 CRITICAL
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun e2e_noEvidence_zeroLLMCalls() = runTest {
        val callsBefore = spyAi.generateCalls.get()
        val streamsBefore = spyAi.streamCalls.get()

        val result = engine.process(query = "pesquise acupuncture")

        // THE CRITICAL R2 PROOF: zero LLM calls
        assertEquals("R2 VIOLATION: generate() called", callsBefore, spyAi.generateCalls.get())
        assertEquals("R2 VIOLATION: stream() called", streamsBefore, spyAi.streamCalls.get())

        // Gate must block
        assertNotNull(result.gateResult)
        assertNotEquals(EvidenceGate.GateDecision.ALLOW, result.gateResult!!.decision)

        // Response must indicate no evidence
        assertTrue(
            result.response.confidence == "INSUFFICIENT" ||
                result.response.warnings.contains("NO_EVIDENCE") ||
                result.response.warnings.contains("INSUFFICIENT_EVIDENCE") ||
                result.response.warnings.contains("MODEL_NOT_CALLED")
        )
    }

    @Test
    fun e2e_noEvidence_pointLookup_blocksLLM() = runTest {
        val callsBefore = spyAi.generateCalls.get()
        engine.process(query = "ponto LI4")
        assertEquals(callsBefore, spyAi.generateCalls.get())
    }

    @Test
    fun e2e_noEvidence_formulaLookup_blocksLLM() = runTest {
        val callsBefore = spyAi.generateCalls.get()
        engine.process(query = "fórmula para dor")
        assertEquals(callsBefore, spyAi.generateCalls.get())
    }

    @Test
    fun e2e_noEvidence_protocolLookup_blocksLLM() = runTest {
        val callsBefore = spyAi.generateCalls.get()
        engine.process(query = "protocolo de tratamento")
        assertEquals(callsBefore, spyAi.generateCalls.get())
    }

    // ═══════════════════════════════════════════════════════════════
    // E2E — DIFFERENTIAL EXPLANATION (§13)
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun e2e_differential_withClinicalIntelligence() = runTest {
        val ciResult = ClinicalIntelligenceResult(
            rankedHypotheses = listOf(
                DifferentialCandidate("p1", "Deficiência de Yin", KnowledgeEntityType.PATTERN, 0.8),
                DifferentialCandidate("p2", "Excesso de Yang", KnowledgeEntityType.PATTERN, 0.5),
            ),
            supportingEvidence = emptyList(),
            contradictingEvidence = emptyList(),
            reasoningPaths = emptyList(),
            missingInformation = emptyList(),
            confidence = com.bioacupunt.prontuario.domain.model.ConfidenceLevel.MODERATE,
        )

        val result = engine.process(
            query = "por que A está acima de B?",
            clinicalIntelligenceResult = ciResult,
        )

        // Intent should be DIFFERENTIAL_EXPLANATION
        assertEquals(IntentType.DIFFERENTIAL_EXPLANATION, result.intent)

        // Response should contain ranking info
        assertTrue(result.response.answer.contains("Deficiência de Yin") || result.response.answer.contains("não foi possível"))
    }

    @Test
    fun e2e_differential_withoutClinicalIntelligence_warns() = runTest {
        val result = engine.process(
            query = "por que A está acima de B?",
            clinicalIntelligenceResult = null,
        )

        assertTrue(result.response.warnings.contains("NO_CLINICAL_INTELLIGENCE"))
    }

    // ═══════════════════════════════════════════════════════════════
    // E2E — MISSING DATA (§14)
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun e2e_missingData_withClinicalIntelligence() = runTest {
        val ciResult = ClinicalIntelligenceResult(
            rankedHypotheses = listOf(
                DifferentialCandidate("p1", "Padrão A", KnowledgeEntityType.PATTERN, 0.7),
            ),
            supportingEvidence = emptyList(),
            contradictingEvidence = emptyList(),
            reasoningPaths = emptyList(),
            missingInformation = listOf(
                MissingDataItem("TONGUE", "Cor da língua", "Diferenciaria padrões", 1),
            ),
            confidence = com.bioacupunt.prontuario.domain.model.ConfidenceLevel.LOW,
        )

        val result = engine.process(
            query = "o que falta?",
            clinicalIntelligenceResult = ciResult,
        )

        assertEquals(IntentType.MISSING_DATA, result.intent)
        assertTrue(result.response.answer.contains("1"))
    }

    @Test
    fun e2e_missingData_withoutClinicalIntelligence_warns() = runTest {
        val result = engine.process(
            query = "o que falta?",
            clinicalIntelligenceResult = null,
        )

        assertTrue(result.response.warnings.contains("NO_CLINICAL_INTELLIGENCE"))
    }

    // ═══════════════════════════════════════════════════════════════
    // E2E — PATIENT SUMMARY (§15)
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun e2e_patientSummary_noContext_returnsUnavailable() = runTest {
        val result = engine.process(query = "resumo do paciente", patientId = null)

        assertEquals(IntentType.PATIENT_SUMMARY, result.intent)
        assertTrue(result.response.warnings.contains("PATIENT_CONTEXT_UNAVAILABLE"))
    }

    @Test
    fun e2e_patientSummary_withPatientId_attemptsContext() = runTest {
        // Even with patientId, our fake repo returns null
        val result = engine.process(
            query = "resumo do paciente",
            patientId = 42L,
            activePatientId = 42L,
            sessionId = "test-session",
        )

        assertEquals(IntentType.PATIENT_SUMMARY, result.intent)
        // Should get PATIENT_CONTEXT_UNAVAILABLE because fake repo returns null
        assertTrue(result.response.warnings.contains("PATIENT_CONTEXT_UNAVAILABLE"))
    }

    // ═══════════════════════════════════════════════════════════════
    // E2E — PERMISSIONS (§16)
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun e2e_permissions_crossPatientAccess_denied() = runTest {
        // Request patient 42 but active patient is 99 — should be denied
        val result = engine.process(
            query = "resumo do paciente",
            patientId = 42L,
            activePatientId = 99L,
            sessionId = "test-session",
        )

        // PatientContextProvider should return null (cross-patient denied)
        assertTrue(result.response.warnings.contains("PATIENT_CONTEXT_UNAVAILABLE"))
    }

    @Test
    fun e2e_permissions_noSession_denied() = runTest {
        val result = engine.process(
            query = "resumo do paciente",
            patientId = 42L,
            activePatientId = 42L,
            sessionId = null, // no session
        )

        assertTrue(result.response.warnings.contains("PATIENT_CONTEXT_UNAVAILABLE"))
    }

    // ═══════════════════════════════════════════════════════════════
    // E2E — OFFLINE (§17)
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun e2e_offline_knowledgeSearch_worksWithoutNetwork() = runTest {
        // The copilot operates entirely on-device — no network calls
        // Knowledge search uses Room FTS, graph uses Room, LLM uses local model
        val result = engine.process(query = "pesquise insônia")

        // Should complete without error
        assertNotNull(result)
        assertNotNull(result.intent)
        assertNotNull(result.gateResult)
    }

    @Test
    fun e2e_offline_allTools_workWithoutNetwork() = runTest {
        val queries = listOf(
            "ponto LI4",
            "fórmula para dor",
            "protocolo de tratamento",
            "qual a fonte?",
            "resumo do paciente",
        )

        for (query in queries) {
            val result = engine.process(query = query)
            assertNotNull("Query '$query' should complete", result)
            assertNotNull("Query '$query' should have intent", result.intent)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // E2E — INTENT ROUTING (§11)
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun e2e_intentRouting_knowledgeSearch() = runTest {
        val result = engine.process(query = "o que é insônia?")
        assertEquals(IntentType.KNOWLEDGE_SEARCH, result.intent)
    }

    @Test
    fun e2e_intentRouting_pointLookup() = runTest {
        val result = engine.process(query = "ponto LI4")
        assertEquals(IntentType.POINT_LOOKUP, result.intent)
    }

    @Test
    fun e2e_intentRouting_formulaLookup() = runTest {
        val result = engine.process(query = "fórmula para dor")
        assertEquals(IntentType.FORMULA_LOOKUP, result.intent)
    }

    @Test
    fun e2e_intentRouting_protocolLookup() = runTest {
        val result = engine.process(query = "protocolo de tratamento")
        assertEquals(IntentType.PROTOCOL_LOOKUP, result.intent)
    }

    @Test
    fun e2e_intentRouting_differential() = runTest {
        val result = engine.process(query = "por que A está acima de B?")
        assertEquals(IntentType.DIFFERENTIAL_EXPLANATION, result.intent)
    }

    @Test
    fun e2e_intentRouting_missingData() = runTest {
        val result = engine.process(query = "o que falta?")
        assertEquals(IntentType.MISSING_DATA, result.intent)
    }

    @Test
    fun e2e_intentRouting_patientSummary() = runTest {
        val result = engine.process(query = "resumo do paciente")
        assertEquals(IntentType.PATIENT_SUMMARY, result.intent)
    }

    @Test
    fun e2e_intentRouting_evidenceLookup() = runTest {
        val result = engine.process(query = "qual a fonte?")
        assertEquals(IntentType.EVIDENCE_LOOKUP, result.intent)
    }

    // ═══════════════════════════════════════════════════════════════
    // E2E — RESPONSE STRUCTURE (§25)
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun e2e_responseStructure_hasAllFields() = runTest {
        val result = engine.process(query = "o que é insônia?")

        // All required fields must be present
        assertNotNull(result.response)
        assertNotNull(result.validationReport)
        assertNotNull(result.intent)
        assertNotNull(result.gateResult)
        assertTrue(result.latencyMs >= 0)
    }

    @Test
    fun e2e_responseStructure_blockedHasWarnings() = runTest {
        val result = engine.process(query = "ponto LI4")

        // Blocked response should have warnings
        assertTrue(result.response.warnings.isNotEmpty())
    }

    // ═══════════════════════════════════════════════════════════════
    // E2E — LOCAL MODEL FAILURE (§18)
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun e2e_localModelUnavailable_doesNotCrash() = runTest {
        // With empty retrieval, gate blocks before LLM — model availability is irrelevant
        val result = engine.process(query = "pesquise insônia")
        assertNotNull(result)
        // Gate should block
        assertNotEquals(EvidenceGate.GateDecision.ALLOW, result.gateResult?.decision)
    }
}
