package com.bioacupunt.copilot.retrieval

import com.bioacupunt.mtc.knowledge.repository.GraphConfig
import com.bioacupunt.mtc.knowledge.repository.KnowledgeGraphRepository

/**
 * §11 GRAPH RETRIEVAL BACKEND
 *
 * Wraps [KnowledgeGraphRepository] as a retrieval backend.
 * Graph expansion: RecognizedEntity → Graph expansion → Related entities → Evidence.
 *
 * Uses BFS traversal with bounded depth to find semantically related entities.
 */
class GraphRetrievalBackend(
    private val graphRepository: KnowledgeGraphRepository,
) {

    /**
     * Expand a recognized entity through the knowledge graph.
     * Returns neighbors and reachable entities with their evidence.
     */
    suspend fun expand(
        entityId: String,
        maxDepth: Int = 2,
        maxResults: Int = 30,
    ): List<RetrievalHit> {
        if (entityId.isBlank()) return emptyList()

        return try {
            val reachable = graphRepository.reachable(
                entityId = entityId,
                config = GraphConfig(
                    maxDepth = maxDepth,
                    maxNodes = maxResults,
                ),
            )

            // GraphTraversalResult has visitedEntities (strings) + relations (edges)
            // We map each visited entity ID to a RetrievalHit
            reachable.visitedEntities.mapIndexed { index, entityIdStr ->
                val edge = reachable.relations.find {
                    it.sourceId == entityId || it.targetId == entityId
                }
                RetrievalHit(
                    entityId = entityIdStr,
                    content = "",
                    score = 1.0 - (index.toDouble() / maxResults), // decay by distance
                    normalizedScore = 1.0 - (index.toDouble() / maxResults),
                    sourceType = RetrievalSource.GRAPH,
                    graphDepth = index, // approximate depth by order
                    evidenceIds = edge?.let { listOf(it.relationType.name) } ?: emptyList(),
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Find paths between two entities for evidence chain exploration.
     */
    suspend fun findPath(
        fromId: String,
        toId: String,
        maxDepth: Int = 4,
    ): List<GraphPathResult> {
        return try {
            val paths = graphRepository.findPath(
                fromId = fromId,
                toId = toId,
                config = GraphConfig(maxDepth = maxDepth),
            )
            paths.map { path ->
                GraphPathResult(
                    entityIds = path.entityIds,
                    edgeTypes = path.edges.map { "${it.sourceId}→${it.targetId}(${it.relationType})" },
                    depth = path.entityIds.size - 1,
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}    data class GraphPathResult(
        val entityIds: List<String>,
        val edgeTypes: List<String>,
        val depth: Int,
    )
