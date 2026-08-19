# BIOACUPUNT — Evidence System

## Overview

The Evidence System provides traceable, provenance-aware evidence resolution for clinical responses.

## Chain of Evidence

```
Claim
  ↓
Evidence (KnowledgeEvidence)
  ↓
Citation (KnowledgeCitation)
  ↓
Source (KnowledgeSource)
  ↓
Provenance (KnowledgeProvenance)
```

Each link in the chain is nullable — if a link is missing, the chain degrades gracefully.

## Components

### EvidenceResolver

Resolves evidence IDs into full evidence traces:

```kotlin
class EvidenceResolver(dao: KnowledgeCoreDao) {
    suspend fun resolveEvidence(evidenceId: String): ResolvedEvidence?
    suspend fun resolveEvidenceBatch(evidenceIds: List<String>): List<ResolvedEvidence>
    suspend fun resolveProvenance(entityId: String): List<ResolvedProvenance>
}
```

### EvidenceResolutionService

Wraps EvidenceResolver for the RAG pipeline:

```kotlin
class EvidenceResolutionService(evidenceResolver: EvidenceResolver) {
    suspend fun resolve(evidenceIds: List<String>): List<ResolvedEvidence>
    suspend fun resolveForHit(hit: RetrievalHit): ResolvedEvidence?
}
```

### EvidenceExplorer

Allows navigation through evidence chains:

```kotlin
class EvidenceExplorer(evidenceResolutionService, graphRepository) {
    suspend fun explore(response: GroundedResponse): EvidenceChain
}
```

Output: `EvidenceChain(claim, evidence, relatedEntities, provenance)`

### ResponseValidator

Validates LLM responses against context:

```kotlin
class ResponseValidator {
    fun validate(response: GroundedResponse, context: StructuredContext): ValidationReport
    fun qualifyClaims(response: GroundedResponse, unsupportedClaims: List<String>): GroundedResponse
}
```

Checks:
- Citations exist when evidence was provided
- Evidence exists when answer is generated
- Unsupported claims detected (heuristic: term overlap < 30%)
- Knowledge version specified
- Confidence level appropriate

Policy for unsupported claims: **QUALIFY** (mark as uncertain, don't reject entire response).

## Evidence Levels

| Level | Description | Bonus |
|---|---|---|
| TRADITION | Traditional Chinese medicine texts | 0.05 |
| MODERN_LITERATURE | Modern published literature | 0.10 |
| CLINICAL_EVIDENCE | Clinical research evidence | 0.20 |

## UI Display

The Evidence Explorer Panel shows:
- Citations (numbered list)
- Evidence IDs
- Knowledge version
- Source information (when available)

Missing fields display "Não informado" — never invented.
