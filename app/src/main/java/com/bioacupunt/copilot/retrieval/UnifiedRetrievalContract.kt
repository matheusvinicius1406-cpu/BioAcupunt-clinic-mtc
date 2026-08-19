package com.bioacupunt.copilot.retrieval

import com.bioacupunt.mtc.knowledge.domain.KnowledgeEntity
import com.bioacupunt.mtc.knowledge.domain.KnowledgeEntityType

/**
 * §4 UNIFIED RETRIEVAL CONTRACT
 *
 * Single contract for all retrieval backends (lexical, vector, graph, metadata).
 * Backend-agnostic: no knowledge of FTS, sqlite-vec, or Room internals.
 *
 * Architecture:
 * ```text
 * User Query
 *     ↓
 * IntentDetector → IntentType
 *     ↓
 * EntityRecognizer → List<RecognizedEntity>
 *     ↓
 * UnifiedRetrievalRequest → HybridRetriever
 *     ↓
 * UnifiedRetrievalResult → Reranker → GroundedResponseGenerator
 * ```
 */
data class UnifiedRetrievalRequest(
    val query: String,
    val normalizedQuery: String = query,
    val expandedTerms: List<String> = emptyList(),
    val recognizedEntities: List<RecognizedEntity> = emptyList(),
    val patientContext: PatientContext? = null,
    val clinicalContext: ClinicalContext? = null,
    val filters: RetrievalFilters = RetrievalFilters(),
    val maxResults: Int = 50,
    val requiredEvidence: Boolean = true,
    val allowedSources: Set<String> = emptySet(),
    val intent: IntentType = IntentType.KNOWLEDGE_SEARCH,
)

data class UnifiedRetrievalResult(
    val results: List<RetrievalHit>,
    val totalCandidates: Int,
    val retrievalLatencyMs: Long,
    val sourceBreakdown: Map<String, Int>,
) {
    val hasResults: Boolean get() = results.isNotEmpty()
    val bestScore: Double get() = results.maxOfOrNull { it.rerankScore } ?: 0.0
}

data class RetrievalHit(
    val entityId: String,
    val documentId: String? = null,
    val sourceId: String? = null,
    val entity: KnowledgeEntity? = null,
    val content: String = "",
    val score: Double = 0.0,
    val normalizedScore: Double = 0.0,
    val rerankScore: Double = 0.0,
    val sourceType: RetrievalSource = RetrievalSource.LEXICAL,
    val evidenceIds: List<String> = emptyList(),
    val provenance: String? = null,
    val knowledgeVersion: String? = null,
    val graphDepth: Int = 0,
    val metadata: Map<String, String> = emptyMap(),
)

enum class RetrievalSource {
    LEXICAL,
    VECTOR,
    GRAPH,
    METADATA,
    HYBRID,
}

data class RecognizedEntity(
    val text: String,
    val entityType: KnowledgeEntityType,
    val entityId: String? = null,
    val confidence: Double = 1.0,
)

data class PatientContext(
    val patientId: Long,
    val activePatient: Boolean = true,
    val currentAssessment: String? = null,
    val recentObservations: List<String> = emptyList(),
    val relevantHistory: List<String> = emptyList(),
)

data class ClinicalContext(
    val assessment: String? = null,
    val symptoms: List<String> = emptyList(),
    val patterns: List<String> = emptyList(),
    val flags: List<String> = emptyList(),
)

data class RetrievalFilters(
    val entityType: KnowledgeEntityType? = null,
    val status: String? = null,
    val evidenceLevel: String? = null,
    val sourceType: String? = null,
    val knowledgeVersion: String? = null,
    val clinicalDomain: String? = null,
    val tenantId: Long? = null,
)

/**
 * Score normalization configuration.
 * Weights are applied BEFORE reranking.
 */
data class RetrievalScoringConfig(
    val lexicalWeight: Double = 0.35,
    val vectorWeight: Double = 0.30,
    val graphWeight: Double = 0.25,
    val metadataBoost: Double = 0.10,
    val evidenceBoost: Double = 0.15,
) {
    init {
        require(lexicalWeight + vectorWeight + graphWeight in 0.9..1.1) {
            "Weights must sum to ~1.0: lexical=$lexicalWeight + vector=$vectorWeight + graph=$graphWeight"
        }
    }
}

enum class IntentType {
    KNOWLEDGE_SEARCH,
    CLINICAL_ANALYSIS,
    PATIENT_SUMMARY,
    DIFFERENTIAL_EXPLANATION,
    MISSING_DATA,
    EVIDENCE_LOOKUP,
    POINT_LOOKUP,
    FORMULA_LOOKUP,
    PROTOCOL_LOOKUP,
    RESEARCH_QUERY,
    GENERAL_CLINICAL_QUERY,
}
