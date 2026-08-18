# BioAcupunt — Official Roadmap

**Last Updated:** 2026-08-18
**Status:** Canonical — replaces all previous conflicting roadmaps

---

## Phase 0 — Audit & Foundation ✅

**Status:** HISTORICALLY COMPLETE

- Repository audit
- Architecture documentation
- Risk identification
- Module mapping
- Anti-pattern catalog
- CLAUDE.md established

---

## Phase 1 — Knowledge Core ✅

**Status:** COMPLETE

- KnowledgeEntity, KnowledgeRelation, Source, Citation, Evidence, Provenance, Version
- Room entities (6 tables, v25)
- KnowledgeRepository interface
- KnowledgeCoreImporter with adapters (Library, MKIS)
- Canonicalizer (normalizeName, canonicalId, merge)
- AppContainer wiring
- 48+ tests (mapper, adapter, importer, DAO, migration, backend)
- Legacy KnowledgeRepository renamed to avoid collision

---

## Phase 2 — Canonical Search ✅

**Status:** COMPLETE

- Coverage Audit (KnowledgeCoverageAudit)
- FTS4 index (knowledge_core_fts, MIGRATION_25_26)
- KnowledgeSearchRepository + RoomKnowledgeSearchRepository
- MtcRetriever migrated to Knowledge Core
- HybridSearchService deprecated (no callers)
- BibliotecaViewModel migrated to ArticleSearchBackend
- PipelineService → PipelineBridge → knowledge_core_entities
- SearchDualRun expanded (32 MTC queries)
- E2E tests (import → search, pipeline → search)
- Legacy dependencies classified
- 86+ tests passing
- Documentation updated

---

## Phase 3 — Clinical Intelligence

**Status:** IN PROGRESS

### 3A — Knowledge Graph Engine
- [ ] Graph DAO queries (getEdgesFrom, getEdgesTo, getEntitiesByType)
- [ ] KnowledgeGraphRepository interface
- [ ] RoomKnowledgeGraphRepository (BFS traversal)
- [ ] Graph tests (FakeDao with pre-populated graph)
- [ ] Graph search integration (enrich FTS with graph neighbors)

### 3B — Evidence & Differential
- [ ] EvidenceResolver (evidence IDs → full traces)
- [ ] ClinicalObservations model
- [ ] PatternScoringEngine (deterministic)
- [ ] MissingDataEngine
- [ ] Integration with ClinicalSynthesisUseCase
- [ ] Clinical Intelligence Orchestrator (pipeline stages)
- [ ] E2E tests

---

## Phase 4 — RAG / GraphRAG / Copilot

**Status:** FUTURE

- BM25 scoring (Bm25Scorer)
- Entity recognition in queries (EntityRecognizer)
- Hybrid retrieval (HybridRetriever)
- Reranker (evidence-based)
- CopilotUseCase (contextual, patient-aware)
- Copilot UI in InteligenciaScreen

---

## Phase 5 — Clinic Intelligence Platform

**Status:** FUTURE

- Modo Atendimento (clinical session mode)
- Clinical NLP (text → structured observation)
- STT integration
- FHIR adapter layer
- Questionnaire pattern for configurable anamnesis
- Timeline longitudinal
- Session comparison

---

## Phase 6 — Multimodal / Knowledge Operations

**Status:** FUTURE

- Tongue Vision (image analysis)
- Pulse analysis (sensor integration)
- 3D Anatomy (acupoint visualization)
- DICOM viewer (OHIF-based)
- Knowledge Packs (CMS)
- Versioning advanced
- MTC Radar

---

## Non-Negotiable Rules (All Phases)

1. **R1 — No LLM in clinical safety path** (ClinicalSafetyEngine stays Kotlin pure)
2. **R2 — RAG without evidence = no model call** (if (!grounding.hasEvidence) stays)
3. **R3 — Fail closed on model integrity** (SHA-256 verification stays)
4. **R4 — No AI-generated clinical content** (knowledge grows by human curation only)
5. **Offline-first** (Room is source of truth)
6. **Additive migrations only** (never drop columns/tables)
7. **All writes check Result** (no silently discarded errors)
8. **Every LazyColumn has a key** (no index collision crashes)

---

## Version History

| Date | Change |
|------|--------|
| 2026-08-18 | Official roadmap established, replaces all conflicting versions |
| 2026-07-25 | Phase 0-2 completed |
| 2026-07-27 | Phase 3 architecture designed |
| 2026-08-18 | Phase 3 readiness assessed, reference repos cataloged |

---

*This is the ONLY authoritative roadmap. All previous versions are superseded.*
