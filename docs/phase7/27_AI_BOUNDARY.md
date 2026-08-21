# 27 — AI BOUNDARY

## Rule

**AI is a consumer, never source of truth.**

## Allowed AI Interactions

```
AI → CRM (ALLOWED)
  ├── Read Person data (for context)
  ├── Read Organization data (for context)
  ├── Read Tasks (for context)
  ├── Read Notes (for context)
  └── Suggest actions (never auto-execute)

AI → Healthcare (ALLOWED)
  ├── Read Patient data (for context, with authorization)
  ├── Read Encounters (for context)
  ├── Suggest clinical insights (never auto-save)
  └── Answer clinical questions (with R2 evidence gate)

AI → Knowledge (ALLOWED)
  ├── Query knowledge graph
  ├── Search articles
  ├── Retrieve evidence
  └── Generate explanations (with evidence)
```

## Forbidden AI Interactions

```
AI → CRM (FORBIDDEN)
  ├── Auto-create records without approval
  ├── Auto-modify records without approval
  ├── Bypass authorization
  ├── Cross tenant boundaries
  └── Direct database mutation

AI → Healthcare (FORBIDDEN)
  ├── Auto-save clinical assessments
  ├── Bypass safety engine
  ├── Auto-approve treatments
  ├── Direct clinical database mutation
  └── Cross tenant boundaries
```

## Tenant Propagation for AI

```
Request → AI Context → Retrieval → Generation
    ↓          ↓            ↓            ↓
Tenant ID  Tenant ID   Tenant ID   Tenant ID
```

AI must propagate tenant context through:
1. Request context (JWT → AI service)
2. Retrieval context (query → Knowledge graph)
3. Generation context (prompt → model)
4. Response context (answer → API)

## AI Tenant Isolation Proof Required

```
Tenant A AI query
    ↓
Knowledge retrieval
    ↓
Only Tenant A evidence
    ↓
Response scoped to Tenant A
```

**Must verify:** AI cannot access Tenant B's knowledge, clinical data, or CRM data.

### Confidence: MEDIUM

The AI boundary is well-defined conceptually but needs runtime verification. The main risk is accidental tenant leakage through:
- Shared embedding space
- Cross-tenant cache
- Background job context loss
