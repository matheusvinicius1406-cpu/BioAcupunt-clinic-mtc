# Phase 7.0.2 — Reconstructed Architecture

**Date:** 2026-08-21
**Status:** RECONSTRUCTED
**Confidence:** HIGH

---

## 1. Architecture Overview

```
                         BIOACUPUNT
                  HEALTHCARE SAAS PLATFORM
                              │
                ┌─────────────┼─────────────┐
                │             │             │
             PLATFORM       CRM        HEALTHCARE
                │             │             │
         ┌──────┼──────┐  ┌──┼──┐    ┌─────┼─────┐
         │      │      │  │     │    │     │     │
      Identity Auth  Tenant Entity  Patient Encounter
      User   JWT   Workspace App    Clinical  Safety
      Member Guards Metadata Search  Notes  Intelligence
                    Ports   Timeline
                            Automation
                                │
                ┌───────────────┼───────────────┐
                │               │               │
             KNOWLEDGE          AI           ANDROID
                │               │             │
            Graph/Search    Copilot      Offline-first
            Evidence        RAG          Sync contract
            Packs           Synthesis
```

---

## 2. Domain Boundaries

### 2.1 Platform Context

| Aspect | Details |
|--------|---------|
| **Purpose** | System infrastructure: auth, tenancy, billing, config |
| **Owned Entities** | Tenant, User, TenantMember, Subscription, ApiKey |
| **Stack** | NestJS/TypeScript (Twenty-derived) |
| **Database** | PostgreSQL (public schema) |
| **API** | GraphQL + REST |

### 2.2 CRM Context

| Aspect | Details |
|--------|---------|
| **Purpose** | Relationship management, sales, tasks, notes |
| **Owned Entities** | Person, Organization, Opportunity, Pipeline, Task, Note, Attachment, Timeline, View, Dashboard |
| **Stack** | NestJS/TypeScript (Twenty-derived) |
| **Database** | PostgreSQL (tenant schemas) |
| **API** | GraphQL (primary) + REST (compatibility) |

### 2.3 Healthcare Context

| Aspect | Details |
|--------|---------|
| **Purpose** | Clinical records, encounters, care plans |
| **Owned Entities** | Patient, Encounter, ClinicalRecord, CarePlan, Diagnosis |
| **Stack** | FastAPI/Python (existing) |
| **Database** | PostgreSQL (tenant schemas) + Room/SQLite (Android) |
| **API** | REST |

### 2.4 Knowledge Context

| Aspect | Details |
|--------|---------|
| **Purpose** | Library articles, evidence, knowledge packs |
| **Owned Entities** | Article, Evidence, KnowledgePack |
| **Stack** | FastAPI/Python (existing) |
| **Database** | PostgreSQL (tenant schemas) |
| **API** | REST |

### 2.5 AI Context

| Aspect | Details |
|--------|---------|
| **Purpose** | Copilot, RAG, clinical synthesis |
| **Owned Entities** | ChatMessage, ClinicalSynthesis |
| **Stack** | FastAPI/Python + Local LLM |
| **Database** | Room/SQLite (Android) |
| **API** | Internal |

### 2.6 Android Context

| Aspect | Details |
|--------|---------|
| **Purpose** | Offline-first mobile client |
| **Owned Entities** | Room entities (local) |
| **Stack** | Kotlin/Room/Compose |
| **Database** | Room/SQLite (local) |
| **API** | Sync contract (REST) |

---

## 3. CRM Runtime Core (Reconstructed)

### 3.1 Thin Entities (Extract Immediately)

```
person.workspace-entity.ts
company.workspace-entity.ts
opportunity.workspace-entity.ts
attachment.workspace-entity.ts
```

**Runtime closure:** ~2 modules each
**Total:** ~8 modules

### 3.2 Rich Modules (Extract with Effort)

```
task/ (8 files)
├── task.workspace-entity.ts
├── task-target.workspace-entity.ts
└── query-hooks/ (6 files)

note/ (8 files)
├── note.workspace-entity.ts
├── note-target.workspace-entity.ts
└── query-hooks/ (6 files)

timeline/ (11 files)
├── timeline-activity.workspace-entity.ts
├── services/
├── jobs/
└── repositories/
```

**Runtime closure:** ~10-15 modules each
**Total:** ~40 modules

### 3.3 Full Engines (Special Handling)

```
workflow/ (391 files)
├── Core automation (keep)
├── Billing checks (remove)
├── Usage tracking (remove)
├── AI tools (defer)
├── Logic functions (defer)
└── Tool providers (defer)

dashboard/ (154 files)
├── Core dashboard (keep)
└── Advanced features (defer)
```

**Runtime closure:** ~100+ modules (workflow), ~50+ (dashboard)
**Total:** ~150+ modules

### 3.4 Engine Infrastructure (Required)

```
twenty-orm/ (ORM layer)
workspace-datasource/ (DB connections)
workspace-cache/ (Metadata cache)
workspace-manager/ (Workspace lifecycle)
workspace-event-emitter/ (Domain events)
object-metadata-repository/ (Metadata queries)
graphql/ (API layer)
record-crud/ (Generic CRUD)
record-position/ (Positioning)
search/ (Full-text search)
file-storage/ (S3/Local)
file/ (File entity)
feature-flag/ (Feature flags)
message-queue/ (Job queue)
cache-storage/ (Caching)
redis-client/ (Redis)
```

**Total:** ~16 modules

### 3.5 Metadata Modules (Required)

```
object-metadata/
field-metadata/
relation-metadata/
index-metadata/
view/
view-field/
view-filter/
view-filter-group/
view-group/
view-sort/
role/
object-permission/
role-permission-flag/
page-layout/
```

**Total:** ~14 modules

### 3.6 Support Services (Required)

```
environment/
twenty-config/
logger/
exception-handler/
health/
email/
throttler/
sql-sanitization/
secret-encryption/
secure-http-client/
event-logs/
```

**Total:** ~11 modules

---

## 4. Total CRM Runtime Core

| Category | Count |
|----------|-------|
| Thin Entities | ~8 |
| Rich Modules | ~40 |
| Full Engines | ~150+ |
| Engine Infrastructure | ~16 |
| Metadata Modules | ~14 |
| Support Services | ~11 |
| **TOTAL** | **~240+ modules** |

**Note:** This is significantly larger than the previous "63 modules" estimate because:
1. Workflow alone has 391 files with deep engine coupling
2. Dashboard has 154 files
3. Engine infrastructure is more extensive than previously estimated

---

## 5. Commercial/Enterprise Boundary

### 5.1 Must Remove

| Component | Files | Action |
|-----------|-------|--------|
| Billing (workflow) | 6 imports | Remove |
| Usage (workflow) | 4 imports | Remove |
| SSO (auth) | 22 files | Remove |
| JWT rotation (jwt) | 4 files | Remove |
| **TOTAL** | **~30 files** | — |

### 5.2 Must Defer

| Component | Reason |
|-----------|--------|
| AI tools (workflow) | Not needed for v1 |
| Logic functions (workflow) | Not needed for v1 |
| Tool providers (workflow) | Not needed for v1 |
| Command menu (workflow) | Not needed for v1 |
| Application (workflow) | Not needed for v1 |

---

## 6. Platform Ports

| Port | Purpose | Owner |
|------|---------|-------|
| AuthPort | Authentication | Platform |
| TenantPort | Tenant resolution | Platform |
| AuthorizationPort | Permissions | Platform |
| StoragePort | File storage | Platform |
| CachePort | Caching | Platform |
| JobPort | Background jobs | Platform |
| EventPort | Domain events | Platform |
| AuditPort | Audit trail | Platform |
| ConfigurationPort | Config | Platform |
| SearchPort | Full-text search | Platform |
| IdentityPort | Identity management | Platform |

---

## 7. Architectural Invariants

| ID | Invariant |
|----|-----------|
| I-01 | Platform owns authentication |
| I-02 | Platform owns tenant resolution |
| I-03 | Healthcare owns clinical truth |
| I-04 | CRM owns CRM truth |
| I-05 | Knowledge owns knowledge truth |
| I-06 | AI never becomes source of truth |
| I-07 | Android never accesses database directly |
| I-08 | Cross-tenant access is forbidden |
| I-09 | Commercial modules cannot enter CRM runtime |
| I-10 | Enterprise modules cannot enter CRM runtime |
| I-11 | Tenant context must reach repository boundaries |
| I-12 | Background jobs must preserve tenant context |

---

## 8. Architecture Fitness Functions

| Rule | Enforcement |
|------|-------------|
| CRM cannot import Healthcare internals | Build-time lint |
| CRM cannot import Knowledge internals | Build-time lint |
| CRM cannot import AI implementation | Build-time lint |
| Healthcare cannot depend on CRM internals | Build-time lint |
| Android cannot access persistence directly | Runtime check |
| AI cannot mutate clinical truth directly | Unit test |
| Commercial modules cannot enter CRM | Build-time lint |
| Enterprise modules cannot enter CRM | Build-time lint |
| Tenant context must reach repos | Integration test |
| Jobs must preserve tenant context | Integration test |

---

## 9. First Implementation Slice

**Recommended:** Person/Company CRUD

**Rationale:**
- Minimal blast radius (thin entities)
- Clear contract (GraphQL CRUD)
- Clear ownership (CRM)
- Clear tests (CRUD operations)
- Clear rollback (remove entities)

**Slice includes:**
1. Extract person.workspace-entity.ts
2. Extract company.workspace-entity.ts
3. Configure schema-per-tenant
4. Set up GraphQL resolvers
5. Validate build
6. Validate tenancy

---

## 10. Summary

| Metric | Value |
|--------|-------|
| **CRM Runtime Core** | ~240+ modules |
| **Thin Entities** | 4 (trivial) |
| **Rich Modules** | 3 (moderate) |
| **Full Engines** | 2 (complex) |
| **Commercial to Remove** | ~30 files |
| **Enterprise to Remove** | ~20 files |
| **Platform Ports** | 11 |
| **Architectural Invariants** | 12 |
| **Fitness Functions** | 10 |

---

**Status:** RECONSTRUCTED
**Confidence:** HIGH
