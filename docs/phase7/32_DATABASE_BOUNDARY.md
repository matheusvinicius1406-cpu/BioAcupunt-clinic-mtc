# 32 — DATABASE BOUNDARY

## Twenty's Database Architecture

```
PostgreSQL
├── core schema (shared)
│   ├── workspace table
│   ├── user table
│   ├── user_workspace table
│   └── ... platform tables
│
├── workspace_<id> schema (per-tenant)
│   ├── person table
│   ├── company table
│   ├── opportunity table
│   ├── task table
│   ├── note table
│   ├── ... CRM entities
│   └── ... metadata tables
│
└── metadata schema (shared)
    ├── object_metadata
    ├── field_metadata
    └── ... metadata definitions
```

### Schema Routing

```
Request → Auth → Workspace Resolution → Schema Switch → Query
                (JWT → workspace_id)   (SET search_path)
```

### Isolation Requirements

| Requirement | Status | Notes |
|------------|--------|-------|
| Schema-per-tenant | ENFORCED | PostgreSQL schemas |
| Cross-tenant query prevention | ENFORCED | search_path |
| Raw SQL bypass risk | MEDIUM | Must verify |
| Migration isolation | ENFORCED | per-schema migrations |
| Connection pooling | NEEDED | PgBouncer/HikariCP |

### Raw SQL Bypass Vector

```
RISK: Raw SQL query without search_path
  → Query executes in wrong schema
  → Cross-tenant data access

MITIGATION:
  → All queries must go through ORM/repository
  → Raw SQL must include schema qualification
  → Architecture fitness function: no raw SQL outside repositories
```

### Decision: KEEP Schema-Per-Tenant

- Already Twenty's native model
- Strong isolation (PostgreSQL schema)
- Compatible with multi-tenant SaaS
- Supports tenant provisioning/deprovisioning

**Adaptation needed:**
- Ensure all queries go through schema-aware repositories
- Test cross-tenant isolation
- Add connection pooling with schema routing
- Validate no raw SQL bypasses

### Confidence: HIGH

Schema-per-tenant is the strongest isolation model that scales. The main risk is raw SQL bypass, which can be enforced through architecture fitness functions.
