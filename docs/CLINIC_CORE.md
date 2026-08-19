# Clinic Core — Domain Architecture

## Overview

The Clinic Core is the **clinical workflow backbone** of BioAcupunt. It models
the lifecycle of a clinical encounter from start to finish, with structured data,
AI-assisted draft generation, and mandatory professional review.

## Principles

1. **AI → DRAFT → PROFESSIONAL REVIEW → FINAL** — never auto-finalize
2. **Source tracking** on every observation — know where data came from
3. **Tenant isolation** — every entity scoped to clinic
4. **Soft delete** — data is never physically removed
5. **Offline-first** — all data lives in Room/SQLite

## Domain Model

```
Patient
  │
  ├── Encounter (PLANNED → IN_PROGRESS → COMPLETED)
  │     │
  │     ├── Structured Observation
  │     │     ├── Source: PATIENT_REPORTED / PRACTITIONER_OBSERVED / AI_EXTRACTED_DRAFT
  │     │     ├── Status: DRAFT → REVIEWED → CONFIRMED / REJECTED
  │     │     └── Linked to: encounter, patient, timeline
  │     │
  │     ├── Clinical Note (SOAP / MTC_EVOLUTION)
  │     │     ├── Status: DRAFT → REVIEWED → FINAL
  │     │     └── Subjective / Objective / Assessment / Plan
  │     │
  │     ├── Treatment Plan
  │     │     ├── Items: ACUPUNCTURE / HERBAL / DIETARY / etc.
  │     │     ├── AI suggested → professional confirms
  │     │     └── Status: DRAFT → CONFIRMED → COMPLETED
  │     │
  │     └── Follow-up
  │           ├── Scheduled → Completed / Missed / Cancelled
  │           └── Expected vs Actual findings
  │
  ├── Questionnaire Response
  │     ├── Versioned questionnaire
  │     ├── Answers map → Structured Observations (via mapper)
  │     └── Status: IN_PROGRESS → COMPLETED
  │
  └── Clinical Timeline
        ├── Aggregates: encounters, observations, notes, treatments, follow-ups
        └── Supports: date range, type filter, recent events
```

## Encounter States

| State | Description |
|---|---|
| `PLANNED` | Scheduled, not yet started |
| `IN_PROGRESS` | Actively being conducted |
| `PAUSED` | Temporarily suspended |
| `COMPLETED` | Clinical work finished |
| `CANCELLED` | Encounter was cancelled |

**Transition rules:**
- `PLANNED → IN_PROGRESS` (start encounter)
- `IN_PROGRESS → PAUSED` (temporarily stop)
- `PAUSED → IN_PROGRESS` (resume)
- `IN_PROGRESS → COMPLETED` (finish)
- `IN_PROGRESS → CANCELLED` (abandon)
- `PLANNED → CANCELLED` (cancel before starting)

## Observation Lifecycle

```
AI_EXTRACTED_DRAFT  ←  NLP / Questionnaire mapper
        ↓
    DRAFT  ←  Manual entry
        ↓
   REVIEWED  ←  Professional reviews
    ↓        ↓
CONFIRMED  REJECTED
```

**Critical rule:** `AI_EXTRACTED_DRAFT ≠ CONFIRMED`. The system NEVER
auto-promotes an AI inference to confirmed clinical data.

## Clinical Note Formats

| Format | Use Case |
|---|---|
| `SOAP` | Standard subjective/objective/assessment/plan |
| `MTC_EVOLUTION` | MTC-specific evolution tracking |
| `FOLLOW_UP` | Return visit notes |
| `DISCHARGE` | Final discharge note |

## Treatment Plan Categories

| Category | Portuguese |
|---|---|
| `ACUPUNCTURE` | Acupuntura |
| `HERBAL` | Fitoterapia |
| `DIETARY` | Alimentação |
| `LIFESTYLE` | Estilo de vida |
| `EXERCISE` | Exercício |
| `MOXIBUSTION` | Moxabustão |
| `CUPPING` | Ventosaterapia |
| `OTHER` | Outro |

## Files

### Domain
- `clinic/domain/model/Encounter.kt`
- `clinic/domain/model/ClinicalNote.kt`
- `clinic/domain/model/TreatmentPlan.kt`
- `clinic/domain/model/FollowUp.kt`
- `clinic/domain/model/ObservationLifecycle.kt`
- `clinic/domain/model/ClinicalTimeline.kt`

### Data
- `clinic/data/local/EncounterEntity.kt` + `EncounterDao.kt`
- `clinic/data/local/ClinicalNoteEntity.kt` + `ClinicalNoteDao.kt`
- `clinic/data/local/TreatmentPlanEntity.kt` + `TreatmentPlanDao.kt`
- `clinic/data/local/FollowUpEntity.kt` + `FollowUpDao.kt`
- `clinic/data/local/StructuredObservationEntity.kt` + `StructuredObservationDao.kt`
- `clinic/data/repository/*RepositoryImpl.kt`
