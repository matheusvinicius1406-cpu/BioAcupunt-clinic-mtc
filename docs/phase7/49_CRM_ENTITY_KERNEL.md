# Phase J — CRM Entity Kernel

**Date:** 2026-08-21
**Confidence:** PROVEN (based on runtime import analysis)

---

## 1. Entity-Level Runtime Analysis

### 1.1 Person (ENTITY-ONLY)

```
File: person.workspace-entity.ts (1 file)
Runtime imports: 1 (twenty-orm/base.workspace-entity)
Type-only imports: 11 (all EntityRelation<T>)
Business logic: NONE
```

**Classification:** PURE DATA DEFINITION
**Runtime closure:** ~2 modules (twenty-orm, twenty-shared/types)

### 1.2 Company (ENTITY-ONLY)

```
File: company.workspace-entity.ts (1 file)
Runtime imports: 0 (only type imports)
Type-only imports: 8 (all EntityRelation<T>)
Business logic: NONE
```

**Classification:** PURE DATA DEFINITION
**Runtime closure:** ~2 modules (twenty-orm, twenty-shared/types)

### 1.3 Opportunity (ENTITY-ONLY)

```
File: opportunity.workspace-entity.ts (1 file)
Runtime imports: 1 (twenty-orm/base.workspace-entity)
Type-only imports: 8 (all EntityRelation<T>)
Business logic: NONE
```

**Classification:** PURE DATA DEFINITION
**Runtime closure:** ~2 modules (twenty-orm, twenty-shared/types)

### 1.4 Attachment (ENTITY-ONLY)

```
File: attachment.workspace-entity.ts (1 file)
Runtime imports: 1 (twenty-orm/base.workspace-entity)
Type-only imports: 11 (all EntityRelation<T>)
Business logic: NONE
```

**Classification:** PURE DATA DEFINITION
**Runtime closure:** ~2 modules (twenty-orm, twenty-shared/types)

### 1.5 Task (SMALL-RUNTIME)

```
Files: 8 (entity + query hooks)
Runtime imports: 27
Type-only imports: 20
Business logic: Query hooks (delete, restore)
```

**Classification:** ENTITY + QUERY HOOKS
**Runtime closure:** ~10 modules (twenty-orm, workspace-orm, auth types, graphql hooks)

### 1.6 Note (SMALL-RUNTIME)

```
Files: 8 (entity + query hooks)
Runtime imports: 27
Type-only imports: 19
Business logic: Query hooks (delete, restore)
```

**Classification:** ENTITY + QUERY HOOKS
**Runtime closure:** ~10 modules (twenty-orm, workspace-orm, auth types, graphql hooks)

### 1.7 Timeline (SMALL-RUNTIME)

```
Files: 11 (entity + services + jobs)
Runtime imports: 37
Type-only imports: 17
Business logic: Timeline activity service, event processing
```

**Classification:** ENTITY + SERVICE + JOBS
**Runtime closure:** ~15 modules (twenty-orm, feature-flag, message-queue, event-emitter, metadata-modules)

### 1.8 Workflow (MASSIVE-RUNTIME)

```
Files: 391
Runtime imports: 2,371
Type-only imports: 18
Business logic: Complete workflow engine
```

**Classification:** FULL ENGINE
**Runtime closure:** ~100+ modules (deeply coupled to engine)

### 1.9 Dashboard (LARGE-RUNTIME)

```
Files: 154
Runtime imports: 452
Type-only imports: 176
Business logic: Dashboard management, chart data
```

**Classification:** FULL FEATURE
**Runtime closure:** ~50+ modules

---

## 2. Entity Runtime Closure Summary

| Entity | Files | Runtime Imports | Closure Size | Classification |
|--------|-------|-----------------|--------------|----------------|
| person | 1 | 1 | ~2 | ENTITY-ONLY |
| company | 1 | 0 | ~2 | ENTITY-ONLY |
| opportunity | 1 | 1 | ~2 | ENTITY-ONLY |
| attachment | 1 | 1 | ~2 | ENTITY-ONLY |
| task | 8 | 27 | ~10 | SMALL-RUNTIME |
| note | 8 | 27 | ~10 | SMALL-RUNTIME |
| timeline | 11 | 37 | ~15 | SMALL-RUNTIME |
| workflow | 391 | 2371 | 18 | MASSIVE-RUNTIME |
| dashboard | 154 | 452 | ~50+ | LARGE-RUNTIME |

---

## 3. Key Insight

The CRM entity kernel is **bimodal**:

**Mode 1: Thin Entities (person, company, opportunity, attachment)**
- Pure data definitions
- No business logic
- Minimal runtime (~2 modules each)
- Can be extracted trivially

**Mode 2: Rich Modules (task, note, timeline)**
- Entity + query hooks or services
- Small runtime (~10-15 modules)
- Can be extracted with moderate effort

**Mode 3: Full Engines (workflow, dashboard)**
- Complete feature implementations
- Massive runtime (~50-100+ modules)
- Deeply coupled to engine infrastructure
- Cannot be extracted without significant adaptation

---

## 4. Implications for Architecture

1. **Thin entities can be extracted immediately** — person, company, opportunity, attachment
2. **Rich modules can be extracted with effort** — task, note, timeline
3. **Full engines need special handling** — workflow, dashboard
4. **Workflow is the critical decision** — 391 files, 2,371 runtime imports
5. **Dashboard is deferrable** — 154 files, can be added later

---

## 5. Confidence

**PROVEN** — Based on file count and runtime import analysis.

**Status:** RECONSTRUCTED
