# 31 — WORKER/JOB ISOLATION

## Twenty's Job Architecture

```
MessageQueueModule
  ├── Bull (Redis-backed)
  ├── Workspace-scoped jobs
  └── Job types:
      ├── Workflow jobs
      ├── Calendar sync jobs
      ├── Messaging sync jobs
      ├── File cleanup jobs
      └── Scheduled maintenance
```

### Tenant Context Propagation

```
Request → Job Enqueue → Worker → Repository
   ↓          ↓           ↓          ↓
Tenant ID  Tenant ID  Tenant ID  Tenant ID
```

### Isolation Requirements

| Requirement | Status | Notes |
|------------|--------|-------|
| Tenant context in job payload | NEEDED | Must verify |
| Worker resolves tenant from payload | NEEDED | Must verify |
| Worker uses tenant-scoped repository | NEEDED | Must verify |
| Cross-tenant job execution | MUST NOT | Critical |

### Job Context Loss Vector

```
RISK: Job enqueued with Tenant A context
  → Job serialized to Redis
  → Worker picks up job
  → Tenant context lost during deserialization
  → Worker uses default tenant → CROSS-TENANT ACCESS
```

**Mitigation:** Job payload must include tenant_id, and worker must restore tenant context before execution.

### Decision: REUSE Twenty's MessageQueueModule

- Bull/Redis is standard
- Workspace-scoped job types
- Clean job/worker model

**Adaptation needed:**
- Ensure tenant_id is in all job payloads
- Test tenant context propagation through serialization
- Add job-level tenant validation

### Confidence: MEDIUM

The architecture is sound, but runtime verification is needed to ensure tenant context survives serialization/deserialization.
