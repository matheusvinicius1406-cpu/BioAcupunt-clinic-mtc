# Phase 7E — Reachability Analysis

**Date:** 2026-08-21
**Method:** Entry point tracing through import graph
**Confidence:** SUPPORTED (static analysis)

---

## 1. CRM Entry Points

### 1.1 GraphQL Resolvers (Primary API)

```
PersonResolver         → PersonService
CompanyResolver        → CompanyService
OpportunityResolver    → OpportunityService
TaskResolver           → TaskService
NoteResolver           → NoteService
TimelineActivityResolver → TimelineActivityService
AttachmentResolver     → AttachmentService
WorkflowResolver       → WorkflowService
DashboardResolver      → DashboardService
ViewResolver           → ViewService
```

### 1.2 REST Endpoints (Compatibility)

```
/api/v1/people         → PersonService
/api/v1/companies      → CompanyService
/api/v1/opportunities  → OpportunityService
/api/v1/tasks          → TaskService
/api/v1/notes          → NoteService
/api/v1/search         → SearchService
```

### 1.3 Background Workers

```
TimelineJobModule      → UpsertTimelineActivityFromInternalEvent
WorkflowTriggerModule  → WorkflowTriggerJob
WorkflowRunnerModule   → WorkflowRunJob
DashboardSyncModule    → DashboardSyncService
```

---

## 2. Reachability from PersonResolver

```
PersonResolver (depth 0)
  ↓
PersonService (depth 1)
  ↓
TwentyORMModule (depth 2)
  ↓
WorkspaceDatasource (depth 3)
  ↓
PostgreSQL (depth 4)
  ↓
Redis (depth 5)

ALSO REACHABLE:
  → CompanyRepository (depth 2)
  → OpportunityRepository (depth 2)
  → TaskTargetRepository (depth 2)
  → NoteTargetRepository (depth 2)
  → AttachmentRepository (depth 2)
  → TimelineActivityRepository (depth 2)
  → MessageParticipantRepository (depth 2)
  → CalendarEventParticipantRepository (depth 2)
```

### 2.1 Reachable Services

| Service | Reachable | Why |
|---------|-----------|-----|
| PersonService | ✅ Direct | Entry point |
| CompanyService | ✅ Via FK | Person → Company |
| OpportunityService | ✅ Via FK | Person → Opportunity |
| TaskService | ✅ Via junction | Person → TaskTarget → Task |
| NoteService | ✅ Via junction | Person → NoteTarget → Note |
| TimelineActivityService | ✅ Via FK | Person → TimelineActivity |
| AttachmentService | ✅ Via FK | Person → Attachment |
| SearchService | ✅ Via metadata | Person metadata → Search |
| ViewService | ✅ Via metadata | Person metadata → View |
| WorkflowService | ⚠️ Via timeline | Person → Timeline → Workflow |
| DashboardService | ⚠️ Via timeline | Person → Timeline → Dashboard |

### 2.2 Unreachable from Person

| Service | Reachable | Why |
|---------|-----------|-----|
| BillingService | ❌ Excluded | Commercial |
| SSOSessionService | ❌ Excluded | Commercial |
| ImpersonationService | ❌ Excluded | Commercial |
| CloudflareService | ❌ Excluded | Commercial |
| DnsManagerService | ❌ Excluded | Commercial |
| CodeInterpreterService | ❌ Excluded | Commercial |
| GeoMapService | ❌ Excluded | Commercial |
| LabService | ❌ Excluded | Commercial |

---

## 3. Reachability from WorkflowResolver

```
WorkflowResolver (depth 0)
  ↓
WorkflowService (depth 1)
  ↓
BillingService (depth 2) ← COMMERCIAL ⚠️
  ↓
BillingUsageService (depth 3) ← COMMERCIAL ⚠️
  ↓
StripeSDK (depth 4) ← COMMERCIAL ⚠️
```

### 3.1 Critical Finding

**Workflow is CONDITIONALLY REACHABLE to commercial code.**

The workflow module imports:
- `BillingModule` (direct import)
- `BillingService` (used in workflow-executor)
- `BillingUsageService` (used in workflow-runner)
- `NO_BILLING_SUBSCRIPTION` constant

**To make workflow reachable without commercial code, we must:**
1. Remove billing checks from workflow-executor
2. Remove billing checks from workflow-runner
3. Remove BillingModule import from workflow modules
4. Provide alternative usage tracking (or none)

---

## 4. Reachability Classification

### 4.1 REACHABLE (Direct)

| Module | Entry Point | Depth |
|--------|-------------|-------|
| person | PersonResolver | 0 |
| company | CompanyResolver | 0 |
| opportunity | OpportunityResolver | 0 |
| task | TaskResolver | 0 |
| note | NoteResolver | 0 |
| timeline | TimelineActivityResolver | 0 |
| attachment | AttachmentResolver | 0 |
| dashboard | DashboardResolver | 0 |
| view | ViewResolver | 0 |

### 4.2 CONDITIONALLY REACHABLE

| Module | Condition | Required Adaptation |
|--------|-----------|-------------------|
| workflow | Remove billing checks | Remove 3 billing imports |
| calendar | Remove billing check | Remove 1 billing import |
| messaging | No billing deps | None needed |

### 4.3 UNREACHABLE (Excluded)

| Module | Reason |
|--------|--------|
| billing | Commercial |
| billing-webhook | Commercial |
| usage | Commercial |
| sso | Commercial |
| enterprise | Commercial |
| impersonation | Commercial |
| cloudflare | Commercial |
| dns-manager | Commercial |
| emailing-domain | Commercial |
| dpa | Commercial |
| code-interpreter | Commercial |
| geo-map | Commercial |
| imap-smtp-caldav | Commercial |
| lab | Commercial |
| admin-panel | Commercial |
| open-api | Commercial |
| public-domain | Commercial |
| approved-access-domain | Commercial |

---

## 5. Reachability Summary

| Category | Count | Action |
|----------|-------|--------|
| REACHABLE | 9 | Use directly |
| CONDITIONALLY REACHABLE | 3 | Adapt (remove billing) |
| UNREACHABLE | 18 | Exclude entirely |
| **TOTAL** | **30** | — |

---

## 6. Key Findings

1. **9 CRM modules are directly reachable** without any commercial code
2. **3 modules need adaptation** (workflow, calendar, messaging) to remove billing dependencies
3. **18 modules are unreachable** and should be excluded
4. **Workflow is the most complex** to adapt due to deep billing integration
5. **Person is the highest fan-in entity** (10 modules depend on it)
6. **twenty-orm is the critical infrastructure bottleneck** (everything depends on it)

**Confidence:** SUPPORTED (based on static import analysis, not runtime tracing)
