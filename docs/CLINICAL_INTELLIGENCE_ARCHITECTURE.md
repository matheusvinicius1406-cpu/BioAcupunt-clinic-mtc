# Clinical Intelligence Architecture

## Overview

The Clinical Intelligence system provides **Clinical Decision Support** — structured, traceable, versioned reasoning over the Knowledge Core. It is NOT autonomous diagnosis.

## Pipeline

```
ClinicalObservation
      ↓
Knowledge Graph (BFS traversal)
      ↓
Evidence Engine (supporting + contradicting)
      ↓
Differential Engine (candidate generation + ranking)
      ↓
Missing Data Engine (what would help differentiate)
      ↓
ClinicalIntelligenceEngine (orchestrator)
      ↓
ClinicalIntelligenceResult
      ↓
Human Review (doctor decides)
```

## Components

### ClinicalObservation
- **Path:** `domain/KnowledgeGraphModels.kt`
- **Purpose:** Structured input from MtcAssessment
- **Fields:** symptoms, tongueFindings, pulseFindings, baGang, zangFuPatterns, history, etiology, context

### KnowledgeGraphRepository
- **Interface:** `repository/KnowledgeGraphRepository.kt`
- **Implementation:** `repository/RoomKnowledgeGraphRepository.kt`
- **Operations:** neighbors, reachable (BFS), findPath, edgesFrom, edgesTo
- **Safety:** visited set (cycle protection), maxDepth, maxNodes, maxResults, minConfidence
- **Config:** `GraphConfig` data class

### EvidenceResolver
- **Path:** `domain/EvidenceResolver.kt`
- **Purpose:** Complete evidence chain resolution
- **Chain:** Evidence → Citation → Source → Provenance
- **Never invents data** — missing links return null

### EvidenceEngine
- **Path:** `domain/EvidenceEngine.kt`
- **Purpose:** Resolve evidence traces, separate supporting/contradicting, score confidence
- **Scoring:** `EvidenceScoringConfig` (baseSupport, levelBonuses, contradictionPenalty)
- **R1/R2/R4 intact:** Deterministic Kotlin, no LLM

### DifferentialEngine
- **Path:** `domain/DifferentialEngine.kt`
- **Purpose:** Generate candidates, score, rank, identify contradictions
- **Scoring:** `DifferentialScoringConfig` (matchWeight, evidenceWeight, relationConfidence, sourceQuality, contradictionPenalty)
- **Output:** `DifferentialResult` with ranked candidates

### MissingDataEngine
- **Path:** `domain/MissingDataEngine.kt`
- **Purpose:** Identify what information would help differentiate candidates
- **Checks:** tongue, pulse, Ba Gang, symptoms, history, etiology
- **Graph analysis:** Finds differentiating entities between top candidates
- **Priority:** Sorted by importance (1 = most important)

### ClinicalIntelligenceEngine
- **Path:** `domain/ClinicalIntelligenceEngine.kt`
- **Purpose:** Orchestrates the full pipeline
- **Input:** ClinicalObservation
- **Output:** ClinicalIntelligenceResult

### RunClinicalIntelligenceUseCase
- **Path:** `domain/RunClinicalIntelligenceUseCase.kt`
- **Purpose:** Maps MtcAssessment → ClinicalObservation → Result
- **Architecture:** UI → UseCase → Engine → Knowledge Core

## Guardrails

1. **LLM is NOT needed for ranking** — all scoring is deterministic
2. **LLM does NOT create evidence** — only resolves from Knowledge Core
3. **LLM does NOT alter scores** — EvidenceScoringConfig is fixed
4. **LLM does NOT create relations** — graph is read-only
5. **Result is clinical decision support** — never "You have X", always "X is a compatible hypothesis"

## Knowledge Version

- All results include `knowledgeVersion` for reproducibility
- Same version + same input = same output (deterministic)

## Auditability

Every result contains:
- `timestamp` — when it was generated
- `engineVersion` — engine version
- `knowledgeVersion` — knowledge core version
- `rankedHypotheses` — candidate IDs + scores
- `supportingEvidence` / `contradictingEvidence` — evidence traces
- `reasoningPaths` — graph paths
- `missingInformation` — what's missing

## Test Coverage

- **Graph tests:** 15 (chain, fan-out, cycle, path, filters, ordering)
- **Evidence tests:** 8 (scoring, contradictions, bounds, config)
- **Evidence Resolver tests:** 16 (full chain, partial, missing, batch, provenance)
- **Differential tests:** 7 (candidates, ranking, missing data, empty, deterministic)
- **Missing Data tests:** 10 (complete, missing tongue/pulse/BaGang, priority, single candidate)
- **E2E tests:** 4 (liver qi stagnation, incomplete, empty, deterministic)
- **Regression tests:** 4 (ranking stability, confidence stability, missing data stability, contradiction penalty)
- **Benchmark tests:** 11 (graph 1/3/5 depth, evidence 1/10/50/100, differential small/medium/large, E2E typical/complex)
- **Full suite:** 389 tests, 0 failures

## Performance

All components perform sub-millisecond on JVM (Robolectric). Device-level validation pending.

## Graph Differentiation

Implemented in `MissingDataEngine.analyzeGraphDifferentiation()` — finds entities that appear in some candidates but not others, identifying features that could differentiate top candidates. This is NOT deferred to Phase 4.
