package com.bioacupunt.mtc.knowledge.domain

import com.bioacupunt.mtc.knowledge.repository.KnowledgeGraphRepository
import com.bioacupunt.mtc.knowledge.repository.KnowledgeRepository

/**
 * Differential Engine — generates, scores, and ranks pattern candidates.
 *
 * Pipeline:
 * ClinicalObservation → Candidate Generation → Graph Expansion → Evidence Analysis
 * → Contradiction Analysis → Scoring → Ranking
 *
 * This is deterministic Kotlin — no LLM calls. The LLM may later explain the result,
 * but it never generates the ranking itself.
 *
 * R1/R2/R4 intact.
 */
class DifferentialEngine(
    private val knowledgeRepository: KnowledgeRepository,
    private val graphRepository: KnowledgeGraphRepository,
    private val evidenceEngine: EvidenceEngine,
    private val scoringConfig: DifferentialScoringConfig = DifferentialScoringConfig(),
    private val evidenceConfig: EvidenceScoringConfig = EvidenceScoringConfig(),
) {

    /**
     * Run differential analysis on a clinical observation.
     * Returns ranked candidates with evidence traces and missing data.
     */
    suspend fun analyze(observation: ClinicalObservation): DifferentialResult {
        // 1. Generate candidates from Knowledge Core
        val candidates = generateCandidates(observation)

        // 2. Expand via graph and gather evidence
        val expandedCandidates = candidates.map { candidate ->
            expandCandidate(candidate, observation)
        }

        // 3. Score and rank
        val scoredCandidates = expandedCandidates.map { candidate ->
            scoreCandidate(candidate, observation)
        }.sortedByDescending { it.score }

        // 4. Identify missing data
        val missingData = identifyMissingData(scoredCandidates, observation)

        // 5. Determine overall confidence
        val confidence = determineConfidence(scoredCandidates)

        return DifferentialResult(
            candidates = scoredCandidates,
            missingInformation = missingData,
            confidence = confidence,
        )
    }

    /**
     * Generate initial candidates from Knowledge Core.
     * Candidates come from the graph — not a hardcoded list.
     */
    private suspend fun generateCandidates(observation: ClinicalObservation): List<DifferentialCandidate> {
        val candidates = mutableListOf<DifferentialCandidate>()
        val seenIds = mutableSetOf<String>()

        // Search by symptoms
        for (symptom in observation.symptoms) {
            val results = knowledgeRepository.search(symptom, limit = 10)
            for (entity in results) {
                if (entity.id !in seenIds && entity.type == KnowledgeEntityType.PATTERN) {
                    seenIds.add(entity.id)
                    candidates.add(DifferentialCandidate(
                        entityId = entity.id,
                        entityName = entity.canonicalName,
                        entityType = entity.type,
                        score = 0.0, // Will be scored later
                    ))
                }
            }
        }

        // Search by tongue findings
        for (finding in observation.tongueFindings) {
            val results = knowledgeRepository.search(finding, limit = 5)
            for (entity in results) {
                if (entity.id !in seenIds && entity.type == KnowledgeEntityType.PATTERN) {
                    seenIds.add(entity.id)
                    candidates.add(DifferentialCandidate(
                        entityId = entity.id,
                        entityName = entity.canonicalName,
                        entityType = entity.type,
                        score = 0.0,
                    ))
                }
            }
        }

        // Search by pulse findings
        for (finding in observation.pulseFindings) {
            val results = knowledgeRepository.search(finding, limit = 5)
            for (entity in results) {
                if (entity.id !in seenIds && entity.type == KnowledgeEntityType.PATTERN) {
                    seenIds.add(entity.id)
                    candidates.add(DifferentialCandidate(
                        entityId = entity.id,
                        entityName = entity.canonicalName,
                        entityType = entity.type,
                        score = 0.0,
                    ))
                }
            }
        }

        // Search by Zang-Fu patterns
        for (pattern in observation.zangFuPatterns) {
            val results = knowledgeRepository.search(pattern, limit = 5)
            for (entity in results) {
                if (entity.id !in seenIds && entity.type == KnowledgeEntityType.PATTERN) {
                    seenIds.add(entity.id)
                    candidates.add(DifferentialCandidate(
                        entityId = entity.id,
                        entityName = entity.canonicalName,
                        entityType = entity.type,
                        score = 0.0,
                    ))
                }
            }
        }

        return candidates
    }

    /**
     * Expand a candidate via graph traversal and gather evidence.
     */
    private suspend fun expandCandidate(
        candidate: DifferentialCandidate,
        observation: ClinicalObservation,
    ): DifferentialCandidate {
        // Get graph neighbors to find related evidence
        val traversal = graphRepository.reachable(candidate.entityId)

        // Resolve evidence traces
        val evidenceTraces = evidenceEngine.resolveEvidenceBatch(
            traversal.visitedEntities.filter { it != candidate.entityId }
        )

        // Separate supporting and contradicting
        val supporting = evidenceTraces.filter { it.supportingEvidence.isNotEmpty() }
        val contradicting = evidenceTraces.filter { it.contradictingEvidence.isNotEmpty() }

        // Build reasoning paths from graph traversal
        val reasoningPaths = traversal.paths

        return candidate.copy(
            supportingTraces = supporting,
            contradictingTraces = contradicting,
            reasoningPaths = reasoningPaths,
        )
    }

    /**
     * Score a candidate based on observation matches, evidence, and contradictions.
     */
    private fun scoreCandidate(
        candidate: DifferentialCandidate,
        observation: ClinicalObservation,
    ): DifferentialCandidate {
        var score = 0.0

        // 1. Match score: how many observation features match the candidate's relations
        val matchScore = calculateMatchScore(candidate, observation)
        score += matchScore * scoringConfig.matchWeight

        // 2. Evidence score: aggregate confidence from supporting evidence
        val evidenceScore = candidate.supportingTraces
            .map { it.confidence }
            .average()
            .takeIf { !it.isNaN() } ?: 0.0
        score += evidenceScore * scoringConfig.evidenceWeight

        // 3. Relation confidence: average confidence of incoming edges
        val relationConfidence = candidate.reasoningPaths
            .flatMap { it.edges }
            .mapNotNull { it.confidence }
            .average()
            .takeIf { !it.isNaN() } ?: 0.0
        score += relationConfidence * scoringConfig.relationConfidenceWeight

        // 4. Source quality: based on evidence levels
        val sourceQuality = calculateSourceQuality(candidate)
        score += sourceQuality * scoringConfig.sourceQualityWeight

        // 5. Contradiction penalty
        val contradictionPenalty = candidate.contradictingTraces.size * scoringConfig.contradictionPenalty
        score -= contradictionPenalty

        return candidate.copy(score = score.coerceIn(0.0, 1.0))
    }

    /**
     * Calculate match score between candidate and observation.
     * Counts how many observation features appear in the candidate's graph neighborhood.
     */
    private fun calculateMatchScore(
        candidate: DifferentialCandidate,
        observation: ClinicalObservation,
    ): Double {
        var matches = 0
        var total = 0

        // Check symptom matches
        total += observation.symptoms.size
        for (symptom in observation.symptoms) {
            if (candidate.entityName.contains(symptom, ignoreCase = true) ||
                candidate.supportingTraces.any { it.entityName.contains(symptom, ignoreCase = true) }) {
                matches++
            }
        }

        // Check tongue matches
        total += observation.tongueFindings.size
        for (finding in observation.tongueFindings) {
            if (candidate.entityName.contains(finding, ignoreCase = true) ||
                candidate.supportingTraces.any { it.entityName.contains(finding, ignoreCase = true) }) {
                matches++
            }
        }

        // Check pulse matches
        total += observation.pulseFindings.size
        for (finding in observation.pulseFindings) {
            if (candidate.entityName.contains(finding, ignoreCase = true) ||
                candidate.supportingTraces.any { it.entityName.contains(finding, ignoreCase = true) }) {
                matches++
            }
        }

        return if (total > 0) matches.toDouble() / total else 0.0
    }

    /**
     * Calculate source quality based on evidence levels.
     */
    private fun calculateSourceQuality(candidate: DifferentialCandidate): Double {
        val allEvidence = candidate.supportingTraces.flatMap { it.supportingEvidence }
        if (allEvidence.isEmpty()) return 0.0

        val levelScores = allEvidence.map { evidence ->
            evidenceConfig.levelBonuses[evidence.level] ?: 0.0
        }
        return levelScores.average().coerceIn(0.0, 1.0)
    }

    /**
     * Identify what data is missing that would help differentiate top candidates.
     */
    private suspend fun identifyMissingData(
        candidates: List<DifferentialCandidate>,
        observation: ClinicalObservation,
    ): List<MissingDataItem> {
        if (candidates.size < 2) return emptyList()

        val missing = mutableListOf<MissingDataItem>()

        // If tongue data is missing, it would help differentiate
        if (observation.tongueFindings.isEmpty()) {
            missing.add(MissingDataItem(
                observationType = "TONGUE",
                description = "Características da língua (cor, forma, saburra)",
                impact = "Diferenciaria padrões de Calor/Frio, Deficiência/Excesso",
                priority = 1,
            ))
        }

        // If pulse data is missing
        if (observation.pulseFindings.isEmpty()) {
            missing.add(MissingDataItem(
                observationType = "PULSE",
                description = "Qualidades do pulso (velocidade, profundidade, forma)",
                impact = "Diferenciaria padrões por profundidade e velocidade",
                priority = 2,
            ))
        }

        // If Ba Gang data is incomplete
        if (observation.baGang == null) {
            missing.add(MissingDataItem(
                observationType = "BAGANG",
                description = "Classificação Ba Gang (polaridade, profundidade, temperatura, força)",
                impact = "Reduziria candidatos para o eixo correto",
                priority = 1,
            ))
        }

        // If no symptoms registered
        if (observation.symptoms.isEmpty()) {
            missing.add(MissingDataItem(
                observationType = "SYMPTOM",
                description = "Sintomas relatados pela paciente",
                impact = "Base para identificação de padrões",
                priority = 1,
            ))
        }

        // If no history
        if (observation.history.isEmpty()) {
            missing.add(MissingDataItem(
                observationType = "HISTORY",
                description = "Histórico da doença e evolução temporal",
                impact = "Diferenciaria padrões agudos de crônicos",
                priority = 3,
            ))
        }

        return missing.sortedBy { it.priority }
    }

    /**
     * Determine overall confidence based on top candidates.
     */
    private fun determineConfidence(candidates: List<DifferentialCandidate>): com.bioacupunt.prontuario.domain.model.ConfidenceLevel {
        if (candidates.isEmpty()) return com.bioacupunt.prontuario.domain.model.ConfidenceLevel.INSUFFICIENT_EVIDENCE

        val topScore = candidates.firstOrNull()?.score ?: 0.0
        val hasContradictions = candidates.any { it.contradictingTraces.isNotEmpty() }

        return when {
            topScore >= 0.7 && !hasContradictions -> com.bioacupunt.prontuario.domain.model.ConfidenceLevel.HIGH
            topScore >= 0.4 -> com.bioacupunt.prontuario.domain.model.ConfidenceLevel.MODERATE
            topScore >= 0.2 -> com.bioacupunt.prontuario.domain.model.ConfidenceLevel.LOW
            else -> com.bioacupunt.prontuario.domain.model.ConfidenceLevel.INSUFFICIENT_EVIDENCE
        }
    }
}
