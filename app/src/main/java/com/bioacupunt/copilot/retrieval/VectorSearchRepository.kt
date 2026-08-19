package com.bioacupunt.copilot.retrieval

import com.bioacupunt.data.local.database.VecKnowledgeNodeRepository

/**
 * §9 VECTOR SEARCH REPOSITORY
 *
 * Abstraction over [VecKnowledgeNodeRepository] (sqlite-vec) for semantic search.
 * Each vector has metadata: entityId, documentId, sourceId, chunkId, knowledgeVersion, embeddingModel.
 *
 * On-device vector search using cosine similarity via sqlite-vec.
 */
class VectorSearchRepository(
    private val vecRepository: VecKnowledgeNodeRepository,
) {

    /**
     * Semantic search using pre-computed embeddings.
     * Returns results ranked by cosine similarity.
     */
    suspend fun search(
        queryEmbedding: ByteArray,
        limit: Int = 50,
    ): List<RetrievalHit> {
        if (queryEmbedding.isEmpty()) return emptyList()

        return try {
            val results = vecRepository.search(queryEmbedding, limit)
            results.map { result ->
                // VecSearchResult has distance (0=identical, 1=orthogonal)
                // Convert to similarity: 1.0 - distance
                val similarity = (1.0 - result.distance).coerceIn(0.0, 1.0)
                RetrievalHit(
                    entityId = result.id,
                    content = result.content,
                    score = similarity,
                    normalizedScore = similarity,
                    sourceType = RetrievalSource.VECTOR,
                    metadata = mapOf(
                        "title" to result.title,
                        "status" to result.status,
                    ),
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Upsert an embedding for a knowledge entity.
     */
    suspend fun upsertEmbedding(
        rowId: Long,
        embedding: ByteArray,
    ): Boolean {
        return vecRepository.upsert(rowId, embedding)
    }

    /**
     * Remove an embedding.
     */
    suspend fun deleteEmbedding(rowId: Long): Boolean {
        return vecRepository.delete(rowId)
    }
}
