package com.bioacupunt.copilot

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
import com.bioacupunt.mtc.knowledge.domain.ClinicalIntelligenceResult

/**
 * §34-37 CLINICAL COPILOT ENGINE
 *
 * The orchestrator that ties everything together:
 * ```text
 * interpret request
 *     ↓
 * select tool/use case
 *     ↓
 * retrieve context
 *     ↓
 * execute deterministic engines
 *     ↓
 * build context
 *     ↓
 * generate response
 *     ↓
 * validate response
 *     ↓
 * return grounded result
 * ```
 *
 * The LLM can ONLY call explicit tools — never accesses Room, SQL, DAO, filesystem.
 */
class ClinicalCopilotEngine(
    private val intentDetector: IntentDetector,
    private val entityRecognizer: EntityRecognizer,
    private val queryNormalizer: QueryNormalizer,
    private val hybridRetriever: HybridRetriever,
    private val reranker: RetrievalReranker,
    private val contextBuilder: ContextBuilder,
    private val evidenceGate: EvidenceGate,
    private val evidenceResolutionService: EvidenceResolutionService,
    private val groundedResponseGenerator: GroundedResponseGenerator,
    private val responseValidator: ResponseValidator,
    private val clinicalIntelligenceIntegration: ClinicalIntelligenceIntegration,
    private val patientContextProvider: PatientContextProvider,
    private val explainDifferentialUseCase: ExplainDifferentialUseCase,
    private val explainMissingDataUseCase: ExplainMissingDataUseCase,
    private val copilotRouter: CopilotRouter,
) {

    data class CopilotResult(
        val response: GroundedResponseGenerator.GroundedResponse,
        val validationReport: ResponseValidator.ValidationReport,
        val intent: IntentType,
        val retrievalResult: UnifiedRetrievalResult?,
        val latencyMs: Long,
        val gateResult: EvidenceGate.GateResult? = null,
    )

    /**
     * Main entry point for the copilot.
     * Routes to the appropriate tool/use case based on intent.
     */
    suspend fun process(
        query: String,
        patientId: Long? = null,
        activePatientId: Long? = null,
        sessionId: String? = null,
        clinicalIntelligenceResult: ClinicalIntelligenceResult? = null,
    ): CopilotResult {
        val startTime = System.currentTimeMillis()

        // 1. Detect intent
        val intentResult = intentDetector.detect(query)

        // 2. Route to appropriate tool
        val route = copilotRouter.route(intentResult.intent)

        // 3. Normalize query
        val normalized = queryNormalizer.normalize(query)

        // 4. Recognize entities
        val entityResult = entityRecognizer.recognize(normalized.normalizedQuery)

        // 5. Build patient context (if applicable)
        val patientContext = if (patientId != null && route.requiresPatientContext) {
            patientContextProvider.buildContext(patientId, activePatientId, sessionId)
        } else null

        // 6. Execute tool
        val toolResult = when (route.tool) {
            CopilotTool.KNOWLEDGE_SEARCH -> executeKnowledgeSearch(
                query, normalized, entityResult, patientContext,
            )
            CopilotTool.PATIENT_SUMMARY -> executePatientSummary(patientId)
            CopilotTool.DIFFERENTIAL_EXPLANATION -> executeDifferentialExplanation(query, clinicalIntelligenceResult)
            CopilotTool.MISSING_DATA -> executeMissingDataExplanation(query, clinicalIntelligenceResult)
            CopilotTool.EVIDENCE_LOOKUP -> executeEvidenceLookup(query)
            CopilotTool.POINT_LOOKUP -> executePointLookup(query)
            CopilotTool.FORMULA_LOOKUP -> executeFormulaLookup(query)
            CopilotTool.PROTOCOL_LOOKUP -> executeProtocolLookup(query)
        }

        // 7. Apply EvidenceGate — §24 RAG EVIDENCE GATE
        // This is the SINGLE enforcement point for R2.
        // All paths that call the LLM must go through this gate.
        //
        // If the tool already produced a complete response (not PENDING),
        // use it directly — the tool knows when it has a meaningful answer.
        if (toolResult.response.confidence != "PENDING" && toolResult.response.answer.isNotBlank()) {
            val validationReport = responseValidator.validate(toolResult.response, toolResult.context)
            return CopilotResult(
                response = toolResult.response,
                validationReport = validationReport,
                intent = intentResult.intent,
                retrievalResult = toolResult.retrievalResult,
                latencyMs = System.currentTimeMillis() - startTime,
                gateResult = toolResult.gateResult,
            )
        }

        val gateResult = if (toolResult.gateResult != null) {
            toolResult.gateResult
        } else {
            evidenceGate.evaluate(toolResult.context, route.tool != CopilotTool.PATIENT_SUMMARY)
        }

        // If gate blocks, return immediately — LLM is NEVER called
        // Use the tool's response if it already has one (specific warnings),
        // otherwise use the generic blocked response.
        val finalResponse = when (gateResult.decision) {
            EvidenceGate.GateDecision.BLOCK_NO_EVIDENCE,
            EvidenceGate.GateDecision.BLOCK_INSUFFICIENT_EVIDENCE -> {
                if (toolResult.response.confidence != "PENDING" && toolResult.response.answer.isNotBlank()) {
                    toolResult.response
                } else {
                    val blockedResponse = evidenceGate.getBlockedResponse(gateResult)
                    GroundedResponseGenerator.GroundedResponse(
                        answer = blockedResponse.answer,
                        warnings = blockedResponse.warnings,
                        confidence = blockedResponse.confidence,
                    )
                }
            }
            EvidenceGate.GateDecision.ALLOW -> {
                // 8. Generate grounded response (LLM called here)
                val generatedResponse = groundedResponseGenerator.generate(
                    query, toolResult.context, intentResult.intent, patientContext,
                )

                // 9. Validate response
                val validationReport = responseValidator.validate(generatedResponse, toolResult.context)

                // 10. Qualify unsupported claims if needed
                when (validationReport.result) {
                    ResponseValidator.ValidationResult.HAS_UNSUPPORTED_CLAIMS -> {
                        val unsupported = validationReport.issues
                            .filter { it.type == ResponseValidator.IssueType.UNSUPPORTED_CLAIM }
                            .map { it.description }
                        responseValidator.qualifyClaims(generatedResponse, unsupported)
                    }
                    else -> generatedResponse
                }
            }
        }

        val validationReport = responseValidator.validate(finalResponse, toolResult.context)

        val elapsed = System.currentTimeMillis() - startTime

        return CopilotResult(
            response = finalResponse,
            validationReport = validationReport,
            intent = intentResult.intent,
            retrievalResult = toolResult.retrievalResult,
            latencyMs = elapsed,
            gateResult = gateResult,
        )
    }

    // ========== TOOL EXECUTION ==========

    private data class ToolExecution(
        val response: GroundedResponseGenerator.GroundedResponse,
        val context: ContextBuilder.StructuredContext,
        val retrievalResult: UnifiedRetrievalResult? = null,
        val gateResult: EvidenceGate.GateResult? = null,
    )

    private suspend fun executeKnowledgeSearch(
        query: String,
        normalized: QueryNormalizer.NormalizedQuery,
        entityResult: EntityRecognizer.RecognitionResult,
        patientContext: PatientContext?,
    ): ToolExecution {
        // Build retrieval request
        val request = UnifiedRetrievalRequest(
            query = query,
            normalizedQuery = normalized.normalizedQuery,
            expandedTerms = normalized.expandedTerms,
            recognizedEntities = entityResult.entities,
            patientContext = patientContext,
            intent = IntentType.KNOWLEDGE_SEARCH,
        )

        // Retrieve
        val retrievalResult = hybridRetriever.retrieve(request)

        // Rerank
        val reranked = reranker.rerank(retrievalResult.results, query, patientContext)

        // Build context
        val rerankedHits = reranked.map { it.hit }
        val retrievalWithReranked = retrievalResult.copy(results = rerankedHits)
        val context = contextBuilder.build(retrievalWithReranked, query)

        // §24: RAG EVIDENCE GATE — evaluate here, enforced in process()
        val gateResult = evidenceGate.evaluate(context, request.requiredEvidence)

        return ToolExecution(
            response = GroundedResponseGenerator.GroundedResponse(
                answer = "", // Will be filled by process() if gate allows LLM call
                confidence = "PENDING",
            ),
            context = context,
            retrievalResult = retrievalResult,
            gateResult = gateResult,
        )
    }

    private suspend fun executePatientSummary(patientId: Long?): ToolExecution {
        val summary = patientContextProvider.generateSummary(patientId ?: 0L)
        val context = ContextBuilder.StructuredContext(
            items = emptyList(),
            totalCharacters = 0,
            totalTokens = 0,
            truncated = false,
            evidenceIds = emptyList(),
        )

        return if (summary != null) {
            ToolExecution(
                response = GroundedResponseGenerator.GroundedResponse(
                    answer = "Resumo do paciente: ${summary.recentSessions} sessões recentes. " +
                        "Observações: ${summary.keyObservations.joinToString("; ")}. " +
                        "Avaliação atual: ${summary.currentAssessment ?: "Não registrada"}. " +
                        "⚠️ Rascunho — revisão humana necessária antes de persistir.",
                    confidence = "MODERATE",
                    warnings = listOf("DRAFT_SUMMARY"),
                ),
                context = context,
            )
        } else {
            ToolExecution(
                response = GroundedResponseGenerator.GroundedResponse(
                    answer = "Não foi possível gerar resumo do paciente.",
                    warnings = listOf("PATIENT_CONTEXT_UNAVAILABLE"),
                    confidence = "INSUFFICIENT",
                ),
                context = context,
                gateResult = EvidenceGate.GateResult(
                    decision = EvidenceGate.GateDecision.BLOCK_NO_EVIDENCE,
                    reason = "Patient context unavailable",
                    evidenceCount = 0,
                    contextItemCount = 0,
                ),
            )
        }
    }

    private suspend fun executeDifferentialExplanation(
        query: String,
        clinicalIntelligenceResult: ClinicalIntelligenceResult?,
    ): ToolExecution {
        // Differential explanation requires actual clinical intelligence results.
        // §7: The LLM CANNOT alter ranking — ranking comes from ClinicalIntelligenceEngine.
        val result = clinicalIntelligenceResult ?: return ToolExecution(
            response = GroundedResponseGenerator.GroundedResponse(
                answer = "Resultado de inteligência clínica não disponível. Execute a análise clínica primeiro.",
                warnings = listOf("NO_CLINICAL_INTELLIGENCE"),
                confidence = "INSUFFICIENT",
            ),
            context = ContextBuilder.StructuredContext(
                items = emptyList(), totalCharacters = 0, totalTokens = 0,
                truncated = false, evidenceIds = emptyList(),
            ),
            gateResult = EvidenceGate.GateResult(
                decision = EvidenceGate.GateDecision.BLOCK_NO_EVIDENCE,
                reason = "No clinical intelligence result provided",
                evidenceCount = 0,
                contextItemCount = 0,
            ),
        )

        // Find top two candidates to compare
        val topCandidates = result.rankedHypotheses
        val entityA = topCandidates.getOrNull(0)?.entityId ?: ""
        val entityB = topCandidates.getOrNull(1)?.entityId ?: ""

        val explanation = if (entityA.isNotBlank() && entityB.isNotBlank()) {
            explainDifferentialUseCase.explain(result, entityA, entityB)
        } else null
        val context = ContextBuilder.StructuredContext(
            items = emptyList(),
            totalCharacters = 0,
            totalTokens = 0,
            truncated = false,
            evidenceIds = emptyList(),
        )
        val gateResult = evidenceGate.evaluate(context, requiredEvidence = false)
        val response = if (explanation != null) {
            GroundedResponseGenerator.GroundedResponse(
                answer = "Ranking de diferenciais: ${explanation.candidateA} (${String.format("%.2f", explanation.scoreA)}) vs ${explanation.candidateB} (${String.format("%.2f", explanation.scoreB)}). " +
                    "Diferença: ${String.format("%.2f", explanation.rankingDifference)}. " +
                    "Evidências para A: ${explanation.supportingEvidenceForA.size}, para B: ${explanation.supportingEvidenceForB.size}.",
                confidence = explanation.confidence,
            )
        } else {
            GroundedResponseGenerator.GroundedResponse(
                answer = "Não foi possível gerar explicação de diferenciais. Verifique se os candidatos estão disponíveis.",
                warnings = listOf("NO_DIFFERENTIAL_DATA"),
                confidence = "INSUFFICIENT",
            )
        }
        return ToolExecution(response = response, context = context, gateResult = gateResult)
    }

    private suspend fun executeMissingDataExplanation(
        query: String,
        clinicalIntelligenceResult: ClinicalIntelligenceResult?,
    ): ToolExecution {
        val result = clinicalIntelligenceResult ?: return ToolExecution(
            response = GroundedResponseGenerator.GroundedResponse(
                answer = "Resultado de inteligência clínica não disponível. Execute a análise clínica primeiro.",
                warnings = listOf("NO_CLINICAL_INTELLIGENCE"),
                confidence = "INSUFFICIENT",
            ),
            context = ContextBuilder.StructuredContext(
                items = emptyList(), totalCharacters = 0, totalTokens = 0,
                truncated = false, evidenceIds = emptyList(),
            ),
            gateResult = EvidenceGate.GateResult(
                decision = EvidenceGate.GateDecision.BLOCK_NO_EVIDENCE,
                reason = "No clinical intelligence result provided",
                evidenceCount = 0,
                contextItemCount = 0,
            ),
        )

        val explanation = explainMissingDataUseCase.explain(result)
        val context = ContextBuilder.StructuredContext(
            items = emptyList(),
            totalCharacters = 0,
            totalTokens = 0,
            truncated = false,
            evidenceIds = emptyList(),
        )
        val gateResult = evidenceGate.evaluate(context, requiredEvidence = false)
        val response = GroundedResponseGenerator.GroundedResponse(
            answer = explanation.summary,
            confidence = if (explanation.totalMissing == 0) "HIGH" else "MODERATE",
        )
        return ToolExecution(response = response, context = context, gateResult = gateResult)
    }

    private suspend fun executeEvidenceLookup(query: String): ToolExecution {
        val request = UnifiedRetrievalRequest(
            query = query,
            intent = IntentType.EVIDENCE_LOOKUP,
        )
        val retrievalResult = hybridRetriever.retrieve(request)
        val context = contextBuilder.build(retrievalResult, query)
        val gateResult = evidenceGate.evaluate(context, requiredEvidence = true)
        return ToolExecution(
            response = GroundedResponseGenerator.GroundedResponse(answer = "", confidence = "PENDING"),
            context = context,
            retrievalResult = retrievalResult,
            gateResult = gateResult,
        )
    }

    private suspend fun executePointLookup(query: String): ToolExecution {
        val request = UnifiedRetrievalRequest(
            query = query,
            intent = IntentType.POINT_LOOKUP,
            filters = RetrievalFilters(
                entityType = com.bioacupunt.mtc.knowledge.domain.KnowledgeEntityType.ACUPOINT,
            ),
        )
        val retrievalResult = hybridRetriever.retrieve(request)
        val context = contextBuilder.build(retrievalResult, query)
        val gateResult = evidenceGate.evaluate(context, requiredEvidence = true)
        return ToolExecution(
            response = GroundedResponseGenerator.GroundedResponse(answer = "", confidence = "PENDING"),
            context = context,
            retrievalResult = retrievalResult,
            gateResult = gateResult,
        )
    }

    private suspend fun executeFormulaLookup(query: String): ToolExecution {
        val request = UnifiedRetrievalRequest(
            query = query,
            intent = IntentType.FORMULA_LOOKUP,
            filters = RetrievalFilters(
                entityType = com.bioacupunt.mtc.knowledge.domain.KnowledgeEntityType.FORMULA,
            ),
        )
        val retrievalResult = hybridRetriever.retrieve(request)
        val context = contextBuilder.build(retrievalResult, query)
        val gateResult = evidenceGate.evaluate(context, requiredEvidence = true)
        return ToolExecution(
            response = GroundedResponseGenerator.GroundedResponse(answer = "", confidence = "PENDING"),
            context = context,
            retrievalResult = retrievalResult,
            gateResult = gateResult,
        )
    }

    private suspend fun executeProtocolLookup(query: String): ToolExecution {
        val request = UnifiedRetrievalRequest(
            query = query,
            intent = IntentType.PROTOCOL_LOOKUP,
            filters = RetrievalFilters(
                entityType = com.bioacupunt.mtc.knowledge.domain.KnowledgeEntityType.PROTOCOL,
            ),
        )
        val retrievalResult = hybridRetriever.retrieve(request)
        val context = contextBuilder.build(retrievalResult, query)
        val gateResult = evidenceGate.evaluate(context, requiredEvidence = true)
        return ToolExecution(
            response = GroundedResponseGenerator.GroundedResponse(answer = "", confidence = "PENDING"),
            context = context,
            retrievalResult = retrievalResult,
            gateResult = gateResult,
        )
    }
}
