# MKIS — Blocker #4 Resolution: Partitioning Strategy

**Date:** 2026-07-22
**Status:** ✅ RESOLVED
**ARB Blocker #4:** knowledge_nodes partitioning undefined → **RESOLVED**
**Scope:** Range partitioning by `created_at`, per-partition HNSW indexes, retention via `pg_partman`, future-proof for cold storage

---

## 1. Context

### 1.1 What the ARB Found

The original MKIS database spec mentioned partitioning for `audit_trail` and `knowledge_node_versions` but **omitted `knowledge_nodes`** — the largest and most queried table. The ARB flagged this as a critical issue:
- 10M+ nodes without partitioning → monstrous indexes, slow vacuum, impossible reindex in short window
- No policy for data tiering (hot/warm/cold) despite LGPD retention requirements
- No backup strategy per partition

### 1.2 What Was Decided in Previous Blockers

| Decision | Reference |
|----------|-----------|
| BGE-M3 1024d embeddings | Blocker #1 |
| 16 canonical enums, CHECK constraints | Blocker #2 |
| Dual-write + CONCURRENTLY + HNSW | Blocker #3 |
| **Small multi-tenant (1-10 tenants)** | This blocker |
| **All-hot storage for 7 years** | This blocker |
| **Cold data: audit-only, no query needed** | This blocker |

### 1.3 Scale Assumptions (from interview)

| Parameter | Value |
|-----------|-------|
| Tenants | 1–10 |
| Initial volume | 10k–100k nodes |
| Growth rate | Continuous ingestion (100–1k nodes/month) |
| 7-year projection | ~100k–1M nodes |
| Storage tier | All hot (SSD, full 7 years) |
| Cold query | Not needed (only compliance export) |

---

## 2. Partitioning Strategy

### 2.1 Choice: Range Partition by `created_at`

**Why range by date, not list by tenant:**
- Only 1–10 tenants → partition pruning by tenant_id adds complexity for minimal gain
- Date-based partitioning enables **retention management** (drop old partitions for compliance expiry)
- Time-range partitions are predictable and automatable via `pg_partman`
- Date-range aligns with how the MKIS ingestion pipeline works (continuous, time-ordered)

**Why not subpartition by tenant:**
- Only 1–10 tenants → no partition pruning benefit
- Each subpartition would need its own HNSW index → too many indexes, too much complexity
- A B-tree composite index on `(tenant_id, created_at)` handles tenant-scoped queries efficiently

### 2.2 Partition Granularity

| Phase | Volume | Partition Size | Notes |
|-------|--------|---------------|-------|
| Initial (0–100k nodes) | 10k–100k | No partitioning needed | Single table. Partition when volume > 500k |
| Growth (100k–1M) | ~1M | **3 months per partition** | ~30k nodes/partition at 10k/month. Manageable HNSW builds |
| Scale (1M+) | 5M+ | **1 month per partition** | ~30k nodes/partition at 30k/month. Faster detach for archive |

**Recommendation:** Start without partitioning. Install `pg_partman` and configure it for monthly partitions before hitting 500k nodes.

---

## 3. Schema Design

### 3.1 Parent Table

```sql
CREATE TABLE knowledge_nodes (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    title TEXT NOT NULL,
    content TEXT NOT NULL,
    knowledge_type TEXT NOT NULL CHECK (...),
    status TEXT NOT NULL DEFAULT 'rascunho' CHECK (...),
    evidence_level TEXT CHECK (...),
    bias_risk TEXT NOT NULL DEFAULT 'nao_avaliado' CHECK (...),
    embedding vector(1024),
    embedding_version TEXT NOT NULL DEFAULT 'bge-m3-v1',
    checksum TEXT NOT NULL,
    created_by UUID NOT NULL,
    approved_by UUID,
    approved_at TIMESTAMPTZ,
    version TEXT NOT NULL DEFAULT '0.1.0',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    -- ...
) PARTITION BY RANGE (created_at);
```

### 3.2 Partition Creation Template

```sql
-- Monthly partition template
CREATE TABLE knowledge_nodes_2026_07 PARTITION OF knowledge_nodes
    FOR VALUES FROM ('2026-07-01') TO ('2026-08-01');

CREATE TABLE knowledge_nodes_2026_08 PARTITION OF knowledge_nodes
    FOR VALUES FROM ('2026-08-01') TO ('2026-09-01');

-- Each partition gets its own HNSW index
CREATE INDEX CONCURRENTLY idx_hnsw_2026_07
    ON knowledge_nodes_2026_07 USING hnsw (embedding vector_cosine_ops)
    WHERE status IN ('aprovado', 'descontinuado');

CREATE INDEX CONCURRENTLY idx_hnsw_2026_08
    ON knowledge_nodes_2026_08 USING hnsw (embedding vector_cosine_ops)
    WHERE status IN ('aprovado', 'descontinuado');
```

### 3.3 Global Indexes (on parent — not supported for HNSW)

**Important:** HNSW indexes cannot be created on the parent partitioned table. They must be created **on each partition individually**.

For non-vector lookups, B-tree indexes on the parent are automatically propagated:

```sql
-- These are automatically created on each partition via the parent:
CREATE INDEX idx_knowledge_nodes_tenant_id ON knowledge_nodes (tenant_id);
CREATE INDEX idx_knowledge_nodes_checksum ON knowledge_nodes (checksum);
CREATE UNIQUE INDEX idx_knowledge_nodes_id ON knowledge_nodes (id);

-- Composite for tenant-scoped search (frequent query pattern)
CREATE INDEX idx_knowledge_nodes_tenant_created
    ON knowledge_nodes (tenant_id, created_at DESC);
```

### 3.4 Partition-Specific HNSW Index Automation

Instead of manually creating HNSW indexes for each partition, use a helper function:

```sql
CREATE OR REPLACE FUNCTION create_partition_indexes()
RETURNS event_trigger AS $$
DECLARE
    partition_name TEXT;
BEGIN
    SELECT obj.identity INTO partition_name
    FROM pg_event_trigger_ddl_commands() obj
    WHERE obj.classid = 'pg_class'::regclass
      AND obj.object_type = 'table'
      AND obj.identity LIKE 'knowledge_nodes_%';

    IF partition_name IS NOT NULL THEN
        EXECUTE format(
            'CREATE INDEX CONCURRENTLY idx_hnsw_%s
             ON %I USING hnsw (embedding vector_cosine_ops)
             WHERE status IN (''aprovado'', ''descontinuado'')',
            partition_name, partition_name
        );
    END IF;
END;
$$ LANGUAGE plpgsql;

-- Note: event triggers fire AFTER the DDL, at which point the partition
-- is already attached. CONCURRENTLY cannot run inside a transaction
-- that has already altered the table. Therefore, run index creation
-- as a separate statement after partition attachment.
```

**Simpler approach:** A cron job or scheduled task that checks for partitions without HNSW indexes:

```sql
-- Run daily. Finds partitions missing HNSW index and creates them.
SELECT format(
    'CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_hnsw_%s
     ON %I USING hnsw (embedding vector_cosine_ops)
     WHERE status IN (''aprovado'', ''descontinuado'')',
    relname, relname
) AS ddl
FROM pg_class
WHERE relkind = 'r'                                 -- regular table (partition)
  AND relname LIKE 'knowledge_nodes_%'
  AND NOT EXISTS (
      SELECT 1 FROM pg_index
      WHERE indrelid = pg_class.oid
        AND indexrelid IN (
            SELECT oid FROM pg_class WHERE relname LIKE 'idx_hnsw_%'
        )
  );
```

---

## 4. Partition Lifecycle with pg_partman

### 4.1 Installation

```sql
CREATE EXTENSION pg_partman;

-- Create schema for pg_partman configuration
CREATE SCHEMA partman;
```

### 4.2 Configuration

```sql
-- Configure monthly partitioning for knowledge_nodes
SELECT partman.create_parent(
    p_parent_table := 'public.knowledge_nodes',
    p_control := 'created_at',
    p_type := 'native',
    p_interval := '1 month',
    p_premake := 3,          -- Create 3 future partitions
    p_start_partition := '2026-07-01'
);

-- Update configuration for retention: keep 84 months = 7 years
UPDATE partman.part_config
SET retention = '84 months',
    retention_keep_table = true,   -- Don't drop, just detach
    infinite_time_partitions = true,
    datetime_positive_infinite = '9999-12-31',
    datetime_negative_infinite = '2026-01-01'
WHERE parent_table = 'public.knowledge_nodes';
```

### 4.3 Maintenance Job

```sql
-- Run daily (via pg_cron or OS cron)
SELECT partman.run_maintenance();

-- This automatically:
-- 1. Creates new monthly partitions (3 ahead)
-- 2. Detaches partitions older than 84 months (7 years)
-- 3. Creates indexes on new partitions (via the trigger/cron in §3.4)
```

### 4.4 When Retention Expires (7+ years)

Partitions older than 7 years are **detached** (not dropped), making them available for compliance export:

```sql
-- pg_partman automatically runs this for partitions past retention:
ALTER TABLE knowledge_nodes DETACH PARTITION knowledge_nodes_2019_07;

-- The detached table still exists with all data and indexes.
-- Options for the detached partition:
--   a) pg_dump + delete: export to archive file, drop local table
--   b) Move to different tablespace (cold storage)
--   c) Convert to Parquet and upload to S3 (pg_parquet)
--   d) Keep detached but accessible for occasional queries
```

---

## 5. Query Patterns & Partition Pruning

### 5.1 Efficient Queries (prunes partitions)

```sql
-- ✅ BEST: filter by tenant + date range
SELECT id, title, embedding <=> query_embedding AS distance
FROM knowledge_nodes
WHERE tenant_id = 'abc-123'
  AND created_at >= '2026-01-01'
  AND created_at < '2026-04-01'
  AND status = 'aprovado'
ORDER BY embedding <=> query_embedding
LIMIT 10;

-- The planner:
-- 1. Prunes to partitions {'2026_01', '2026_02', '2026_03'}
-- 2. Scans each partition using its HNSW index
-- 3. Merge-append results from 3 partitions → top 10
```

### 5.2 Fallback: Tenant-Only Filter (no date)

```sql
-- ✅ GOOD: tenant_id only, no date range
SELECT id, title, embedding <=> query_embedding AS distance
FROM knowledge_nodes
WHERE tenant_id = 'abc-123'
  AND status = 'aprovado'
ORDER BY embedding <=> query_embedding
LIMIT 10;

-- Partition pruning: ALL partitions scanned (but filtered by tenant via B-tree)
-- HNSW index used per partition (partial: only 'aprovado' status)
-- Acceptable for up to ~3 years of partitions (~36)
```

### 5.3 Worst Case: No Partition Key Filter

```sql
-- ❌ AVOID: no tenant_id, no date range
SELECT id, title, embedding <=> query_embedding AS distance
FROM knowledge_nodes
ORDER BY embedding <=> query_embedding
LIMIT 10;

-- Partition pruning: NONE — scans ALL partitions
-- Planner may choose Sequential Scan if too many partitions
-- Mitigation: always require at least tenant_id filter in the app layer
```

### 5.4 Application Layer — Ensuring Partition Pruning

The MKIS API should enforce partition-key filters:

```python
# api/search.py
def search_knowledge(
    tenant_id: UUID,
    query: str,
    date_from: datetime | None = None,
    date_to: datetime | None = None,
):
    """
    Search knowledge nodes.

    tenant_id is REQUIRED — this is the partition key and is mandatory
    for efficient query execution. Requests without tenant_id are rejected.
    """
    if not date_from:
        date_from = datetime.now() - timedelta(days=365*3)  # default: last 3 years
    ...
```

---

## 6. Storage & Cost Analysis

### 6.1 Storage Projection (7 years, all-hot)

| Metric | 100k nodes | 1M nodes | 10M nodes |
|--------|-----------|---------|----------|
| **Table + metadata** | ~500 MB | ~5 GB | ~50 GB |
| **Embeddings (1024d)** | ~400 MB | ~4 GB | ~40 GB |
| **HNSW indexes** | ~600 MB | ~6 GB | ~60 GB |
| **B-tree indexes** | ~200 MB | ~2 GB | ~20 GB |
| **Total** | **~1.7 GB** | **~17 GB** | **~170 GB** |
| **SSD cost (NVMe, ~R$ 2/GB/mês)** | ~R$ 3.40 | ~R$ 34 | ~R$ 340 |

**Conclusion:** Even at 10M nodes, all-hot storage on SSD is affordable for a single VPS.

### 6.2 Number of Partitions Over 7 Years

| Partition size | Monthly | Quarterly |
|---------------|---------|-----------|
| Partitions in 7 years | 84 | 28 |
| Nodes per partition (at 1M total) | ~12k | ~36k |
| HNSW build time per partition | ~30s | ~90s |

**Monthly is recommended** — faster partition creation, faster reindex, easier detach.

---

## 7. Phased Implementation

### Phase 1 — No Partitioning (current, 0–500k nodes)

```sql
-- Single table, no partitioning. Simple.
CREATE TABLE knowledge_nodes (
    ...like CREATE TABLE in blocker-2...
);
CREATE INDEX CONCURRENTLY idx_hnsw ON knowledge_nodes USING hnsw (embedding vector_cosine_ops)
    WHERE status IN ('aprovado', 'descontinuado');
```

### Phase 2 — Convert to Partitioned (at ~500k nodes)

```sql
-- PostgreSQL 13+ supports converting a table to partitioned via:
ALTER TABLE knowledge_nodes RENAME TO knowledge_nodes_old;

-- Create new partitioned parent
CREATE TABLE knowledge_nodes (LIKE knowledge_nodes_old INCLUDING DEFAULTS INCLUDING CONSTRAINTS)
    PARTITION BY RANGE (created_at);

-- Create monthly partitions
SELECT partman.create_parent(...);

-- Migrate data (can be done in batches)
INSERT INTO knowledge_nodes SELECT * FROM knowledge_nodes_old;

-- Create HNSW indexes on each partition
-- (automated by the cron job in §3.4)

-- Drop old table
DROP TABLE knowledge_nodes_old;
```

### Phase 3 — Install pg_partman (at any time)

```sql
CREATE EXTENSION pg_partman;
-- Configure as §4.2
-- Add to pg_cron: SELECT partman.run_maintenance();
```

---

## 8. Future: Cold Storage (When Needed)

If storage costs become significant or data exceeds 10M nodes, the design supports adding cold storage without schema changes:

```sql
-- Step 1: Detach old partition (instant, zero downtime)
ALTER TABLE knowledge_nodes DETACH PARTITION knowledge_nodes_2020_01;

-- Step 2: Archive to Parquet in S3
-- (using pg_parquet or a custom export script)
COPY knowledge_nodes_2020_01 TO '/tmp/2020_01.parquet' WITH (FORMAT PARQUET);
aws s3 cp /tmp/2020_01.parquet s3://mkis-archive/knowledge_nodes/2020_01/

-- Step 3: Drop local table
DROP TABLE knowledge_nodes_2020_01;

-- Step 4 (optional): Foreign data wrapper for querying archived data
-- CREATE FOREIGN TABLE knowledge_nodes_2020_01 (...) SERVER s3_server;
```

---

## 9. Implementation Checklist

### Before Partitioning
- [ ] Install `pg_partman` extension
- [ ] Install `pg_cron` (or schedule `run_maintenance()` via OS cron)
- [ ] Configure `partman.create_parent()` for monthly partitions
- [ ] Set retention policy: 84 months with `retention_keep_table = true`
- [ ] Create HNSW index automation script (§3.4)

### Query Optimization
- [ ] Ensure app layer always passes `tenant_id` in search queries (§5.4)
- [ ] Enforce default `date_from` (3 years) to limit partition scan
- [ ] Monitor partition pruning via `EXPLAIN ANALYZE` on representative queries

### Monitoring
- [ ] Track: number of partitions, size per partition, HNSW index size
- [ ] Alert: any partition without HNSW index
- [ ] Alert: partition approaching retention limit (6.5 years)

---

## 10. Remaining Blockers

| # | Blocker | Status | Next Step |
|---|--------|--------|-----------|
| 1 | Vendor/Stack choice | ✅ **RESOLVED** | BGE-M3 + Qwen 2.5 7B |
| 2 | Canonical enums | ✅ **RESOLVED** | 16 enums closed |
| 3 | pgvector migration | ✅ **RESOLVED** | Dual-write + CONCURRENTLY |
| **4** | **Partitioning** | **✅ RESOLVED** | **This document** |
| 5 | Pipeline states | ❌ Open | Add: quarantined, awaiting_approval |
| 6 | Deep delete LGPD | ❌ Open | Design cascade for vectors, KG, cache |

---

## 11. References

- pg_partman docs: https://github.com/pgpartman/pg_partman
- PostgreSQL declarative partitioning: https://www.postgresql.org/docs/current/ddl-partitioning.html
- Crunchy Data pg_partman guide: https://www.crunchydata.com/blog/auto-archiving-and-data-retention-management-in-postgres-with-pg_partman
- pgvector HNSW on partitioned tables: https://github.com/pgvector/pgvector
- ARB report: `docs/mkis/ARB-architecture-review-report.md`
- Blocker #1 (vendor): `docs/mkis-blocker-1-vendor-decision.md`
- Blocker #2 (enums): `docs/mkis-blocker-2-enums-canonical.md`
- Blocker #3 (pgvector): `docs/mkis-blocker-3-pgvector-migration.md`

---

*End of document — Blocker #4 resolved ✅*
