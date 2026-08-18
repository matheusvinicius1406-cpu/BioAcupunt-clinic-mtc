package com.bioacupunt.mtc.knowledge.repository

import com.bioacupunt.mtc.knowledge.data.KnowledgeCoreDao
import com.bioacupunt.mtc.knowledge.domain.*

/**
 * Room-based graph traversal over knowledge_core_entities + knowledge_core_relations.
 *
 * Architecture: BFS traversal in Kotlin over Room results. For the expected graph size
 * (~10K entities, ~50K relations), in-memory BFS is fast enough. No need for recursive CTE
 * or external graph DB.
 *
 * Safety:
 * - visited set prevents cycles (A → B → C → A)
 * - maxDepth bounds traversal depth
 * - maxNodes bounds total visited nodes
 * - maxResults bounds output size
 * - deterministic ordering by ID for reproducibility
 */
class RoomKnowledgeGraphRepository(
    private val dao: KnowledgeCoreDao,
) : KnowledgeGraphRepository {

    override suspend fun neighbors(
        entityId: String,
        config: GraphConfig,
    ): GraphTraversalResult {
        val entity = dao.getById(entityId) ?: return GraphTraversalResult(
            visitedEntities = emptyList(),
            relations = emptyList(),
        )

        val allEdges = getFilteredEdges(entityId, config)
        val neighborIds = allEdges.map { if (it.sourceId == entityId) it.targetId else it.sourceId }.distinct()

        // Fetch neighbor entities to verify they exist and filter by status
        val neighborEntities = if (config.statusFilter != null) {
            dao.getByIds(neighborIds).filter { it.status == config.statusFilter!!.name }
        } else {
            dao.getByIds(neighborIds)
        }
        val validNeighborIds = neighborEntities.map { it.id }.toSet()
        val filteredEdges = allEdges.filter {
            (it.sourceId == entityId && it.targetId in validNeighborIds) ||
            (it.targetId == entityId && it.sourceId in validNeighborIds)
        }

        return GraphTraversalResult(
            visitedEntities = listOf(entityId) + validNeighborIds.take(config.maxResults),
            relations = filteredEdges.take(config.maxResults),
        )
    }

    override suspend fun reachable(
        entityId: String,
        config: GraphConfig,
    ): GraphTraversalResult {
        val entity = dao.getById(entityId) ?: return GraphTraversalResult(
            visitedEntities = emptyList(),
            relations = emptyList(),
        )

        val visited = mutableSetOf(entityId)
        val allEdges = mutableListOf<GraphEdge>()
        var currentLevel = listOf(entityId)

        for (depth in 0 until config.maxDepth) {
            if (visited.size >= config.maxNodes) break
            if (currentLevel.isEmpty()) break

            val nextLevel = mutableListOf<String>()
            for (nodeId in currentLevel) {
                val edges = getFilteredEdges(nodeId, config)
                for (edge in edges) {
                    val neighborId = if (edge.sourceId == nodeId) edge.targetId else edge.sourceId
                    if (neighborId !in visited) {
                        visited.add(neighborId)
                        allEdges.add(edge)
                        nextLevel.add(neighborId)
                        if (visited.size >= config.maxNodes) break
                    }
                }
                if (visited.size >= config.maxNodes) break
            }
            currentLevel = nextLevel
        }

        // Filter by status if needed
        val validIds = if (config.statusFilter != null) {
            val entities = dao.getByIds(visited.toList()).filter { it.status == config.statusFilter!!.name }
            entities.map { it.id }.toSet()
        } else {
            visited
        }

        val filteredEdges = allEdges.filter {
            it.sourceId in validIds && it.targetId in validIds
        }.take(config.maxResults)

        return GraphTraversalResult(
            visitedEntities = visited.sorted().take(config.maxResults),
            relations = filteredEdges,
        )
    }

    override suspend fun findPath(
        fromId: String,
        toId: String,
        config: GraphConfig,
    ): List<GraphPath> {
        // BFS to find shortest path
        val visited = mutableSetOf(fromId)
        val queue = ArrayDeque<List<String>>()
        queue.add(listOf(fromId))

        while (queue.isNotEmpty()) {
            val path = queue.removeFirst()
            val current = path.last()

            if (current == toId) {
                // Build edge list from path
                val edges = mutableListOf<GraphEdge>()
                for (i in 0 until path.size - 1) {
                    val edge = dao.getEdgesFrom(path[i]).find {
                        it.target_entity_id == path[i + 1]
                    } ?: dao.getEdgesTo(path[i + 1]).find {
                        it.source_entity_id == path[i]
                    } ?: continue

                    edges.add(GraphEdge(
                        sourceId = edge.source_entity_id,
                        relationType = KnowledgeRelationType.valueOf(edge.relation_type),
                        targetId = edge.target_entity_id,
                        confidence = edge.confidence,
                        evidenceIds = parseJsonStringList(edge.evidence_ids_json),
                    ))
                }
                return listOf(GraphPath(edges = edges, entityIds = path))
            }

            if (path.size - 1 >= config.maxDepth) continue

            val edges = getFilteredEdges(current, config)
            for (edge in edges) {
                val neighbor = if (edge.sourceId == current) edge.targetId else edge.sourceId
                if (neighbor !in visited) {
                    visited.add(neighbor)
                    queue.add(path + neighbor)
                }
            }
        }

        return emptyList() // No path found
    }

    override suspend fun edgesFrom(entityId: String): List<GraphEdge> {
        return dao.getEdgesFrom(entityId).map { row ->
            GraphEdge(
                sourceId = row.source_entity_id,
                relationType = KnowledgeRelationType.valueOf(row.relation_type),
                targetId = row.target_entity_id,
                confidence = row.confidence,
                evidenceIds = parseJsonStringList(row.evidence_ids_json),
            )
        }
    }

    override suspend fun edgesTo(entityId: String): List<GraphEdge> {
        return dao.getEdgesTo(entityId).map { row ->
            GraphEdge(
                sourceId = row.source_entity_id,
                relationType = KnowledgeRelationType.valueOf(row.relation_type),
                targetId = row.target_entity_id,
                confidence = row.confidence,
                evidenceIds = parseJsonStringList(row.evidence_ids_json),
            )
        }
    }

    override suspend fun entitiesNear(
        entityId: String,
        targetType: KnowledgeEntityType,
        config: GraphConfig,
    ): List<KnowledgeEntity> {
        val traversal = reachable(entityId, config)
        val neighborIds = traversal.visitedEntities.filter { it != entityId }
        if (neighborIds.isEmpty()) return emptyList()

        val entities = dao.getByIds(neighborIds)
        return entities
            .filter { it.type == targetType.wireName }
            .map { it.toDomain() }
            .take(config.maxResults)
    }

    // ── Internal helpers ──────────────────────────────────────────────

    private suspend fun getFilteredEdges(entityId: String, config: GraphConfig): List<GraphEdge> {
        val allEdges = dao.getEdgesFrom(entityId) + dao.getEdgesTo(entityId)
        return allEdges
            .map { row ->
                GraphEdge(
                    sourceId = row.source_entity_id,
                    relationType = KnowledgeRelationType.valueOf(row.relation_type),
                    targetId = row.target_entity_id,
                    confidence = row.confidence,
                    evidenceIds = parseJsonStringList(row.evidence_ids_json),
                )
            }
            .filter { edge ->
                // Filter by relation types if specified
                (config.relationTypes == null || edge.relationType in config.relationTypes!!) &&
                // Filter by minimum confidence if specified
                (config.minConfidence == null || (edge.confidence ?: 0.0) >= config.minConfidence!!)
            }
            .sortedBy { it.targetId } // Deterministic ordering
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

/** Mirror of KnowledgeCoreEntityEntity.toDomain() from KnowledgeRepository.kt */
private fun com.bioacupunt.mtc.knowledge.data.KnowledgeCoreEntityEntity.toDomain(): KnowledgeEntity {
    return KnowledgeEntity(
        id = id,
        type = KnowledgeEntityType.from(type),
        canonicalName = canonical_name,
        aliases = try { org.json.JSONArray(aliases_json).let { arr -> (0 until arr.length()).map { arr.optString(it) } } } catch (_: Exception) { emptyList() },
        summary = summary,
        content = content,
        metadata = try { org.json.JSONObject(metadata_json).let { obj -> obj.keys().asSequence().associateWith { obj.getString(it) } } } catch (_: Exception) { emptyMap() },
        sourceIds = parseJsonStringList(source_ids_json),
        citationIds = parseJsonStringList(citation_ids_json),
        evidenceIds = parseJsonStringList(evidence_ids_json),
        version = KnowledgeVersion(version, created_at, updated_at, reviewed_at, runCatching { KnowledgeStatus.valueOf(status) }.getOrDefault(KnowledgeStatus.DRAFT)),
        createdAt = created_at,
        updatedAt = updated_at,
    )
}

private fun parseJsonStringList(json: String): List<String> {
    return try {
        val array = org.json.JSONArray(json)
        (0 until array.length()).mapNotNull { array.optString(it).takeIf { s -> s.isNotBlank() } }
    } catch (_: Exception) {
        emptyList()
    }
}
