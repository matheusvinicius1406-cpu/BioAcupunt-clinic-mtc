# 26 — KNOWLEDGE BOUNDARY

## Rule

**Knowledge owns clinical evidence. CRM does not.**

## Current BioAcupunt Knowledge Core

```
KnowledgeGraphRepository      ← graph traversal
EvidenceResolver              ← evidence traces
DifferentialEngine            ← pattern scoring
MissingDataEngine             ← gap identification
ClinicalIntelligenceEngine    ← orchestrator
ClinicalCopilotEngine         ← conversational AI
EvidenceGate                  ← R2 enforcement
AskLibraryUseCase             ← library search
```

## Allowed Relationships

```
CRM → Knowledge (ALLOWED)
  ├── Search query → Knowledge search (via API contract)
  ├── Patient360 → Evidence summary (read-only)
  └── Copilot → Knowledge query (via AI boundary)

Knowledge → CRM (ALLOWED)
  └── None currently (Knowledge is independent)
```

## Forbidden Relationships

```
CRM → Knowledge (FORBIDDEN)
  ├── CRM persistence of knowledge data
  ├── CRM modification of knowledge graph
  ├── CRM access to EvidenceResolver internals
  ├── CRM access to DifferentialEngine internals
  └── CRM direct access to knowledge database

Knowledge → CRM (FORBIDDEN)
  ├── Knowledge modification of CRM records
  └── Knowledge direct access to CRM database
```

## Key Insight

Knowledge and CRM are **independent bounded contexts** that happen to coexist in the same platform. They share:
- Tenant context (same database, different schemas)
- Authentication (same user system)
- Authorization (same permission model)

They do NOT share:
- Data models
- Business logic
- Persistence
- Search infrastructure

## Integration Points

| Integration | Mechanism | Direction |
|------------|-----------|-----------|
| Patient asks about treatment | CRM → AI → Knowledge | Read-only |
| Knowledge suggests protocols | Knowledge → AI → CRM | Read-only (suggestion) |
| Clinical timeline | Healthcare → Timeline | Event-based |
| Search results | CRM search ≠ Knowledge search | Separate systems |

### Confidence: HIGH
