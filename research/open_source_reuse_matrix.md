# Open-Source Reuse Matrix

**Created:** 2026-08-18
**Purpose:** What can be reused, adapted, inspired, or must be blocked from reference projects

---

## Reuse Decisions

| Component | Project | What to Learn | Can Reuse Code? | Can Adapt Concept? | License | Decision |
|-----------|---------|---------------|-----------------|-------------------|---------|----------|
| **Entity Type Taxonomy** | TCM Knowledge Graph | 20 entity types, 46 relation types | ❌ No (Python/CSV) | ✅ Yes — expand KnowledgeEntityType enum | NO LICENSE | ADAPT |
| **Herb Property Encoding** | GraphAI-for-TCM | 91-dim feature vector for herb representation | ❌ No (PyTorch) | ✅ Yes — concept for herb data model | MIT | ADAPT |
| **Graph Attention Explainability** | GraphAI-for-TCM | Attention weights for herb compatibility | ❌ No (PyTorch GNN) | ✅ Yes — explainability pattern | MIT | INSPIRE |
| **Hypergraph Concept** | GraphAI-for-TCM | Formula as hypergraph | ❌ No (research) | ⚠️ Maybe — only if justified | MIT | INSPIRE |
| **Topic Modeling** | PTM | Herb-symptom co-occurrence patterns | ❌ No (Java) | ✅ Yes — co-occurrence scoring concept | Research-only | INSPIRE |
| **Evidence Traceability** | nihaisha-nishi-tcm | Page-level evidence, screenshot indexing | ❌ No (agent skill) | ✅ Yes — evidence chain pattern | NO LICENSE | INSPIRE |
| **Safety Boundaries** | nihaisha-nishi-tcm | Explicit scope definition | ✅ Yes (pattern) | ✅ Yes — safety scope concept | NO LICENSE | ADAPT |
| **On-Device Inference** | MedGem | STT → NLP → SOAP pipeline | ⚠️ Partial (Kotlin patterns) | ✅ Yes — pipeline architecture | MIT | ADAPT |
| **Graph Retrieval** | Medical-Graph-RAG | Graph context + evidence citation | ❌ No (Python/Neo4j) | ✅ Yes — retrieval pattern | MIT | ADAPT |
| **Evidence Citation Chain** | Medical-Graph-RAG | Claim → evidence → source → citation | ❌ No (Python) | ✅ Yes — citation chain concept | MIT | ADAPT |
| **Context Assembly** | Medical-Graph-RAG | How to combine graph + text results | ❌ No (Python) | ✅ Yes — assembly pattern | MIT | ADAPT |
| **Agent Pipeline** | agentic-med-diag | PLAN→RESEARCH→RETRIEVE→REASON→VERIFY→ANSWER | ❌ No (Python/LangChain) | ✅ Yes — pipeline stages | Apache 2.0 | INSPIRE |
| **Verification Step** | agentic-med-diag | Catch hallucination before output | ❌ No (Python) | ✅ Yes — verify pattern | Apache 2.0 | ADAPT |
| **FHIR Resource Model** | FHIRCore | Patient, Encounter, Observation mapping | ⚠️ Partial (Kotlin) | ✅ Yes — resource structure | Apache 2.0 | INSPIRE |
| **Offline-First Sync** | FHIRCore | Conflict resolution, local-first | ⚠️ Partial (Kotlin) | ✅ Yes — sync comparison | Apache 2.0 | INSPIRE |
| **Questionnaire Pattern** | Beda FHIR EMR | Configurable form rendering | ❌ No (React) | ✅ Yes — form configurability concept | MIT | INSPIRE |
| **Clinical NLP Pipeline** | Phlox | STT → extraction → review | ❌ No (TypeScript) | ✅ Yes — NLP pipeline stages | MIT | INSPIRE |
| **Structured Note Template** | Phlox | Template-based clinical notes | ❌ No (TypeScript) | ✅ Yes — template pattern | MIT | INSPIRE |
| **DICOM Viewing** | OHIF Viewers | Image viewer extensibility | ❌ No (TypeScript) | ❌ No (Phase 6 only) | MIT | IGNORE |
| **3D Anatomy** | 3D Anatomy Atlas | Spatial anatomy model | ❌ No (Unity/C#) | ⚠️ Maybe (concept only) | GPL v3 | BLOCKED |
| **Clinical NLP Extraction** | Infherno | Text → structured observation | ❌ No (Python) | ✅ Yes — extraction pattern | NO LICENSE | INSPIRE |
| **Hybrid Retrieval** | MediGRAF | Graph + BM25 + vector | ❌ No (Python) | ✅ Yes — hybrid pattern | NO LICENSE | INSPIRE |
| **Evidence Grounding** | MediGRAF | Clinical reasoning with evidence | ❌ No (Python) | ✅ Yes — grounding pattern | NO LICENSE | INSPIRE |

---

## Summary by Decision

### REUSE — Can incorporate code/patterns directly
*None at this stage — all need adaptation to Kotlin/Room architecture*

### ADAPT — Study and translate to BioAcupunt patterns

1. **Entity Type Taxonomy** → Expand `KnowledgeEntityType` enum (add FLAVOR, TROPISM, TOXICITY, PROPERTY)
2. **Herb Property Encoding** → Design herb feature representation in Knowledge Core
3. **Evidence Traceability** → Build EvidenceResolver with source→citation→page chain
4. **Safety Boundaries** → Apply explicit scope definition to Clinical Intelligence
5. **Graph Retrieval Pattern** → Implement KnowledgeGraphRepository with BFS traversal
6. **Evidence Citation Chain** → Build citation resolution in Phase 2
7. **Context Assembly** → Combine FTS + graph results in HybridRetriever
8. **Verification Step** → Add verifier to Clinical Intelligence Orchestrator
9. **Clinical NLP Pipeline** → Design text→structured extraction flow
10. **Structured Note Template** → Apply template pattern to clinical notes

### INSPIRE — Study for architectural understanding

1. **Graph Attention Explainability** → Attention-based pattern compatibility scoring
2. **Hypergraph Concept** → Evaluate if needed for formula representation
3. **Topic Modeling** → Herb-symptom co-occurrence for scoring
4. **Agent Pipeline** → Pipeline stages for Clinical Intelligence
5. **FHIR Resource Model** → Future Phase 5 adapter layer
6. **Offline-First Sync** → Compare with existing SyncEngine
7. **Questionnaire Pattern** → Future configurable anamnesis
8. **Clinical NLP Extraction** → Text→observation pipeline stages
9. **Hybrid Retrieval** → Multi-signal retrieval architecture
10. **Evidence Grounding** → Clinical reasoning with traceability

### IGNORE — Not relevant to current phases

1. **DICOM Viewing** → Phase 6 only
2. **3D Anatomy** → Phase 6 only, GPL v3 blocks incorporation

### BLOCKED — Cannot use

1. **3D Anatomy Atlas** → GPL v3 copyleft incompatible with project license
2. **zhongyi-graph** → Repository not found

---

## Implementation Priority (Mapped to Phases)

| Phase | Components to Adapt/Inspire | Source Projects |
|-------|---------------------------|-----------------|
| **Phase 3A** | Entity taxonomy expansion, graph traversal, evidence chain | TCM KG, Medical-Graph-RAG, nihaisha |
| **Phase 3B** | Pattern scoring, differential engine, explainability | GraphAI-for-TCM, agentic-med-diag |
| **Phase 4** | Hybrid retrieval, BM25, entity recognition, reranking | MediGRAF, Medical-Graph-RAG |
| **Phase 5** | Protocol engine, FHIR adapter | FHIRCore, Beda EMR |
| **Phase 6** | Copilot, NLP pipeline | agentic-med-diag, Infherno, Phlox |
| **Phase 7** | Decision flows, clinical session | None directly — build from scratch |
| **Phase 8** | Modo atendimento, STT | MedGem, Phlox |
| **Phase 9** | Atlas (acupoints, meridians) | TCM KG entity model |
| **Phase 10** | FHIR interop | FHIRCore, Beda EMR |

---

*This matrix is the definitive reuse decision reference. All incorporations must align with R1-R4 ground rules.*
