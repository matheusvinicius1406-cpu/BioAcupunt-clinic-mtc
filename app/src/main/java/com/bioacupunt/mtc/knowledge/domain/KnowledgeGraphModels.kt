package com.bioacupunt.mtc.knowledge.domain

// ── Clinical Observation ─────────────────────────────────────────────

/**
 * Structured clinical observation extracted from a patient assessment.
 * This is the input to the Clinical Intelligence pipeline.
 *
 * All fields are nullable — absence of data is explicit, never inferred.
 * The system derives what it can from the Knowledge Graph; it never invents.
 */
data class ClinicalObservation(
    val symptoms: List<String> = emptyList(),
    val signs: List<String> = emptyList(),
    val tongueFindings: List<String> = emptyList(),
    val pulseFindings: List<String> = emptyList(),
    val etiology: List<String> = emptyList(),
    val history: List<String> = emptyList(),
    val baGang: BaGangData? = null,
    val zangFuPatterns: List<String> = emptyList(),
    val duration: String? = null,
    val severity: String? = null,
    val context: Map<String, String> = emptyMap(),
)

data class BaGangData(
    val polarity: String? = null,   // YIN / YANG
    val depth: String? = null,      // EXTERIOR / INTERIOR
    val temperature: String? = null, // COLD / HEAT
    val strength: String? = null,   // DEFICIENCY / EXCESS
)

// ── Knowledge Graph ─────────────────────────────────────────────────

/**
 * A directed edge in the knowledge graph.
 */
data class GraphEdge(
    val sourceId: String,
    val relationType: KnowledgeRelationType,
    val targetId: String,
    val confidence: Double? = null,
    val evidenceIds: List<String> = emptyList(),
)

/**
 * A path through the knowledge graph (sequence of edges).
 */
data class GraphPath(
    val edges: List<GraphEdge>,
    val entityIds: List<String>,
)

/**
 * Result of a graph traversal operation.
 */
data class GraphTraversalResult(
    val visitedEntities: List<String>,
    val relations: List<GraphEdge>,
    val paths: List<GraphPath> = emptyList(),
)

// ── Evidence ────────────────────────────────────────────────────────

/**
 * Evidence supporting or contradicting a hypothesis.
 */
data class EvidenceItem(
    val id: String,
    val claim: String,
    val level: String? = null,      // TRADITION, MODERN_LITERATURE, CLINICAL_EVIDENCE, etc.
    val confidence: Double? = null,
    val citationIds: List<String> = emptyList(),
    val sourceId: String? = null,
    val sourceName: String? = null,
    val locator: String? = null,    // "p. 245" or "doi:10.1234/..."
    val excerpt: String? = null,
)

/**
 * Complete evidence trace for a hypothesis.
 */
data class EvidenceTrace(
    val entityId: String,
    val entityName: String,
    val entityType: KnowledgeEntityType,
    val relationType: KnowledgeRelationType? = null,
    val supportingEvidence: List<EvidenceItem> = emptyList(),
    val contradictingEvidence: List<EvidenceItem> = emptyList(),
    val confidence: Double = 0.0,
)

/**
 * Centralized scoring configuration for evidence evaluation.
 */
data class EvidenceScoringConfig(
    val baseSupportPerEvidence: Double = 0.15,
    val maxSupportFromEvidence: Double = 0.60,
    val confidenceWeight: Double = 0.30,
    val levelBonuses: Map<String, Double> = mapOf(
        "TRADITION" to 0.05,
        "MODERN_LITERATURE" to 0.10,
        "CLINICAL_EVIDENCE" to 0.20,
    ),
    val contradictionPenalty: Double = 0.25,
    val minConfidence: Double = 0.0,
    val maxConfidence: Double = 1.0,
)

// ── Differential ────────────────────────────────────────────────────

/**
 * A candidate hypothesis in the differential ranking.
 */
data class DifferentialCandidate(
    val entityId: String,
    val entityName: String,
    val entityType: KnowledgeEntityType,
    val score: Double,
    val supportingTraces: List<EvidenceTrace> = emptyList(),
    val contradictingTraces: List<EvidenceTrace> = emptyList(),
    val reasoningPaths: List<GraphPath> = emptyList(),
    val missingData: List<MissingDataItem> = emptyList(),
)

/**
 * Centralized scoring configuration for differential ranking.
 */
data class DifferentialScoringConfig(
    val matchWeight: Double = 0.40,
    val evidenceWeight: Double = 0.30,
    val relationConfidenceWeight: Double = 0.15,
    val sourceQualityWeight: Double = 0.15,
    val contradictionPenalty: Double = 0.30,
)

/**
 * The complete result of a differential analysis.
 */
data class DifferentialResult(
    val candidates: List<DifferentialCandidate>,
    val missingInformation: List<MissingDataItem>,
    val confidence: com.bioacupunt.prontuario.domain.model.ConfidenceLevel,
)

// ── Missing Data ────────────────────────────────────────────────────

/**
 * A piece of information that would help differentiate candidates.
 */
data class MissingDataItem(
    val observationType: String,   // TONGUE, PULSE, SYMPTOM, BAGANG, ZANG_FU, HISTORY
    val description: String,       // "Cor da língua"
    val impact: String,            // "Diferenciaria Padrão A de Padrão B"
    val priority: Int = 1,         // 1 = most important to collect
)

// ── Clinical Intelligence ───────────────────────────────────────────

/**
 * The complete output of the Clinical Intelligence Engine.
 * This is a Clinical Decision Support result — NOT a diagnosis.
 */
data class ClinicalIntelligenceResult(
    val rankedHypotheses: List<DifferentialCandidate>,
    val supportingEvidence: List<EvidenceTrace>,
    val contradictingEvidence: List<EvidenceTrace>,
    val reasoningPaths: List<GraphPath>,
    val missingInformation: List<MissingDataItem>,
    val confidence: com.bioacupunt.prontuario.domain.model.ConfidenceLevel,
    val knowledgeVersion: String = "",
    val timestamp: Long = System.currentTimeMillis(),
)
