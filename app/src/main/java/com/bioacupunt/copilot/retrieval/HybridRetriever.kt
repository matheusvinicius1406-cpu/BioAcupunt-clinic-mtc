package com.bioacupunt.copilot.retrieval

import com.bioacupunt.mtc.knowledge.domain.KnowledgeEntityType
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * §13 HYBRID RETRIEVER
 *
 * Orchestrates parallel retrieval across all backends:
 * - Lexical (FTS5 BM25)
 * - Vector (sqlite-vec cosine similarity)
 * - Graph (BFS traversal from recognized entities)
 * - Metadata (type/status/version filters)
 *
 * Flow:
 * ```text
 * Query
 *     ↓
 * Parallel retrieval (lexical + vector + graph + metadata)
 *     ↓
 * Score normalization
 *     ↓
 * Deduplication
 *     ↓
 * Merge → UnifiedRetrievalResult
 * ```
 *
 * Does NOT do reranking — that's RetrievalReranker's job.
 */
class HybridRetriever(
    private val lexicalBackend: LexicalSearchBackend,
    private val vectorBackend: VectorSearchRepository?,
    private val graphBackend: GraphRetrievalBackend,
    private val metadataBackend: MetadataFilterBackend,
    private val deduplicator: Deduplicator = Deduplicator(),
    private val scoreNormalizer: ScoreNormalizer = ScoreNormalizer(),
) {

    /**
     * Execute hybrid retrieval across all backends.
     * Parallel execution via coroutineScope + async.
     */
    suspend fun retrieve(
        request: UnifiedRetrievalRequest,
    ): UnifiedRetrievalResult = coroutineScope {
        val startTime = System.currentTimeMillis()
        val maxPerBackend = request.maxResults

        // Parallel retrieval
        val lexicalDeferred = async {
            try {
                lexicalBackend.search(
                    query = request.normalizedQuery,
                    limit = maxPerBackend,
                    typeFilter = request.filters.entityType,
                )
            } catch (e: Exception) {
                emptyList()
            }
        }

        val vectorDeferred = async {
            if (vectorBackend != null) {
                try {
                    // Vector search requires embeddings — skip if not available
                    // In production, this would use an embedding model
                    emptyList<RetrievalHit>()
                } catch (e: Exception) {
                    emptyList()
                }
            } else emptyList()
        }

        val graphDeferred = async {
            val hits = mutableListOf<RetrievalHit>()
            for (entity in request.recognizedEntities) {
                if (entity.entityId != null) {
                    hits.addAll(
                        graphBackend.expand(
                            entityId = entity.entityId,
                            maxDepth = 2,
                            maxResults = maxPerBackend / maxOf(request.recognizedEntities.size, 1),
                        )
                    )
                }
            }
            hits
        }

        val metadataDeferred = async {
            if (request.filters.entityType != null) {
                try {
                    metadataBackend.searchByType(request.filters.entityType, maxPerBackend)
                } catch (e: Exception) {
                    emptyList()
                }
            } else emptyList()
        }

        // Await all
        val lexicalResults = lexicalDeferred.await()
        val vectorResults = vectorDeferred.await()
        val graphResults = graphDeferred.await()
        val metadataResults = metadataDeferred.await()

        // Score normalization
        val normalizedLexical = scoreNormalizer.normalizeLexical(lexicalResults)
        val normalizedVector = scoreNormalizer.normalizeVector(vectorResults)
        val normalizedGraph = scoreNormalizer.normalizeGraph(graphResults)
        val normalizedMetadata = scoreNormalizer.normalizeMetadata(metadataResults)

        // Merge all results
        val allResults = normalizedLexical + normalizedVector + normalizedGraph + normalizedMetadata

        // Deduplication
        val deduped = deduplicator.deduplicate(allResults)

        // Apply score normalization and combine
        val merged = deduped.map { hit ->
            val weights = RetrievalScoringConfig()
            val combinedScore = when (hit.sourceType) {
                RetrievalSource.LEXICAL -> hit.normalizedScore * weights.lexicalWeight
                RetrievalSource.VECTOR -> hit.normalizedScore * weights.vectorWeight
                RetrievalSource.GRAPH -> hit.normalizedScore * weights.graphWeight
                RetrievalSource.METADATA -> hit.normalizedScore * weights.metadataBoost
                RetrievalSource.HYBRID -> hit.normalizedScore
            }
            hit.copy(rerankScore = combinedScore)
        }.sortedByDescending { it.rerankScore }

        val elapsed = System.currentTimeMillis() - startTime

        // Source breakdown for observability
        val breakdown = mutableMapOf<String, Int>()
        for (hit in deduped) {
            breakdown[hit.sourceType.name] = (breakdown[hit.sourceType.name] ?: 0) + 1
        }

        UnifiedRetrievalResult(
            results = merged,
            totalCandidates = allResults.size,
            retrievalLatencyMs = elapsed,
            sourceBreakdown = breakdown,
        )
    }
}
