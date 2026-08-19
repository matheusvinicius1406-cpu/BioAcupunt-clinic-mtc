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

/**
 * §20-22 PERFORMANCE BENCHMARK
 *
 * Measures latency for each pipeline component.
 * All measurements are on JVM (not device) — documented as such.
 */

private class BenchmarkAiRepository : AiRepository {
    override suspend fun generate(request: AiRequest): Result<AiResult> {
        return Result.success(AiResult(text = """{"answer":"Test","claims":[],"citations":[]}""", providerId = "bench", modelId = "bench"))
    }
    override suspend fun stream(request: AiRequest) = flowOf("test")
}

private class BenchDao : KnowledgeCoreDao {
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

class CopilotBenchmarkTest {

    private lateinit var engine: ClinicalCopilotEngine
    private lateinit var intentDetector: IntentDetector
    private lateinit var queryNormalizer: QueryNormalizer
    private lateinit var reranker: RetrievalReranker
    private lateinit var contextBuilder: ContextBuilder
    private lateinit var evidenceGate: EvidenceGate
    private lateinit var responseValidator: ResponseValidator

    @Before
    fun setup() {
        val ai = BenchmarkAiRepository()
        val dao = BenchDao()
        val searchRepo = object : KnowledgeSearchRepository {
            override suspend fun search(query: String, limit: Int) = emptyList<KnowledgeSearchResult>()
            override suspend fun searchByType(query: String, type: KnowledgeEntityType, limit: Int) = emptyList<KnowledgeSearchResult>()
            override suspend fun searchByStatus(query: String, status: KnowledgeStatus, limit: Int) = emptyList<KnowledgeSearchResult>()
            override suspend fun getById(id: String) = null
            override suspend fun getByType(type: KnowledgeEntityType, limit: Int): List<KnowledgeEntity> = emptyList()
            override suspend fun count() = 0
            override suspend fun countByType(type: KnowledgeEntityType) = 0
        }
        val graphRepo = object : KnowledgeGraphRepository {
            override suspend fun reachable(entityId: String, config: GraphConfig) = GraphTraversalResult(emptyList(), emptyList())
            override suspend fun neighbors(entityId: String, config: GraphConfig) = GraphTraversalResult(emptyList(), emptyList())
            override suspend fun findPath(fromId: String, toId: String, config: GraphConfig) = emptyList<GraphPath>()
            override suspend fun edgesFrom(entityId: String) = emptyList<GraphEdge>()
            override suspend fun edgesTo(entityId: String) = emptyList<GraphEdge>()
            override suspend fun entitiesNear(entityId: String, targetType: KnowledgeEntityType, config: GraphConfig) = emptyList<KnowledgeEntity>()
        }
        val knowledgeRepo = object : KnowledgeRepository {
            override suspend fun search(query: String, limit: Int) = emptyList<KnowledgeEntity>()
            override suspend fun getById(id: String) = null
            override suspend fun getRelations(entityId: String) = emptyList<KnowledgeRelation>()
            override fun observeAll(): kotlinx.coroutines.flow.Flow<List<KnowledgeEntity>> = flowOf(emptyList())
        }
        val evidenceResolver = EvidenceResolver(dao)
        val evidenceResolutionService = EvidenceResolutionService(evidenceResolver)
        val evidenceEngine = EvidenceEngine(knowledgeRepo, graphRepo, evidenceResolver)
        val missingDataEngine = MissingDataEngine(graphRepo)
        val differentialEngine = DifferentialEngine(knowledgeRepo, graphRepo, evidenceEngine)
        val clinicalIntelligenceEngine = ClinicalIntelligenceEngine(differentialEngine, evidenceEngine, missingDataEngine, evidenceResolver)
        val runClinicalIntelligenceUseCase = RunClinicalIntelligenceUseCase(clinicalIntelligenceEngine)

        intentDetector = IntentDetector()
        queryNormalizer = QueryNormalizer()
        reranker = RetrievalReranker()
        contextBuilder = ContextBuilder()
        evidenceGate = EvidenceGate()
        responseValidator = ResponseValidator()

        engine = ClinicalCopilotEngine(
            intentDetector = intentDetector,
            entityRecognizer = EntityRecognizer(searchRepo),
            queryNormalizer = queryNormalizer,
            hybridRetriever = HybridRetriever(
                lexicalBackend = LexicalSearchBackend(searchRepo),
                vectorBackend = null,
                graphBackend = GraphRetrievalBackend(graphRepo),
                metadataBackend = MetadataFilterBackend(searchRepo),
            ),
            reranker = reranker,
            contextBuilder = contextBuilder,
            evidenceGate = evidenceGate,
            evidenceResolutionService = evidenceResolutionService,
            groundedResponseGenerator = GroundedResponseGenerator(ai, evidenceResolutionService),
            responseValidator = responseValidator,
            clinicalIntelligenceIntegration = ClinicalIntelligenceIntegration(
                clinicalIntelligenceEngine = clinicalIntelligenceEngine,
                runClinicalIntelligenceUseCase = runClinicalIntelligenceUseCase,
            ),
            patientContextProvider = PatientContextProvider(object : PatientContextProvider.PatientContextRepository {
                override suspend fun getPatientContext(patientId: Long) = null
                override suspend fun getRecentObservations(patientId: Long, limit: Int): List<String> = emptyList()
                override suspend fun getRelevantHistory(patientId: Long): List<String> = emptyList()
                override suspend fun getCurrentAssessment(patientId: Long): String? = null
            }),
            explainDifferentialUseCase = ExplainDifferentialUseCase(),
            explainMissingDataUseCase = ExplainMissingDataUseCase(),
            copilotRouter = CopilotRouter(),
        )
    }

    // ═══════════════════════════════════════════════════════════════
    // COMPONENT BENCHMARKS
    // ═══════════════════════════════════════════════════════════════

    private fun benchmarkComponent(name: String, iterations: Int = 100, block: () -> Unit): BenchmarkResult {
        val times = mutableListOf<Long>()
        repeat(iterations) {
            val start = System.nanoTime()
            block()
            val elapsed = System.nanoTime() - start
            times.add(elapsed)
        }
        val sorted = times.sorted()
        return BenchmarkResult(
            name = name,
            iterations = iterations,
            minNs = sorted.first(),
            maxNs = sorted.last(),
            avgNs = times.average().toLong(),
            medianNs = sorted[sorted.size / 2],
            p95Ns = sorted[(sorted.size * 0.95).toInt().coerceAtMost(sorted.size - 1)],
        )
    }

    @Test
    fun benchmark_intentDetection() = runTest {
        val queries = listOf(
            "o que é insônia?", "ponto LI4", "fórmula para dor",
            "protocolo de tratamento", "por que A está acima de B?",
            "o que falta?", "resumo do paciente", "qual a fonte?",
        )
        val result = benchmarkComponent("Intent Detection", 1000) {
            for (q in queries) intentDetector.detect(q)
        }
        printResult(result)
        // Intent detection should be fast (< 1ms average)
        assertTrue("Intent detection avg should be < 1ms: ${result.avgNs / 1_000_000}ms",
            result.avgNs < 1_000_000)
    }

    @Test
    fun benchmark_queryNormalization() {
        val queries = listOf(
            "o que é insônia?", "hegu", "zusanli", "dor de cabeça",
        )
        val result = benchmarkComponent("Query Normalization", 1000) {
            for (q in queries) queryNormalizer.normalize(q)
        }
        printResult(result)
        assertTrue("Query normalization avg should be < 1ms: ${result.avgNs / 1_000_000}ms",
            result.avgNs < 1_000_000)
    }

    @Test
    fun benchmark_reranker() {
        val hits = (1..20).map { i ->
            RetrievalHit(
                entityId = "entity.$i",
                content = "Content for entity $i",
                score = 1.0 - i * 0.05,
                normalizedScore = 1.0 - i * 0.05,
                sourceType = RetrievalSource.LEXICAL,
                evidenceIds = if (i % 3 == 0) listOf("ev.$i") else emptyList(),
            )
        }
        val result = benchmarkComponent("Reranker", 1000) {
            reranker.rerank(hits, "test query", null)
        }
        printResult(result)
        assertTrue("Reranker avg should be < 5ms: ${result.avgNs / 1_000_000}ms",
            result.avgNs < 5_000_000)
    }

    @Test
    fun benchmark_contextBuilder() {
        val hits = (1..20).map { i ->
            RetrievalHit(
                entityId = "entity.$i",
                content = "A".repeat(200),
                score = 1.0,
                normalizedScore = 1.0,
                sourceType = RetrievalSource.LEXICAL,
                evidenceIds = listOf("ev.$i"),
            )
        }
        val retrievalResult = UnifiedRetrievalResult(
            results = hits,
            totalCandidates = 20,
            retrievalLatencyMs = 0,
            sourceBreakdown = mapOf("LEXICAL" to 20),
        )
        val result = benchmarkComponent("Context Builder", 1000) {
            contextBuilder.build(retrievalResult, "test query")
        }
        printResult(result)
        assertTrue("ContextBuilder avg should be < 5ms: ${result.avgNs / 1_000_000}ms",
            result.avgNs < 5_000_000)
    }

    @Test
    fun benchmark_evidenceGate() {
        val context = ContextBuilder.StructuredContext(
            items = listOf(
                ContextBuilder.ContextItem(entity = "X", content = "Y", evidence = listOf("ev.1")),
            ),
            totalCharacters = 1,
            totalTokens = 0,
            truncated = false,
            evidenceIds = listOf("ev.1"),
        )
        val result = benchmarkComponent("Evidence Gate", 10000) {
            evidenceGate.evaluate(context, requiredEvidence = true)
        }
        printResult(result)
        assertTrue("EvidenceGate avg should be < 0.1ms: ${result.avgNs / 1_000}us",
            result.avgNs < 100_000)
    }

    @Test
    fun benchmark_responseValidator() {
        val response = GroundedResponseGenerator.GroundedResponse(
            answer = "Test response with some content",
            claims = listOf("claim 1", "claim 2"),
            citations = listOf("Maciocia"),
            evidenceIds = listOf("ev.1"),
            knowledgeVersion = "1.0",
        )
        val context = ContextBuilder.StructuredContext(
            items = listOf(
                ContextBuilder.ContextItem(entity = "X", content = "Test response with some content", evidence = listOf("ev.1")),
            ),
            totalCharacters = 30,
            totalTokens = 7,
            truncated = false,
            evidenceIds = listOf("ev.1"),
        )
        val result = benchmarkComponent("Response Validator", 10000) {
            responseValidator.validate(response, context)
        }
        printResult(result)
        assertTrue("ResponseValidator avg should be < 0.5ms: ${result.avgNs / 1_000}us",
            result.avgNs < 500_000)
    }

    // ═══════════════════════════════════════════════════════════════
    // FULL PIPELINE BENCHMARK
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun benchmark_fullPipeline() = runTest {
        val queries = listOf(
            "o que é insônia?",
            "ponto LI4",
            "fórmula para dor",
            "por que A está acima de B?",
            "o que falta?",
        )
        val times = mutableListOf<Long>()
        repeat(50) {
            val start = System.nanoTime()
            for (q in queries) {
                engine.process(query = q)
            }
            val elapsed = System.nanoTime() - start
            times.add(elapsed)
        }
        val sorted = times.sorted()
        val avgNs = times.average().toLong()
        val medianNs = sorted[sorted.size / 2]
        val p95Ns = sorted[(sorted.size * 0.95).toInt().coerceAtMost(sorted.size - 1)]
        println("Full Pipeline (50 iterations x 5 queries):")
        println("  avg: ${formatTime(avgNs)}")
        println("  median: ${formatTime(medianNs)}")
        println("  p95: ${formatTime(p95Ns)}")
        assertTrue("Full pipeline avg should be < 50ms: ${avgNs / 1_000_000}ms",
            avgNs < 50_000_000)
    }

    // ═══════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════

    private data class BenchmarkResult(
        val name: String,
        val iterations: Int,
        val minNs: Long,
        val maxNs: Long,
        val avgNs: Long,
        val medianNs: Long,
        val p95Ns: Long,
    )

    private fun printResult(r: BenchmarkResult) {
        println("""
            |=== ${r.name} (${r.iterations} iterations) ===
            |  min:    ${formatTime(r.minNs)}
            |  avg:    ${formatTime(r.avgNs)}
            |  median: ${formatTime(r.medianNs)}
            |  p95:    ${formatTime(r.p95Ns)}
            |  max:    ${formatTime(r.maxNs)}
        """.trimMargin())
    }

    private fun formatTime(ns: Long): String {
        return when {
            ns < 1_000 -> "${ns}ns"
            ns < 1_000_000 -> "${ns / 1_000}us"
            ns < 1_000_000_000 -> "${ns / 1_000_000}ms"
            else -> "${ns / 1_000_000_000}s"
        }
    }
}
