# Questionnaires — Dynamic Clinical Forms

## Overview

The Questionnaire engine provides versioned, dynamic clinical forms that map
patient responses to structured observations. Supports conditional logic,
multiple item types, and explicit observation mapping.

## Architecture

```
Questionnaire (versioned definition)
    ↓
QuestionnaireRenderer (UI — Compose, Phase 6)
    ↓
User fills form
    ↓
QuestionnaireResponse (answers map)
    ↓
QuestionnaireToObservationMapper
    ↓
StructuredObservations (DRAFT status)
    ↓
Professional Review → CONFIRMED
```

## Questionnaire Structure

```
Questionnaire
  ├── id, version, title, status
  └── Sections[]
       ├── id, title, condition
       └── Items[]
            ├── id, type, label, required
            ├── options[] (for choice items)
            ├── condition (if X → show Y)
            ├── observationMapping (→ ObservationType)
            └── validation (min/max length/pattern)
```

## Item Types

| Type | Description |
|---|---|
| `TEXT` | Free text input |
| `BOOLEAN` | Yes/No |
| `INTEGER` | Whole number |
| `DECIMAL` | Decimal number |
| `SINGLE_CHOICE` | One selection from options |
| `MULTIPLE_CHOICE` | Multiple selections |
| `DATE` | Date picker |
| `TIME` | Time picker |
| `QUANTITY` | Number + unit |

## Conditional Logic

```kotlin
QuestionnaireCondition(
    dependsOnItemId = "pregnancy",
    operator = ConditionOperator.EQUALS,
    value = "true",
)
// Show this item/section only when pregnancy == true
```

## Observation Mapping

Only items with an explicit `observationMapping` produce clinical observations:

```kotlin
QuestionnaireItem(
    id = "sleep_quality",
    label = "Qualidade do sono",
    observationMapping = ObservationType.SLEEP, // ← explicit mapping
    options = listOf(
        QuestionnaireOption(id = "good", label = "Boa", value = "good"),
        QuestionnaireOption(id = "bad", label = "Ruim", value = "bad"),
    ),
)
```

Without `observationMapping`, the answer is stored but doesn't generate
a clinical observation.

## Questionnaire → Observation Mapping Rules

1. Only items with `observationMapping != null` produce observations
2. All mapped observations start as `AI_EXTRACTED_DRAFT` status
3. Empty required items are skipped
4. Option labels are used instead of IDs in observation content
5. Metadata includes questionnaireId, version, itemId

## Versioning

Each questionnaire has:
- `id` — stable identifier
- `version` — incremented on changes
- `status` — DRAFT / ACTIVE / DEPRECATED

Responses track which version was answered.

## Files

- `clinic/domain/model/Questionnaire.kt` — all questionnaire models
- `clinic/domain/nlp/QuestionnaireToObservationMapper.kt` — Q→O mapping
- `clinic/data/local/QuestionnaireResponseEntity.kt` — Room entity
- `clinic/data/local/QuestionnaireResponseDao.kt` — DAO
- `clinic/domain/repository/QuestionnaireResponseRepository.kt` — interface
- `clinic/data/repository/QuestionnaireResponseRepositoryImpl.kt` — impl
