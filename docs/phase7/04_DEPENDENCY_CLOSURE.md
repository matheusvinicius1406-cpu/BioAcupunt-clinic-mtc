# Phase 7D — Transitive Dependency Closure

**Date:** 2026-08-21
**Method:** Import graph traversal + module dependency analysis
**Confidence:** SUPPORTED (static analysis, not runtime)

---

## 1. Closure per CRM Capability

### 1.1 Person

```
DIRECT: company, opportunity, task-target, note-target, attachment,
        timeline-activity, message-participant, calendar-event-participant,
        emailing, twenty-orm, workspace-manager

TRANSITIVE:
  company → person, opportunity, task-target, note-target, attachment,
            timeline-activity, workspace-member
  opportunity → person, company, task-target, note-target, attachment,
                timeline-activity, workspace-member
  task-target → person, company, opportunity, task, workspace-member
  note-target → person, company, opportunity, note, workspace-member
  attachment → person, company, opportunity, task, note, dashboard,
               workflow, workspace-member
  timeline-activity → person, company, opportunity, task, note, workflow,
                      dashboard, emailing, workspace-member
  message-participant → person, messaging, timeline-activity
  calendar-event-participant → person, calendar, timeline-activity
  emailing → person, message-list, message-campaign, timeline-activity

INFRASTRUCTURE:
  twenty-orm → workspace-datasource, workspace-cache, workspace-manager,
               metadata-modules
  workspace-manager → metadata-modules, workspace-datasource

TOTAL PERSON CLOSURE: ~45 modules
```

### 1.2 Organization (Company)

```
DIRECT: person, opportunity, task-target, note-target, attachment,
        timeline-activity, workspace-member, workspace-manager

TRANSITIVE: (same as Person, minus message-participant, calendar-event-participant)

TOTAL ORGANIZATION CLOSURE: ~40 modules
```

### 1.3 Opportunity

```
DIRECT: person, company, task-target, note-target, attachment,
        timeline-activity, workspace-member, twenty-orm, workspace-manager

TRANSITIVE: (same as Person)

TOTAL OPPORTUNITY CLOSURE: ~40 modules
```

### 1.4 Task

```
DIRECT: person, company, opportunity, task-target, attachment,
        timeline-activity, workspace-member, twenty-orm, auth,
        workspace, graphql, workspace-manager

TRANSITIVE:
  auth → jwt, user, workspace
  workspace → user, workspace-invitation
  graphql → workspace-query-runner, metadata-modules

TOTAL TASK CLOSURE: ~35 modules
```

### 1.5 Note

```
DIRECT: person, company, opportunity, note-target, attachment,
        timeline-activity, twenty-orm, auth, workspace, graphql,
        workspace-manager

TRANSITIVE: (same as Task)

TOTAL NOTE CLOSURE: ~35 modules
```

### 1.6 Timeline

```
DIRECT: person, company, opportunity, task, note, workflow, dashboard,
        emailing, workspace-member, twenty-orm, feature-flag,
        message-queue, event-emitter, metadata-modules,
        object-metadata-repository, workspace-event-emitter,
        workspace-manager

TRANSITIVE:
  workflow → record-crud, billing (COMMERCIAL ⚠️), cache-storage,
             feature-flag, message-queue, cron, email, logic-function,
             application, event-logs
  dashboard → record-crud, record-position, actor, application,
              exception-handler, tool-provider, metadata-modules
  emailing → message-list, message-campaign, message-participant,
             timeline-activity

TOTAL TIMELINE CLOSURE: ~60 modules (including commercial)
```

### 1.7 Attachment

```
DIRECT: person, company, opportunity, task, note, dashboard, workflow,
        workspace-member, twenty-orm, workspace-manager

TRANSITIVE:
  dashboard → record-crud, record-position, actor, application,
              exception-handler, tool-provider
  workflow → billing (COMMERCIAL ⚠️), cache-storage, feature-flag,
             message-queue, cron, email, logic-function, application

TOTAL ATTACHMENT CLOSURE: ~50 modules (including commercial)
```

### 1.8 Workflow

```
DIRECT: timeline-activity, record-crud, workspace-member, twenty-orm,
        auth, graphql, cache-storage, billing (COMMERCIAL ⚠️),
        feature-flag, message-queue, cron, email, logic-function,
        application, event-logs, workspace-manager

TRANSITIVE:
  billing → billing-webhook, stripe, usage (all COMMERCIAL)
  record-crud → common-api-context-builder, metadata-modules
  cache-storage → redis-client
  message-queue → redis-client
  logic-function → code-interpreter, application

TOTAL WORKFLOW CLOSURE: ~55 modules (including 8+ commercial)
```

### 1.9 Dashboard

```
DIRECT: person, company, opportunity, task, note, timeline-activity,
        workspace-member, twenty-orm, auth, graphql, record-crud,
        record-position, actor, application, exception-handler,
        tool-provider, metadata-modules, workspace-manager

TRANSITIVE:
  record-crud → common-api-context-builder, metadata-modules
  record-position → twenty-orm
  tool-provider → record-crud, feature-flag

TOTAL DASHBOARD CLOSURE: ~40 modules
```

### 1.10 View (Search/Filter/Sort)

```
DIRECT: metadata-modules (view, view-field, view-filter, view-filter-group,
        view-group, view-sort), twenty-orm, workspace-manager

TRANSITIVE:
  metadata-modules → object-metadata, field-metadata, index-metadata

TOTAL VIEW CLOSURE: ~15 modules
```

---

## 2. Aggregate Closure

### 2.1 Union of All CRM Closures

```
CRM RUNTIME CORE (Union)
├── CRM Domain Modules (10)
│   person, company, opportunity, task, note, timeline,
│   attachment, workflow, dashboard, view
│
├── CRM Junction Entities (4)
│   task-target, note-target, message-participant,
│   calendar-event-participant
│
├── Engine Infrastructure (15)
│   twenty-orm, workspace-datasource, workspace-cache,
│   workspace-manager, workspace-event-emitter,
│   object-metadata-repository, core-entity-cache,
│   dataloaders, graphql, workspace-query-runner,
│   metadata-modules, feature-flag, message-queue,
│   cache-storage, redis-client
│
├── Core Services (12)
│   auth, jwt, user, workspace, workspace-invitation,
│   record-crud, record-position, record-transformer,
│   search, file-storage, file, event-logs
│
├── Support Services (8)
│   environment, twenty-config, logger, exception-handler,
│   health, email, throttler, sql-sanitization
│
├── Metadata Modules (14)
│   object-metadata, field-metadata, relation-metadata,
│   index-metadata, view, view-field, view-filter,
│   view-filter-group, view-group, view-sort,
│   role, object-permission, role-permission-flag,
│   page-layout
│
└── Optional CRM Modules (4)
    calendar, messaging, connected-account, dashboard-sync
```

### 2.2 Total Count

| Category | Count |
|----------|-------|
| CRM Domain | 10 |
| Junction Entities | 4 |
| Engine Infrastructure | 15 |
| Core Services | 12 |
| Support Services | 8 |
| Metadata Modules | 14 |
| Optional CRM | 4 |
| **TOTAL** | **67** |

### 2.3 Commercial Dependencies (Must Remove)

| Module | Type | Action |
|--------|------|--------|
| billing | Commercial | Remove billing checks |
| billing-webhook | Commercial | Remove entirely |
| usage | Commercial | Remove entirely |
| sso | Commercial | Remove entirely |
| enterprise | Commercial | Remove entirely |
| impersonation | Commercial | Remove entirely |
| cloudflare | Commercial | Remove entirely |
| dns-manager | Commercial | Remove entirely |
| emailing-domain | Commercial | Remove entirely |
| dpa | Commercial | Remove entirely |
| code-interpreter | Commercial | Remove entirely |
| geo-map | Commercial | Remove entirely |
| imap-smtp-caldav | Commercial | Remove entirely |
| lab | Commercial | Remove entirely |
| admin-panel | Commercial | Remove entirely |
| open-api | Commercial | Remove entirely |
| public-domain | Commercial | Remove entirely |
| approved-access-domain | Commercial | Remove entirely |

**Total commercial to remove:** 18 modules

---

## 3. Closure After Commercial Removal

```
CLEAN CRM RUNTIME CORE
├── CRM Domain (10)
├── Junction Entities (4)
├── Engine Infrastructure (15)
├── Core Services (12)
├── Support Services (8)
├── Metadata Modules (14)
├── Optional CRM (4)
└── TOTAL: 67 modules (after removing 18 commercial)
```

---

## 4. What Breaks When We Remove Commercial?

### 4.1 Billing Removal

| Consumer | Impact | Mitigation |
|----------|--------|------------|
| workflow-executor | Checks billing before execution | Remove billing check, always allow |
| workflow-runner | Checks billing usage | Remove billing check |
| calendar-import | Checks billing | Remove billing check |
| CoreEngineModule | Imports BillingModule | Remove import |

### 4.2 SSO Removal

| Consumer | Impact | Mitigation |
|----------|--------|------------|
| auth | SSO strategies | Remove SSO strategies, keep JWT/password |
| workspace | SSO configuration | Remove SSO config |
| user-session | SSO sessions | Remove SSO sessions |

### 4.3 Other Commercial

| Module | Impact | Mitigation |
|--------|--------|------------|
| cloudflare | DNS management | Remove entirely |
| dns-manager | Domain management | Remove entirely |
| impersonation | User impersonation | Remove entirely |
| code-interpreter | Code execution | Remove entirely |
| geo-map | Map integration | Remove entirely |

---

## 5. Summary

| Metric | Value |
|--------|-------|
| **Total CRM closure** | 67 modules |
| **Commercial to remove** | 18 modules |
| **Clean CRM closure** | 67 modules (post-removal) |
| **Critical bottleneck** | twenty-orm (15 dependents) |
| **Highest fan-out** | workflow (20+ imports) |
| **Highest fan-in** | person (10 importers) |

**Key Finding:** The CRM Runtime Core requires 67 modules after removing 18 commercial ones. The workflow module has the deepest commercial dependencies (billing) and requires the most adaptation.
