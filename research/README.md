# BioAcupunt — Research Repository Index

**Created:** 2026-08-18
**Purpose:** Local research cache for open-source reference projects. Not versioned as production code.

---

## Research Directories

| Directory | Purpose |
|-----------|---------|
| `open_source/repositories/` | Cloned reference repositories (local study cache) |
| `open_source/notes/` | Per-repository analysis notes |
| `open_source/comparisons/` | Cross-project comparison reports |
| `open_source/inventories/` | Inventory data |
| `datasets/` | Datasets downloaded for analysis |
| `papers/` | Academic papers and references |
| `experiments/` | Experimental code and prototypes |
| `prototypes/` | Proof-of-concept implementations |

---

## Reference Repositories

| # | Project | Local Path | URL | Status | License | Main Contribution | BioAcupunt Relevance |
|---|---------|-----------|-----|--------|---------|-------------------|---------------------|
| 1 | TCM Knowledge Graph | `open_source/repositories/TCM_knowledge_graph/` | https://github.com/AI-HPC-Research-Team/TCM_knowledge_graph | ✅ Cloned | NO LICENSE | Unified TCM database (20 entity types, 46 relation types, 3.4M records) | **HIGH** — entity/relation model, data pipeline, ontology alignment |
| 2 | GraphAI-for-TCM | `open_source/repositories/GraphAI-for-TCM/` | https://github.com/ZENGJingqi/GraphAI-for-TCM | ✅ Cloned | MIT | Interpretable graph AI for TCM formula compatibility | **HIGH** — hypergraph, formula analysis, explainability |
| 3 | PTM | `open_source/repositories/PTM/` | https://github.com/yao8839836/PTM | ✅ Cloned | NO LICENSE (research only) | Prescription topic modeling (98K prescriptions) | **MEDIUM** — topic modeling, herb-symptom co-occurrence |
| 4 | zhongyi-graph | N/A | https://github.com/Jana-o-O-o-O/zhongyi-graph | ❌ NOT FOUND | N/A | N/A | N/A — URL invalid, no alternative found |
| 5 | nihaisha-nishi-tcm | `open_source/repositories/nihaisha-nishi-tcm/` | https://github.com/JuneYaooo/nihaisha-nishi-tcm | ✅ Cloned | NO LICENSE | TCM course distillation, agent skill, evidence indexing | **MEDIUM** — evidence traceability, structured extraction |
| 6 | MedGem | `open_source/repositories/MedGem/` | https://github.com/kamalkraj/MedGem | ✅ Cloned | MIT | On-device medical AI (STT, vision, RAG, Android) | **HIGH** — on-device architecture, local inference patterns |
| 7 | Medical-Graph-RAG | `open_source/repositories/Medical-Graph-RAG/` | https://github.com/ImprintLab/Medical-Graph-RAG | ✅ Cloned | MIT | Graph RAG for medical information retrieval | **HIGH** — graph retrieval, evidence citation, context assembly |
| 8 | agentic-med-diag | `open_source/repositories/agentic-med-diag/` | https://github.com/avnlp/agentic-med-diag | ✅ Cloned | Apache 2.0 | Agent-based medical diagnosis (PLAN→RESEARCH→RETRIEVE→REASON→VERIFY→ANSWER) | **HIGH** — agent pipeline pattern (adapt as clinical decision support, NOT autonomous diagnosis) |
| 9 | FHIRCore | `open_source/repositories/fhircore/` | https://github.com/opensrp/fhircore | ✅ Cloned (checkout issues) | Apache 2.0 | FHIR-native Android app, offline-first, forms, sync | **MEDIUM** — FHIR data model, offline-first patterns |
| 10 | Beda FHIR EMR | `open_source/repositories/fhir-emr/` | https://github.com/beda-software/fhir-emr | ✅ Cloned | MIT | FHIR EMR with QuestionnaireRenderer, structured data | **MEDIUM** — Questionnaire pattern for configurable anamnesis |
| 11 | Phlox | `open_source/repositories/phlox/` | https://github.com/bloodworks-io/phlox | ✅ Cloned | MIT | Medical scribe, STT, local-first, templates, RAG | **MEDIUM** — clinical NLP pipeline, structured note extraction |
| 12 | OHIF Viewers | `open_source/repositories/Viewers/` | https://github.com/OHIF/Viewers | ✅ Cloned | MIT | DICOM viewer, extensible platform | **LOW** — future roadmap (Phase 6), not current priority |
| 13 | 3D Anatomy Atlas | `open_source/repositories/3D-Anatomy-Atlas/` | https://github.com/hunglkt/3D-Anatomy-Atlas | ✅ Cloned | GPL v3 | 3D anatomical model | **LOW** — future roadmap (Phase 6), body→meridian→acupoint |
| 14 | Infherno | `open_source/repositories/Infherno/` | https://github.com/j-frei/Infherno | ✅ Cloned | NO LICENSE | Clinical NLP, text→structured observation | **HIGH** — clinical NLP pipeline, symptom extraction |
| 15 | MediGRAF | `open_source/repositories/mediGRAF/` | https://github.com/sthio90/medical-ehr-graphrag | ✅ Cloned (alt URL) | NO LICENSE | Hybrid graph RAG for medical QA | **HIGH** — graph+vector retrieval, clinical reasoning |

---

## Summary

- **Requested:** 15 repositories
- **Cloned:** 14 (1 unavailable — zhongyi-graph)
- **Alternative found:** MediGRAF → sthio90/medical-ehr-graphrag
- **License audit:** 7 MIT, 2 Apache 2.0, 1 GPL v3, 5 NO LICENSE

---

## Key Documents

| Document | Path | Description |
|----------|------|-------------|
| Inventory | `open_source_inventory.md` | Full catalog with license details |
| Architecture Matrix | `open_source_architecture_matrix.md` | Feature comparison across projects |
| Reuse Matrix | `open_source_reuse_matrix.md` | What can be reused/adapted/inspired |
| Phase 3 Readiness | `../docs/PHASE_3_READINESS.md` | Current state + gaps for Clinical Intelligence |
| Official Roadmap | `../docs/ROADMAP.md` | Canonical project roadmap |

---

*This index is the entry point for all research artifacts. Updated 2026-08-18.*
