package com.bioacupunt.mtc.knowledge.domain

import com.bioacupunt.mtc.knowledge.data.KnowledgeCoreDao

/**
 * Evidence Resolver — resolves the complete evidence chain:
 *
 * Evidence ID → KnowledgeEvidence → Citation IDs → KnowledgeCitation → KnowledgeSource → Provenance
 *
 * Never invents data. If a link in the chain is missing, returns null for that link.
 * The chain is only as strong as its weakest link.
 */
class EvidenceResolver(
    private val dao: KnowledgeCoreDao,
) {

    /**
     * Resolve a single evidence item by its ID.
     * Returns null if evidence not found.
     */
    suspend fun resolveEvidence(evidenceId: String): ResolvedEvidence? {
        val evidenceEntity = dao.getEvidenceById(evidenceId) ?: return null

        // Resolve citations
        val citationIds = parseJsonStringList(evidenceEntity.citation_ids_json)
        val citations = if (citationIds.isNotEmpty()) {
            dao.getCitationsByIds(citationIds)
        } else {
            emptyList()
        }

        // Resolve sources from citations
        val sourceIds = citations.map { it.source_id }.distinct()
        val sources = if (sourceIds.isNotEmpty()) {
            dao.getSourcesByIds(sourceIds)
        } else {
            emptyList()
        }

        // Build citation chain
        val citationChain = citations.map { citation ->
            val source = sources.find { it.id == citation.source_id }
            ResolvedCitation(
                citationId = citation.id,
                sourceId = citation.source_id,
                sourceName = source?.name,
                locator = citation.locator,
                excerpt = citation.excerpt,
                sourceLicense = source?.license,
            )
        }

        return ResolvedEvidence(
            evidenceId = evidenceEntity.id,
            claim = evidenceEntity.claim,
            level = evidenceEntity.level,
            confidence = evidenceEntity.confidence,
            citations = citationChain,
            sourceCount = sources.size,
        )
    }

    /**
     * Resolve multiple evidence items by their IDs.
     * Returns resolved evidence sorted by confidence (descending).
     */
    suspend fun resolveEvidenceBatch(evidenceIds: List<String>): List<ResolvedEvidence> {
        return evidenceIds.mapNotNull { resolveEvidence(it) }
            .sortedByDescending { it.confidence ?: 0.0 }
    }

    /**
     * Resolve provenance for an entity.
     */
    suspend fun resolveProvenance(entityId: String): List<ResolvedProvenance> {
        val provenanceEntities = dao.getProvenanceByEntity(entityId)
        return provenanceEntities.map { p ->
            ResolvedProvenance(
                entityId = p.entity_id,
                originalSource = p.original_source,
                originalId = p.original_id,
                originalType = p.original_type,
                sourceReference = p.source_reference,
                migrationVersion = p.migration_version,
                importedAt = p.imported_at,
            )
        }
    }

    /**
     * Get source quality level for scoring.
     * Returns a bonus value based on source type and license.
     */
    fun getSourceQualityBonus(sourceName: String?, license: String?): Double {
        // Sources with explicit licenses are higher quality
        if (license != null && license.isNotBlank()) {
            return when {
                license.contains("MIT", ignoreCase = true) -> 0.15
                license.contains("Apache", ignoreCase = true) -> 0.15
                license.contains("CC-BY", ignoreCase = true) -> 0.12
                else -> 0.08
            }
        }
        // Named sources without license are still somewhat useful
        if (sourceName != null && sourceName.isNotBlank()) {
            return 0.05
        }
        return 0.0
    }

    private fun parseJsonStringList(json: String): List<String> {
        return try {
            val array = org.json.JSONArray(json)
            (0 until array.length()).mapNotNull { array.optString(it).takeIf { s -> s.isNotBlank() } }
        } catch (_: Exception) {
            emptyList()
        }
    }
}

/**
 * A fully resolved evidence item with citation chain.
 */
data class ResolvedEvidence(
    val evidenceId: String,
    val claim: String,
    val level: String?,
    val confidence: Double?,
    val citations: List<ResolvedCitation>,
    val sourceCount: Int,
) {
    val hasCitations: Boolean get() = citations.isNotEmpty()
    val hasSources: Boolean get() = sourceCount > 0
    val hasLevel: Boolean get() = level != null && level.isNotBlank()
}

/**
 * A resolved citation with source information.
 */
data class ResolvedCitation(
    val citationId: String,
    val sourceId: String,
    val sourceName: String?,
    val locator: String?,   // "p. 245" or "doi:10.1234/..."
    val excerpt: String?,
    val sourceLicense: String?,
)

/**
 * Resolved provenance for an entity.
 */
data class ResolvedProvenance(
    val entityId: String,
    val originalSource: String,
    val originalId: String,
    val originalType: String,
    val sourceReference: String?,
    val migrationVersion: String,
    val importedAt: Long,
)
