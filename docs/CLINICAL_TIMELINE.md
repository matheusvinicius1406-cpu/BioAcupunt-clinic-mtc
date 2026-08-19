# Clinical Timeline — Unified Patient History

## Overview

The Clinical Timeline aggregates all clinical events into a single chronological
view, enabling practitioners to see the full patient history at a glance.

## Event Types

| Type | Source Entity | Title |
|---|---|---|
| `ENCOUNTER` | Encounter | "Atendimento — {type}" |
| `OBSERVATION` | StructuredObservation | Observation type |
| `ASSESSMENT` | MtcAssessment | "Avaliação MTC" |
| `TREATMENT` | TreatmentPlan | "Plano de Tratamento" |
| `NOTE` | ClinicalNote | "Nota — {format}" |
| `FOLLOW_UP` | FollowUp | "Retorno — {reason}" |
| `DOCUMENT` | Document | Document title |

## Timeline Event Structure

```kotlin
data class ClinicalTimelineEvent(
    val id: String,          // "encounter_42", "obs_103"
    val patientId: Long,
    val tenantId: Long,
    val type: TimelineEventType,
    val date: String,        // ISO-8601
    val title: String,
    val summary: String,
    val entityId: Long?,     // reference to source entity
    val metadata: Map<String, String>,
)
```

## Query Capabilities

```kotlin
// Full timeline (newest first)
timelineRepository.getTimeline(patientId)

// Filter by type
timelineRepository.getTimelineByType(patientId, TimelineEventType.OBSERVATION)

// Date range
timelineRepository.getTimelineByDateRange(patientId, from, to)

// Recent N events
timelineRepository.getRecentEvents(patientId, limit = 10)

// Single encounter events
timelineRepository.getEventsByEncounter(encounterId)
```

## Longitudinal Patient Context

A focused subset of the timeline for the copilot and clinical intelligence:

```kotlin
data class LongitudinalPatientContext(
    val patientId: Long,
    val recentObservations: List<StructuredObservation>,
    val persistentFindings: List<String>,
    val recentAssessments: List<String>,
    val treatmentHistory: List<String>,
    val followUps: List<FollowUp>,
    val recentNotes: List<String>,
    val recurringPatterns: List<String>,
    val currentConcerns: List<String>,
    val sessionCount: Int,
    val lastEncounterDate: String?,
)
```

**NOT the full prontuário** — only information relevant to the current clinical
decision. Built by `BuildLongitudinalPatientContextUseCase`.

## Session Comparison

Compares two clinical sessions to identify changes:

```kotlin
val comparison = compareUseCase.compare(
    sessionAObservations, sessionBObservations
)
// newFindings, resolvedFindings, persistentFindings,
// worsenedFindings, improvedFindings
```

All differences are derived from **structured data**, never from text inference.

## Files

- `clinic/domain/model/ClinicalTimeline.kt` — models
- `clinic/domain/repository/ClinicalTimelineRepository.kt` — interface
- `clinic/data/repository/ClinicalTimelineRepositoryImpl.kt` — implementation
- `clinic/domain/usecase/GetClinicalTimelineUseCase.kt` — use case
- `clinic/domain/usecase/BuildLongitudinalPatientContextUseCase.kt` — longitudinal
- `clinic/domain/usecase/CompareClinicalSessionsUseCase.kt` — comparison
