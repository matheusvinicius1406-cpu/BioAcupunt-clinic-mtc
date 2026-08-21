# 30 — CACHE ISOLATION

## Twenty's Cache Architecture

```
WorkspaceCacheStorageService
  ├── Redis-backed
  ├── Workspace-scoped keys
  └── Invalidation by workspace
```

### Cache Key Model

Twenty uses workspace-scoped cache keys:
```
workspace:<workspace_id>:<key>
```

### Isolation Requirements

| Requirement | Status | Notes |
|------------|--------|-------|
| Tenant-scoped keys | ENFORCED | workspace_id in key |
| Cross-tenant cache access | ENFORCED | key includes workspace_id |
| Invalidation scope | ENFORCED | invalidates by workspace |
| TTL management | IMPLEMENTED | per-key TTL |

### Cache Collision Vector

```
ATTACK: Tenant A reads Tenant B's cached data
  → Cache key = workspace:<workspace_id>:<key>
  → If workspace_id doesn't match → CACHE MISS → fresh query
  → Fresh query is tenant-scoped → SAFE
```

**The key design prevents cross-tenant cache access.**

### Decision: REUSE Twenty's Cache Architecture

- Workspace-scoped by design
- Redis-backed (standard)
- Clean invalidation model

**Adaptation needed:**
- Ensure workspace_id is always included in cache keys
- Test cache isolation with concurrent tenants
- Add cache metrics (hit rate, miss rate per tenant)

### Confidence: HIGH
