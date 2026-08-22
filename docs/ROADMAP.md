# BioAcupunt — Official Roadmap

**Last Updated:** 2026-08-20
**Status:** Canonical — replaces all previous conflicting roadmaps
**Source:** Clinical Intelligence 2.0 Master Roadmap (Phases 1–12)

---

## Current State

```
PHASE 1  — ✅ COMPLETE   Knowledge Core
PHASE 2  — ✅ COMPLETE   Canonical Search
PHASE 3  — ✅ COMPLETE   Clinical Intelligence
PHASE 4  — ✅ COMPLETE   RAG / GraphRAG / Copilot
PHASE 5  — ✅ COMPLETE   Clinical Workflow Platform
PHASE 6  — 🚧 CURRENT    Multimodal Clinical Intelligence + Knowledge Operations
PHASE 7  — ⏳ NEXT       BioAcupunt CRM
PHASE 8  — ⏳            DDD + Architecture + Security Hardening
PHASE 9  — ⏳            Product Experience + Frontend Transformation
PHASE 10 — ⏳            Scale + SaaS + Commercial Platform
PHASE 11 — ⏳            Multiprofessional Clinical Platform
PHASE 12 — ⏳            Open Source + Technology Evolution
```

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

---

## Phase 3 — Clinical Intelligence ✅

**Status:** COMPLETE

- KnowledgeGraphRepository (BFS traversal, graph queries)
- EvidenceResolver (evidence IDs → full traces, provenance chain)
- DifferentialEngine (deterministic pattern scoring)
- MissingDataEngine (identifies missing differentiating features)
- ClinicalIntelligenceEngine (orchestrator)
- RunClinicalIntelligenceUseCase (end-to-end pipeline)
- Knowledge graph models (entity/edge/path types)
- E2E tests, benchmark tests, regression tests
- 389 tests (Phase 1–3 cumulative), 0 failures

---

## Phase 4 — RAG / GraphRAG / Copilot ✅

**Status:** COMPLETE

- IntentDetector (11 intent types)
- EntityRecognizer (patient, point, pattern, formula extraction)
- QueryNormalizer (query preprocessing)
- HybridRetriever (lexical FTS5 + graph + metadata backends)
- Deduplicator + ScoreNormalizer + RetrievalReranker
- ContextBuilder (budget-limited structured context)
- EvidenceGate (R2 enforcement — no evidence = no model call)
- EvidenceResolutionService + EvidenceExplorer
- GroundedResponseGenerator + ResponseValidator
- ClinicalCopilotEngine (full orchestrator)
- CopilotRouter (intent → tool mapping)
- CopilotScreen UI (context indicator, evidence panel, differential, missing data)
- ExplainDifferentialUseCase + ExplainMissingDataUseCase
- ModelRouter (local only)
- PromptAssembler
- 144 new tests, 533 cumulative, 0 failures

---

## Phase 5 — Clinical Workflow Platform ✅

**Status:** COMPLETE

### Encounter Domain
- Encounter (PLANNED → IN_PROGRESS → COMPLETED/CANCELLED)
- ClinicalNote (SOAP/MTC_EVOLUTION, DRAFT → FINAL)
- TreatmentPlan (AI suggestion → professional confirm)
- FollowUp (SCHEDULED → COMPLETED/MISSED/CANCELLED)

### Observation Lifecycle
- StructuredObservation (DRAFT → REVIEWED → CONFIRMED / REJECTED)
- Source tracking (PATIENT_REPORTED / PRACTITIONER_OBSERVED / AI_EXTRACTED_DRAFT)
- AI never auto-promotes to CONFIRMED

### Clinical NLP
- Deterministic regex extraction (no LLM)
- Symptoms, temporal patterns, clinical findings, negation

### Speech-to-Text
- SpeechToTextEngine interface
- AndroidSpeechToTextEngine (Android SpeechRecognizer)
- FakeSpeechToTextEngine (test double)

### Questionnaires
- Versioned, conditional logic, item → observation mapping
- QuestionnaireToObservationMapper (mapped items → DRAFT observations)

### Timeline & Longitudinal
- ClinicalTimeline (7 event types aggregated)
- BuildLongitudinalPatientContextUseCase (focused subset for copilot)

### Clinical Drafts
- GenerateClinicalDraftUseCase (never auto-finalizes)
- CompareClinicalSessionsUseCase (structured comparison)

### FHIR Interoperability
- FHIR mapping layer (Patient, Encounter, Observation, Note, CarePlan, Flag)
- ExportPatientToFhirUseCase (user-initiated, never automatic)

### Room Migration
- v27: 6 new tables, additive, no DEFAULT, soft delete, tenantId + index

### Tests
- 48 new Phase 5 tests, 581 cumulative, 0 failures

---

## Phase 6 — Multimodal Clinical Intelligence & Knowledge Operations

**Status:** 🚧 CURRENT TARGET

### Mission

Transform the structured clinical platform into a multimodal, visual, auditable, updatable, knowledge-operated platform.

### 6A — Knowledge Operations
- [ ] KnowledgePack, KnowledgePackManifest, KnowledgePackValidator
- [ ] KnowledgePackInstaller, KnowledgePackRollback, KnowledgePackDiff
- [ ] Pack manifest: packId, version, schemaVersion, publisher, status, checksums, signature
- [ ] Pack validation pipeline: Manifest → Schema → Checksum → Signature → Reference → Version Compatibility → Stage → Activate
- [ ] Atomic installation: CURRENT → NEW → STAGING → VALIDATION → ACTIVATION (fail-safe: current stays active)
- [ ] Rollback: ACTIVE → PREVIOUS VALID VERSION
- [ ] Entity versioning: entityId, version, status (ACTIVE/DEPRECATED/RETIRED), packId
- [ ] Provenance: Source, Citation, Evidence, Version, Pack, Reviewer
- [ ] Knowledge Pack Diff: ADDED / REMOVED / CHANGED / DEPRECATED (human-auditable)
- [ ] Editorial lifecycle: AUTHOR → DRAFT → REVIEW → CLINICAL APPROVAL → PUBLISH → PACK → INSTALL → ACTIVE
- [ ] Offline knowledge updates: Remote Pack → Download → Validate → Checksum → Signature → Stage → Activate
- [ ] Update UI states: CURRENT / AVAILABLE / DOWNLOADING / VALIDATING / STAGED / ACTIVE / FAILED / ROLLED BACK

### 6B — Clinical Media Domain
- [ ] ClinicalMedia model (IMAGE / AUDIO / VIDEO / DOCUMENT)
- [ ] Metadata: id, patientId, encounterId, type, uri, mimeType, hash, source, capturedAt, device, processingVersion
- [ ] Media security: authentication, authorization, encryption, retention, deletion, backup, export, audit
- [ ] No clinical data in public paths or uncontrolled URLs

### 6C — Tongue / Vision Foundation
- [ ] TongueObservation (color, shape, coating, moisture, cracks, marks, movement, specialFindings, imageId)
- [ ] Lifecycle: CAPTURED → FEATURES_EXTRACTED → DRAFT → REVIEWED → CONFIRMED / REJECTED
- [ ] Tongue Image Pipeline: Photo → Image Validation → Preprocessing → Vision Engine → Feature Extraction → Draft Observation → Human Review → Confirmed
- [ ] VisionEngine abstraction (input: image → output: VisionResult with features, regions, confidence, modelName)
- [ ] Rule: VISION ≠ DIAGNOSIS — always AI_DRAFT until human review
- [ ] Tongue regions: tip, center, root, left, right
- [ ] Interpretations from Knowledge Core, never invented

### 6D — Pulse Foundation
- [ ] PulseObservation (depth, rate, strength, width, quality, left, right, cun, guan, chi)
- [ ] PulseInputProvider abstraction (MANUAL / DEVICE / IMPORTED / AI_ASSISTED)
- [ ] PulseFeature (value, unit, confidence, source)
- [ ] Pulse analysis: Raw Data → Features → Draft Interpretation → Knowledge Core → Human Review
- [ ] Never: Pulse Sensor → Automatic Diagnosis

### 6E — Anatomy / Atlas Foundation
- [ ] AnatomicalRegion, AnatomicalStructure, MeridianSegment, AcupointLocation
- [ ] Spatial model: coordinateSystem, latitude, longitude, depth, anatomicalReference
- [ ] Acupoint spatial relation: Acupoint → LocatedOn → AnatomicalRegion
- [ ] Meridian layer: Meridian → MeridianSegment → Acupoint
- [ ] Atlas UI (2D first): Body → Region → Meridian → Point → Point Detail
- [ ] Acupoint detail: name, code, meridian, location, actions, indications, combinations, anatomy, evidence, sources
- [ ] 3D abstractions only: AtlasRenderer, SpatialLayer, AnatomyLayer, MeridianLayer, AcupointLayer
- [ ] Consumes existing Knowledge Core

### 6F — Integration / Validation / Hardening
- [ ] Multimodal → Clinical Intelligence: only confirmed observations feed clinical facts
- [ ] Copilot multimodal: questions about tongue/pulse history, confirmed vs detected
- [ ] Audit trail: PackInstalled, PackActivated, MediaCaptured, VisionAnalyzed, ObservationConfirmed, PulseRecorded, etc.
- [ ] Security audit: media encryption, access control, Keystore, backup, export, delete, trusted keys, signature validation
- [ ] Performance: pack validation/installation, search after update, vision inference, atlas rendering (median, p95, max)
- [ ] Device validation: Knowledge Pack update, Camera, Image picker, Media storage, Copilot, Atlas, Offline mode
- [ ] Regression: Phase 1–5 all green
- [ ] Documentation: PHASE_6_READINESS, KNOWLEDGE_PACKS, MULTIMODAL, TONGUE, PULSE, ATLAS, KNOWLEDGE_OPERATIONS, MEDIA_SECURITY

### Tests Required (Phase 6)
- Pack: valid pack, invalid manifest/checksum/signature, missing entity, duplicate entity, version mismatch, rollback, activation, failed installation
- Vision: valid/invalid image, model unavailable, success/failure, low confidence, review lifecycle
- Media: capture, upload, metadata, hash, authorization, delete, export, backup
- Pulse: manual input, device abstraction, missing values, validation, status lifecycle, source tracking
- Atlas: point lookup, meridian lookup, anatomical relation, missing coordinate, evidence linkage

### E2E Required (Phase 6)
- Knowledge: Pack → Validate → Checksum → Signature → Stage → Activate → Search → Graph → Evidence
- Tongue: Image → Vision → Features → Draft → Review → Confirmed → Clinical Intelligence
- Pulse: Input → Observation → Review → Confirmed → Clinical Intelligence
- Atlas: Point Search → Acupoint → Meridian → Anatomy → Evidence → UI

---

## Phase 7 — BioAcupunt CRM

**Status:** ⏳ NEXT (after Phase 6)

- Integrate/adapt Twenty CRM
- Patient 360
- Pipelines, Tasks, Activities, Workflows
- Unified Timeline
- Respect Twenty license, no disconnected clinical DB

---

## Phase 8 — DDD + Architecture + Security Hardening

**Status:** ⏳

- Domain-Driven Design, Bounded Contexts
- Dependency boundaries
- Secret management, encryption, threat modeling
- Authorization, tenant isolation, security testing
- Supply-chain security, observability, production hardening

---

## Phase 9 — Product Experience + Frontend Transformation

**Status:** ⏳

- Design System, Brand, Navigation
- Patient 360 UX, Encounter UX, Copilot UX, Evidence UX
- Accessibility, responsive UX, performance
- Product must convey: confidence, precision, professionalism, security, authority

---

## Phase 10 — Scale + SaaS + Commercial Platform

**Status:** ⏳

- Multi-tenancy, organizations, workspaces
- RBAC, billing, subscriptions, entitlements, usage metering
- Scalable API, queues, caching, observability, backup, disaster recovery
- Web, desktop, feature flags, operational scaling
- Priority: Modular Monolith → Measure → Extract services only when necessary

---

## Phase 11 — Multiprofessional Clinical Platform

**Status:** ⏳

- Shared Clinical Core + Specialty Modules
- Specialties: MTC, Acupuntura, Medicina, Biomedicina, Nutrição, Fisioterapia, Veterinária
- Each specialty: Clinical Model, Questionnaires, Knowledge, Protocols, Templates, Assessment, Workflow, Copilot Context
- Never mix specialty knowledge automatically

---

## Phase 12 — Open Source + Technology Evolution

**Status:** ⏳

- Open Source First, self-host when beneficial, reduce vendor lock-in
- Technology radar: ADOPT / TRIAL / ASSESS / HOLD / REJECT
- Flow: NEED → RESEARCH → LICENSE → SECURITY → BENCHMARK → COST → PROTOTYPE → INTEGRATE → MONITOR
- Maintain: docs/TECHNOLOGY_RADAR.md

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

## Global Clinical Rule

```
OBSERVE → STRUCTURE → RETRIEVE → REASON → DRAFT → REVIEW → CONFIRM → RECORD → FOLLOW-UP
```

Never: `LLM → FACT` · Never: `VISION → DIAGNOSIS` · Never: `AI → FINAL CLINICAL RECORD`

## Global Security Rule

All sensitive data treated as potentially protected: patient data, clinical notes, audio, images, documents, tokens, credentials, API keys, private keys, session data. Apply: encryption, access control, least privilege, audit, retention, deletion, secure backup, secure export, secret management.

## Execution Rule

Preserve existing functionality. Minimize diffs. Reuse code. Test. Document. Audit dependencies. Verify licenses. Protect data.

Never: hardcode API keys, expose secrets, copy restrictive datasets, copy proprietary content, ignore licenses, break regressions, persist AI as fact without review, invent evidence.

## Phase Gate (end of each sub-phase)

```
BUILD → UNIT TESTS → INTEGRATION TESTS → REGRESSION → DOCUMENTATION → GATE
```

Proceed only when gate passes or limitation is explicitly documented.

## Documentation Required (each phase)

readiness · architecture · security · tests · limitations · final report

---

## Version History

| Date | Change |
|------|--------|
| 2026-08-20 | Expanded to 12-phase roadmap (Clinical Intelligence 2.0 Master) |
| 2026-08-18 | Official roadmap established (6 phases) |
| 2026-07-25 | Phase 0–2 completed |

---

*This is the ONLY authoritative roadmap. All previous versions are superseded.*
