package com.bioacupunt.copilot.evidence

import com.bioacupunt.copilot.rag.EvidenceResolutionService
import com.bioacupunt.copilot.rag.GroundedResponseGenerator
import com.bioacupunt.mtc.knowledge.repository.GraphConfig
import com.bioacupunt.mtc.knowledge.repository.KnowledgeGraphRepository

/**
 * §33 EVIDENCE EXPLORER
 *
 * Allows navigation through evidence chains:
 * ```text
 * Claim
 *     ↓
 * Evidence
 *     ↓
 * Citation
 *     ↓
 * Source
 *     ↓
 * Related Knowledge
 * ```
 *
 * Domain layer only — UI comes later (§44).
 */
class EvidenceExplorer(
    private val evidenceResolutionService: EvidenceResolutionService,
    private val graphRepository: KnowledgeGraphRepository,
) {

    data class EvidenceChain(
        val claim: String,
        val evidence: List<EvidenceResolutionService.ResolvedEvidence>,
        val relatedEntities: List<RelatedEntity>,
        val provenance: String?,
    )

    data class RelatedEntity(
        val entityId: String,
        val name: String,
        val type: String,
        val relationType: String,
    )

    /**
     * Explore the full evidence chain for a response.
     */
    suspend fun explore(response: GroundedResponseGenerator.GroundedResponse): EvidenceChain {
        // Resolve evidence
        val resolvedEvidence = evidenceResolutionService.resolve(response.evidenceIds)

        // Find related entities via graph
        val relatedEntities = mutableListOf<RelatedEntity>()
        for (evidence in resolvedEvidence.take(5)) {
            try {
                val neighbors = graphRepository.neighbors(
                    evidence.evidenceId,
                    GraphConfig(maxDepth = 1, maxNodes = 5),
                )
                for (entityIdStr in neighbors.visitedEntities) {
                    val edge = neighbors.relations.firstOrNull {
                        it.sourceId == evidence.evidenceId || it.targetId == evidence.evidenceId
                    }
                    relatedEntities.add(
                        RelatedEntity(
                            entityId = entityIdStr,
                            name = entityIdStr,
                            type = "UNKNOWN",
                            relationType = edge?.relationType?.name ?: "RELATED_TO",
                        )
                    )
                }
            } catch (e: Exception) {
                // Graph traversal failed — skip, don't crash
            }
        }

        return EvidenceChain(
            claim = response.answer,
            evidence = resolvedEvidence,
            relatedEntities = relatedEntities.distinctBy { it.entityId },
            provenance = resolvedEvidence.firstOrNull()?.let { "Evidence → Citations → Sources" },
        )
    }
}
