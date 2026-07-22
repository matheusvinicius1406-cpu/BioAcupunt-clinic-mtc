# MKIS — Blocker #1 Resolution: Vendor & Stack Decision

**Date:** 2026-07-22
**Status:** ✅ DECIDED
**Decision:** Self-hosted open-source stack (BGE-M3 + Qwen 2.5 + sentence-transformers)
**ARB Blocker #1:** LLM/embedding vendor undefined → **RESOLVED**

---

## 1. Decision Overview

| Component | Chosen | Alternative | Reason |
|-----------|--------|-------------|--------|
| **Embedding Model** | **BGE-M3** (BAAI) | OpenAI text-embedding-3-small | LGPD compliance: self-hosted, data never leaves Brazil |
| **LLM (Pipeline)** | **Qwen 2.5 7B Instruct** | Gemma 2B/9B, Llama 3.2 3B | Apache 2.0 license, strong Portuguese+Chinese (MTC), CPU-runnable |
| **Inference Engine** | **sentence-transformers** (embedding) + **llama.cpp / Ollama** (LLM) | vLLM, TGI | CPU-only deployment; llama.cpp is optimized for CPU/quantized models |
| **Vector DB** | **pgvector** (existing PostgreSQL) | Qdrant, Pinecone | Already in the architecture (ADR 002); no new infrastructure |
| **Re-ranking** | **BGE-reranker-v2-m3** | Cohere Rerank | Self-hosted, same family as BGE-M3, CPU-friendly |

---

## 2. Why This Stack?

### 2.1 Alignment with Existing Architecture

The Android app already uses **Qwen 2.5 1.5B** and **Gemma 3 1B** as on-device models. Choosing **Qwen 2.5 7B** for the server pipeline means:

- Same model family (Qwen) on device + server → consistent behaviour
- The `AskLibraryUseCase` (RAG) on Android queries the server, which uses the same Qwen model for answer synthesis
- Shared tokenizer, shared prompt patterns

### 2.2 LGPD Compliance

- **BGE-M3** and **Qwen 2.5**: both Apache 2.0 / MIT licensed — no third-party data processing
- Inference runs on your own infrastructure (cloud GPU or on-prem)
- Patient data never leaves your PostgreSQL + inference server
- Pipeline extraction happens entirely in your VPC / datacenter

### 2.3 Portuguese + Chinese (MTC) Suitability

| Model | Portuguese | Chinese (MTC terms) | Medical terms |
|-------|-----------|---------------------|---------------|
| **BGE-M3** | ✅ Native multilingual | ✅ Native CJK support | ✅ Can be fine-tuned |
| **Qwen 2.5 7B** | ✅ Good (trained on multilingual data) | ✅ Excellent (Qwen is Chinese-origin) | ✅ Strong bench scores |
| BERTimbau | ✅ Best Portuguese | ❌ No Chinese | ⚠️ Needs custom pooling |
| OpenAI ada-002 | ✅ Good | ✅ Good | ❌ Data leaves Brazil |

### 2.4 CPU-Only Viability

| Model | Quantization | RAM needed | Speed (tokens/sec)* |
|-------|-------------|-----------|-------------------|
| **Qwen 2.5 7B** | Q4_K_M (llama.cpp) | ~6 GB | 8-12 tok/s |
| **BGE-M3** | FP16 (ONNX) | ~4 GB | 50-100 docs/s** |
| **BGE-reranker-v2-m3** | Q8 (llama.cpp) | ~3 GB | 10-20 queries/s |

*\*Estimated on modern CPU with AVX-512*
*\*\*Batch inference, depends on document length*

---

## 3. Detailed Component Specifications

### 3.1 Embedding: BGE-M3 (BAAI)

| Property | Value |
|----------|-------|
| **Model** | `BAAI/bge-m3` |
| **Dimensions** | 1024 (sweet spot: quality vs storage) |
| **Max tokens** | 8192 (longer context than E5's 512) |
| **Languages** | 100+ (native multilingual) |
| **License** | Apache 2.0 ✅ |
| **HuggingFace** | https://huggingface.co/BAAI/bge-m3 |
| **Run via** | `sentence-transformers` (Python) or ONNX runtime |
| **Index type** | HNSW (pgvector) — `m: 16, ef_construction: 200` |
| **Distance function** | `cosine` (preferred for normalized embeddings) |

**Why 1024d instead of 768 or 1536:**
- 1536d = 50% more storage + RAM for marginal gain (per ARB analysis)
- 768d is sufficient for most cases, but 1024d gives better semantic resolution for specialized medical terminology
- BGE-M3 natively outputs 1024d — no dimension reduction needed

### 3.2 LLM (Pipeline): Qwen 2.5 7B Instruct

| Property | Value |
|----------|-------|
| **Model** | `Qwen/Qwen2.5-7B-Instruct` |
| **License** | Apache 2.0 ✅ |
| **Context window** | 32,768 tokens |
| **Quantization** | Q4_K_M via llama.cpp (~6 GB RAM) |
| **Run via** | Ollama or llama.cpp server |
| **Endpoint** | Local HTTP API (OpenAI-compatible) |
| **Use cases** | Document summarization, evidence scoring, keyword extraction, classification |
| **Temperature** | 0.0 (extraction) / 0.3 (scoring) |

**Why Qwen over Gemma:**
- **Apache 2.0 license** — no terms-of-use acceptance needed (Gemma requires Gemma Terms)
- **Strong Chinese support** — critical for TCM acupuncture point names, meridian terms
- **Portuguese competence** — trained on multilingual corpus
- **Already in Android catalog** — `qwen2.5-1.5b-instruct` is listed in `LocalModelCatalog`

**Why 7B instead of 1.5B/2B:**
- 1.5B is good for on-device inference (latency + RAM constraints)
- 7B is the minimum for reliable extraction + scoring in the pipeline
- 7B fits in ~6 GB RAM with Q4 quantization (CPU-friendly)

### 3.3 Re-ranking: BGE-reranker-v2-m3

| Property | Value |
|----------|-------|
| **Model** | `BAAI/bge-reranker-v2-m3` |
| **License** | Apache 2.0 ✅ |
| **Use** | Re-rank top-50 results from hybrid search → top-10 |
| **Run via** | sentence-transformers or llama.cpp |
| **RAM** | ~3 GB (Q8) |

### 3.4 Infrastructure Requirements

| Resource | Minimum | Recommended |
|----------|---------|-------------|
| **CPU cores** | 4 | 8+ |
| **RAM** | 16 GB | 32 GB |
| **Storage** | 50 GB (models + database) | 200 GB+ |
| **GPU** | Not required | Optional (10x speed on LLM inference) |
| **OS** | Linux (Ubuntu 22.04+) | Ubuntu 24.04 |

---

## 4. Integration Architecture

```
┌──────────────────────────────────────────────────────────────────┐
│                        MKIS Pipeline                              │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│   ┌──────────┐    ┌──────────────┐    ┌─────────────────┐        │
│   │ Document  │───▶│ BGE-M3      │───▶│ pgvector        │        │
│   │ uploaded  │    │ (embedding)  │    │ (vector search) │        │
│   └──────────┘    └──────────────┘    └────────┬────────┘        │
│                                                 │                 │
│   ┌─────────────────────────────────────────────▼──────────┐     │
│   │              Hybrid Search (BM25 + vector)              │     │
│   │  BM25 (tsvector) + cosine BGE-M3 → RRF fusion →       │     │
│   │  BGE-reranker-v2-m3 (re-rank) → top-10 results         │     │
│   └──────────────────────────────────────┬──────────────────┘     │
│                                          │                        │
│   ┌──────────────────────────────────────▼──────────────────┐     │
│   │              Qwen 2.5 7B (LLM Pipeline)                  │     │
│   │  - Document summarization & keyword extraction          │     │
│   │  - Evidence scoring & bias classification               │     │
│   │  - RAG answer synthesis (AskLibraryUseCase fallback)    │     │
│   └──────────────────────────────────────┬──────────────────┘     │
│                                          │                        │
│   ┌──────────────────────────────────────▼──────────────────┐     │
│   │              Local LLM on Android                         │     │
│   │  Qwen 2.5 1.5B / Gemma 3 1B / Phi-4 Mini               │     │
│   │  LiteRT runtime — same Qwen family, same prompt style   │     │
│   └──────────────────────────────────────────────────────────┘     │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

---

## 5. Cost Analysis

### 5.1 Infrastructure (Monthly Estimate)

| Resource | Cost (BRL) | Cost (USD) |
|----------|-----------|------------|
| VPS (8 CPU, 32 GB RAM) — e.g., Hetzner AX102 | ~R$ 250/mês | ~$45/mês |
| Storage (200 GB SSD) | Included | Included |
| CDN for model downloads | Free (HuggingFace) | Free |
| **Total (CPU only)** | **~R$ 250/mês** | **~$45/mês** |

### 5.2 Comparison with Cloud APIs

| Service | Monthly Cost (10k docs + 10k queries) | Data Residency | LGPD Ready |
|---------|--------------------------------------|----------------|------------|
| **Self-hosted (this proposal)** | **~R$ 250** | ✅ Full control | ✅ Yes |
| OpenAI (text-embedding-3-small + GPT-4o-mini) | ~$200 (~R$ 1.100) | ❌ US servers | ❌ Risky |
| Anthropic Claude Haiku | ~$150 (~R$ 830) | ❌ US servers | ❌ Risky |
| Google Gemini (self-host GDC) | ~$500+ (~R$ 2.800) | ⚠️ GDC air-gapped | ✅ Possible |

**Self-hosted is ~4-10x cheaper** + LGPD compliant by design.

---

## 6. Migration Strategy (pgvector)

Following the best practices from ARB + web research:

### Phase 1 — Schema (additive only, zero downtime)
```sql
-- Add new columns to knowledge_nodes
ALTER TABLE knowledge_nodes ADD COLUMN embedding_version TEXT NOT NULL DEFAULT 'bge-m3-v1';
ALTER TABLE knowledge_nodes ADD COLUMN embedding_bge_m3 vector(1024);

-- Dual-write: app writes to both embedding (old) and embedding_bge_m3 (new)
-- Read queries still use the old column until backfill completes
```

### Phase 2 — Backfill (background, no downtime)
```bash
# Batch backfill old rows
for batch in $(seq 0 1000 $MAX); do
    psql -c "UPDATE knowledge_nodes SET embedding_bge_m3 = \
        bge_m3_embed(content) WHERE id IN (SELECT id FROM knowledge_nodes \
        WHERE embedding_bge_m3 IS NULL LIMIT 100)"
done
```

### Phase 3 — Switch (atomic)
```sql
-- Once backfill completes, create HNSW index on new column
SET maintenance_work_mem = '8GB';
CREATE INDEX CONCURRENTLY idx_knowledge_nodes_bge_m3
    ON knowledge_nodes USING hnsw (embedding_bge_m3 vector_cosine_ops);

-- Update app code to query embedding_bge_m3
-- After validation period, drop old column
ALTER TABLE knowledge_nodes DROP COLUMN embedding;
ALTER TABLE knowledge_nodes RENAME COLUMN embedding_bge_m3 TO embedding;
ALTER TABLE knowledge_nodes RENAME COLUMN embedding_version TO embedding_version;
```

---

## 7. What This Unlocks

With Blocker #1 resolved, the following MKIS components become implementable:

| Component | Now Possible? | Dependency |
|-----------|--------------|------------|
| `knowledge_nodes` schema | ✅ | BGE-M3 dimensions (1024) |
| Embedding generation | ✅ | BGE-M3 |
| Hybrid search (BM25 + vector) | ✅ | BGE-M3 embeddings in pgvector |
| RAG answer synthesis | ✅ | Qwen 2.5 7B for LLM |
| Document extraction pipeline | ✅ | Qwen 2.5 7B |
| Evidence scoring | ✅ | Qwen 2.5 7B |
| Re-ranking | ✅ | BGE-reranker-v2-m3 |

---

## 8. Remaining Blockers

| # | Blocker | Status | Next Step |
|---|--------|--------|-----------|
| **1** | Vendor/Stack choice | ✅ **RESOLVED** | This document |
| **2** | Canonical enums | ❌ Open | Define enums: knowledge_type, status, evidence_level, bias_risk |
| **3** | pgvector migration | ✅ Drafted in §6 | Needs DBA review |
| **4** | Partitioning | ❌ Open | Define partition key + schedule |
| **5** | Pipeline states | ❌ Open | Add: quarantined, awaiting_approval, needs_human_review |
| **6** | Deep delete LGPD | ❌ Open | Design cascade: vectors → KG → cache → audit |

---

## 9. References

- **BGE-M3:** https://huggingface.co/BAAI/bge-m3
- **Qwen 2.5 7B:** https://huggingface.co/Qwen/Qwen2.5-7B-Instruct
- **BGE-reranker-v2-m3:** https://huggingface.co/BAAI/bge-reranker-v2-m3
- **llama.cpp:** https://github.com/ggerganov/llama.cpp
- **Ollama:** https://ollama.com
- **pgvector HNSW:** https://github.com/pgvector/pgvector
- **ARB Report:** `docs/mkis/ARB-architecture-review-report.md`
- **Database Spec:** `database-spec.md`
- **ADR 002 — PostgreSQL:** `docs/architecture/ADR/002-postgresql.md`

---

*End of report — Blocker #1 resolved ✅*
