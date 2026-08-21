# ADR-007-005: TENANCY MODEL

## Status

ACCEPTED

## Context

BioAcupunt is a multi-tenant SaaS platform for healthcare. Tenant isolation is critical for:
- LGPD compliance (Brazilian data protection law)
- Healthcare data security
- CRM data isolation
- Knowledge/AI isolation

## Decision

Use **schema-per-tenant** as the primary tenancy model.

## Evidence

- Twenty already uses schema-per-workspace (proven in production)
- PostgreSQL schemas provide strong isolation
- Compatible with multi-tenant SaaS patterns
- Supports tenant provisioning/deprovisioning
- Sensitive analysis shows best score across criteria

## Alternatives Considered

| Model | Isolation | LGPD | Performance | Cost | Verdict |
|-------|-----------|------|-------------|------|---------|
| Column-per-tenant | MEDIUM | LOW | HIGH | LOW | REJECTED |
| RLS | HIGH | MEDIUM | MEDIUM | LOW | REJECTED |
| **Schema-per-tenant** | **HIGH** | **HIGH** | **MEDIUM** | **MEDIUM** | **ACCEPTED** |
| DB-per-tenant | VERY HIGH | VERY HIGH | LOW | HIGH | REJECTED |
| Hybrid | HIGH | HIGH | MEDIUM | MEDIUM | DEFERRED |

## Consequences

### Positive
- Strong isolation (PostgreSQL schema)
- LGPD compliant
- Compatible with Twenty's native model
- Supports tenant provisioning/deprovisioning
- Clean separation of data

### Negative
- Schema proliferation (many schemas)
- Connection pooling complexity
- Migration complexity (per-schema)
- Development complexity (schema switching)

## Implementation

```
core schema (shared)
  └── workspace table (tenant definition)
  └── user table (shared users)
  └── user_workspace table (junction)

workspace_<id> schema (per-tenant)
  └── person table
  └── company table
  └── opportunity table
  └── task table
  └── note table
  └── ... all CRM entities
  └── ... all metadata
  └── ... all views/filters/sorts
```

## Risks

- Schema count growth with many tenants
- Connection pool exhaustion
- Migration complexity
- Backup/restore complexity

## Reversibility

MEDIUM — changing tenancy model requires significant data migration.
