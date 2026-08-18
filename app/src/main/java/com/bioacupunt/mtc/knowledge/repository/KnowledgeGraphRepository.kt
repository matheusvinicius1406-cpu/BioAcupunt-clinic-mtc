package com.bioacupunt.mtc.knowledge.repository

import com.bioacupunt.mtc.knowledge.domain.*

/**
 * Graph repository interface for traversing the Knowledge Core as a graph.
 *
 * The graph is a logical projection over `knowledge_core_entities` + `knowledge_core_relations`.
 * No separate graph database is needed — Room adjacency list is sufficient for ~10K entities.
 *
 * All traversals are bounded by [GraphConfig] to prevent runaway queries.
 */
interface KnowledgeGraphRepository {

    /**
     * Direct neighbors of an entity (both outgoing and incoming relations).
     */
    suspend fun neighbors(
        entityId: String,
        config: GraphConfig = GraphConfig(),
    ): GraphTraversalResult

    /**
     * All entities reachable from a starting entity within maxDepth hops.
     * Uses BFS traversal.
     */
    suspend fun reachable(
        entityId: String,
        config: GraphConfig = GraphConfig(),
    ): GraphTraversalResult

    /**
     * Find all paths from a source entity to a target entity.
     * Returns empty list if no path exists.
     */
    suspend fun findPath(
        fromId: String,
        toId: String,
        config: GraphConfig = GraphConfig(),
    ): List<GraphPath>

    /**
     * Get all edges from a specific entity (outgoing only).
     */
    suspend fun edgesFrom(entityId: String): List<GraphEdge>

    /**
     * Get all edges to a specific entity (incoming only).
     */
    suspend fun edgesTo(entityId: String): List<GraphEdge>

    /**
     * Find entities by type within a relation neighborhood.
     */
    suspend fun entitiesNear(
        entityId: String,
        targetType: KnowledgeEntityType,
        config: GraphConfig = GraphConfig(),
    ): List<KnowledgeEntity>
}

/**
 * Configuration for graph traversal limits.
 * Protects against cycles and runaway queries.
 */
data class GraphConfig(
    val maxDepth: Int = 3,
    val maxNodes: Int = 100,
    val maxResults: Int = 50,
    val relationTypes: Set<KnowledgeRelationType>? = null, // null = all types
    val minConfidence: Double? = null,
    val statusFilter: KnowledgeStatus? = null,
)
