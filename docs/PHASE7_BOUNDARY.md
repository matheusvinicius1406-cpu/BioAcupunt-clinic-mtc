# Phase 7 — Definitive Boundary: BioAcupunt × Twenty

**Decision date:** 2026-08-22
**Status:** ACTIVE — replaces all previous CRM architecture decisions

---

## Source of Truth Matrix

| Entity | Source of Truth | Stored Where | Notes |
|--------|----------------|-------------|-------|
| Patient | BIOACUPUNT | Room (Android) + PostgreSQL (backend) | Clinical truth, never duplicated |
| Encounter | BIOACUPUNT | Room (Android) | Clinical events |
| MtcAssessment | BIOACUPUNT | Room (Android) | Clinical assessment |
| ClinicalNote | BIOACUPUNT | Room (Android) | SOAP/MTC notes |
| Person | TWENTY | Twenty PostgreSQL | CRM identity |
| Organization | TWENTY | Twenty PostgreSQL | CRM organization |
| Lead | TWENTY | Twenty PostgreSQL | CRM pipeline entry |
| Opportunity | TWENTY | Twenty PostgreSQL | CRM deal |
| Task | TWENTY | Twenty PostgreSQL | CRM task |
| Activity | TWENTY | Twenty PostgreSQL | CRM activity log |
| Note (CRM) | TWENTY | Twenty PostgreSQL | CRM note (not clinical) |
| Tag | TWENTY | Twenty PostgreSQL | CRM label |
| Pipeline | TWENTY | Twenty PostgreSQL | CRM pipeline |
| Workflow | TWENTY | Twenty PostgreSQL | CRM automation |
| CrmIdentityMap | BRIDGE | PostgreSQL (backend) + Room (Android) | Maps Patient ↔ Person |
| Patient360Context | DERIVED | Computed on demand | Aggregates Clinical + CRM |
| UnifiedTimeline | DERIVED | Computed on demand | Aggregates all events |

## Rules

1. **Never use CrmPatient as proxy for CRM entities.** CrmPatient is a legacy local-only
   table. It may contain operational metadata (pipeline stage, NPS, referral source) but
   is NOT the source of truth for tasks, leads, activities, or audit trail.

2. **Twenty is the CRM source of truth.** When Twenty is not available, CRM endpoints
   must return 503 Service Unavailable, NOT fake data derived from patients.

3. **Clinical data never enters Twenty.** Patient, Encounter, MtcAssessment, ClinicalNote,
   Knowledge Graph — all stay in BioAcupunt. Twenty receives only CRM-personal data
   (name, email, phone, organization) via the IdentityMap bridge.

4. **IdentityMap is the single bridge.** Every cross-system reference goes through
   CrmIdentityMap. No direct foreign keys between BioAcupunt and Twenty tables.

5. **Derivations are computed, not stored.** Patient360Context and UnifiedTimeline are
   built on demand from both sources. They are never persisted as source data.

## Endpoint Behavior When Twenty Is Offline

| Endpoint | Behavior |
|----------|----------|
| `GET /crm` (list patients) | ✅ Returns CrmPatient data (local) |
| `GET /crm/pipeline` | ✅ Returns CrmPatient pipeline summary (local) |
| `GET /crm/dashboard` | ⚠️ Returns partial data (local patients only) |
| `GET /crm/leads` | 503 if Twenty offline; real data from Twenty when online |
| `GET /crm/tasks` | 503 if Twenty offline; real data from Twenty when online |
| `GET /crm/activities` | 503 if Twenty offline; real data from Twenty when online |
| `GET /crm/search` | ⚠️ Searches CrmPatient locally; Twenty search when online |
| `GET /crm/timeline` | ⚠️ Returns clinical events only; full when Twenty online |
| `GET /crm/referrals` | 503 if Twenty offline; real data from Twenty when online |
| `GET /crm/audit` | 503 if Twenty offline; real audit trail when online |

## Migration Order (when Twenty is available)

1. Person (BioAcupunt Patient → Twenty Person via IdentityMap)
2. Organization
3. Lead
4. Task
5. Activity
6. Note
7. Tag
8. Pipeline
9. Workflow

## What This Means for the Backend

The backend CRM repository must become a **CRM Gateway** that:
- Knows whether Twenty is available (health check on startup/periodic)
- Routes requests to Twenty when available
- Returns structured 503 with retry-after when not
- Never fabricates data
