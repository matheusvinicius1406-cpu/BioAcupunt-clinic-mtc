# Phase 7A — Baseline Freeze

**Date:** 2026-08-21
**Status:** CAPTURED

---

## BioAcupunt Repository

| Metric | Value |
|--------|-------|
| **Branch** | main |
| **Latest commit** | 37df800 (feat: Phase 4+5) |
| **Modified files** | 7 |
| **Untracked files** | 137 |
| **Total pending** | ~144 |
| **Android modules** | 29 top-level packages |
| **Backend Python files** | 2,280 |
| **Web TS/TSX files** | 1,737 |
| **Android test files** | 86 |

### Android Top-Level Modules

```
agenda, ai, auth, backup, biblioteca, cache, clinic, copilot,
core, crm, dashboard, data, di, educacao, financeiro, healthcare,
mkis, mtc, observability, patient, pharma, platform, prontuario,
relatorios, security, sync, ui
```

### Backend Structure

```
backend/app/
├── api/routers/    (12 endpoints)
├── core/           (config, deps)
├── db/             (base, session)
├── knowledge/      (knowledge core)
├── middleware/      (auth, cors)
├── models/         (12 models)
├── repositories/   (7 repositories)
├── schemas/        (Pydantic)
└── services/       (business logic)
```

### Web Structure

```
web/app/
├── (app)/          (dashboard, crm, pacientes, agenda, etc.)
├── api/            (backend proxy)
├── login/          (auth)
├── error.tsx
├── layout.tsx
└── page.tsx
```

---

## Twenty Repository

| Metric | Value |
|--------|-------|
| **Commit** | ae5243fa |
| **Version** | 0.2.1 |
| **Branch** | main |
| **License** | AGPL-3.0 + Commercial + MIT |
| **Package manager** | Yarn 4.13.0 |
| **Node** | ^24.5.0 |
| **Workspace packages** | 18 |
| **Total package.json** | 41 |
| **Server dependencies** | 102 (@-prefixed) |
| **Frontend dependencies** | 84 (@-prefixed) |
| **Total TS/TSX files** | 16,276 |
| **Enterprise files (server)** | 242 |
| **Enterprise files (frontend)** | 41 |
| **Enterprise total** | 283 |
| **Non-enterprise files** | ~15,993 |

### Workspace Packages

```
twenty-front           AGPL-3.0
twenty-server          AGPL-3.0
twenty-emails          AGPL-3.0
twenty-ui              MIT ✅
twenty-utils           (no license field)
twenty-zapier          AGPL-3.0
twenty-website         AGPL-3.0
twenty-docs            AGPL-3.0
twenty-e2e-testing     AGPL-3.0
twenty-shared          MIT ✅
twenty-sdk             MIT ✅
twenty-front-component-renderer  AGPL-3.0
twenty-client-sdk      MIT ✅
twenty-cli             AGPL-3.0
create-twenty-app      MIT ✅
twenty-codex-plugin    AGPL-3.0
twenty-oxlint-rules    AGPL-3.0
twenty-claude-skills   AGPL-3.0
```

### Twenty Server Modules (Business Domain)

```
attachment, blocklist, calendar, call-recording, company,
connected-account, connected-account-sync-webhooks,
contact-creation-manager, dashboard, dashboard-sync, emailing,
match-participant, messaging, messaging-webhooks, note,
onboarding-invite-suggestions, onboarding-recent-messages-import,
opportunity, person, task, timeline, workflow, workspace-member
```

### Twenty Engine Core Modules (Infrastructure)

```
actor, admin-panel, api-key, app-token, application,
approved-access-domain, auth, billing, billing-webhook,
cache-lock, cache-storage, calendar, captcha, client-config,
cloudflare, code-interpreter, company-enrichment, cron,
dns-manager, domain, dpa, email, email-verification,
emailing-domain, enterprise, environment, event-emitter,
event-logs, exception-handler, feature-flag, file, file-storage,
geo-map, graphql, guard-redirect, health, i18n,
imap-smtp-caldav-connection, impersonation, jwt, key-value-pair,
lab, logger, logic-function, message-queue, messaging, metrics,
onboarding, open-api, public-domain, record-crud, record-position,
record-transformer, redis-client, related-person-ids, sdk-client,
search, secret-encryption, secure-http-client, sentry,
server-route-trigger, session-storage, sql-sanitization, sso,
telemetry, throttler, tool, tool-provider, twenty-config,
two-factor-authentication, upgrade, usage, user, user-session,
user-workspace, well-known, workflow, workspace, workspace-invitation
```

### Twenty Metadata Modules

```
ai, application-translation-catalog, calendar-channel,
command-menu-item, connected-account, data-source, field-metadata,
flat-agent, flat-application-variable, flat-command-menu-item,
flat-connection-provider, flat-entity, flat-field-metadata,
flat-field-permission, flat-front-component, flat-index-metadata,
flat-navigation-menu-item, flat-object-metadata,
flat-object-permission, flat-page-layout, flat-page-layout-tab,
flat-page-layout-widget, flat-permission-flag, flat-role,
flat-role-permission-flag, flat-role-target,
flat-row-level-permission-predicate, flat-search-field-metadata,
flat-skill, flat-view, flat-view-field, flat-view-field-group,
flat-view-filter, flat-view-filter-group, flat-view-group,
flat-view-sort, flat-webhook, front-component, index-metadata,
logic-function, logic-function-layer, message-channel,
message-folder, metadata-side-effect, minimal-metadata,
navigation-menu-item, object-metadata, object-permission,
page-layout, page-layout-tab, page-layout-widget, pagination,
permission-flag, permissions, role, role-permission-flag,
role-target, role-validation, route-trigger,
row-level-permission-predicate, search-field-metadata, skill,
user-role, utils, view, view-field, view-field-group, view-filter,
view-filter-group, view-group, view-permissions, view-sort, webhook,
workspace-feature-flags-map-cache, workspace-metadata-version
```

### Twenty Frontend Modules

```
accounts, activities, advanced-text-editor, ai, analytics, apollo,
app, applications, auth, blocknote-editor, browser-event, captcha,
client-config, command-menu, command-menu-item, companies,
context-store, dashboards, debug, domain-manager,
dropdown-context-state-management, error-handler, file, file-upload,
front-components, geo-map, information-banner,
keyboard-shortcut-menu, layout-customization, localization,
logic-functions, marketplace, mention, metadata-error-handler,
metadata-store, navigation, navigation-menu-item, object-core,
object-metadata, object-record, onboarding, page-layout, people,
settings, side-panel, sign-in-background-mock, spreadsheet-import,
sse-db-event, support, types, ui, users, views, workflow, workspace,
workspace-invitation, workspace-member
```

---

## Baseline Summary

| Dimension | BioAcupunt | Twenty | Ratio |
|-----------|------------|--------|-------|
| **Language** | Kotlin + Python + TS | TypeScript | — |
| **Backend** | FastAPI (Python) | NestJS (TypeScript) | — |
| **Frontend** | Next.js + Compose | React + Vite | — |
| **Database** | Room/SQLite + PostgreSQL | PostgreSQL | — |
| **API** | REST | GraphQL + REST | — |
| **Files** | ~4,000 | ~16,276 | 4:1 |
| **Tests** | 86 | (not counted) | — |
| **Enterprise** | 0 | 283 | — |
| **CRM modules** | 1 (stubs) | 24 (complete) | — |

**Baseline frozen. No modifications made.**
