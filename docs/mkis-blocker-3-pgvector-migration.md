# MKIS — Blocker #3 Resolution: pgvector Migration Strategy

**Date:** 2026-07-22
**Status:** ✅ RESOLVED
**ARB Blocker #3:** pgvector migration strategy undefined → **RESOLVED**
**Scope:** Zero-downtime dual-write migration, HNSW reindexing, embedding model versioning

---

## 1. Context

### 1.1 What the ARB Found

The original MKIS specification had `vector(1536)` hardcoded with no migration path if the embedding model changed. The ARB flagged this as **Risk #1 (Critical)**: changing the embedding model would require a full table rewrite of 10M+ rows, causing hours of downtime.

### 1.2 What We Decided (Blocker #1)

- **Model:** BGE-M3 (BAAI) — 1024 dimensions, Apache 2.0
- **Distance:** `cosine` (embeddings are normalized)
- **Index:** HNSW with `vector_cosine_ops`

### 1.3 Scale Assumptions (from interview)

| Parameter | Value |
|-----------|-------|
| Initial volume | 10k–100k knowledge nodes |
| Growth rate | Continuous ingestion |
| Maintenance window | **Zero downtime required** — 100% online |
| Old column cleanup | 7 days after successful cutover |

---

## 2. The Dual-Write Strategy (Detailed)

The core idea: **two columns, one active at a time**. Writes go to both, reads target one. This provides instant rollback: if the new model degrades search quality, the app flips back to the old column with zero data loss.

### 2.1 Schema Design

```sql
-- knowledge_nodes table — embedding columns
embedding_version TEXT NOT NULL DEFAULT 'bge-m3-v1',
embedding         vector(1024),   -- ACTIVE column (currently queried)
embedding_v2      vector(1024),   -- STANDBY column (being built / hot standby)
```

**embedding_version** doubles as:
- A query filter: `WHERE embedding_version = 'bge-m3-v1'`
- A partial index condition
- An immutable record of which model produced each row

### 2.2 Normalization Contract

BGE-M3 outputs normalized vectors (L2 norm = 1.0). The contract:

```sql
-- Always use cosine distance for BGE-M3 (equivalent to inner_product on normalized)
-- Normalized vectors: cosine(u,v) = inner_product(u,v)
-- Index: vector_cosine_ops
CREATE INDEX ON knowledge_nodes USING hnsw (embedding vector_cosine_ops);
```

---

## 3. Phase 1: Initial Deployment (First Time)

When MKIS goes live for the first time, there is no "old" column. The initial load is straightforward.

```sql
-- Phase 1.1: Create the table with the initial embedding column
-- (See blocker-2 document for full CREATE TABLE)

-- Phase 1.2: Populate embeddings (background batch job)
-- Python psuedocode:
--   for batch in slide(knowledge_nodes, window=100):
--       embeddings = bge_m3.encode(batch['content'])
--       UPDATE knowledge_nodes SET embedding = $1 WHERE id = $2

-- Phase 1.3: Build HNSW index (concurrently, zero-downtime)
SET maintenance_work_mem = '8GB';
SET max_parallel_maintenance_workers = 4;

CREATE INDEX CONCURRENTLY idx_knowledge_nodes_embedding
    ON knowledge_nodes USING hnsw (embedding vector_cosine_ops)
    WHERE status IN ('aprovado', 'descontinuado');  -- partial: only searchable nodes
```

**Why partial index?** Drafts and rejected nodes don't need vector search. The partial index is ~40% smaller and faster to build.

---

## 4. Phase 2: Production Migration (Model Change)

When a new embedding model arrives (e.g., BGE-M3 v2 with better Portuguese medical accuracy):

```
Timeline:
  T+0  : Add embedding_v2 column + dual-write
  T+0  : Start background backfill of existing rows
  T+N  : Backfill complete. Build HNSW on embedding_v2 (CONCURRENTLY)
  T+N  : Validation: compare search quality for 100 benchmark queries
  T+N+7: Cutover: app points reads to embedding_v2
  T+N+14: Drop old embedding column + index
```

### 4.1 Step 1 — Add Standby Column (zero downtime)

```sql
-- Additive DDL: takes milliseconds, no lock
ALTER TABLE knowledge_nodes ADD COLUMN embedding_v2 vector(1024);

-- Update the dual-write trigger (or app logic)
-- Every INSERT/UPDATE now writes to BOTH embedding AND embedding_v2
```

### 4.2 Step 2 — Background Backfill (zero downtime)

```sql
-- Batch backfill rows that are NULL in embedding_v2
-- Run as a background worker with idempotency

-- Query to find next batch:
SELECT id, content FROM knowledge_nodes
WHERE embedding_v2 IS NULL
  AND status IN ('aprovado', 'descontinuado')
ORDER BY id
LIMIT 100;
```

**Backfill worker specification:**

| Property | Value |
|----------|-------|
| Batch size | 100 rows |
| Concurrency | 2–4 parallel workers |
| Pacing | Max 50% CPU (can be throttled) |
| Idempotency key | `('backfill_v2', tenant_id, node_id)` |
| Monitoring | `SELECT COUNT(*) FROM knowledge_nodes WHERE embedding_v2 IS NULL` |

**BGE-M3 CPU batch performance (estimated):**
- ~50–100 docs/second on 8-core CPU with ONNX runtime
- 100k nodes → ~17–33 minutes of backfill time
- With 4 parallel workers → ~5–10 minutes

### 4.3 Step 3 — Build HNSW Index on New Column (zero downtime)

```sql
-- Concurrent index build: zero blocking
SET maintenance_work_mem = '8GB';
SET max_parallel_maintenance_workers = 4;

CREATE INDEX CONCURRENTLY idx_knowledge_nodes_embedding_v2
    ON knowledge_nodes USING hnsw (embedding_v2 vector_cosine_ops)
    WHERE status IN ('aprovado', 'descontinuado');

-- Monitor progress:
SELECT phase, blocks_done, blocks_total
FROM pg_stat_progress_create_index;
```

HNSW build time for 100k nodes with `m=16, ef_construction=64`:
- ~2–5 minutes on modern CPU
- ~30 seconds on GPU
- Index size: ~100k × 1024 × 4 bytes = ~400 MB

### 4.4 Step 4 — Validation

Before cutover, validate that the new model produces equivalent or better search results:

```sql
-- Benchmark: 100 representative queries
-- For each query, compare top-10 results from old vs new embedding
-- Metrics: recall@10, MRR, NDCG

-- Automated validation query:
WITH old_results AS (
    SELECT id, embedding <=> query_embedding AS dist
    FROM knowledge_nodes
    ORDER BY embedding <=> query_embedding
    LIMIT 10
),
new_results AS (
    SELECT id, embedding_v2 <=> query_embedding AS dist
    FROM knowledge_nodes
    ORDER BY embedding_v2 <=> query_embedding
    LIMIT 10
)
SELECT
    (SELECT COUNT(*) FROM old_results) AS old_count,
    (SELECT COUNT(*) FROM new_results) AS new_count,
    (SELECT COUNT(*) FROM old_results INTERSECT SELECT id FROM new_results) AS overlap;
```

**Cutover criteria:**
- Overlap@10 ≥ 80% (at least 8 of top-10 same between models)
- No evidence of regression in manual review of edge cases

### 4.5 Step 5 — Cutover (zero downtime)

```sql
-- Step 5a: Update app code to query embedding_v2 instead of embedding
-- This is a configuration change (environment variable: ACTIVE_EMBEDDING_COLUMN=v2)
-- Deploy new app version. Old queries use embedding_v2 now.

-- Step 5b: Drop old index (concurrently)
DROP INDEX CONCURRENTLY idx_knowledge_nodes_embedding;

-- Step 5c: Remove old column (short lock — table rewrite happens in background)
-- Wait 7 days after cutover before this step
ALTER TABLE knowledge_nodes DROP COLUMN embedding;

-- Step 5d: Rename v2 to canonical name
ALTER TABLE knowledge_nodes RENAME COLUMN embedding_v2 TO embedding;

-- Step 5e: Update embedding_version
UPDATE knowledge_nodes SET embedding_version = 'bge-m3-v2'
WHERE embedding_version = 'bge-m3-v1';
```

### 4.6 Rollback Plan

If search quality degrades after cutover:

```sql
-- Rollback (within 7-day window):
-- 1. App code switches ACTIVE_EMBEDDING_COLUMN back to 'embedding_v2'... wait.
--    Actually, if we already renamed, we need to handle this differently.

-- Better approach: keep the old column as "cold standby" for 7 days
ALTER TABLE knowledge_nodes RENAME COLUMN embedding TO embedding_old;
ALTER TABLE knowledge_nodes RENAME COLUMN embedding_v2 TO embedding;

-- To rollback:
ALTER TABLE knowledge_nodes RENAME COLUMN embedding TO embedding_v2;
ALTER TABLE knowledge_nodes RENAME COLUMN embedding_old TO embedding;
-- Rebuild index on old column
CREATE INDEX CONCURRENTLY idx_rollback ON knowledge_nodes USING hnsw (embedding vector_cosine_ops)
    WHERE status IN ('aprovado', 'descontinuado');
```

**Simpler rollback:** The app uses a **column name from config**. No rename needed:

```python
# config.py
ACTIVE_EMBEDDING_COLUMN = os.getenv('ACTIVE_EMBEDDING_COLUMN', 'embedding')

# search.py
query = f"""
    SELECT id, {ACTIVE_EMBEDDING_COLUMN} <=> %s AS distance
    FROM knowledge_nodes
    ORDER BY {ACTIVE_EMBEDDING_COLUMN} <=> %s
    LIMIT 10
"""
```

Rollback = change env var + restart. No DB changes needed.

---

## 5. HNSW Tuning Guide

### 5.1 Build-Time Parameters

| Parameter | Default | Recommended (MKIS) | Effect |
|-----------|---------|-------------------|--------|
| `m` | 16 | 16 (default) | Higher = more connections = better recall, 2x index size |
| `ef_construction` | 64 | 128 | Higher = better graph quality = slower build |
| `maintenance_work_mem` | 64 MB | **8 GB** (before index build) | Prevents disk spill during build |
| `max_parallel_maintenance_workers` | 2 | 4 | Parallel index build |

### 5.2 Query-Time Parameters (per-query tuning)

```sql
-- High recall (RAG pipeline): ef_search = 200
SET LOCAL hnsw.ef_search = 200;
SELECT ... ORDER BY embedding <=> query LIMIT 50;

-- Low latency (user-facing search): ef_search = 40
SET LOCAL hnsw.ef_search = 40;
SELECT ... ORDER BY embedding <=> query LIMIT 10;
```

| Workload | `ef_search` | Recall | Latency (100k nodes) |
|----------|-------------|--------|---------------------|
| User-facing search | 40 | ~95% | ~5 ms |
| RAG pipeline | 200 | ~99% | ~20 ms |
| Batch reranking | 400 | ~99.5% | ~40 ms |

### 5.3 Iterative Scan (pgvector ≥ 0.8.0)

Filtered HNSW scans can return zero results when the top-k candidates are all filtered out by `WHERE`.

```sql
-- Enable iterative scan to automatically retry with larger candidate list
SET hnsw.iterative_scan = 'relaxed_order';

SELECT id, embedding <=> query AS distance
FROM knowledge_nodes
WHERE status = 'aprovado' AND tenant_id = '...'
ORDER BY embedding <=> query
LIMIT 10;
```

---

## 6. pgvector & BGE-M3: Dimension Compatibility

| Model | Dimensions | Distance Function | Index Op Class |
|-------|-----------|-------------------|----------------|
| **BGE-M3** (current) | 1024 | cosine | `vector_cosine_ops` |
| Future model (e.g., BGE-M3 v2) | 1024 | cosine | `vector_cosine_ops` |
| Future larger model | 2048+ | cosine | `vector_cosine_ops` (need `halfvec` if > 2000d) |

**BGE-M3 outputs L2-normalized vectors.** For normalized vectors:
- `cosine(u, v) = inner_product(u, v)` — identical
- `l2_distance(u, v) = 2 - 2*cosine(u, v)` — equivalent for ranking

**Use `vector_cosine_ops` always** — it's optimized for normalized embeddings and produces the same ranking as `<=>` but with a faster index.

---

## 7. Vacuum & Maintenance

### 7.1 Routine Maintenance

```sql
-- Daily: analyze for query planner
ANALYZE knowledge_nodes;

-- Weekly: vacuum to reclaim space from updates
VACUUM knowledge_nodes;

-- Monthly: reindex if index bloat detected
REINDEX INDEX CONCURRENTLY idx_knowledge_nodes_embedding;
```

### 7.2 Bloat Detection

```sql
-- Check HNSW index bloat
SELECT
    idx.indexrelid::regclass AS index_name,
    pg_size_pretty(pg_relation_size(idx.indexrelid)) AS index_size,
    idx.idx_scan AS scans
FROM pg_stat_user_indexes idx
WHERE idx.relname = 'knowledge_nodes'
ORDER BY idx.idx_scan;
```

### 7.3 Monitoring Dashboard

| Metric | Alert Threshold | Action |
|--------|----------------|--------|
| P95 vector search latency | > 50ms for user search | Tune `ef_search` or reindex |
| Embedding NULL percentage | > 1% of `aprovado` nodes | Investigate backfill worker |
| Index bloat | > 20% | `REINDEX CONCURRENTLY` |
| Search recall regression | Overlap@10 < 75% vs benchmark | Rollback embedding model |

---

## 8. Cost Analysis (Storage)

For 100k knowledge nodes, 1024 dimensions:

| Component | Size | Monthly Cost (approx) |
|-----------|------|----------------------|
| Embeddings (100k × 1024 × 4 bytes) | ~400 MB | Included in VPS |
| HNSW index (1.5x data size) | ~600 MB | Included in VPS |
| Dual-write overhead (2 columns) | ~800 MB | Included in VPS |
| `knowledge_nodes` table (metadata) | ~500 MB | Included in VPS |
| **Total (100k nodes)** | **~1.7 GB** | **~R$ 250/mês** (same VPS) |

For 1M nodes, scale up to ~17 GB — still fits in a 32 GB RAM VPS.

---

## 9. Implementation Checklist

### Pre-Migration
- [ ] Install pgvector extension on PostgreSQL: `CREATE EXTENSION vector;`
- [ ] Deploy BGE-M3 model (ONNX format for CPU, sentence-transformers for GPU)
- [ ] Create `knowledge_nodes` table with `embedding vector(1024)` + `embedding_version`
- [ ] Configure app to use environment variable `ACTIVE_EMBEDDING_COLUMN`

### Initial Load
- [ ] Run batch embedding job (100 docs/batch, 2–4 workers)
- [ ] Build HNSW index with `CREATE INDEX CONCURRENTLY`
- [ ] Validate: run 100 benchmark queries, check recall

### Future Migration (model change)
- [ ] Add `embedding_v2 vector(1024)` column
- [ ] Enable dual-write (app writes to both columns)
- [ ] Run backfill worker for existing NULL rows
- [ ] Build `CREATE INDEX CONCURRENTLY` on new column
- [ ] Validate search quality (overlap@10 ≥ 80%)
- [ ] Change `ACTIVE_EMBEDDING_COLUMN` env var → deploy
- [ ] Monitor for 7 days
- [ ] Drop old column + index

### Rollback (if needed within 7 days)
- [ ] Change `ACTIVE_EMBEDDING_COLUMN` back
- [ ] Deploy → search returns to old model instantly
- [ ] Investigate quality regression

---

## 10. Remaining Blockers

| # | Blocker | Status | Next Step |
|---|--------|--------|-----------|
| 1 | Vendor/Stack choice | ✅ **RESOLVED** | BGE-M3 + Qwen 2.5 7B |
| 2 | Canonical enums | ✅ **RESOLVED** | 16 enums closed |
| **3** | **pgvector migration** | **✅ RESOLVED** | **This document** |
| 4 | Partitioning | ❌ Open | Define partition key + schedule |
| 5 | Pipeline states | ❌ Open | Add: quarantined, awaiting_approval, needs_human_review |
| 6 | Deep delete LGPD | ❌ Open | Design cascade: vectors → KG → cache → audit |

---

## 11. References

- pgvector documentation: https://github.com/pgvector/pgvector
- HNSW tuning (Crunchy Data): https://www.crunchydata.com/blog/hnsw-indexes-with-postgres-and-pgvector
- pgvector best practices (2026): https://rivestack.io/blog/pgvector-hnsw-tuning-managed-postgres
- BGE-M3: https://huggingface.co/BAAI/bge-m3
- BGE-M3 ONNX export: https://huggingface.co/aapot/bge-m3-onnx
- ARB report: `docs/mkis/ARB-architecture-review-report.md`
- Blocker #1 (vendor): `docs/mkis-blocker-1-vendor-decision.md`
- Blocker #2 (enums): `docs/mkis-blocker-2-enums-canonical.md`

---

*End of document — Blocker #3 resolved ✅*
