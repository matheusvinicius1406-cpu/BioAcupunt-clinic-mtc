# FHIR — Interoperability Layer

## Overview

FHIR (Fast Healthcare Interoperability Resources) provides a standard format for
exporting clinical data. BioAcupunt uses a **mapping layer** — internal models
remain the source of truth. The FHIR Bundle is for export/interoperability only.

**Does NOT transmit automatically.** The user must initiate export.

## Architecture

```
Internal Domain Models
    ↓
ExportPatientToFhirUseCase
    ↓
FHIR Mapping Layer
    ├── Patient → FHIR Patient
    ├── Encounter → FHIR Encounter
    ├── Observation → FHIR Observation
    ├── ClinicalNote → FHIR DocumentReference
    ├── TreatmentPlan → FHIR CarePlan
    └── FollowUp → FHIR Flag
    ↓
FhirBundle
    ↓
Export / Transmission (user-initiated)
```

## Resource Mappings

### Patient → FHIR Patient
```json
{
  "resourceType": "Patient",
  "id": "patient-1",
  "name": [{"text": "Maria Silva"}],
  "gender": "unknown",
  "active": true
}
```

### Encounter → FHIR Encounter
```json
{
  "resourceType": "Encounter",
  "id": "encounter-1",
  "status": "finished",
  "class": {"code": "AMB"},
  "type": [{"text": "Acupuntura"}],
  "subject": {"reference": "patient-1"},
  "period": {"start": "...", "end": "..."}
}
```

### Observation → FHIR Observation
```json
{
  "resourceType": "Observation",
  "id": "observation-1",
  "status": "final",
  "category": [{"text": "Sintoma"}],
  "code": {"text": "Sintoma"},
  "valueString": "Insônia há 2 semanas"
}
```

### ClinicalNote → FHIR DocumentReference
```json
{
  "resourceType": "DocumentReference",
  "id": "note-1",
  "status": "final",
  "type": {"text": "SOAP"},
  "content": [{"attachment": {"contentType": "text/plain", "data": "..."}}]
}
```

### TreatmentPlan → FHIR CarePlan
```json
{
  "resourceType": "CarePlan",
  "id": "plan-1",
  "status": "active",
  "intent": "plan",
  "goal": [{"description": "Melhorar sono"}]
}
```

### FollowUp → FHIR Flag
```json
{
  "resourceType": "Flag",
  "id": "followup-1",
  "status": "active",
  "code": {"text": "Follow-up"},
  "note": [{"text": "Retorno em 2 semanas"}]
}
```

## Status Mappings

| Internal | FHIR |
|---|---|
| PLANNED | planned |
| IN_PROGRESS | in-progress |
| COMPLETED | finished |
| CANCELLED | cancelled |
| DRAFT (observation) | registered |
| CONFIRMED (observation) | final |
| DRAFT (note) | current |
| FINAL (note) | final |

## MTC Extensions (Future)

When needed, MTC-specific data will use FHIR extensions:
- TongueObservation
- PulseObservation
- PatternAssessment
- AcupunctureSession
- AcupointSelection

**Not implemented in Phase 5** — reserved for when FHIR consumers need MTC-specific data.

## Files

- `clinic/domain/model/FhirModels.kt` — FHIR domain models
- `clinic/domain/usecase/ExportPatientToFhirUseCase.kt` — export use case
