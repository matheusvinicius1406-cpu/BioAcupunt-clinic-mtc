# PHASE 5 — CLINIC INTELLIGENCE PLATFORM

## Architecture Overview

Phase 5 transforms BioAcupunt from a knowledge + intelligence + copilot system
into a **clinical workflow platform** with structured data, longitudinal care,
clinical NLP, voice workflow, questionnaires, and FHIR interoperability.

### Implementation Status

| Component | Status | Notes |
|---|---|---|
| Encounter domain | ✅ | Entity, DAO, Repository, States |
| Clinical Note | ✅ | SOAP/MTC_EVOLUTION format, DRAFT→FINAL lifecycle |
| Treatment Plan | ✅ | AI suggestion → professional confirm |
| Follow-up | ✅ | Scheduled/Completed/Missed lifecycle |
| Observation Lifecycle | ✅ | DRAFT→REVIEWED→CONFIRMED→REJECTED |
| Structured Observations | ✅ | Source tracking, NLP extraction, AI_EXTRACTED_DRAFT |
| Clinical NLP | ✅ | Deterministic regex extraction (no LLM) |
| STT Abstraction | ✅ | SpeechToTextEngine interface + Android impl + Fake |
| Questionnaire Engine | ✅ | Versioned, conditional logic, item→observation mapping |
| Questionnaire Mapper | ✅ | Only mapped items → observations, status=DRAFT |
| Clinical Timeline | ✅ | Aggregated events from all entities |
| Longitudinal Context | ✅ | Focused subset for copilot/intelligence |
| Draft Note Generation | ✅ | Never auto-finalizes, structured output |
| FHIR Foundation | ✅ | Mapping layer, Bundle export, MTC extensions |
| Room Migration v27 | ✅ | 6 new tables, additive, no DEFAULT |
| Phase 1-4 Regression | ✅ | 581 tests, 0 failures |

## Domain Model

```
Patient
  ├── Encounter (PLANNED→IN_PROGRESS→COMPLETED)
  │     ├── Observation (DRAFT→CONFIRMED)
  │     ├── Clinical Note (DRAFT→FINAL)
  │     ├── Treatment Plan (DRAFT→CONFIRMED)
  │     └── Follow-up (SCHEDULED→COMPLETED)
  ├── Questionnaire Response
  └── Timeline Events
```

## Safety Invariants

1. **AI → DRAFT → PROFESSIONAL REVIEW → FINAL** (never auto-finalize)
2. **AI_EXTRACTED_DRAFT ≠ CONFIRMED** (never auto-promote)
3. **Deterministic NLP** (no LLM in extraction path)
4. **Source tracking** on every observation
5. **Tenant isolation** on every repository

## Tables (v27)

```sql
encounters           -- clinical encounters
clinical_notes       -- SOAP/MTC notes
treatment_plans      -- treatment plans
follow_ups           -- follow-up tasks
structured_observations -- clinical observations with lifecycle
questionnaire_responses -- questionnaire answers
```

All tables:
- Have `tenantId` + index
- Have `deleted` soft delete
- Have `updatedAt` index
- Have `created_at` for timeline
- Use `deleted_at.is_(None)` filter pattern

## Files Created

### Domain Models (clinic/domain/model/)
- `Encounter.kt` — Clinical encounter with states
- `ClinicalNote.kt` — SOAP note format
- `TreatmentPlan.kt` — Treatment plan with items
- `FollowUp.kt` — Follow-up scheduling
- `ObservationLifecycle.kt` — Observation status + source
- `ClinicalTimeline.kt` — Timeline events + longitudinal context
- `ClinicalNlp.kt` — NLP extraction result types
- `SpeechToText.kt` — STT engine interface
- `Questionnaire.kt` — Dynamic questionnaire engine
- `DraftNote.kt` — AI draft note + review
- `FhirModels.kt` — FHIR mapping layer

### Room Entities (clinic/data/local/)
- `EncounterEntity.kt` + `EncounterDao.kt`
- `ClinicalNoteEntity.kt` + `ClinicalNoteDao.kt`
- `TreatmentPlanEntity.kt` + `TreatmentPlanDao.kt`
- `FollowUpEntity.kt` + `FollowUpDao.kt`
- `StructuredObservationEntity.kt` + `StructuredObservationDao.kt`
- `QuestionnaireResponseEntity.kt` + `QuestionnaireResponseDao.kt`
- `Migration26_27.kt` — ADDITIVE, no DEFAULT

### Repositories (clinic/data/repository/)
- `EncounterRepositoryImpl.kt`
- `ClinicalNoteRepositoryImpl.kt`
- `TreatmentPlanRepositoryImpl.kt`
- `FollowUpRepositoryImpl.kt`
- `ObservationRepositoryImpl.kt`
- `QuestionnaireResponseRepositoryImpl.kt`
- `ClinicalTimelineRepositoryImpl.kt`

### Use Cases (clinic/domain/usecase/)
- `ClinicalNlpUseCase.kt` — Deterministic text extraction
- `CompareClinicalSessionsUseCase.kt` — Session comparison
- `GenerateClinicalDraftUseCase.kt` — Draft note generation
- `ExportPatientToFhirUseCase.kt` — FHIR Bundle export
- `GetClinicalTimelineUseCase.kt` — Timeline aggregation
- `BuildLongitudinalPatientContextUseCase.kt` — Longitudinal context

### STT (clinic/data/stt/)
- `AndroidSpeechToTextEngine.kt` — Android SpeechRecognizer
- `FakeSpeechToTextEngine.kt` — Test double

### NLP (clinic/domain/nlp/)
- `QuestionnaireToObservationMapper.kt` — Q→O mapping

### Tests
- `ClinicalNlpUseCaseTest.kt` — 10 tests
- `CompareClinicalSessionsUseCaseTest.kt` — 6 tests
- `GenerateClinicalDraftUseCaseTest.kt` — 5 tests
- `ExportPatientToFhirUseCaseTest.kt` — 5 tests
- `QuestionnaireToObservationMapperTest.kt` — 5 tests
- `SpeechToTextTest.kt` — 6 tests
- `DraftNoteTest.kt` — 5 tests

### Documentation
- `docs/PHASE_5_READINESS.md`
- `docs/PHASE_5_ARCHITECTURE.md`

## Known Limitations (Phase 6 scope)

1. **QuestionnaireRenderer UI** — Compose dynamic renderer (UI, not domain)
2. **Copilot encounter context** — Wire CopilotScreen to encounter data
3. **AtendimentoMode** — Full clinical workflow screen
4. **Timeline UI** — Visual timeline component
5. **Session Comparison UI** — Side-by-side comparison
6. **Device validation** — Requires Android emulator/device
7. **Performance benchmarks** — Requires real device execution

## Build Status

```
compileDebugKotlin:     ✅ (pre-existing warnings only)
testDebugUnitTest:      ✅ 581 tests, 0 failures
assembleDebug:          ✅
Phase 1-4 regression:   ✅
```
