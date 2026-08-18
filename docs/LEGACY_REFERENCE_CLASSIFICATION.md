# Legacy Reference Classification

**Date:** 2026-08-18
**Scope:** All references to legacy tables in the codebase

---

## Classification Key

- **MIGRADO** — Consumer now uses Knowledge Core
- **BRIDGE TEMPORÁRIO** — Intentional bridge during migration (will be removed)
- **LEGADO AINDA NECESSÁRIO** — Legacy table still primary source (not yet migrated)

---

## MIGRADO

| Consumer | Was | Now |
|----------|-----|-----|
| `AppContainer.mtcRetriever` | `FtsSearchService` (article_fts) | `KnowledgeCoreSearchBackend` → Knowledge Core |

---

## BRIDGE TEMPORÁRIO

| Component | Purpose | Removal Trigger |
|-----------|---------|-----------------|
| `LegacyImporter` | Reads biblioteca_nodes + knowledge_nodes → Knowledge Core | After all consumers migrated |
| `KnowledgeCoverageAudit` | Compares legacy vs canonical | After migration verified |
| `KnowledgeCoreSearchBackend` | Adapts KnowledgeSearchRepository as ArticleSearchBackend | Becomes primary (not a bridge) |
| `SearchDualRun` | Compares legacy vs core results | After migration verified |

---

## LEGADO AINDA NECESSÁRIO

### biblioteca_nodes (Curated Library)

| File | Reference | Why Still Needed |
|------|-----------|-----------------|
| `BibliotecaNodeEntity.kt` | `@Entity(tableName = "biblioteca_nodes")` | Source entity for curated articles |
| `BibliotecaDao.kt` | All queries on `biblioteca_nodes` | CRUD for curated articles |
| `BibliotecaMapper.kt` | `BibliotecaNodeEntity.toDomain()` | Domain mapping |
| `BibliotecaRepositoryImpl.kt` | `BibliotecaDao` dependency | Repository for UI |
| `LibraryStagingRepository.kt` | `BibliotecaDao` + `BibliotecaNodeEntity` | Curadoria pipeline |
| `FtsSearchService.kt` | Reads from `article_fts` + `biblioteca_nodes` | Legacy search (now superseded) |
| `ArticleFtsEntity.kt` | `@Entity(tableName = "article_fts")` | Legacy FTS index |
| `ArticleSearchDao.kt` | Queries on `article_fts` | Legacy FTS queries |
| `MtcKnowledgeBase.kt` | 16 hardcoded articles | Fixed content (not in Room) |
| `BibliotecaViewModel.kt` | `BibliotecaDao` + `KnowledgeNodeDao` | UI still reads from legacy |
| `MkisDetailSheet.kt` | `KnowledgeNodeEntity` | MKIS detail display |
| `DatabaseModule.kt` | Migration SQL for `article_fts`, `biblioteca_nodes` | Schema definitions |

### knowledge_nodes (MKIS Pipeline)

| File | Reference | Why Still Needed |
|------|-----------|-----------------|
| `KnowledgeNodeEntity.kt` | `@Entity(tableName = "knowledge_nodes")` | MKIS pipeline entity |
| `KnowledgeNodeDao.kt` | All queries on `knowledge_nodes` | MKIS CRUD |
| `VecKnowledgeNodeDao.kt` | Queries on `vec_knowledge_nodes` + `knowledge_nodes` | Vector search |
| `PipelineService.kt` | `KnowledgeNodeDao` writes | MKIS ingestion |
| `EmbeddingService.kt` | Generates embeddings for `vec_knowledge_nodes` | Vector embeddings |
| `HybridSearchService.kt` | Reads from `knowledge_nodes` + `vec_knowledge_nodes` | Hybrid search (empty store) |
| `LegacyKnowledgeNodeRepository.kt` | `KnowledgeNodeDao` | Legacy repository |
| `DatabaseModule.kt` | Migration SQL for `knowledge_nodes`, `vec_knowledge_nodes` | Schema definitions |

### AppContainer (Wiring)

| File | Reference | Why Still Needed |
|------|-----------|-----------------|
| `AppContainer.knowledgeNodeDao` | `database.knowledgeNodeDao()` | MKIS pipeline dependency |
| `AppContainer.bibliotecaDao` | `database.bibliotecaDao()` | Curated library dependency |
| `AppContainer.ftsSearchService` | FTS4 search (legacy) | Superseded but not removed |
| `AppContainer.hybridSearchService` | Hybrid search (legacy) | Superseded but not removed |
| `AppContainer.pipelineService` | MKIS ingestion | Still writes to knowledge_nodes |

---

## Migration Priority

### Next (Phase 2 continued)
1. `BibliotecaViewModel` — migrate to read from Knowledge Core
2. `FtsSearchService` — deprecate (replaced by KnowledgeCoreSearchBackend)
3. `HybridSearchService` — deprecate (knowledge_nodes is empty)

### Later (Phase 3+)
4. `PipelineService` — write to Knowledge Core instead of knowledge_nodes
5. `BibliotecaDao` / `BibliotecaNodeEntity` — keep for curadoria, deprecate for reads
6. `VecKnowledgeNodeDao` / `EmbeddingService` — evaluate if vector search is needed

### Never Remove (in this project)
- `BibliotecaNodeEntity` / `BibliotecaDao` — curadoria pipeline needs them
- `KnowledgeNodeEntity` / `KnowledgeNodeDao` — MKIS pipeline needs them
- Migration SQL in `DatabaseModule` — additive migrations only
