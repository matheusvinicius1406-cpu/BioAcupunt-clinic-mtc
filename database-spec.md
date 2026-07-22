# Database Architecture Spec — BioAcupunt Supremo

**File:** `database-spec.md`
**Date:** 2026-07-22
**Status:** Draft — compiled from audit + stakeholder interview
**Authors:** Buffy (AI Agent) + User

---

## 1. Purpose

This document captures the complete database architecture of the BioAcupunt Supremo system, covering all three data stores, their relationships, migration history, sync strategy, and a roadmap to production-readiness for the MKIS layer.

---

## 2. System Overview — Three Databases

```
┌────────────────────────────────────────────────────────────────────┐
│                    BioAcupunt Supremo — Data Layer                   │
├─────────────────┬──────────────────────┬───────────────────────────┤
│  Android Local  │   Backend Server     │  MKIS Knowledge System    │
│    (SQLite)     │    (PostgreSQL)      │  (PostgreSQL + pgvector)  │
│                 │                      │                           │
│  Room ORM       │  SQLAlchemy Async    │  SQLAlchemy Async         │
│  Offline-first  │  Multi-tenant        │  Vector embeddings        │
│  19 entities    │  8 tables            │  5 core tables            │
│  Version 17     │  2 Alembic migs      │  Design phase (5.2/10)    │
│  FTS4 search    │  Delta sync by rev   │  HNSW index               │
└────────┬────────┴──────────┬───────────┴─────────────┬─────────────┘
         │                   │                         │
         │     Sync Engine   │                         │
         │  (offline-first)  │                         │
         └───────────────────┘                         │
                                                       │
         (PostgreSQL + pgvector — not yet live)        │
         └─────────────────────────────────────────────┘
```

---

## 3. Android Local — SQLite via Room

### 3.1 Connection
- **Database:** `bioacupunt_db` (SQLite file, stored in app's private directory)
- **ORM:** Room (compile-time verified SQL)
- **Current version:** 17 (16 migrations: 1→2, 2→3, ..., 16→17)
- **Thread safety:** Double-checked locking with `@Volatile` (was bug: `lateinit`/`init`)

### 3.2 Full Entity List (19 tables)

| # | Entity | Table | Context | Syncable? |
|---|--------|-------|---------|-----------|
| 1 | `KnowledgeNode` | `knowledge_nodes` | Biblioteca | No (local) |
| 2 | `PatientEntity` | `crm_patients` | CRM | ✅ Yes |
| 3 | `CrmPatientEntity` | `crm_patients` | CRM | ✅ Yes |
| 4 | `AppointmentEntity` | `appointments` | Agenda | ✅ Yes |
| 5 | `TransacaoEntity` | `transacoes` | Financeiro | ✅ Yes |
| 6 | `ProntuarioEntity` | `prontuarios` | Prontuário | No (local) |
| 7 | `ProntuarioEntryEntity` | `prontuario_entries` | Prontuário (evoluções) | No (local) |
| 8 | `MtcAssessmentEntity` | `mtc_assessments` | Avaliação MTC | No (local) |
| 9 | `VitalSignEntity` | `vital_signs` | Sinais vitais | No (local) |
| 10 | `LabExamEntity` | `lab_exams` | Exames laboratoriais | No (local) |
| 11 | `MedicationEntity` | `medications` | Medicações | No (local) |
| 12 | `AllergyEntity` | `allergies` | Alergias | No (local) |
| 13 | `ProntuarioDocumentEntity` | `prontuario_documents` | Documentos anexados | No (local) |
| 14 | `BibliotecaNodeEntity` | `biblioteca_nodes` | Biblioteca (curadoria) | No (local) |
| 15 | `FavoriteArticleEntity` | `favorite_articles` | Favoritos | No (local) |
| 16 | `ArticleFtsEntity` | `article_fts` (FTS4) | Busca textual | No (FTS virtual) |
| 17 | `SyncQueueEntity` | `sync_queue` | Fila de sync | No (infra) |
| 18 | `SyncStateEntity` | `sync_state` | Estado do sync | No (infra) |
| 19 | `SyncConflictEntity` | `sync_conflicts` | Conflitos | No (infra) |
| 20 | `ReportEntity` | `report` | Relatórios | No (local) |

### 3.3 Key Migration History

| Migration | Key Change | Risk |
|-----------|-----------|------|
| 1→2 | `patients.pendingSync` | Low |
| 2→3 | `patients.tenantId` | Low |
| 3→4 | `sync_queue.retryCount` | Low |
| 8→9 | `mtc_assessments` — Ba Gang, tongue, pulse, flags | **Medium** — new clinical table |
| 9→10 | `vital_signs`, `lab_exams`, `medications`, `allergies`, `prontuario_documents` | Low |
| 11→12 | MTC assessment extra fields (relieving, aggravating, ROS) | Low |
| **12→13** | **One patient registry merge** — legacy `patients` → `crm_patients` | **Critical** — FK repoint |
| 13→14 | Sync identity (clientId, serverId, baseRev) on syncable tables | **Medium** — data backfill |
| 14→15 | FTS4 virtual table `article_fts` | Low (additive) |
| 15→16 | FTS4 provenance column (drop+recreate FTS) | Low (rebuild) |
| **16→17** | `transacoes.tenantId` + override columns on `mtc_assessments` | **Medium** — pending (#1) |

### 3.4 Known Migration Bugs (Fixed)
- **Offset bug (pre-v17):** `getCurrentDbVersion()` used to read byte offset 24 (change counter) instead of offset 60 (user_version). Pre-migration backup never ran. Fixed.
- **One patient registry (v13):** Two rival patient tables (`patients` + `crm_patients`) caused silent FK violations. All 8 clinical tables repointed to `crm_patients`.

---

## 4. Backend Server — PostgreSQL

### 4.1 Connection
- **Driver:** `asyncpg` (async)
- **ORM:** SQLAlchemy 2.0 `DeclarativeBase` with async sessions
- **Migration tool:** Alembic
- **Multi-tenancy:** By `clinic_id` (the backend's term for tenant)
- **URL scheme:** Auto-normalized: `postgres://` → `postgresql+asyncpg://`

### 4.2 Full Table List (8 tables)

| Table | Purpose | Syncable? |
|-------|---------|-----------|
| `clinics` | Clinic registration | No (authority) |
| `users` | Auth + RBAC (admin/professional/staff) | No (authority) |
| `patients` | Patient records (mirrors `crm_patients`) | ✅ Yes |
| `appointments` | Appointment records | ✅ Yes |
| `transactions` | Financial movements | ✅ Yes |
| `data_access_logs` | LGPD audit log | No (append-only) |
| `refresh_sessions` | Auth token rotation | No (internal) |
| `clinic_revisions` | Revision counter per clinic | No (internal) |

### 4.3 Sync Protocol

**Key design decision:** Revision counter, not timestamps. Device clocks drift; a counter issued server-side cannot drift.

```
Pull flow:
  Client: GET /sync?clinic_id=X&since=N
  Server: SELECT * FROM {table} WHERE clinic_id=X AND rev > N

Push flow:
  Client: POST /sync { rows: [{ clientId, data, baseRev }] }
  Server: row_lock → if server.rev == baseRev → accept + issue new rev
          else → return conflict
```

**Syncable tables:** `patients`, `appointments`, `transactions`
**Clinical tables deliberately NOT syncable** (stay on device per R1/R4).

---

## 5. MKIS (Medical Knowledge Intelligence System) — Future PostgreSQL + pgvector

**Status:** Design phase — ARB score 5.2/10 — **6 blockers before implementation**

### 5.1 Core Tables (5)

| Table | Key Fields | Purpose |
|-------|-----------|---------|
| `knowledge_nodes` | `vector(1536)`, `tenant_id`, `doi`, `checksum`, `embedding_version` | Scientific knowledge items |
| `knowledge_node_versions` | `snapshot` (JSONB), `previous_version` | Immutable version history |
| `knowledge_artifacts` | `storage_key`, `sha256`, `scan_status`, `parsed_text` | Uploaded files (PDFs, images) |
| `knowledge_graph_edges` | `subject_id`, `predicate`, `object_id` | Relationship graph |
| `audit_trail` | `previous_hash`, `payload_hash` | Append-only chain |

### 5.2 MKIS Blockers (must resolve before coding)

| # | Blocker | Owner | Priority |
|---|--------|-------|----------|
| 1 | LLM/embedding vendor not chosen | Engineering + Finance | **Critical** |
| 2 | Canonical enums not closed (knowledge_type, status, evidence_level, bias_risk) | Backend | **Critical** |
| 3 | pgvector migration strategy (dual-write + shadow index) | DBA | **Critical** |
| 4 | Partitioning strategy for `knowledge_nodes` | DBA | **High** |
| 5 | Pipeline states missing: `quarantined`, `awaiting_approval`, `needs_human_review` | Backend | **High** |
| 6 | Deep delete LGPD workflow (cascade: vectors → KG edges → cache → audit summary) | Compliance | **High** |

### 5.3 Known Architectural Issues (ARB findings)

- `KnowledgeNode` has `related_*` UUID arrays that duplicate the graph → **must remove**
- `vector(1536)` is fixed — changing embedding model requires full table rewrite
- `audit_trail.previous_hash` serializes inserts → lock contention risk at >1k events/sec
- No `Tenant` entity in domain model (only `tenant_id` as FK)

---

## 6. LGPD Compliance Requirements (Sync & Data Residency)

### 6.1 Current State
- **Consent:** Patients have consent records in the app (not yet server-side)
- **Audit logs:** `data_access_logs` on server; no equivalent on Android
- **Soft delete:** `deleted_at` on syncable tables
- **Right to erasure:** NOT implemented (deep delete is blocker #6 above)

### 6.2 Gaps Identified

| Gap | Severity | Description |
|-----|----------|-------------|
| No deep delete cascade | **Critical** | Soft delete leaves vectors, KG edges, cache entries |
| No data minimization policy | **Medium** | IP addresses, user-agent collected without retention policy |
| No explicit consent for AI processing | **Medium** | Patient data fed into RAG without separate consent |
| No data residency policy | **High** | Undefined where patient data can/cannot be stored |
| No ANVISA classification | **High** | Not determined if MKIS qualifies as medical device |

### 6.3 Data Classification (proposed)

| Classification | Examples | Storage Restriction |
|---------------|----------|-------------------|
| **PUBLIC** | Acupuncture point names, meridian descriptions | No restriction |
| **INTERNAL** | Aggregated appointment counts, revenue | PostgreSQL (any region) |
| **RESTRICTED** | Treatment protocols, clinical patterns | PostgreSQL (Brazil preferred) |
| **PII** | Patient names, CPF, addresses, phone | SQLite (device only) + masked on server |

---

## 7. Key Decisions & Priorities (from Interview)

### 7.1 Immediate Priorities
1. ✅ **Understand full architecture** (this document)
2. 🔶 **Destravar MKIS** — resolver os 6 bloqueadores para começar implementação
3. 🔶 **LGPD compliance** — manter sync engine existente, ajustar residência e deep delete
4. 🔷 **Residência de dados** — ainda não decidido (precisa estudar leis)

### 7.2 Design Decisions Confirmed
- **Keep offline-first** — SQLite (local) + PostgreSQL (server) separation stays
- **Keep revision-counter sync** — timestamp-based sync was correctly rejected
- **Full MKIS** — not incremental: all 5 tables + embeddings + hybrid search + versions + audit
- **Clinical data stays on device** — prontuário, avaliação MTC, língua/pulso are NOT syncable

### 7.3 Open Questions
- Data residency: Brazil-only, or international cloud OK?
- ANVISA classification needed for MKIS (clinical decision support)?
- GPT/embedding vendor preference (OpenAI? Anthropic? Open-source self-hosted?)?

---

## 8. Migration Inventory — Complete Timeline

```
v1 → v2   [Room]   ALTER patients ADD pendingSync
v2 → v3   [Room]   ALTER patients ADD tenantId + INDEX
v3 → v4   [Room]   ALTER sync_queue ADD retryCount + lastError
v4 → v5   [Room]   ALTER sync_queue ADD version
v5 → v6   [Room]   ALTER appointments ADD notes
v6 → v7   [Room]   ALTER appointments ADD sessionNumber
v7 → v8   [Room]   ALTER appointments ADD valueBrl + paid
v8 → v9   [Room]   CREATE mtc_assessments (Ba Gang, tongue, pulse, flagsCsv)
v9 → v10  [Room]   CREATE vital_signs, lab_exams, medications, allergies, prontuario_documents
v10 → v11 [Room]   CREATE favorite_articles
v11 → v12 [Room]   ALTER mtc_assessments ADD 5 columns (relieving, aggravating, ROS, etc.)
v12 → v13 [Room]   One patient registry: repoint 8 FK from patients → crm_patients
v13 → v14 [Room]   Sync identity: clientId, serverId, baseRev on syncable tables
v14 → v15 [Room]   CREATE VIRTUAL TABLE article_fts (FTS4)
v15 → v16 [Room]   DROP+CREATE article_fts WITH provenance column
v16 → v17 [Room]   ALTER transacoes ADD tenantId; ALTER mtc_assessments ADD override columns

v1 (Alembic) [PostgreSQL] 66a9bfd6d790 — initial schema (7 tables)
v2 (Alembic) [PostgreSQL] b2f4c1a70d31 — delta sync (transactions, clinic_revisions, sync columns)
```

---

## 9. Risk Register

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| Room migration fails on device | Low | **Critical** — chart loss | Pre-migration backup + fallback to version check |
| Sync conflict overwrites clinical data | Medium | **Critical** | Conflict table + manual resolution UI |
| MKIS vector index degrades without reindex | Medium | **High** | Shadow index + feature flag swap |
| Embedding model change requires full rewrite | Medium | **High** | `embedding_version` column + dual-write strategy |
| LGPD deep delete incomplete | Medium | **Critical** | Cascade mapping + purge job + confirmation cert |
| Third-party LLM/embedding cost explosion | High | **High** | Budget per tenant + circuit breaker + fallback chain |

---

## 10. Next Steps Roadmap

### Phase 1 — Foundation (current)
- [x] Full audit of existing database architecture (this document)
- [ ] Resolve MKIS blockers #1 (vendor) and #2 (canonical enums)
- [ ] Decide data residency policy (Brazil vs international)

### Phase 2 — MKIS Implementation
- [ ] Design complete MKIS schema with blocker resolutions
- [ ] Implement `knowledge_nodes` + `vector(1536)` + HNSW index
- [ ] Implement dual-write migration strategy for embedding versioning
- [ ] Add missing pipeline states (`quarantined`, `awaiting_approval`)
- [ ] Implement deep delete LGPD cascade

### Phase 3 — LGPD Hardening
- [ ] Add consent table (granular: AI processing, data sharing, analytics)
- [ ] Add client-side audit log
- [ ] Implement data minimization (IP/UA retention policy)
- [ ] Determine ANVISA classification

### Phase 4 — Production Readiness
- [ ] Performance benchmarks: Room queries, sync throughput, vector search latency
- [ ] Disaster recovery runbook: PostgreSQL backup + restore, device data recovery
- [ ] Encryption audit: SQLite at rest, PostgreSQL TLS, backup encryption

---

*End of database-spec.md*
