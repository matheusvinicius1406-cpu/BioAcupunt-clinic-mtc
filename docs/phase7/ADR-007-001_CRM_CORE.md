# ADR-007-001: CRM CORE

## Status

ACCEPTED

## Context

BioAcupunt needs a CRM system for managing patients, organizations, opportunities, tasks, and notes. Twenty provides a mature CRM implementation that can be strategically reused.

## Decision

Extract Twenty's CRM entity definitions (Person, Company, Opportunity, Task, Note, Attachment) and supporting infrastructure (metadata engine, record CRUD, search, views, permissions) into BioAcupunt.

## Evidence

- Twenty's CRM entities are well-isolated (1-8 files each)
- Metadata engine is proven in production
- Schema-per-tenant provides strong isolation
- No Enterprise dependencies in CRM entity modules

## Consequences

### Positive
- Reuse mature, tested CRM implementation
- Strong metadata-driven architecture
- Schema-per-tenant isolation
- Extensible via custom fields/objects

### Negative
- AGPL license requires source availability
- Upstream dependency for updates
- Need anti-corruption layers for BioAcupunt integration
- Metadata engine complexity

## Alternatives Considered

1. **Build from scratch** — Rejected (too much work, reinventing wheel)
2. **Use existing BioAcupunt CRM** — Rejected (limited functionality)
3. **Integrate Twenty as external service** — Rejected (adds complexity, breaks offline-first)

## Risks

- Upstream Twenty changes may break extraction
- License changes could affect usage
- Metadata engine complexity may cause issues
- Migration from old CRM may cause data loss

## Reversibility

HIGH — extraction can be reverted by removing extracted code and restoring original CRM.
