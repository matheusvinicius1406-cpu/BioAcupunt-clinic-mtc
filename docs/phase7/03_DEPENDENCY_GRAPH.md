# Phase 7C — Dependency Graph

**Date:** 2026-08-21
**Method:** Import analysis (grep-based, verified against source)
**Confidence:** SUPPORTED (based on static import analysis)

---

## 1. CRM Module Dependencies (Direct Imports)

### 1.1 Entity-Level Dependencies

| Module | Imports From (engine) | Imports From (modules) |
|--------|----------------------|----------------------|
| **person** | twenty-orm, workspace-manager | company, opportunity, task-target, note-target, attachment, timeline-activity, message-participant, calendar-event-participant, emailing |
| **company** | workspace-manager | person, opportunity, task-target, note-target, attachment, timeline-activity, workspace-member |
| **opportunity** | twenty-orm, workspace-manager | person, company, task-target, note-target, attachment, timeline-activity, workspace-member |
| **task** | twenty-orm, auth, workspace, graphql | person, company, opportunity, task-target, attachment, timeline-activity, workspace-member |
| **note** | twenty-orm, auth, workspace, graphql | person, company, opportunity, note-target, attachment, timeline-activity |
| **timeline** | twenty-orm, feature-flag, message-queue, event-emitter, metadata-modules, object-metadata-repository, workspace-event-emitter | person, company, opportunity, task, note, workflow, dashboard, emailing, workspace-member |
| **attachment** | twenty-orm, workspace-manager | person, company, opportunity, task, note, dashboard, workflow, workspace-member |
| **workflow** | twenty-orm, auth, graphql, cache-storage, billing, feature-flag, message-queue, cron, email, logic-function, application, event-logs | timeline-activity, record-crud, workspace-member |
| **dashboard** | twenty-orm, auth, graphql, record-crud, record-position, actor, application, exception-handler, tool-provider, metadata-modules | person, company, opportunity, task, note, timeline-activity, workspace-member |
| **workspace-member** | twenty-orm, workspace-manager | person, company, opportunity, task, timeline-activity |

### 1.2 Critical Dependencies (Bottlenecks)

```
twenty-orm ← EVERYTHING depends on this
workspace-manager ← entity definitions
auth/types ← task, note, dashboard
graphql ← task, note, dashboard, workflow
record-crud ← dashboard, workflow
feature-flag ← timeline, workflow
message-queue ← timeline, workflow
event-emitter ← timeline
cache-storage ← workflow
billing ← workflow (COMMERCIAL ⚠️)
```

---

## 2. Engine Core Module Dependencies

### 2.1 Modules with Billing Dependencies (21 modules)

```
admin-panel, application, auth, billing-webhook, billing,
cache-storage, client-config, domain, dpa, email, emailing-domain,
enterprise, event-logs, i18n, logic-function, message-queue,
onboarding, sso, twenty-config, usage, workspace
```

### 2.2 Modules with SSO Dependencies (28 modules)

```
application, auth, billing, calendar, client-config, code-interpreter,
dpa, email, emailing-domain, enterprise, event-logs, file, i18n, jwt,
logic-function, message-queue, messaging, onboarding, public-domain,
record-crud, sdk-client, sso, tool, twenty-config, upgrade,
user-session, user-workspace, user, workspace
```

### 2.3 CoreEngineModule Imports (79 total)

**Commercial/Enterprise (21):**
- admin-panel, approved-access-domain, billing, billing-webhook, cloudflare, code-interpreter, dns-manager, dpa, emailing-domain, geo-map, impersonation, lab, open-api, public-domain, sso, usage + 5 more

**Non-Commercial (58):**
- actor, api-key, app-token, application, auth, cache-storage, calendar, captcha, client-config, cron, email, environment, event-emitter, event-logs, exception-handler, feature-flag, file, file-storage, graphql, guard-redirect, health, i18n, jwt, key-value-pair, logger, logic-function, message-queue, messaging, metrics, onboarding, record-crud, record-position, record-transformer, redis-client, related-person-ids, search, secret-encryption, secure-http-client, sentry, server-route-trigger, session-storage, sql-sanitization, telemetry, throttler, tool, tool-provider, twenty-config, two-factor-authentication, upgrade, user, user-session, user-workspace, well-known, workflow, workspace, workspace-invitation + metadata modules

---

## 3. Transitive Dependency Chain

### 3.1 CRM → Engine Dependencies

```
CRM Module
  ↓
twenty-orm (ORM layer)
  ↓
workspace-datasource (DB connections)
  ↓
workspace-cache (Metadata cache)
  ↓
workspace-manager (Workspace lifecycle)
  ↓
metadata-modules (Object/field definitions)
  ↓
graphql (API layer)
  ↓
auth (Authentication)
  ↓
workspace (Multi-tenancy)
  ↓
message-queue (Job processing)
  ↓
cache-storage (Caching)
  ↓
redis-client (Redis)
  ↓
postgresql (Database)
```

### 3.2 Hidden Dependencies (Through CoreEngineModule)

```
CRM Module
  ↓
CoreEngineModule (monolithic)
  ↓
BillingModule (COMMERCIAL ⚠️)
  ↓
BillingWebhookModule (COMMERCIAL ⚠️)
  ↓
UsageModule (COMMERCIAL ⚠️)
  ↓
WorkspaceSSOModule (COMMERCIAL ⚠️)
  ↓
ImpersonationModule (COMMERCIAL ⚠️)
  ↓
... (21 commercial modules total)
```

---

## 4. Cycle Detection

### 4.1 Identified Cycles

| Cycle | Entities | Severity |
|-------|----------|----------|
| person ↔ company | Person → Company, Company → Person | Low (bidirectional FK) |
| person ↔ opportunity | Person → Opportunity, Opportunity → Person | Low (bidirectional FK) |
| task ↔ task-target | Task → TaskTarget, TaskTarget → Task | Low (junction table) |
| note ↔ note-target | Note → NoteTarget, NoteTarget → Note | Low (junction table) |
| timeline ↔ all entities | TimelineActivity → Person/Company/Task/Note | Low (event sourcing) |

**Assessment:** All cycles are at the entity level (FK relationships), not at the service/module level. No architectural cycles detected.

### 4.2 Strongly Connected Components

```
SCC-1: {person, company, opportunity, task, note, timeline, attachment, workspace-member}
SCC-2: {workflow, record-crud}
SCC-3: {dashboard, record-crud, record-position}
```

**Assessment:** SCCs are expected for CRM entities. No problematic cycles.

---

## 5. Fan-In / Fan-Out Analysis

### 5.1 Most Imported (High Fan-In)

| Entity | Fan-In | Role |
|--------|--------|------|
| **person** | 10 | Core identity |
| **company** | 8 | Core identity |
| **opportunity** | 8 | Core business |
| **timeline-activity** | 9 | Cross-cutting |
| **task-target** | 5 | Junction |
| **note-target** | 5 | Junction |
| **workspace-member** | 6 | Cross-cutting |

### 5.2 Most Importing (High Fan-Out)

| Module | Fan-Out | Risk |
|--------|---------|------|
| **timeline** | 15 | High (cross-cutting) |
| **workflow** | 20+ | Very High (deep engine deps) |
| **dashboard** | 15 | High (cross-cutting) |
| **attachment** | 10 | Medium |
| **task** | 12 | Medium |
| **note** | 10 | Medium |

### 5.3 Hotspots (High Fan-In + High Fan-Out)

| Entity | Fan-In | Fan-Out | Centrality |
|--------|--------|---------|------------|
| **person** | 10 | 3 | HIGH |
| **company** | 8 | 3 | HIGH |
| **timeline-activity** | 9 | 1 | HIGH |
| **twenty-orm** | 15 | 5 | CRITICAL |

**Assessment:** `twenty-orm` is the single most critical infrastructure dependency. Everything depends on it.

---

## 6. Dependency Depth

### 6.1 Maximum Depth from CRM Entry Point

```
CRM API (depth 0)
  → CRM Service (depth 1)
    → twenty-orm (depth 2)
      → workspace-datasource (depth 3)
        → postgresql (depth 4)
      → workspace-cache (depth 3)
        → redis (depth 4)
      → metadata-modules (depth 3)
        → object-metadata (depth 4)
          → field-metadata (depth 5)
    → auth (depth 2)
      → jwt (depth 3)
        → user (depth 4)
    → message-queue (depth 2)
      → redis (depth 3)
    → cache-storage (depth 2)
      → redis (depth 3)
```

**Maximum depth:** 5 levels
**Average depth:** 3 levels

---

## 7. Summary

| Metric | Value |
|--------|-------|
| **CRM modules** | 10 |
| **Engine dependencies** | ~15 essential |
| **Commercial dependencies** | 21 (through CoreEngineModule) |
| **Cycles** | 5 (all entity-level, expected) |
| **SCCs** | 3 (all expected) |
| **Max fan-in** | 10 (person) |
| **Max fan-out** | 20+ (workflow) |
| **Max depth** | 5 |
| **Critical bottleneck** | twenty-orm |

**Key Finding:** The CoreEngineModule is a monolithic aggregator that imports 79 modules, including 21 commercial ones. To extract the CRM Core, we must decompose this monolith and select only the non-commercial modules.
