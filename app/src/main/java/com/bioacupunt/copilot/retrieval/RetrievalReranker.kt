package com.bioacupunt.copilot.retrieval

/**
 * §16-17 RETRIEVAL RERANKER
 *
 * Deterministic reranking of hybrid retrieval results.
 * Same input + same config → same output, ALWAYS (tested explicitly in §17).
 *
 * Ranking factors:
 * 1. Query relevance (normalized score from retrieval)
 * 2. Entity exactness (exact match vs fuzzy)
 * 3. Graph proximity (closer to query entity = better)
 * 4. Evidence quality (more evidence = higher)
 * 5. Source quality (approved > draft)
 * 6. Clinical context (patient-relevant > generic)
 *
 * LLM is NOT involved in ranking — this is fully deterministic.
 */
class RetrievalReranker(
    private val config: RerankerConfig = RerankerConfig(),
) {

    data class RerankerConfig(
        val queryRelevanceWeight: Double = 0.30,
        val entityExactnessWeight: Double = 0.20,
        val graphProximityWeight: Double = 0.15,
        val evidenceQualityWeight: Double = 0.15,
        val sourceQualityWeight: Double = 0.10,
        val clinicalContextWeight: Double = 0.10,
    )

    data class RerankedResult(
        val hit: RetrievalHit,
        val rerankScore: Double,
        val factors: Map<String, Double>,
    )

    /**
     * Rerank a list of retrieval hits.
     * Deterministic: same input → same output, always.
     */
    fun rerank(
        hits: List<RetrievalHit>,
        query: String,
        patientContext: PatientContext? = null,
    ): List<RerankedResult> {
        return hits.map { hit ->
            val factors = calculateFactors(hit, query, patientContext)
            val rerankScore = calculateWeightedScore(factors)
            RerankedResult(
                hit = hit.copy(rerankScore = rerankScore),
                rerankScore = rerankScore,
                factors = factors,
            )
        }.sortedByDescending { it.rerankScore }
    }

    private fun calculateFactors(
        hit: RetrievalHit,
        query: String,
        patientContext: PatientContext?,
    ): Map<String, Double> {
        val queryLower = query.lowercase()
        val contentLower = hit.content.lowercase()
        val entityName = hit.entity?.canonicalName?.lowercase() ?: ""

        // 1. Query relevance: how well does the hit match the query?
        val queryRelevance = when {
            entityName.contains(queryLower) || queryLower.contains(entityName) -> 1.0
            contentLower.contains(queryLower) -> 0.8
            queryLower.split(" ").any { contentLower.contains(it) } -> 0.6
            else -> hit.normalizedScore * 0.5
        }

        // 2. Entity exactness: exact match vs fuzzy
        val entityExactness = when {
            entityName == queryLower -> 1.0
            entityName.startsWith(queryLower) -> 0.9
            queryLower.startsWith(entityName) -> 0.85
            else -> 0.5
        }

        // 3. Graph proximity: closer to query entity = better
        val graphProximity = when (hit.graphDepth) {
            0 -> 1.0
            1 -> 0.8
            2 -> 0.6
            3 -> 0.4
            else -> 0.2
        }

        // 4. Evidence quality: more evidence = higher
        val evidenceQuality = when {
            hit.evidenceIds.size >= 3 -> 1.0
            hit.evidenceIds.size == 2 -> 0.8
            hit.evidenceIds.size == 1 -> 0.6
            else -> 0.3
        }

        // 5. Source quality: approved > draft
        val sourceQuality = when (hit.entity?.version?.status?.name) {
            "APPROVED" -> 1.0
            "VERIFIED" -> 0.9
            "DRAFT" -> 0.5
            else -> 0.7
        }

        // 6. Clinical context: patient-relevant > generic
        val clinicalContext = if (patientContext != null && patientContext.activePatient) {
            when {
                hit.entity?.type?.name == "SYMPTOM" -> 0.9
                hit.entity?.type?.name == "PATTERN" -> 0.85
                hit.entity?.type?.name == "PROTOCOL" -> 0.8
                else -> 0.6
            }
        } else {
            0.5
        }

        return mapOf(
            "queryRelevance" to queryRelevance.coerceIn(0.0, 1.0),
            "entityExactness" to entityExactness.coerceIn(0.0, 1.0),
            "graphProximity" to graphProximity.coerceIn(0.0, 1.0),
            "evidenceQuality" to evidenceQuality.coerceIn(0.0, 1.0),
            "sourceQuality" to sourceQuality.coerceIn(0.0, 1.0),
            "clinicalContext" to clinicalContext.coerceIn(0.0, 1.0),
        )
    }

    private fun calculateWeightedScore(factors: Map<String, Double>): Double {
        return factors["queryRelevance"]!! * config.queryRelevanceWeight +
            factors["entityExactness"]!! * config.entityExactnessWeight +
            factors["graphProximity"]!! * config.graphProximityWeight +
            factors["evidenceQuality"]!! * config.evidenceQualityWeight +
            factors["sourceQuality"]!! * config.sourceQualityWeight +
            factors["clinicalContext"]!! * config.clinicalContextWeight
    }
}
