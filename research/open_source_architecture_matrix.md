# Open-Source Architecture Matrix

**Created:** 2026-08-18
**Purpose:** Feature comparison across reference projects for BioAcupunt Clinical Intelligence 2.0

---

## Feature Matrix

| Project | Graph | RAG | Vector | BM25 | Evidence | FHIR | Android | Offline | MTC | Vision | Voice | Relevance |
|---------|-------|-----|--------|------|----------|------|---------|---------|-----|--------|-------|-----------|
| TCM Knowledge Graph | ✅ Neo4j | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ Full | ❌ | ❌ | HIGH |
| GraphAI-for-TCM | ✅ GNN | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ Formulas | ❌ | ❌ | HIGH |
| PTM | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ Prescriptions | ❌ | ❌ | MEDIUM |
| nihaisha-nishi-tcm | ✅ KG (disabled) | ✅ | ❌ | ❌ | ✅ Evidence layer | ❌ | ❌ | ✅ Local | ✅ TCM courses | ❌ | ❌ | MEDIUM |
| MedGem | ❌ | ✅ | ✅ | ❌ | ❌ | ❌ | ✅ | ✅ | ❌ | ✅ | ✅ | HIGH |
| Medical-Graph-RAG | ✅ Neo4j | ✅ | ✅ Qdrant | ❌ | ✅ Citations | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | HIGH |
| agentic-med-diag | ❌ | ✅ | ❌ | ❌ | ✅ Verify step | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | HIGH |
| FHIRCore | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ Full | ✅ | ✅ | ❌ | ❌ | ❌ | MEDIUM |
| Beda FHIR EMR | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ Full | ❌ | ❌ | ❌ | ❌ | ❌ | MEDIUM |
| Phlox | ❌ | ✅ | ❌ | ❌ | ✅ Review step | ❌ | ❌ | ✅ Local | ❌ | ❌ | ✅ STT | MEDIUM |
| OHIF Viewers | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ DICOM | ❌ | LOW |
| 3D Anatomy Atlas | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ 3D | ❌ | LOW |
| Infherno | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | HIGH (NLP) |
| MediGRAF | ✅ Graph | ✅ | ✅ Vector | ✅ BM25 | ✅ Citations | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | HIGH |
| Phlox | ❌ | ✅ | ❌ | ❌ | ✅ | ❌ | ❌ | ✅ | ❌ | ❌ | ✅ | MEDIUM |

---

## BioAcupunt Current State vs Reference Projects

| Capability | BioAcupunt Today | Best Reference | Gap Analysis |
|-----------|-----------------|----------------|--------------|
| **Knowledge Graph** | ✅ KnowledgeCoreModels (17 types, 15 relations) | TCM Knowledge Graph (20 types, 46 relations) | BioAcupunt has solid model; TCM KG has richer ontology — study for type expansion |
| **Graph Traversal** | ❌ Not implemented | Medical-Graph-RAG, MediGRAF | **CRITICAL GAP** — Phase 3 must build this |
| **RAG** | ✅ FTS4-based (AskLibraryUseCase) | Medical-Graph-RAG (graph+vector) | BioAcupunt has basic RAG; gap is graph-enhanced retrieval |
| **Vector Search** | ❌ Not implemented | MedGem, Medical-Graph-RAG | Phase 4 gap — BM25 first, vector later |
| **BM25** | ❌ Basic LIKE search | MediGRAF | Phase 4 gap — proper BM25 scoring needed |
| **Evidence Chain** | ✅ Basic (KnowledgeEvidence model) | Medical-Graph-RAG, nihaisha | Model exists; resolution and UI traceability missing — Phase 2 |
| **FHIR** | ❌ Not implemented | FHIRCore, Beda EMR | Phase 5 gap — adapter layer only |
| **Android** | ✅ Full native app | MedGem, FHIRCore | BioAcupunt is strongest here — preserve and enhance |
| **Offline-First** | ✅ Room + sync | FHIRCore, Phlox | BioAcupunt already excellent — maintain |
| **MTC Domain** | ✅ ClinicalSafetyEngine, assessments | TCM KG, GraphAI-for-TCM | BioAcupunt has clinical domain; reference projects have richer MTC data models |
| **Vision** | ❌ Not implemented (Phase 6) | OHIF, 3D Atlas, MedGem | Future phase — Tongue Vision |
| **Voice/STT** | ❌ Not implemented (Phase 5) | MedGem, Phlox | Future phase — clinical session mode |
| **Clinical NLP** | ✅ Basic (StructureChiefComplaintUseCase) | Infherno, Phlox | BioAcupunt has extractive NLP; gap is deeper clinical NLP pipeline |
| **Agent Pipeline** | ❌ Not implemented | agentic-med-diag | Phase 3-4 gap — deterministic first, agent later |
| **Explainability** | ❌ Not implemented | GraphAI-for-TCM | Phase 3 gap — attention-based explainability concept |
| **Protocol Engine** | ❌ Not implemented | None directly | Phase 5 gap — build from scratch using knowledge graph |
| **Decision Flows** | ❌ Not implemented | None directly | Phase 7 gap — build interactive diagnostic flows |

---

## Key Architectural Insights from Reference Projects

### 1. TCM Knowledge Graph — Entity Model Richness
- **20 entity types** vs BioAcupunt's 17 — potential types to add: FLAVOR, TROPISM, TOXICITY, PROPERTY
- **46 relation types** vs BioAcupunt's 15 — richer relationship vocabulary
- **3.4M records** from 6 integrated databases — shows scale of MTC knowledge
- **Insight:** BioAcupunt's KnowledgeEntityType enum should be expanded based on this taxonomy

### 2. GraphAI-for-TCM — Explainability Pattern
- **91-dimensional herb feature encoding** — origin, property, compatibility, efficacy, dosage
- **Graph Attention Network** for formula compatibility analysis
- **5D mechanism prediction** with attention heatmaps
- **Insight:** Herb property encoding concept applicable to BioAcupunt's herb representation

### 3. Medical-Graph-RAG — Hybrid Retrieval
- **Graph context + vector search + BM25** — three retrieval signals combined
- **Evidence citation chain** — claim → evidence → source → citation
- **Context assembly** — graph neighbors enrich FTS results
- **Insight:** BioAcupunt should implement hybrid retrieval in Phase 4

### 4. agentic-med-diag — Pipeline Pattern
- **PLAN → RESEARCH → RETRIEVE → REASON → VERIFY → ANSWER** — deterministic pipeline
- **VERIFY step** catches hallucination before output
- **Insight:** Adapt as Clinical Intelligence Orchestrator; NEVER as autonomous diagnosis

### 5. MedGem — On-Device Architecture
- **Local inference** with model download — similar to BioAcupunt's LocalLlmProvider
- **STT → NLP → SOAP** pipeline for clinical notes
- **Insight:** Compare on-device patterns; BioAcupunt already has WorkManager download

### 6. nihaisha-nishi-tcm — Evidence Traceability
- **2986 screenshot evidence items** with structured indexing
- **PDF evidence layer** with page-level citation
- **Safety boundaries** explicitly defined
- **Insight:** Evidence traceability pattern directly applicable to Phase 2

---

## Technology Stack Comparison

| Project | Backend | Database | AI Framework | Mobile | Deploy |
|---------|---------|----------|--------------|--------|--------|
| BioAcupunt | FastAPI | Room + PostgreSQL | Local LLM (Phi-4) | Kotlin/Compose | Vercel + Neon |
| TCM Knowledge Graph | Python scripts | Neo4j | None (data only) | None | Local |
| GraphAI-for-TCM | Python | In-memory | PyTorch + GNN | None | Local |
| MedGem | None | SQLite | MediaPipe + ONNX | Kotlin/Compose | Android |
| Medical-Graph-RAG | FastAPI | Neo4j + Qdrant | LangChain | None | Docker |
| agentic-med-diag | Python | None | LangChain + OpenAI | None | Local |
| FHIRCore | Android | Room | None | Kotlin | Android |
| Phlox | Next.js | PostgreSQL | Whisper + LLMs | React (web) | Vercel |

---

*This matrix is the definitive feature comparison. Use it to identify gaps and plan Phase 3-4 implementation priorities.*
