# Open-Source Reference Repositories — Inventory

**Created:** 2026-08-18
**Scope:** All requested reference repositories for BioAcupunt Clinical Intelligence 2.0

---

## 1. TCM Knowledge Graph (TCMM)

| Field | Value |
|-------|-------|
| **Repository** | TCM Knowledge Graph |
| **URL** | https://github.com/AI-HPC-Research-Team/TCM_knowledge_graph |
| **Verified URL** | ✅ Same |
| **Status** | ✅ Cloned |
| **Default Branch** | main |
| **License** | NO LICENSE FILE |
| **Dataset License** | Data from CPMC, TCMBANK, SymMap, TCID 2.0, PharMeBINet, PrimeKG — each has own terms |
| **Model License** | N/A (data processing only) |
| **Commercial Use** | ⚠️ Unclear without explicit license |
| **Redistribution** | ⚠️ Unclear |
| **Modification** | ⚠️ Unclear |
| **Attribution** | ⚠️ Unclear |
| **Maintenance Status** | Active (2024-2025) |
| **Stars** | ~200+ |
| **Recent Activity** | Data processing scripts, entity alignment |
| **Main Language** | Python |
| **Dependencies** | pandas, networkx, matplotlib |
| **Architecture** | CSV-based data pipeline → Neo4j graph → Python extraction scripts |
| **Relevant Modules** | Entity alignment, relation extraction, herb/syndrome/symptom/formula modeling |
| **Potential Reuse** | Entity type taxonomy (20 types), relation types (46 kinds), data processing pipeline logic |
| **Potential Restrictions** | No explicit license; dataset terms vary by source |
| **Decision** | INSPIRE — study entity model and relation taxonomy; do NOT import data directly |

---

## 2. GraphAI-for-TCM

| Field | Value |
|-------|-------|
| **Repository** | GraphAI-for-TCM |
| **URL** | https://github.com/ZENGJingqi/GraphAI-for-TCM |
| **Verified URL** | ✅ Same |
| **Status** | ✅ Cloned |
| **Default Branch** | main |
| **License** | MIT License (c) 2024 Zeng Jingqi |
| **Dataset License** | TCM-MKG dataset (Zenodo) — CC-BY-4.0 |
| **Model License** | MIT (code) + CC-BY-4.0 (dataset) |
| **Commercial Use** | ✅ Yes (MIT + CC-BY-4.0) |
| **Redistribution** | ✅ Yes |
| **Modification** | ✅ Yes |
| **Attribution** | Required |
| **Maintenance Status** | Active (April 2026 update) |
| **Stars** | ~100+ |
| **Recent Activity** | Online platform TCMXAI, paper published in J. Pharmaceutical Analysis |
| **Main Language** | Python |
| **Dependencies** | PyTorch, torch-geometric, pandas, networkx |
| **Architecture** | Knowledge graph → GNN (Graph Attention Network) → 5D mechanism prediction |
| **Relevant Modules** | Formula compatibility analysis, herb property encoding (91-dim), attention-based explainability |
| **Potential Reuse** | Hypergraph concept, herb property feature encoding, attention explainability pattern |
| **Potential Restrictions** | None significant |
| **Decision** | ADAPT — herb property encoding concept; hypergraph only if justified by data |

---

## 3. PTM (Prescription Topic Model)

| Field | Value |
|-------|-------|
| **Repository** | PTM |
| **URL** | https://github.com/yao8839836/PTM |
| **Verified URL** | ✅ Same |
| **Status** | ✅ Cloned |
| **Default Branch** | master |
| **License** | NO LICENSE FILE |
| **Dataset License** | CKCEST — research use only, commercial prohibited |
| **Model License** | N/A (Java implementation) |
| **Commercial Use** | ❌ Dataset research-only; code unclear |
| **Redistribution** | ⚠️ Unclear |
| **Modification** | ⚠️ Unclear |
| **Attribution** | Required (paper citation) |
| **Maintenance Status** | Inactive (2018 paper) |
| **Stars** | ~50+ |
| **Recent Activity** | None recent |
| **Main Language** | Java |
| **Dependencies** | Java 7+, Eclipse |
| **Architecture** | Topic model (LDA variant) for herb-symptom co-occurrence |
| **Relevant Modules** | 98K prescriptions, herb-symptom correspondence, topic modeling |
| **Potential Reuse** | Topic modeling algorithm concept; herb-symptom co-occurrence patterns |
| **Potential Restrictions** | Dataset commercial use prohibited |
| **Decision** | INSPIRE — study topic modeling approach; dataset cannot be used commercially |

---

## 4. zhongyi-graph

| Field | Value |
|-------|-------|
| **Repository** | zhongyi-graph |
| **URL** | https://github.com/Jana-o-O-o-O/zhongyi-graph |
| **Verified URL** | ❌ NOT FOUND (404) |
| **Status** | ❌ Unavailable |
| **Decision** | BLOCKED — repository does not exist. No alternative found. |

---

## 5. nihaisha-nishi-tcm

| Field | Value |
|-------|-------|
| **Repository** | nihaisha-nishi-tcm |
| **URL** | https://github.com/JuneYaooo/nihaisha-nishi-tcm |
| **Verified URL** | ✅ Same |
| **Status** | ✅ Cloned |
| **Default Branch** | main |
| **License** | NO LICENSE FILE |
| **Dataset License** | Course materials — fair use/educational only |
| **Model License** | N/A |
| **Commercial Use** | ❌ Educational content, not for commercial use |
| **Redistribution** | ❌ Course materials under fair use |
| **Modification** | ⚠️ For learning/reference only |
| **Attribution** | Required |
| **Maintenance Status** | Active (August 2026 updates) |
| **Stars** | ~500+ |
| **Recent Activity** | RAG+KG mode (temporarily disabled), safety references, evaluation set |
| **Main Language** | Markdown + Python (RAG pipeline) |
| **Dependencies** | Claude Code / Codex agent skill format |
| **Architecture** | Agent skill with evidence indexing, PDF evidence layer, screenshot evidence (2986 items) |
| **Relevant Modules** | Evidence traceability, structured extraction from unstructured courses, safety boundaries |
| **Potential Reuse** | Evidence indexing pattern, safety boundary concept, structured extraction approach |
| **Potential Restrictions** | Course materials are not freely redistributable |
| **Decision** | INSPIRE — evidence traceability pattern; NOT for content import |

---

## 6. MedGem

| Field | Value |
|-------|-------|
| **Repository** | MedGem |
| **URL** | https://github.com/kamalkraj/MedGem |
| **Verified URL** | ✅ Same |
| **Status** | ✅ Cloned |
| **Default Branch** | main |
| **License** | MIT License (c) 2026 Kamal Raj Kanakarajan |
| **Dataset License** | N/A |
| **Model License** | N/A (uses external models) |
| **Commercial Use** | ✅ Yes |
| **Redistribution** | ✅ Yes |
| **Modification** | ✅ Yes |
| **Attribution** | Required |
| **Maintenance Status** | Active (2026) |
| **Stars** | ~200+ |
| **Recent Activity** | Active development |
| **Main Language** | Kotlin/Java (Android) + Python |
| **Dependencies** | Android SDK, MediaPipe, ONNX |
| **Architecture** | On-device medical AI: STT → NLP → SOAP extraction → local inference |
| **Relevant Modules** | On-device inference, STT pipeline, SOAP structure, Android architecture |
| **Potential Reuse** | On-device inference pattern, STT integration, SOAP extraction concept |
| **Potential Restrictions** | None significant |
| **Decision** | ADAPT — on-device inference patterns; compare with existing LocalLlmProvider |

---

## 7. Medical-Graph-RAG

| Field | Value |
|-------|-------|
| **Repository** | Medical-Graph-RAG |
| **URL** | https://github.com/ImprintLab/Medical-Graph-RAG |
| **Verified URL** | ✅ Same |
| **Status** | ✅ Cloned |
| **Default Branch** | main |
| **License** | MIT License (c) 2024 Junde Wu |
| **Dataset License** | N/A |
| **Model License** | N/A (uses external models) |
| **Commercial Use** | ✅ Yes |
| **Redistribution** | ✅ Yes |
| **Modification** | ✅ Yes |
| **Attribution** | Required |
| **Maintenance Status** | Active |
| **Stars** | ~500+ |
| **Recent Activity** | Graph RAG implementation |
| **Main Language** | Python |
| **Dependencies** | Neo4j, Qdrant, FastAPI, LangChain |
| **Architecture** | Medical KG → graph retrieval + vector search → evidence-based answers |
| **Relevant Modules** | Chunking strategy, graph context assembly, evidence citation, retrieval pipeline |
| **Potential Reuse** | Graph retrieval pattern, evidence citation chain, context assembly logic |
| **Potential Restrictions** | None significant |
| **Decision** | ADAPT — retrieval and evidence patterns; NOT Neo4j (Room is sufficient) |

---

## 8. agentic-med-diag

| Field | Value |
|-------|-------|
| **Repository** | agentic-med-diag |
| **URL** | https://github.com/avnlp/agentic-med-diag |
| **Verified URL** | ✅ Same |
| **Status** | ✅ Cloned |
| **Default Branch** | main |
| **License** | Apache License 2.0 |
| **Dataset License** | N/A |
| **Model License** | N/A (uses external models) |
| **Commercial Use** | ✅ Yes |
| **Redistribution** | ✅ Yes |
| **Modification** | ✅ Yes |
| **Attribution** | Required |
| **Maintenance Status** | Active |
| **Stars** | ~100+ |
| **Recent Activity** | Agent pipeline development |
| **Main Language** | Python |
| **Dependencies** | LangChain, OpenAI, medical ontologies |
| **Architecture** | PLAN → RESEARCH → RETRIEVE → REASON → VERIFY → ANSWER pipeline |
| **Relevant Modules** | Agent pipeline pattern, verification step, structured reasoning |
| **Potential Reuse** | Pipeline pattern (adapt as clinical decision support, NOT autonomous diagnosis) |
| **Potential Restrictions** | None significant |
| **Decision** | INSPIRE — pipeline pattern; NEVER implement autonomous diagnosis (R1) |

---

## 9. FHIRCore

| Field | Value |
|-------|-------|
| **Repository** | FHIRCore |
| **URL** | https://github.com/opensrp/fhircore |
| **Verified URL** | ✅ Same |
| **Status** | ✅ Cloned (checkout issues — large repo) |
| **Default Branch** | main |
| **License** | Apache License 2.0 |
| **Dataset License** | N/A |
| **Model License** | N/A |
| **Commercial Use** | ✅ Yes |
| **Redistribution** | ✅ Yes |
| **Modification** | ✅ Yes |
| **Attribution** | Required |
| **Maintenance Status** | Active (OpenSRP project) |
| **Stars** | ~300+ |
| **Recent Activity** | FHIR R4 implementation |
| **Main Language** | Kotlin (Android) + FHIR |
| **Dependencies** | HAPI FHIR, Room, Android SDK |
| **Architecture** | FHIR-native Android app with offline-first, sync, forms |
| **Relevant Modules** | FHIR resource model, offline-first sync, form engine, security |
| **Potential Reuse** | FHIR adapter patterns, offline-first comparison |
| **Potential Restrictions** | None significant |
| **Decision** | INSPIRE — FHIR data model for Phase 5; compare sync patterns |

---

## 10. Beda FHIR EMR

| Field | Value |
|-------|-------|
| **Repository** | Beda FHIR EMR |
| **URL** | https://github.com/beda-software/fhir-emr |
| **Verified URL** | ✅ Same |
| **Status** | ✅ Cloned |
| **Default Branch** | main |
| **License** | MIT License (c) 2023 Ilya Beda |
| **Dataset License** | N/A |
| **Model License** | N/A |
| **Commercial Use** | ✅ Yes |
| **Redistribution** | ✅ Yes |
| **Modification** | ✅ Yes |
| **Attribution** | Required |
| **Maintenance Status** | Active |
| **Stars** | ~200+ |
| **Recent Activity** | FHIR EMR development |
| **Main Language** | TypeScript (React) + FHIR |
| **Dependencies** | React, FHIR SDK |
| **Architecture** | Web-based FHIR EMR with QuestionnaireRenderer |
| **Relevant Modules** | Questionnaire pattern, structured data capture, renderer |
| **Potential Reuse** | QuestionnaireRenderer concept for configurable anamnesis forms |
| **Potential Restrictions** | None significant |
| **Decision** | INSPIRE — Questionnaire pattern for future anamnesis configurability |

---

## 11. Phlox

| Field | Value |
|-------|-------|
| **Repository** | Phlox |
| **URL** | https://github.com/bloodworks-io/phlox |
| **Verified URL** | ✅ Same |
| **Status** | ✅ Cloned |
| **Default Branch** | main |
| **License** | MIT License (c) 2024 Filipe Gonsalves |
| **Dataset License** | N/A |
| **Model License** | N/A |
| **Commercial Use** | ✅ Yes |
| **Redistribution** | ✅ Yes |
| **Modification** | ✅ Yes |
| **Attribution** | Required |
| **Maintenance Status** | Active |
| **Stars** | ~100+ |
| **Recent Activity** | Medical scribe development |
| **Main Language** | TypeScript |
| **Dependencies** | Next.js, FHIR, speech recognition |
| **Architecture** | Medical scribe: STT → extraction → structured notes → review |
| **Relevant Modules** | Clinical NLP pipeline, template-based notes, local-first pattern |
| **Potential Reuse** | NLP extraction pipeline, structured note template pattern |
| **Potential Restrictions** | None significant |
| **Decision** | ADAPT — clinical NLP pipeline concept; review-before-persist pattern |

---

## 12. OHIF Viewers

| Field | Value |
|-------|-------|
| **Repository** | OHIF Viewers |
| **URL** | https://github.com/OHIF/Viewers |
| **Verified URL** | ✅ Same |
| **Status** | ✅ Cloned |
| **Default Branch** | master |
| **License** | MIT License (c) 2018 Open Health Imaging Foundation |
| **Dataset License** | N/A |
| **Model License** | N/A |
| **Commercial Use** | ✅ Yes |
| **Redistribution** | ✅ Yes |
| **Modification** | ✅ Yes |
| **Attribution** | Required |
| **Maintenance Status** | Very Active |
| **Stars** | ~4000+ |
| **Recent Activity** | Active DICOM viewer development |
| **Main Language** | TypeScript |
| **Dependencies** | Cornerstone.js, React |
| **Architecture** | Extensible DICOM viewer platform |
| **Relevant Modules** | DICOM parsing, annotation, extensibility pattern |
| **Potential Reuse** | Future Phase 6 — DICOM viewing capability |
| **Potential Restrictions** | None significant |
| **Decision** | IGNORE for now — Phase 6 roadmap item only |

---

## 13. 3D Anatomy Atlas

| Field | Value |
|-------|-------|
| **Repository** | 3D Anatomy Atlas |
| **URL** | https://github.com/hunglkt/3D-Anatomy-Atlas |
| **Verified URL** | ✅ Same |
| **Status** | ✅ Cloned |
| **Default Branch** | master |
| **License** | GNU General Public License v3 |
| **Dataset License** | N/A |
| **Model License** | N/A |
| **Commercial Use** | ⚠️ GPL v3 — copyleft, affects derivatives |
| **Redistribution** | ✅ Yes (with GPL) |
| **Modification** | ✅ Yes (with GPL) |
| **Attribution** | Required |
| **Maintenance Status** | Inactive |
| **Stars** | ~50+ |
| **Recent Activity** | None recent |
| **Main Language** | C#/Unity |
| **Dependencies** | Unity, 3D models |
| **Architecture** | 3D anatomical model viewer |
| **Relevant Modules** | 3D anatomy spatial model |
| **Potential Reuse** | Future Phase 6 — body→meridian→acupoint visualization |
| **Potential Restrictions** | GPL v3 copyleft — cannot incorporate into proprietary code |
| **Decision** | BLOCKED — GPL v3 incompatible with project; study concept only |

---

## 14. Infherno

| Field | Value |
|-------|-------|
| **Repository** | Infherno |
| **URL** | https://github.com/j-frei/Infherno |
| **Verified URL** | ✅ Same |
| **Status** | ✅ Cloned |
| **Default Branch** | main |
| **License** | NO LICENSE FILE |
| **Dataset License** | N/A |
| **Model License** | N/A |
| **Commercial Use** | ⚠️ Unclear |
| **Redistribution** | ⚠️ Unclear |
| **Modification** | ⚠️ Unclear |
| **Attribution** | ⚠️ Unclear |
| **Maintenance Status** | Active |
| **Stars** | ~100+ |
| **Recent Activity** | Clinical NLP development |
| **Main Language** | Python |
| **Dependencies** | LLMs, medical NLP libraries |
| **Architecture** | Clinical NLP: text → structured observation extraction |
| **Relevant Modules** | Symptom extraction, timeline parsing, entity recognition |
| **Potential Reuse** | Clinical NLP pipeline pattern; text→structured observation concept |
| **Potential Restrictions** | No license — can study but not incorporate |
| **Decision** | INSPIRE — NLP extraction patterns; cannot use code directly |

---

## 15. MediGRAF

| Field | Value |
|-------|-------|
| **Repository** | mediGRAF (medical-ehr-graphrag) |
| **URL** | https://github.com/sthio90/medical-ehr-graphrag |
| **Verified URL** | ✅ Alternative found (original MediGRAF/MediGRAF not found) |
| **Status** | ✅ Cloned |
| **Default Branch** | main |
| **License** | NO LICENSE FILE |
| **Dataset License** | N/A |
| **Model License** | N/A |
| **Commercial Use** | ⚠️ Unclear |
| **Redistribution** | ⚠️ Unclear |
| **Modification** | ⚠️ Unclear |
| **Attribution** | ⚠️ Unclear |
| **Maintenance Status** | Active |
| **Stars** | ~50+ |
| **Recent Activity** | Graph RAG hybrid retrieval |
| **Main Language** | Python |
| **Dependencies** | Neo4j, vector DB, LLMs |
| **Architecture** | Hybrid graph + vector retrieval for medical QA |
| **Relevant Modules** | Graph+BM25+vector hybrid retrieval, clinical reasoning |
| **Potential Reuse** | Hybrid retrieval pattern, evidence grounding |
| **Potential Restrictions** | No license — can study but not incorporate |
| **Decision** | INSPIRE — hybrid retrieval architecture; adapt pattern to Room-based graph |

---

## License Classification Summary

### GREEN — Can potentially be incorporated after validation

| Repository | License | Notes |
|-----------|---------|-------|
| GraphAI-for-TCM | MIT + CC-BY-4.0 | Full commercial use OK |
| MedGem | MIT | Full commercial use OK |
| Medical-Graph-RAG | MIT | Full commercial use OK |
| Beda FHIR EMR | MIT | Full commercial use OK |
| OHIF Viewers | MIT | Full commercial use OK (Phase 6) |
| agentic-med-diag | Apache 2.0 | Full commercial use OK |
| FHIRCore | Apache 2.0 | Full commercial use OK |

### YELLOW — Study/reference only, may need adaptation

| Repository | License | Notes |
|-----------|---------|-------|
| TCM Knowledge Graph | NO LICENSE | Study entity model only |
| PTM | NO LICENSE + research-only dataset | Study algorithm only |
| nihaisha-nishi-tcm | NO LICENSE | Evidence pattern only |
| Infherno | NO LICENSE | NLP pattern only |
| MediGRAF | NO LICENSE | Retrieval pattern only |
| Phlox | MIT | Adapt NLP pipeline concept |

### RED — Cannot incorporate

| Repository | License | Notes |
|-----------|---------|-------|
| zhongyi-graph | N/A | Repository not found |
| 3D Anatomy Atlas | GPL v3 | Copyleft incompatible |

---

*This inventory is the definitive reference for license compliance. All incorporations must be validated against this document.*
