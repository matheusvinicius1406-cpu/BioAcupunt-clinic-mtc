package com.bioacupunt.mtc.knowledge.data

import com.bioacupunt.data.local.model.KnowledgeNodeEntity
import com.bioacupunt.mtc.knowledge.domain.KnowledgeImport
import com.bioacupunt.mtc.knowledge.repository.KnowledgeCoreImporter
import com.bioacupunt.observability.AppLogger

/**
 * PIPELINE BRIDGE — writes PipelineService output to Knowledge Core.
 *
 * After the MKIS pipeline creates a KnowledgeNodeEntity in `knowledge_nodes`,
 * this bridge converts it to canonical KnowledgeCoreEntityEntity and imports
 * it into `knowledge_core_entities`.
 *
 * Architecture:
 * ```text
 * PipelineService
 *     ↓ (writes to knowledge_nodes)
 * KnowledgeNodeEntity
 *     ↓ (bridge converts)
 * MkisAdapter.toCanonical()
 *     ↓
 * KnowledgeCoreImporter.import()
 *     ↓ (writes to knowledge_core_entities)
 * Knowledge Core (source of truth)
 * ```
 *
 * This ensures that any content processed by the pipeline is also available
 * in the canonical Knowledge Core for search and retrieval.
 *
 * The bridge is idempotent — re-running for the same entity merges (not duplicates).
 */
class PipelineBridge(
    private val mkisAdapter: MkisAdapter,
    private val importer: KnowledgeCoreImporter,
) {
    /**
     * Bridge a single KnowledgeNodeEntity to Knowledge Core.
     * Called after PipelineService writes to knowledge_nodes.
     *
     * @return true if import succeeded, false on error
     */
    suspend fun bridgeEntity(node: KnowledgeNodeEntity): Boolean {
        return try {
            val knowledgeImport = mkisAdapter.toCanonical(node)
            val result = importer.import(listOf(knowledgeImport))
            AppLogger.d(TAG, "Bridged entity '${node.title}': " +
                "${result.entities.size} entities, ${result.conflicts.size} conflicts")
            true
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to bridge entity '${node.title}'", e)
            false
        }
    }

    /**
     * Bridge multiple KnowledgeNodeEntities to Knowledge Core.
     * Called after batch pipeline processing.
     */
    suspend fun bridgeAll(nodes: List<KnowledgeNodeEntity>): BridgeResult {
        var success = 0
        var failed = 0
        var conflicts = 0

        nodes.forEach { node ->
            try {
                val knowledgeImport = mkisAdapter.toCanonical(node)
                val result = importer.import(listOf(knowledgeImport))
                success++
                conflicts += result.conflicts.size
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to bridge entity '${node.title}'", e)
                failed++
            }
        }

        val bridgeResult = BridgeResult(success, failed, conflicts)
        AppLogger.i(TAG, "Bridge complete: $bridgeResult")
        return bridgeResult
    }

    data class BridgeResult(
        val success: Int,
        val failed: Int,
        val conflicts: Int,
    )

    companion object {
        private const val TAG = "PipelineBridge"
    }
}
