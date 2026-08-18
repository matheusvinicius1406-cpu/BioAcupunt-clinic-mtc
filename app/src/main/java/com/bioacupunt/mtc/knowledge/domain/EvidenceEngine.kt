package com.bioacupunt.mtc.knowledge.domain

import com.bioacupunt.mtc.knowledge.repository.KnowledgeGraphRepository
import com.bioacupunt.mtc.knowledge.repository.KnowledgeRepository

/**
 * Evidence Engine — resolves evidence traces for hypotheses and scores support.
 *
 * Responsibilities:
 * - Resolve evidence IDs from KnowledgeEntity into full EvidenceTrace
 * - Separate supporting vs contradicting evidence
 * - Score hypothesis support using EvidenceScoringConfig
 * - Never invent evidence — if not in Knowledge Core, it doesn't exist
 *
 * Uses EvidenceResolver for the complete chain:
 *   Evidence → Citation → Source → Provenance
 *
 * R1/R2/R4 intact: this engine is deterministic Kotlin, no LLM calls.
 */
class EvidenceEngine(
    private val knowledgeRepository: KnowledgeRepository,
    private val graphRepository: KnowledgeGraphRepository,
    private val evidenceResolver: EvidenceResolver,
    private val config: EvidenceScoringConfig = EvidenceScoringConfig(),
) {

    /**
     * Resolve evidence for a single entity (hypothesis candidate).
     * Returns an EvidenceTrace with supporting and contradicting evidence separated.
     */
    suspend fun resolveEvidence(entityId: String): EvidenceTrace? {
        val entity = knowledgeRepository.getById(entityId) ?: return null

        val supporting = mutableListOf<EvidenceItem>()
        val contradicting = mutableListOf<EvidenceItem>()

        // Gather evidence from the entity itself
        for (evidenceId in entity.evidenceIds) {
            val evidenceItem = resolveEvidenceItem(evidenceId)
            if (evidenceItem != null) {
                // All entity-level evidence is supporting
                // Contradictions come from explicit CONTRAINDICATED_BY relations
                supporting.add(evidenceItem)
            }
        }

        // Gather evidence from CONTRAINDICATED_BY relations
        val relations = knowledgeRepository.getRelations(entityId)
        for (relation in relations) {
            if (relation.relationType == KnowledgeRelationType.CONTRAINDICATED_BY) {
                val contradictingEntity = knowledgeRepository.getById(relation.targetEntityId)
                if (contradictingEntity != null) {
                    for (evidenceId in relation.evidenceIds) {
                        val evidenceItem = resolveEvidenceItem(evidenceId)
                        if (evidenceItem != null) {
                            contradicting.add(evidenceItem)
                        }
                    }
                }
            }
        }

        // Gather evidence from SUPPORTED_BY relations (additional support)
        for (relation in relations) {
            if (relation.relationType == KnowledgeRelationType.SUPPORTED_BY) {
                val supportingEntity = knowledgeRepository.getById(relation.targetEntityId)
                if (supportingEntity != null) {
                    for (evidenceId in relation.evidenceIds) {
                        val evidenceItem = resolveEvidenceItem(evidenceId)
                        if (evidenceItem != null) {
                            supporting.add(evidenceItem)
                        }
                    }
                }
            }
        }

        val confidence = calculateConfidence(supporting, contradicting)

        return EvidenceTrace(
            entityId = entity.id,
            entityName = entity.canonicalName,
            entityType = entity.type,
            supportingEvidence = supporting,
            contradictingEvidence = contradicting,
            confidence = confidence,
        )
    }

    /**
     * Resolve evidence for a list of entity IDs.
     * Returns traces sorted by confidence (descending).
     */
    suspend fun resolveEvidenceBatch(entityIds: List<String>): List<EvidenceTrace> {
        return entityIds.mapNotNull { resolveEvidence(it) }
            .sortedByDescending { it.confidence }
    }

    /**
     * Resolve the full evidence chain for a list of evidence IDs.
     * Uses EvidenceResolver for Evidence → Citation → Source → Provenance.
     */
    suspend fun resolveFullChain(evidenceIds: List<String>): List<ResolvedEvidence> {
        return evidenceResolver.resolveEvidenceBatch(evidenceIds)
    }

    /**
     * Calculate confidence score for a hypothesis based on its evidence.
     *
     * Formula:
     * support = min(maxSupport, supportingCount * baseSupportPerEvidence)
     * quality = sum(levelBonuses for each evidence level)
     * confidence = support + quality - contradictionPenalty * contradictingCount
     * confidence = clamp(confidence, minConfidence, maxConfidence)
     */
    fun calculateConfidence(
        supporting: List<EvidenceItem>,
        contradicting: List<EvidenceItem>,
    ): Double {
        if (supporting.isEmpty() && contradicting.isEmpty()) return 0.0

        // Base support from evidence count
        val baseSupport = (supporting.size * config.baseSupportPerEvidence)
            .coerceAtMost(config.maxSupportFromEvidence)

        // Quality bonus from evidence levels
        val levelBonus = supporting.sumOf { evidence ->
            config.levelBonuses[evidence.level] ?: 0.0
        }.coerceAtMost(0.30) // Cap level bonus

        // Average confidence of supporting evidence
        val avgConfidence = if (supporting.isNotEmpty()) {
            supporting.mapNotNull { it.confidence }.average().takeIf { !it.isNaN() } ?: 0.0
        } else 0.0

        // Contradiction penalty
        val contradictionPenalty = contradicting.size * config.contradictionPenalty

        // Final score
        val raw = baseSupport + levelBonus + (avgConfidence * config.confidenceWeight) - contradictionPenalty
        return raw.coerceIn(config.minConfidence, config.maxConfidence)
    }

    /**
     * Resolve a single evidence item from its ID using the EvidenceResolver.
     * Falls back to a minimal EvidenceItem if resolution fails.
     */
    private suspend fun resolveEvidenceItem(evidenceId: String): EvidenceItem? {
        val resolved = evidenceResolver.resolveEvidence(evidenceId)
            ?: return null

        // Map ResolvedEvidence to EvidenceItem for the EvidenceTrace
        return EvidenceItem(
            id = resolved.evidenceId,
            claim = resolved.claim,
            level = resolved.level,
            confidence = resolved.confidence,
            citationIds = resolved.citations.map { it.citationId },
            sourceId = resolved.citations.firstOrNull()?.sourceId,
            sourceName = resolved.citations.firstOrNull()?.sourceName,
            locator = resolved.citations.firstOrNull()?.locator,
            excerpt = resolved.citations.firstOrNull()?.excerpt,
        )
    }
}
