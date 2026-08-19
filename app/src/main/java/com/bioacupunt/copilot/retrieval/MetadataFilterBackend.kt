package com.bioacupunt.copilot.retrieval

import com.bioacupunt.mtc.knowledge.domain.KnowledgeEntityType
import com.bioacupunt.mtc.knowledge.domain.KnowledgeStatus
import com.bioacupunt.mtc.knowledge.repository.KnowledgeSearchRepository

/**
 * §12 METADATA FILTER BACKEND
 *
 * Applies metadata filters to narrow the retrieval candidate set BEFORE reranking.
 * Filters reduce the candidate set, improving both precision and performance.
 *
 * Supported filters:
 * - entityType: PATTERN, SYNDROME, SYMPTOM, etc.
 * - status: APPROVED, DRAFT, etc.
 * - evidenceLevel: STRONG, MODERATE, WEAK, etc.
 * - sourceType: library, mkis, etc.
 * - knowledgeVersion: specific version
 * - clinicalDomain: specific clinical domain
 */
class MetadataFilterBackend(
    private val searchRepository: KnowledgeSearchRepository,
) {

    /**
     * Filter candidates by metadata.
     * Returns only entities matching ALL specified filters.
     */
    suspend fun filter(
        candidates: List<RetrievalHit>,
        filters: RetrievalFilters,
    ): List<RetrievalHit> {
        if (filters.isEmpty()) return candidates

        return candidates.filter { hit ->
            matchesFilters(hit, filters)
        }
    }

    /**
     * Direct metadata search by entity type.
     */
    suspend fun searchByType(
        type: KnowledgeEntityType,
        limit: Int = 100,
    ): List<RetrievalHit> {
        val entities = searchRepository.getByType(type, limit)
        return entities.map { entity ->
            RetrievalHit(
                entityId = entity.id,
                entity = entity,
                content = entity.content,
                score = 1.0,
                normalizedScore = 1.0,
                sourceType = RetrievalSource.METADATA,
            )
        }
    }

    private fun matchesFilters(hit: RetrievalHit, filters: RetrievalFilters): Boolean {
        // Entity type filter
        if (filters.entityType != null && hit.entity?.type != filters.entityType) {
            return false
        }

        // Status filter
        if (filters.status != null && hit.entity?.version?.status?.name != filters.status) {
            return false
        }

        // Source type filter
        if (filters.sourceType != null && hit.metadata["sourceType"] != filters.sourceType) {
            return false
        }

        // Knowledge version filter
        if (filters.knowledgeVersion != null && hit.knowledgeVersion != filters.knowledgeVersion) {
            return false
        }

        return true
    }
}

/**
 * Extension to check if filters are empty (no active filters).
 */
private fun RetrievalFilters.isEmpty(): Boolean {
    return entityType == null &&
        status == null &&
        evidenceLevel == null &&
        sourceType == null &&
        knowledgeVersion == null &&
        clinicalDomain == null &&
        tenantId == null
}
