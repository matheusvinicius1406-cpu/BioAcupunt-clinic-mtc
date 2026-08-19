# Encounters — Clinical Encounter Management

## Overview

An **Encounter** is a single clinical session with a patient. It is distinct from
an **Appointment** (which is scheduling) — the Encounter represents the actual
clinical event.

## Relationship to Appointment

```
Appointment (scheduling)
  │
  ├── patientId
  ├── scheduledAt
  └── status (SCHEDULED, CONFIRMED, COMPLETED, CANCELLED)
        │
        ↓ (when appointment happens)
        │
Encounter (clinical event)
  ├── appointmentId (link back)
  ├── patientId
  ├── status (PLANNED → IN_PROGRESS → COMPLETED)
  └── clinical data (observations, notes, treatments)
```

**Appointment = when** · **Encounter = what happened**

## Encounter Lifecycle

### Starting an Encounter

```kotlin
val encounter = Encounter(
    patientId = patientId,
    status = EncounterStatus.IN_PROGRESS,
    type = EncounterType.ACUPUNCTURE,
    startedAt = Instant.now().toString(),
    appointmentId = appointmentId, // optional link
)
encounterRepository.create(encounter)
```

### During an Encounter

- Add observations (manual, NLP-extracted, questionnaire-mapped)
- Generate draft notes (AI-assisted)
- Create treatment plans (AI-suggested)
- Run clinical intelligence

### Completing an Encounter

```kotlin
encounterRepository.complete(encounterId)
// Sets: status = COMPLETED, endedAt = now
```

## Encounter Types

| Type | Label | Use Case |
|---|---|---|
| `ACUPUNCTURE` | Acupuntura | Acupuncture session |
| `CONSULTATION` | Consulta | General consultation |
| `FOLLOW_UP` | Retorno | Follow-up visit |
| `ASSESSMENT` | Avaliação | Assessment only |
| `TREATMENT` | Tratamento | Treatment session |

## Integration Points

- **Questionnaire** → creates observations for the encounter
- **Clinical NLP** → extracts observations from voice/text
- **Clinical Intelligence** → analyzes observations for patterns
- **Copilot** → provides context-aware assistance during encounter
- **Draft Note** → generates SOAP note from encounter data
- **Timeline** → encounter appears as timeline event
- **FHIR** → encounter maps to FHIR Encounter resource

## Files

- `clinic/domain/model/Encounter.kt` — domain model
- `clinic/data/local/EncounterEntity.kt` — Room entity
- `clinic/data/local/EncounterDao.kt` — DAO
- `clinic/domain/repository/EncounterRepository.kt` — interface
- `clinic/data/repository/EncounterRepositoryImpl.kt` — implementation
