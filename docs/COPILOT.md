# BIOACUPUNT — Clinical Copilot

## Overview

The Clinical Copilot is the AI-powered assistant that integrates retrieval-augmented generation (RAG) with clinical intelligence to provide grounded, evidence-based responses.

## Architecture

```
USER
  ↓
COPILOT ROUTER (CopilotRouter)
  ↓
INTENT + ENTITY (IntentDetector + EntityRecognizer)
  ↓
HYBRID RETRIEVAL (HybridRetriever)
  ├── Lexical (FTS5 BM25)
  ├── Vector (sqlite-vec) [deferred]
  ├── Graph (BFS traversal)
  └── Metadata (type/status filters)
  ↓
DEDUP (Deduplicator)
  ↓
RERANK (RetrievalReranker)
  ↓
CONTEXT BUILDER (ContextBuilder)
  ↓
EVIDENCE RESOLUTION (EvidenceResolutionService)
  ↓
CLINICAL INTELLIGENCE (ClinicalIntelligenceIntegration)
  ↓
EVIDENCE GATE (EvidenceGate) ← R2 enforcement
  ├── BLOCK → NO MODEL CALL → INSUFFICIENT_EVIDENCE
  └── ALLOW → LLM → RESPONSE VALIDATOR → GROUNDED RESPONSE
  ↓
COPILOT UI → HUMAN REVIEW
```

## Key Components

### EvidenceGate (R2 Enforcement)

The single enforcement point for the R2 rule: **RAG WITHOUT EVIDENCE = NO MODEL CALL**.

```kotlin
class EvidenceGate {
    fun evaluate(context: StructuredContext, requiredEvidence: Boolean): GateResult
}
```

Decision flow:
1. `requiredEvidence = false` → ALLOW (e.g., patient summary)
2. `context.items.isEmpty()` → BLOCK_NO_EVIDENCE
3. `context.evidenceIds.isEmpty() && items.size < MIN` → BLOCK_INSUFFICIENT_EVIDENCE
4. Otherwise → ALLOW

### CopilotRouter

Maps intent to tool:

| Intent | Tool | Requires Patient |
|---|---|---|
| KNOWLEDGE_SEARCH | KNOWLEDGE_SEARCH | No |
| PATIENT_SUMMARY | PATIENT_SUMMARY | Yes |
| DIFFERENTIAL_EXPLANATION | DIFFERENTIAL_EXPLANATION | Yes |
| MISSING_DATA | MISSING_DATA | Yes |
| EVIDENCE_LOOKUP | EVIDENCE_LOOKUP | No |
| POINT_LOOKUP | POINT_LOOKUP | No |
| FORMULA_LOOKUP | FORMULA_LOOKUP | No |
| PROTOCOL_LOOKUP | PROTOCOL_LOOKUP | No |
| CLINICAL_ANALYSIS | KNOWLEDGE_SEARCH | Yes |
| RESEARCH_QUERY | KNOWLEDGE_SEARCH | No |
| GENERAL_CLINICAL_QUERY | KNOWLEDGE_SEARCH | No |

### ClinicalCopilotEngine

The orchestrator that ties everything together:

```kotlin
class ClinicalCopilotEngine(
    intentDetector, entityRecognizer, queryNormalizer,
    hybridRetriever, reranker, contextBuilder,
    evidenceGate, evidenceResolutionService,
    groundedResponseGenerator, responseValidator,
    clinicalIntelligenceIntegration, patientContextProvider,
    explainDifferentialUseCase, explainMissingDataUseCase,
    copilotRouter,
)
```

## UI Integration

The `CopilotScreen` provides:
- **Context Indicator** — Shows active mode (Patient/Knowledge/Differential/General)
- **Chat Messages** — User input + copilot responses
- **Intent Badge** — Detected intent per response
- **Confidence Badge** — HIGH/MODERATE/LOW/INSUFFICIENT
- **Evidence Explorer Panel** — Citations, evidence IDs, knowledge version
- **Differential Explanation Panel** — Claims, uncertainties
- **Missing Data Panel** — Missing observations, impact
- **Validation Warning Bar** — Unsupported claims

## Wire in AppContainer

```kotlin
val clinicalCopilotEngine: ClinicalCopilotEngine by lazy { ... }
val evidenceExplorer: EvidenceExplorer by lazy { ... }
```

## UI States

| State | Meaning |
|---|---|
| IDLE | Initial state |
| LOADING | Processing query |
| SUCCESS | Response generated with evidence |
| NO_EVIDENCE | Gate blocked — no evidence found |
| PARTIAL_RESULT | Response has unsupported claims |
| PATIENT_CONTEXT_UNAVAILABLE | Patient context not loaded |
| MODEL_UNAVAILABLE | Local LLM not available |
| OFFLINE | Network unavailable |
| ERROR | Processing error |
