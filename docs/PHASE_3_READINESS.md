# Phase 3 Readiness — Clinical Intelligence

**Date:** 2026-08-18
**Status:** ✅ READY TO BEGIN (with noted gaps)
**Scope:** Assessment of current state vs Phase 3 requirements

---

## Executive Summary

BioAcupunt has completed Phase 0 (Audit), Phase 1 (Knowledge Core), and Phase 2 (Canonical Search). The Knowledge Core is wired, tested (86+ tests), and serving search queries. Phase 3 (Clinical Intelligence) can begin, but three foundational gaps must be addressed first.

---

## 1. Current Architecture

### What Exists and Works

| Component | Status | Evidence |
|-----------|--------|----------|
| KnowledgeCoreModels | ✅ 17 entity types, 15 relation types, evidence, source, citation, provenance | `mtc/knowledge/domain/KnowledgeCoreModels.kt` |
| KnowledgeCoreDao | ✅ CRUD + search + FTS4 | Room v26, `MIGRATION_25_26` |
| KnowledgeRepository | ✅ Read interface + canonical search | `mtc/knowledge/repository/` |
| KnowledgeSearchRepository | ✅ FTS4-based, BM25 scoring | `RoomKnowledgeSearchRepository` |
| MtcRetriever | ✅ FTS4 + section extraction, migrated to Knowledge Core | Phase 2 Final Gate verified |
| PipelineBridge | ✅ Legacy→Core import, idempotent | `PipelineBridgeTest` (9 tests) |
| ClinicalSafetyEngine | ✅ 18/18 flags, Kotlin pure, no LLM | R1 intact |
| ClinicalSynthesisUseCase | ✅ Structured output, RAG-gated | 6 tests passing |
| AskLibraryUseCase | ✅ RAG with evidence gate | R2 intact |
| LocalLlmProvider | ✅ Phi-4 Mini Instruct, 4096 context | On-device only |

### What Exists but Is Incomplete

| Component | Status | Gap |
|-----------|--------|-----|
| KnowledgeGraphRepository | ❌ Interface designed in architecture doc, NOT implemented | **CRITICAL** — Phase 3A first task |
| EvidenceResolver | ❌ Designed, NOT implemented | Phase 3B dependency |
| PatternScoringEngine | ❌ Designed, NOT implemented | Phase 3B core |
| ClinicalObservations model | ❌ Designed, NOT implemented | Phase 3B input model |
| HybridRetriever | ❌ Designed, NOT implemented | Phase 4 |

---

## 2. Current Knowledge Core

### Domain Model
- **17 entity types:** PATTERN, SYNDROME, SYMPTOM, TONGUE, PULSE, ACUPOINT, MERIDIAN, HERB, FORMULA, DISEASE, PROTOCOL, DOCUMENT, EVIDENCE, PRINCIPLE, TECHNIQUE, PROPERTY, CONSTITUTION
- **15 relation types:** SUGGESTS, ASSOCIATED_WITH, TREATED_BY, BELONGS_TO, CONTAINS, CONTRAINDICATED_BY, RELATED_TO, SUPPORTED_BY, DERIVED_FROM, PART_OF, HAS_SYMPTOM, HAS_PATTERN, HAS_POINT, HAS_FORMULA, HAS_EVIDENCE
- **6 tables:** knowledge_core_entities, knowledge_core_relations, knowledge_core_sources, knowledge_core_citations, knowledge_core_evidence, knowledge_core_provenance
- **FTS4 index:** knowledge_core_fts (virtual table, BM25 ranking)

### What's Missing for Phase 3
- **Graph traversal queries** — no BFS/DFS over relations
- **Path finding** — no shortest path between entities
- **Neighbor queries** — no efficient "get all related entities" beyond depth 1
- **Community detection** — no clustering of related entities
- **Missing entity types** for richer MTC ontology (FLAVOR, TROPISM, TOXICITY, PROPERTY — from TCM Knowledge Graph reference)

---

## 3. Current Search

### What Works
- FTS4 with BM25 ranking on `knowledge_core_fts`
- `KnowledgeSearchRepository` with type-scoped search
- `MtcRetriever` with section extraction for RAG
- `SearchDualRun` tool for comparing legacy vs canonical results

### What's Missing for Phase 3
- **Hybrid retrieval** — no graph-enhanced search
- **Entity recognition in queries** — no "LI4 → ACUPOINT" mapping
- **Reranking** — no evidence-based reranking of results
- **Graph context enrichment** — search results don't include graph neighbors

---

## 4. Current Clinical Domain

### What Works
- `MtcAssessment` with Ba Gang, Zang-Fu, tongue, pulse, flags
- `ClinicalSafetyEngine` with 18/18 flags
- `ClinicalSynthesisUseCase` with structured output
- `PharmaSafetyEngine` for drug safety
- Override audit with justification

### What's Missing for Phase 3
- **ClinicalObservations model** — unified observation input for scoring
- **Pattern scoring** — no deterministic pattern ranking
- **Differential engine** — no candidate ranking with evidence
- **Missing data identification** — no "what else should I check?" engine
- **Explainability** — no "why this pattern over that?" traceability

---

## 5. Current AI

### What Works
- `AiRepository` with capability-based routing
- `LocalLlmProvider` with Phi-4 Mini (4096 context)
- `ClinicalSynthesisUseCase` — structured diagnosis suggestions
- `StructureChiefComplaintUseCase` — extractive NLP
- `AskLibraryUseCase` — RAG with evidence gate

### What's Missing for Phase 3
- **Multi-step planning** — no plan→retrieve→reason→verify pipeline
- **Evidence resolution** — AI outputs don't link to knowledge graph
- **Verification step** — no post-generation fact-checking
- **Patient-aware context** — chat doesn't use current assessment data

---

## 6. Reference Repositories

### Cloned and Analyzed
- **14 of 15** repositories cloned (zhongyi-graph not found)
- **7 MIT**, **2 Apache 2.0**, **1 GPL v3**, **5 NO LICENSE**
- **Key patterns identified:** graph traversal, evidence traceability, hybrid retrieval, agent pipeline, clinical NLP

### Top Insights for Phase 3
1. **TCM Knowledge Graph** → expand entity types (FLAVOR, TROPISM, TOXICITY, PROPERTY)
2. **Medical-Graph-RAG** → hybrid retrieval pattern (graph + FTS + evidence)
3. **agentic-med-diag** → pipeline stages (PLAN→RESEARCH→RETRIEVE→REASON→VERIFY→ANSWER)
4. **nihaisha-nishi-tcm** → evidence traceability pattern (page-level citations)
5. **GraphAI-for-TCM** → herb property encoding concept

---

## 7. Roadmap Conflicts Resolved

### Old Conflicting Roadmaps
- Phase 1 = Knowledge Core, Phase 2 = MTC Core, Phase 3 = Search, Phase 4 = Diagnostic
- Phase 1 = Knowledge Core Stabilization, Phase 2 = Canonical Search Migration, Phase 3 = Clinical Intelligence

### Official Roadmap (Canonical)
```
Phase 0 — Audit & Foundation
Phase 1 — Knowledge Core ✅
Phase 2 — Canonical Search ✅
Phase 3 — Clinical Intelligence ← WE ARE HERE
Phase 4 — RAG / GraphRAG / Copilot
Phase 5 — Clinic Intelligence Platform
Phase 6 — Multimodal / Knowledge Operations
```

---

## 8. Phase 3 Gaps (Ordered by Dependency)

| # | Gap | Priority | Estimated Effort |
|---|-----|----------|-----------------|
| 1 | KnowledgeGraphRepository (BFS/DFS traversal) | CRITICAL | 2-3 days |
| 2 | Graph DAO queries (getEdgesFrom, getEdgesTo, getEntitiesByType) | CRITICAL | 1 day |
| 3 | Graph tests (FakeDao with pre-populated graph) | CRITICAL | 1 day |
| 4 | ClinicalObservations model | HIGH | 0.5 day |
| 5 | EvidenceResolver (resolve evidence IDs → full traces) | HIGH | 1-2 days |
| 6 | PatternScoringEngine (deterministic pattern ranking) | HIGH | 2-3 days |
| 7 | MissingDataEngine (identify what data is missing) | HIGH | 1 day |
| 8 | Integration with ClinicalSynthesisUseCase | HIGH | 1 day |
| 9 | Graph search integration (enrich FTS with graph neighbors) | MEDIUM | 1-2 days |
| 10 | Clinical Intelligence Orchestrator (pipeline stages) | MEDIUM | 2-3 days |
| 11 | E2E tests | MEDIUM | 1-2 days |
| 12 | Documentation | LOW | 0.5 day |

**Total estimated effort:** 14-20 days

---

## 9. Implementation Plan (Phase 3A)

### Sprint 1: Knowledge Graph Engine
1. Add DAO queries (getEdgesFrom, getEdgesTo, getEntitiesByType, getEdgesBetween)
2. Implement KnowledgeGraphRepository interface
3. Implement RoomKnowledgeGraphRepository (BFS traversal)
4. Write graph tests (FakeDao with pattern→point chains)
5. No UI changes yet

### Sprint 2: Evidence Traceability
1. Build EvidenceResolver
2. Integrate with ClinicalSynthesisUseCase
3. Add citation resolution to AskLibraryUseCase
4. Write evidence tests

### Sprint 3: Differential Engine
1. Build ClinicalObservations model
2. Build PatternScoringEngine (deterministic)
3. Build MissingDataEngine
4. Integrate with ClinicalSynthesisUseCase
5. Write scoring tests (critical: LLM explanation must not contradict engine)

---

## 10. Risk Register

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| Graph traversal too slow on Room | Medium | Low | BFS in Kotlin over ~10K entities is fast enough; no recursive CTE needed |
| Pattern scoring weights incorrect | High | Medium | Start with simple matched/total ratio; iterate with doctor feedback |
| LLM contradicts engine scoring | High | Medium | Test that engine output is passed as context, not generated |
| Evidence resolution returns null | Low | High | Graceful degradation — "evidence not found" is valid |
| Phase 3 scope creep into Phase 4 | Medium | High | Strict phase boundaries; no hybrid retrieval in Phase 3 |

---

## 11. Success Criteria

| Scenario | Requires | Status |
|----------|----------|--------|
| Search "insônia" → symptoms, patterns, points, formulas, references | Phase 3A + 4 | ❌ Not yet |
| Open patient → graph traversal shows related patterns | Phase 3A | ❌ Not yet |
| "Por que padrão A está acima de B?" → evidence chain | Phase 3A + 3B | ❌ Not yet |
| "O que falta para diferenciar A de B?" → MissingData | Phase 3B | ❌ Not yet |
| Clinical synthesis includes scored differential | Phase 3B | ❌ Not yet |

---

## 12. Blockers

| Blocker | Status | Resolution |
|---------|--------|------------|
| zhongyi-graph repository not found | ✅ Resolved | No alternative needed |
| MediGRAF original not found | ✅ Resolved | sthio90/medical-ehr-graphrag used instead |
| No clinical MTC data in Knowledge Core yet | ⚠️ Known | R4: content comes from human curation only |
| Phase 3 not tested in device | ⚠️ Known | Same limitation as all Compose features |

---

## 13. Decision: PHASE 3 READY?

**YES** — with the following conditions:

1. Graph traversal (KnowledgeGraphRepository) must be built first
2. Pattern scoring starts simple, iterates with doctor feedback
3. No hybrid retrieval (Phase 4 scope)
4. No copilot UI (Phase 4 scope)
5. R1/R2/R4 remain intact throughout

---

*This document is the authoritative readiness assessment. Updated 2026-08-18.*
