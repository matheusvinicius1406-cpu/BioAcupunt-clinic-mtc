# 19 — SEARCH KERNEL

## Discovery

### Twenty's Search Architecture

```
SearchModule
├── SearchResolver     ← GraphQL endpoint
├── SearchService      ← orchestrates search
└── depends on:
    ├── FileModule                    ← file search
    └── FlatEntityMapsCacheModule     ← metadata cache
```

### Search Service Analysis

Twenty's search is **metadata-driven**:
- `SearchService` reads `SearchFieldMetadata` to know which fields are searchable
- It queries workspace-specific schemas dynamically
- It supports full-text search across multiple object types
- Results are scoped to the workspace (tenant)

### CRM Search Requirements

| Capability | Required? | Source |
|-----------|-----------|--------|
| Record search (Person, Company, etc.) | YES | Core CRM |
| Full-text search | YES | User expectation |
| Field-level search config | YES | Metadata-driven |
| Cross-object search | YES | UX requirement |
| Fuzzy matching | DESIRABLE | UX improvement |
| Search analytics | DEFER | Later |
| AI-powered search | DEFER | Later |

### Key Finding: Search ≠ Knowledge Search

```
CRM Search (Twenty-derived)
  → searches Person, Company, Opportunity, Task, Note
  → metadata-driven (which fields, which objects)
  → workspace-scoped (tenant isolation)
  → PostgreSQL full-text (per-schema)

Knowledge Search (BioAcupunt existing)
  → searches articles, evidence, clinical knowledge
  → FTS4 on Android, PostgreSQL FTS on backend
  → tenant-scoped
  → domain-specific (MTC terminology)
```

These are **separate search systems** with different:
- Data sources
- Search strategies
- Relevance models
- UI presentation

**Rule: CRM search must NOT import Knowledge search infrastructure.**

### Minimum CRM Search Runtime

1. `SearchService` (orchestration)
2. `SearchFieldMetadata` (which fields are searchable)
3. PostgreSQL full-text search (per-schema)
4. Workspace context (tenant isolation)

### Reuse Decision: REUSE (with adaptation)

Twenty's search module is clean:
- No commercial dependencies
- No enterprise dependencies
- Metadata-driven (aligns with our architecture)
- Tenant-scoped (aligns with our tenancy model)

**Adaptation needed:**
- Replace any internal Twenty-specific service references
- Ensure tenant isolation is enforced
- Add search analytics capability (deferred)

### Confidence: HIGH
