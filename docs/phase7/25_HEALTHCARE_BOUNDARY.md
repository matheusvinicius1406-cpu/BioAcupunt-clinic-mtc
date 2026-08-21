# 25 — HEALTHCARE BOUNDARY

## Rule

**Healthcare owns clinical truth. CRM does not.**

## Allowed Relationships

```
CRM → Healthcare (ALLOWED)
  ├── Person ID → PatientProfile lookup (read-only)
  ├── Organization → Clinic reference (read-only)
  ├── Timeline → Clinical events (read-only, via event bus)
  └── Patient360 → Aggregated view (read-only, via API)

Healthcare → CRM (ALLOWED)
  ├── Patient → Person reference (read-only)
  └── Clinical events → CRM timeline (via event bus)
```

## Forbidden Relationships

```
CRM → Healthcare (FORBIDDEN)
  ├── CRM persistence of clinical data
  ├── CRM modification of clinical records
  ├── CRM access to SafetyEngine internals
  ├── CRM access to ClinicalIntelligence internals
  └── CRM direct access to clinical database tables

Healthcare → CRM (FORBIDDEN)
  ├── Healthcare modification of CRM records
  └── Healthcare direct access to CRM database tables
```

## Boundary Enforcement

| Mechanism | Status | Notes |
|-----------|--------|-------|
| Package-level import restrictions | DOCUMENTED | Needs implementation |
| Architecture fitness functions | PLANNED | Need to create tests |
| API contract enforcement | PLANNED | Need OpenAPI specs |
| Database schema isolation | ENFORCED | Schema-per-tenant |
| Runtime validation | NEEDED | Need runtime checks |

## Clinical Data Classification

| Data Type | Owner | CRM Access |
|-----------|-------|-----------|
| Patient demographics | Healthcare | Read-only via PatientProfile |
| Encounter records | Healthcare | No access |
| Clinical notes | Healthcare | No access |
| MTC assessments | Healthcare | No access |
| Safety flags | Healthcare | No access |
| Clinical intelligence | Healthcare | No access |
| Person (CRM) | CRM | Full access |
| Organization (CRM) | CRM | Full access |
| Opportunity (CRM) | CRM | Full access |
| Task (CRM) | CRM | Full access |
| Note (CRM) | CRM | Full access |

## Patient360 Integration

Patient360 is a READ-ONLY aggregation that combines:
- CRM data (Person, Organization, Tasks, Notes)
- Healthcare data (Patient, Encounters, Assessments)
- Timeline (clinical + CRM events)
- Care journey (milestones, referrals)

Patient360 must NOT:
- Write clinical data
- Modify CRM data based on clinical rules
- Bypass authorization
- Cross tenant boundaries

### Confidence: HIGH

The boundary is clear and well-defined. The main risk is accidental coupling through shared infrastructure (database, cache, events). Platform ports help enforce this.
