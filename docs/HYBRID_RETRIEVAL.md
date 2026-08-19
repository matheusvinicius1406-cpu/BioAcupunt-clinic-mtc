# BIOACUPUNT — Hybrid Retrieval

## Overview

The Hybrid Retriever orchestrates parallel retrieval across multiple backends, normalizes scores, deduplicates results, and produces a unified result set.

## Architecture

```
Query
  ↓
Parallel Retrieval
  ├── Lexical (FTS5 BM25)
  ├── Vector (sqlite-vec cosine similarity) [deferred]
  ├── Graph (BFS traversal from recognized entities)
  └── Metadata (type/status/version filters)
  ↓
Score Normalization (0.0–1.0)
  ↓
Deduplication (by entityId)
  ↓
Weighted Merge
  ↓
UnifiedRetrievalResult
```

## Backends

### LexicalSearchBackend

Wraps `KnowledgeSearchRepository` (FTS5 BM25):

```kotlin
class LexicalSearchBackend(searchRepository: KnowledgeSearchRepository) {
    suspend fun search(query: String, limit: Int, typeFilter: KnowledgeEntityType?): List<RetrievalHit>
}
```

FTS5 BM25 scores are negative (closer to 0 = better). Normalized to 0.0–1.0.

### VectorSearchRepository

Wraps `VecKnowledgeNodeRepository` (sqlite-vec):

```kotlin
class VectorSearchRepository(vecRepository: VecKnowledgeNodeRepository) {
    suspend fun search(queryEmbedding: ByteArray, limit: Int): List<RetrievalHit>
}
```

**Status:** Deferred — no on-device embedding model available. Returns empty results.

### GraphRetrievalBackend

Wraps `KnowledgeGraphRepository` (BFS traversal):

```kotlin
class GraphRetrievalBackend(graphRepository: KnowledgeGraphRepository) {
    suspend fun expand(entityId: String, maxDepth: Int, maxResults: Int): List<RetrievalHit>
    suspend fun findPath(fromId: String, toId: String, maxDepth: Int): List<GraphPathResult>
}
```

Graph depth is used for proximity scoring (closer = higher score).

### MetadataFilterBackend

Applies metadata filters:

```kotlin
class MetadataFilterBackend(searchRepository: KnowledgeSearchRepository) {
    suspend fun filter(candidates: List<RetrievalHit>, filters: RetrievalFilters): List<RetrievalHit>
    suspend fun searchByType(type: KnowledgeEntityType, limit: Int): List<RetrievalHit>
}
```

## Score Normalization

Each backend has different score semantics:

| Backend | Raw Score | Normalized |
|---|---|---|
| Lexical (FTS5) | Negative (-10 to 0) | 0.0–1.0 |
| Vector (cosine) | 0.0–1.0 | 0.0–1.0 (clamped) |
| Graph (distance) | Depth-based | 1.0–0.0 (inverse) |
| Metadata | 1.0 if match | 0.0–1.0 |

## Weighted Merge

After deduplication, results are scored with configurable weights:

```kotlin
data class RetrievalScoringConfig(
    val lexicalWeight: Double = 0.35,
    val vectorWeight: Double = 0.30,
    val graphWeight: Double = 0.25,
    val metadataBoost: Double = 0.10,
    val evidenceBoost: Double = 0.15,
)
```

Weights must sum to ~1.0 (validated in `init` block).

## Deduplication

Same entity from multiple sources → keep highest score, merge evidence IDs:

```kotlin
class Deduplicator {
    fun deduplicate(hits: List<RetrievalHit>): List<RetrievalHit>
}
```

Dedup key: `entityId` (canonical ID). When duplicates exist, the merged hit gets `sourceType = HYBRID`.

## Reranker

Deterministic reranking with weighted factors:

| Factor | Weight | Description |
|---|---|---|
| Query Relevance | 0.30 | How well hit matches query |
| Entity Exactness | 0.20 | Exact vs fuzzy match |
| Graph Proximity | 0.15 | Closer to query entity |
| Evidence Quality | 0.15 | More evidence = higher |
| Source Quality | 0.10 | Approved > Draft |
| Clinical Context | 0.10 | Patient-relevant > generic |

Same input + same config → same output (tested explicitly).

## Context Builder

Builds structured context from retrieval results with budget limits:

| Limit | Default | Description |
|---|---|---|
| maxDocuments | 20 | Maximum context items |
| maxCharacters | 8000 | Maximum characters |
| maxTokens | 2000 | Estimated tokens |
| maxGraphNodes | 30 | Maximum graph nodes |
| maxEvidenceItems | 50 | Maximum evidence items |

Prioritization:
1. Direct evidence (highest)
2. Direct entity matches
3. High-confidence graph paths
4. Strong citations
5. Related context
6. Generic background
