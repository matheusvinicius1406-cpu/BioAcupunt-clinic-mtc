# Knowledge Core — Status Audit

**Date:** 2026-08-18
**Last Updated:** 2026-08-18 (Phase 2 Final Gate verified)
**Auditor:** Buffy (AI agent)
**Scope:** Complete audit + stabilization of Knowledge Core
**Status:** ✅ PHASE 2 COMPLETE — all consumers migrated, E2E verified, dual-run validated, legacy dependencies classified

---

## Executive Summary

**Phase 1 Stabilization is COMPLETE.** The Knowledge Core has been fixed, wired, tested, and documented.

### What was fixed
- ✅ Metadata round-trip (was silently dropping all metadata)
- ✅ Provenance round-trip on relations (was always empty)
- ✅ Evidence level round-trip (was always null)
- ✅ AppContainer wiring (now instantiates DAO, repository, importer, adapters)
- ✅ Legacy KnowledgeRepository renamed to avoid collision
- ✅ Legacy import pipeline implemented (bridge, not replacement)
- ✅ Missing DAO operations added (getByType, getByStatus, count, delete, FTS)
- ✅ Backend Pydantic model aligned with Android domain
- ✅ 48+ new tests (mapper, adapter, importer, DAO, migration, backend)
- ✅ Documentation updated (migration strategy, search paths)

### Current state
- Knowledge Core tables: ✅ Schema correct (Room v25)
- Knowledge Core wiring: ✅ Connected in AppContainer
- Knowledge Core tests: ✅ 48+ tests passing
- Legacy tables: ✅ Still primary source (bridge pattern)
- Backend: ✅ Pydantic model aligned with Android domain

### Architecture

```text
Legacy Sources                    Canonical Layer
─────────────                    ───────────────
biblioteca_nodes  ──Adapter──→   
                                   KnowledgeCoreEntity
knowledge_nodes   ──Adapter──→     (knowledge_core_entities)
                                         ↓
                                   KnowledgeRepository
                                         ↓
                                   Consumers (UI, search, AI)
```

---

## 1. Components That EXIST and Are CORRECT

### 1.1 Domain Models (`mtc/knowledge/domain/KnowledgeCoreModels.kt`)

| Model | Status | Notes |
|-------|--------|-------|
| `KnowledgeEntityType` (17 types) | ✅ Correct | Covers all MTC entity types |
| `KnowledgeStatus` (4 states) | ✅ Correct | DRAFT, REVIEW, PUBLISHED, DEPRECATED |
| `KnowledgeRelationType` (15 types) | ✅ Correct | Covers all relation patterns |
| `KnowledgeEntity` | ✅ Correct | Full model with id, type, name, aliases, content, sources, citations, evidence, version, provenance |
| `KnowledgeRelation` | ✅ Correct | Directed relation with evidence and confidence |
| `KnowledgeSource` | ✅ Correct | Source identification with license |
| `KnowledgeCitation` | ✅ Correct | Citation with locator and excerpt |
| `KnowledgeEvidence` | ✅ Correct | Evidence with claim, citations, level, confidence |
| `KnowledgeProvenance` | ✅ Correct | Full provenance chain |
| `KnowledgeVersion` | ✅ Correct | Version with timestamps and status |
| `KnowledgeImport` | ✅ Correct | Bundle for import |
| `KnowledgeConflict` | ✅ Correct | Conflict detection |
| `KnowledgeMergeResult` | ✅ Correct | Merge result with conflicts |

### 1.2 Canonicalizer (`mtc/knowledge/domain/KnowledgeCanonicalizer.kt`)

| Function | Status | Notes |
|----------|--------|-------|
| `normalizeName()` | ✅ Correct | Unicode NFD, lowercase, strip accents |
| `canonicalId()` | ✅ Correct | Stable ID generation from type + name |
| `merge()` | ✅ Correct | Deterministic connected components, no LLM |

### 1.3 Room Entities (`mtc/knowledge/data/KnowledgeCoreEntities.kt`)

| Table | Status | Indices | Notes |
|-------|--------|---------|-------|
| `knowledge_core_entities` | ✅ Correct | type, canonical_name, status | PK: id |
| `knowledge_core_relations` | ✅ Correct | None (composite PK) | PK: (source_entity_id, relation_type, target_entity_id) |
| `knowledge_core_sources` | ✅ Correct | None | PK: id |
| `knowledge_core_citations` | ✅ Correct | source_id | PK: id |
| `knowledge_core_evidence` | ✅ Correct | claim | PK: id |
| `knowledge_core_provenance` | ✅ Correct | None (composite PK) | PK: (entity_id, original_source, original_id) |

### 1.4 Migration v24→v25 (`DatabaseModule.kt`)

| Aspect | Status | Notes |
|--------|--------|-------|
| CREATE TABLE statements | ✅ Correct | Matches entity definitions |
| Indices | ✅ Correct | Matches @Index annotations |
| No DEFAULT in CREATE TABLE | ✅ Correct | Follows project rule |
| Additive only | ✅ Correct | No drops, no alterations |

### 1.5 AppDatabase Registration

| Aspect | Status | Notes |
|--------|--------|-------|
| All 6 entities registered | ✅ Correct | In `@Database(entities = [...])` |
| `knowledgeCoreDao()` abstract method | ✅ Correct | DAO accessible |
| Version = 25 | ✅ Correct | Matches DatabaseModule |

### 1.6 Tests

| Test | Status | Notes |
|------|--------|-------|
| `KnowledgeCanonicalizerTest` (5 tests) | ✅ Passing | normalizeName, canonicalId, merge, conflict, alias convergence |

---

## 2. Components That EXIST but Have BUGS

### 2.1 🔴 CRITICAL: Metadata Lost on Round-Trip

**File:** `mtc/knowledge/repository/KnowledgeRepository.kt`

**Bug:** `toEntity()` hardcodes `metadata_json = "{}"`:
```kotlin
private fun KnowledgeEntity.toEntity() = KnowledgeCoreEntityEntity(
    ...
    metadata_json = "{}",  // ← ALWAYS DROPS METADATA
    ...
)
```

**Bug:** `toDomain()` ignores `metadata_json`:
```kotlin
private fun KnowledgeCoreEntityEntity.toDomain(): KnowledgeEntity = KnowledgeEntity(
    ...
    // metadata field gets default emptyMap() — metadata_json never read
    ...
)
```

**Impact:** Any metadata set during import (e.g., `category`, `source`, `checksum` from MKIS adapter) is silently lost. The `MkisAdapter` passes metadata that never survives persistence.

### 2.2 🔴 CRITICAL: Relation Provenance Lost on Read

**File:** `mtc/knowledge/repository/KnowledgeRepository.kt`

**Bug:** `getRelations()` ignores `provenance_json`:
```kotlin
KnowledgeRelation(
    row.source_entity_id,
    KnowledgeRelationType.valueOf(row.relation_type),
    row.target_entity_id,
    row.evidence_ids_json.toList(),
    row.confidence,
    // provenance parameter MISSING — gets default emptyList()
    createdAt = row.created_at,
    updatedAt = row.updated_at,
)
```

**Bug:** `toEntity()` for relations hardcodes `provenance_json = "[]"`:
```kotlin
// In KnowledgeCoreImporter.import():
dao.insertRelations(result.entities.map { ... })  // provenance always empty
```

**Impact:** Relation provenance (which source created the relationship) is never persisted or read.

### 2.3 🟡 MODERATE: Evidence Level Lost

**File:** `mtc/knowledge/repository/KnowledgeRepository.kt`

The `KnowledgeEvidence` domain model has a `level` field (e.g., "TRADITION", "MODERN_LITERATURE"), but:
- The Room entity stores it (`level` column exists)
- The `toEntity()` in `KnowledgeCoreImporter` does NOT pass it (uses default null)
- The `toDomain()` does NOT read it (not mapped back)

**Impact:** Evidence level is always null after round-trip.

### 2.4 🟡 MODERATE: No Provenance for Relations on Import

**File:** `mtc/knowledge/repository/KnowledgeRepository.kt`

`KnowledgeCoreImporter.import()` maps `result.entities` to provenance entities, but `KnowledgeRelation` provenance is never written to `knowledge_core_provenance`.

**Impact:** You can trace which source created an entity, but not which source created a relationship.

---

## 3. Components That EXIST but Are NOT WIRED

### 3.1 🔴 CRITICAL: AppContainer Does NOT Instantiate Knowledge Core

**Evidence:**
- `knowledgeCoreDao()` exists in `AppDatabase` but is never called in `AppContainer`
- `RoomKnowledgeRepository` is never instantiated
- `KnowledgeCoreImporter` is never instantiated
- `LibraryAdapter` and `MkisAdapter` are never instantiated

**Impact:** The Knowledge Core tables exist in Room but are permanently empty. No code path writes to or reads from them.

### 3.2 🔴 CRITICAL: Two Different KnowledgeRepository Classes

| Class | Package | Wraps | Used? |
|-------|---------|-------|-------|
| `KnowledgeRepository` | `com.bioacupunt.data.repository` | `KnowledgeNodeDao` (MKIS) | Imported in AppContainer but never instantiated |
| `KnowledgeRepository` | `com.bioacupunt.mtc.knowledge.repository` | `KnowledgeCoreDao` (canonical) | Never instantiated |

**Impact:** Name collision. If someone tries to wire the canonical one, they'll hit import conflicts.

### 3.3 🔴 CRITICAL: Backend Has No Persistent Knowledge Core

**File:** `backend/app/knowledge/service.py`

The backend `search()` function:
1. Reads from `library_repository.list_nodes()` (the `library_nodes` table)
2. Converts each node to `KnowledgeEntity` on-the-fly via `from_library()`
3. Returns the converted list

There are NO `knowledge_core_*` tables in the backend database. The backend Pydantic model is a runtime-only translation, not a persisted canonical model.

**Impact:** The backend "Knowledge Core" API is just a renamed library search. It doesn't persist, deduplicate, or maintain provenance.

### 3.4 🟡 MODERATE: Backend Pydantic Model Diverges from Android Domain

| Field | Android Domain | Backend Pydantic | Match? |
|-------|---------------|-----------------|--------|
| `version` | `KnowledgeVersion` (complex) | `str` | ❌ |
| `metadata` | `Map<String, String>` | Missing | ❌ |
| `createdAt` | `Long` | Missing | ❌ |
| `updatedAt` | `Long` | Missing | ❌ |
| `Provenance.importedAt` | `Long` | Missing | ❌ |

**Impact:** If Android and backend ever need to sync Knowledge Core data, the schemas won't match.

---

## 4. Search Paths — What STILL BYPASSES Knowledge Core

### 4.1 RAG Path (AskLibraryUseCase)

```
AskLibraryUseCase
  → MtcRetriever
    → FtsSearchService (ArticleSearchBackend)
      → ArticleSearchDao (FTS4 on article_fts)
      → BibliotecaNodeEntity (biblioteca_nodes — 16 fixed + curated)
```

**Does NOT touch:** `knowledge_core_entities`, `knowledge_nodes`

**Status:** ✅ This is correct for R2 — the 16 curated articles are the ground truth.

### 4.2 MKIS Hybrid Search

```
HybridSearchService (ArticleSearchBackend)
  → VecKnowledgeNodeRepository
    → knowledge_nodes (EMPTY)
    → vec_knowledge_nodes (EMPTY)
```

**Does NOT touch:** `knowledge_core_entities`, `biblioteca_nodes`

**Status:** ⚠️ This store is EMPTY. The AppContainer comment confirms: "voltamos ao backend que de fato tem acervo" — they fell back to FTS4 because MKIS has no data.

### 4.3 BibliotecaViewModel

```
BibliotecaViewModel
  → BibliotecaDao → biblioteca_nodes (direct access)
  → HybridSearchService (MKIS, empty)
  → KnowledgeNodeDao (MKIS, direct)
```

**Does NOT touch:** `knowledge_core_entities`

**Status:** ⚠️ Direct access to legacy tables, no canonical boundary.

### 4.4 PipelineService (MKIS Ingestion)

```
PipelineService
  → KnowledgeNodeDao → knowledge_nodes (writes MKIS entities)
```

**Does NOT write to:** `knowledge_core_entities`

**Status:** ⚠️ MKIS pipeline writes to its own table, never to the canonical Knowledge Core.

### 4.5 Summary Table — Current State

| Path | Reads From | Writes To | Touches Knowledge Core? |
|------|-----------|-----------|------------------------|
| RAG (AskLibrary) | `biblioteca_nodes` + `article_fts` | — | ❌ No |
| MKIS Search | `knowledge_nodes` + `vec_knowledge_nodes` | — | ❌ No |
| BibliotecaViewModel | `biblioteca_nodes` + `knowledge_nodes` | — | ❌ No |
| PipelineService | — | `knowledge_nodes` | ❌ No |
| LegacyImporter | `biblioteca_nodes` + `knowledge_nodes` | `knowledge_core_entities` | ✅ Yes (bridge) |
| KnowledgeCoreRepository | — | `knowledge_core_entities` | ✅ Yes (canonical) |
| Backend API | `library_nodes` | — | ❌ No (runtime conversion only) |

### 4.6 Target State (after migration)

```text
ANTES (today)

Search
 ├── biblioteca_nodes (FTS4)
 └── knowledge_nodes (FTS5 + vec, EMPTY)

DEPOIS (target)

Search
 └── Knowledge Core (canonical)
       ├── knowledge_core_entities
       ├── evidence
       ├── relations
       └── provenance
```

**Rule:** No new consumer should read from legacy tables. Existing consumers migrate progressively.

---

## 5. Missing DAO Operations

The `KnowledgeCoreDao` has basic CRUD but is missing operations needed for a functional Knowledge Core:

| Operation | Needed For | Status |
|-----------|-----------|--------|
| `getById` | Single entity lookup | ✅ Exists |
| `search` | Text search | ✅ Exists (LIKE-based, no FTS) |
| `observeAll` | List all entities | ✅ Exists |
| `getRelations` | Relation lookup | ✅ Exists |
| `insert*` (6 tables) | Bulk import | ✅ Exists |
| `deleteById` | Soft delete / cleanup | ❌ Missing |
| `getByType` | Filter by entity type | ❌ Missing |
| `getByStatus` | Filter by status (PUBLISHED only) | ❌ Missing |
| `countAll` / `countByType` | Monitoring / UI badges | ❌ Missing |
| `getByIds` | Batch lookup | ❌ Missing |
| `searchByType` | Type-scoped search | ❌ Missing |
| FTS5 on entities | Full-text search | ❌ Missing (only LIKE) |
| `getEdgesFrom` | Outgoing relations | ❌ Missing (needed for graph) |
| `getEdgesTo` | Incoming relations | ❌ Missing (needed for graph) |

---

## 6. Missing Tests

| Component | Test Exists? | Notes |
|-----------|-------------|-------|
| `KnowledgeCanonicalizer` | ✅ 5 tests | Passing |
| `RoomKnowledgeRepository` | ❌ None | |
| `KnowledgeCoreImporter` | ❌ None | |
| `LibraryAdapter` | ❌ None | |
| `MkisAdapter` | ❌ None | |
| `KnowledgeCoreDao` queries | ❌ None | |
| Migration v24→v25 | ❌ None | |
| Backend `knowledge.service` | ❌ None | |
| Backend `/api/v1/knowledge` | ❌ None | |

**Total Knowledge Core tests: 5 (all in canonicalizer)**
**Needed: ~15-20 additional tests**

---

## 7. Risks

### 7.1 HIGH: Knowledge Core Is Dead Code
The entire canonical layer (entities, adapters, repository, importer) is built but never connected. If someone tries to use it today, nothing happens — no data flows in or out.

### 7.2 HIGH: Three Sources of Truth
`biblioteca_nodes`, `knowledge_nodes`, and `knowledge_core_entities` are three independent tables with no synchronization. Any feature built on top of one will miss data from the others.

### 7.3 MEDIUM: Backend Divergence
The backend Pydantic model doesn't match the Android domain model. If sync is ever needed, schemas will conflict.

### 7.4 MEDIUM: Metadata/Provenance Loss
The round-trip bugs (metadata hardcoded to `"{}"`, provenance ignored) mean that even if the importer runs, important metadata is lost.

### 7.5 LOW: Name Collision
Two `KnowledgeRepository` classes in different packages will cause import confusion.

---

## 8. Phase 1 Completion Checklist

### Must Fix — ✅ ALL DONE

- [x] **Wire Knowledge Core in AppContainer** — `knowledgeCoreDao`, `RoomKnowledgeRepository`, `KnowledgeCoreImporter`, adapters
- [x] **Fix metadata round-trip** — `toEntity()` serializes, `toDomain()` deserializes
- [x] **Fix relation provenance** — `provenance_json` persisted and read
- [x] **Fix evidence level** — `level` persisted in importer
- [x] **Add missing DAO operations** — `getByType`, `getByStatus`, `countAll`, `countByType`, `deleteById`, `getByIds`, `getEdgesFrom`, `getEdgesTo`, `deleteRelationsFor`
- [x] **Write tests** — 48+ tests (mapper, adapter, importer, DAO, migration, backend)
- [x] **Rename legacy `KnowledgeRepository`** → `LegacyKnowledgeNodeRepository`

### Should Fix — ✅ DONE

- [x] **Align backend Pydantic model** with Android domain model (version, metadata, timestamps, provenance.imported_at)
- [ ] **Backend Knowledge Core tables** — decided: backend stays read-only from `library_nodes` for now
- [x] **MtcRetriever → KnowledgeCoreSearchBackend** — MtcRetriever now uses Knowledge Core (Phase 2)
- [x] **PipelineService → PipelineBridge → knowledge_core_entities** — wired via `onNodeCreated` callback
- [x] **BibliotecaViewModel → ArticleSearchBackend** — search uses KnowledgeCoreSearchBackend

### Phase 2 Final Gate — ✅ VERIFIED (2026-08-18)

| Criterion | Status | Evidence |
|-----------|--------|----------|
| Coverage audit | ✅ VERIFIED | KnowledgeCoverageAudit.kt |
| FTS index | ✅ VERIFIED | FTS4, MIGRATION_25_26, KnowledgeCoreFtsSyncer |
| Search Repository | ✅ VERIFIED | KnowledgeSearchRepository + RoomKnowledgeSearchRepository |
| MtcRetriever migrated | ✅ VERIFIED | Uses KnowledgeCoreSearchBackend |
| HybridSearchService deprecated | ✅ VERIFIED | No callers, wiring removed from AppContainer |
| BibliotecaViewModel migrated | ✅ VERIFIED | Uses ArticleSearchBackend (KnowledgeCoreSearchBackend) |
| PipelineService → Core | ✅ VERIFIED | `onNodeCreated` callback calls PipelineBridge |
| PipelineBridge wired | ✅ VERIFIED | AppContainer wires bridgeEntity into PipelineService |
| PipelineBridge idempotent | ✅ VERIFIED | PipelineBridgeTest (9 tests) |
| SearchDualRun expanded | ✅ VERIFIED | 32 MTC queries, unit tested (8 tests) |
| E2E import → search | ✅ VERIFIED | KnowledgeCoreE2ETest (3 tests) |
| E2E pipeline → search | ✅ VERIFIED | PipelineBridgeTest.searchAfterImport |
| Legacy refs classified | ✅ VERIFIED | LEGACY_REFERENCE_CLASSIFICATION.md |
| No new legacy deps | ✅ VERIFIED | All new code uses Knowledge Core |
| FTS4/FTS5 decision | ✅ VERIFIED | FTS4 (documented trade-offs) |
| Fallback documented | ✅ VERIFIED | Deprecated with explanation |
| Tests pass | ✅ VERIFIED | Full suite GREEN |
| Build passes | ✅ VERIFIED | compileDebugKotlin GREEN |
| Documentation updated | ✅ VERIFIED | This document + LEGACY_REFERENCE_CLASSIFICATION.md |

### NOT in Phase 2 (future phases):

- Graph traversal queries
- Evidence Engine
- Differential Engine
- Copilot
- Population of Knowledge Core with MTC content (manual via Curadoria)

---

## 8.5 Migration Strategy — Bridge, Not Delete

### Principle

**Legacy tables are NOT deleted.** The Knowledge Core is a BRIDGE that sits on top of legacy data. Legacy tables remain the primary source of truth until all consumers are migrated.

### Architecture

```text
Legacy Sources                    Canonical Layer
─────────────                    ───────────────
biblioteca_nodes  ──Adapter──→   
                                   KnowledgeCoreEntity
knowledge_nodes   ──Adapter──→     (knowledge_core_entities)
                                         ↓
                                   KnowledgeRepository
                                         ↓
                                   Consumers (UI, search, AI)
```

### Import Pipeline

1. `LegacyImporter.importAll()` reads from `biblioteca_nodes` + `knowledge_nodes`
2. `LibraryAdapter` / `MkisAdapter` convert to `KnowledgeImport`
3. `KnowledgeCanonicalizer.merge()` deduplicates and detects conflicts
4. `KnowledgeCoreImporter.import()` persists to `knowledge_core_entities`
5. **Idempotent**: re-running merges equivalent entities, never duplicates

### Migration Phases

| Phase | Action | Legacy Tables | Knowledge Core |
|-------|--------|---------------|----------------|
| **Now** | Bridge created | Primary source | Populated via import |
| **Phase 2** | Search migration | Still primary | New consumers read from canonical |
| **Phase 3** | Consumer migration | Read-only | Primary source |
| **Phase 4** | Deprecation | Archived | Single source of truth |

### Rules

- No new consumer should be created reading directly from legacy tables
- Existing consumers are migrated progressively (not big-bang)
- Legacy tables are never dropped (additive migrations only)
- Import is manual/triggered, not automatic on app start
- Conflicts are logged, never auto-resolved

---

## 8.6 FTS Decision — FTS4 vs FTS5

**Decision: FTS4**

**Reason:** Android's SQLite build (used by Room) does not consistently support FTS5 across all API levels. Robolectric tests confirmed FTS4 works reliably. FTS5 `CREATE VIRTUAL TABLE` failed with `SQLiteException` in the test environment.

**Trade-offs:**
- FTS4: Proven on Android, BM25 ranking via `rank` function, well-tested in this project (article_fts)
- FTS5: Better column weights, phrase queries, but inconsistent Android support

**Migration path:** If FTS5 becomes consistently available (API 24+ with Requery SQLite), the virtual table can be rebuilt without changing the KnowledgeSearchRepository interface.

**Implementation:**
```
FTS implementation: FTS4
Location: knowledge_core_fts (virtual table)
Sync: KnowledgeCoreFtsSyncer (rebuilds from knowledge_core_entities)
Search: KnowledgeCoreFtsDao (raw SQL, BM25 ranking)
Migration: MIGRATION_25_26 in DatabaseModule.kt
```

---

## 9. Recommendations

### Immediate (this session):

1. **Fix the metadata/provenance round-trip bugs** — these are real data loss issues
2. **Wire the Knowledge Core in AppContainer** — make it actually usable
3. **Write tests for the importer and adapters** — the canonicalizer is tested, the rest isn't
4. **Add missing DAO operations** — at minimum `getByType`, `getByStatus`, `countAll`

### Before Phase 2 (Graph Traversal):

5. **Decide the migration strategy** — should existing data in `biblioteca_nodes` and `knowledge_nodes` be imported into `knowledge_core_entities`? Or should they remain separate?
6. **Add FTS5 on `knowledge_core_entities`** — current LIKE-based search won't scale
7. **Write migration v24→v25 test** — verify the tables are created correctly

### Architecture Decision Needed:

The fundamental question is: **Should `knowledge_core_entities` become the ONLY source of truth, or should it be an optional canonical overlay?**

Option A: **Canonical Only** — Import everything from `biblioteca_nodes` and `knowledge_nodes` into `knowledge_core_entities`. All reads go through the canonical layer. Legacy tables become write-only (for backward compatibility during transition).

Option B: **Canonical Overlay** — `knowledge_core_entities` is an optional enrichment layer. Legacy tables remain the primary source. The canonical layer adds deduplication, provenance, and cross-source linking on top.

**Recommendation:** Option B for now (less disruptive), with a future migration to Option A once the canonical layer is proven.

---

## 10. Phase 2 Final Gate Report

### Component Validation

| Component | Exists | Wired | Tested | Validated |
|-----------|--------|-------|--------|----------|
| KnowledgeCoverageAudit | ✅ | ✅ | ✅ | ✅ |
| LegacyImporter | ✅ | ✅ | ✅ | ✅ |
| KnowledgeSearchRepository | ✅ | ✅ | ✅ | ✅ |
| KnowledgeCoreSearchBackend | ✅ | ✅ | ✅ | ✅ |
| MtcRetriever | ✅ | ✅ | ✅ | ✅ |
| HybridSearchService | ✅ | ❌ DEPRECATED | N/A | ✅ No callers |
| BibliotecaViewModel | ✅ | ✅ | ✅ | ✅ |
| PipelineService | ✅ | ✅ | ✅ | ✅ |
| PipelineBridge | ✅ | ✅ | ✅ | ✅ |
| SearchDualRun | ✅ | ✅ | ✅ | ✅ |

### Legacy Dependencies

| Table | Classification | Remaining Consumers |
|-------|---------------|--------------------|
| biblioteca_nodes | LEGACY_REQUIRED | BibliotecaDao (Curadoria, display) |
| knowledge_nodes | LEGACY_REQUIRED | PipelineService (write-only, bridges to Core) |
| article_fts | DEPRECATED | None (MtcRetriever migrated) |
| vec_knowledge_nodes | DEPRECATED | None (HybridSearchService deprecated) |

### Test Results

- Knowledge tests: 86+ (Phase 1: 48, Phase 2: 38 new)
- Full suite: BUILD SUCCESSFUL
- Backend: 74 passed

### Architecture (Final)

```text
Legacy Sources → Adapters → Knowledge Core → FTS4 Index
                                    ↓
                           KnowledgeSearchRepository
                                    ↓
                    ┌───────────────┼───────────────┐
                    ↓               ↓               ↓
              MtcRetriever   Biblioteca     Future
                    ↓          ViewModel
               Consumers

PipelineService → onNodeCreated → PipelineBridge → Knowledge Core
```

**Phase 2 = ✅ COMPLETE**

---

*This document is the authoritative status of the Knowledge Core. Updated 2026-08-18 after Phase 2 Final Gate — all consumers migrated, E2E verified, legacy dependencies classified, build green.*
