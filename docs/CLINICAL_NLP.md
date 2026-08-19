# Clinical NLP — Deterministic Text Extraction

## Overview

The Clinical NLP engine extracts structured clinical observations from raw
Portuguese text using deterministic regex patterns. It is **conservative by
design**: `UNKNOWN` is always preferred over unsupported inference.

**Critical rule:** The NLP never transforms "talvez" into "confirmed". It never
generates diagnosis — only observations.

## Architecture

```
Raw Text (PT-BR)
    ↓
ClinicalNlpUseCase.extract()
    ↓
ClinicalExtractionResult
    ├── observations (symptoms, signs, pain, tongue, pulse)
    ├── symptoms (patient-reported)
    ├── findings (practitioner-observed)
    ├── temporalPatterns (time-based)
    ├── recognizedEntities (acupoints, patterns)
    ├── uncertainties (negation, ambiguity markers)
    └── sourceText (original)
```

## Extraction Categories

### Source Detection
Detects who provided the information:
- "Paciente relata/refere/queixa" → `PATIENT_REPORTED`
- "Observado/ao exame/apresenta" → `PRACTITIONER_OBSERVED`
- "Nega/não relata" → `PATIENT_REPORTED` (negated)

### Symptoms (patient-reported)
```regex
paciente (relata|refere|queixa|apresenta) {text}
```

### Signs (practitioner-observed)
```regex
língua (apresenta|com|vermelha|pálida|...)
pulso (apresenta|com|é|está) {text}
observa-se|verifica-se {text}
```

### Temporal Patterns
- Time of day: "à noite", "pela manhã"
- Frequency: "frequentemente", "sempre", "diário"
- Duration: "há 2 semanas", "desde março"
- Aggravating/relieving: "piora com...", "melhora com..."

### Pain
- Location: "dor em/na/no {region}"
- Quality: "EVA 7/10", "dor lancinante"
- Radiation: "irradia para..."

### Acupoint Codes
```regex
\b([A-Z]{1,3}\d{1,2})\b  →  LI4, ST36, SP6
```

### MTC Patterns (recognition only, not diagnosis)
```
deficiência de qi, estagnação de qi, deficiência de yin, ...
```

## Confidence Scoring

Base confidence: 0.5
- Text > 30 chars: +0.1
- Text > 60 chars: +0.1
- Clinical terms present: +0.05
- Maximum: 0.9

## Uncertainty Detection

Markers: "talvez", "possivelmente", "pode ser", "não tenho certeza", "acho que"

## Files

- `clinic/domain/model/ClinicalNlp.kt` — extraction result types
- `clinic/domain/usecase/ClinicalNlpUseCase.kt` — extraction engine
- `clinic/domain/nlp/QuestionnaireToObservationMapper.kt` — Q→O mapping
