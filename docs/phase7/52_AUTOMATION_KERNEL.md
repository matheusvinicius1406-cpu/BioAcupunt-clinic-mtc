# Phase O — Automation / Workflow Reconstruction

**Date:** 2026-08-21
**Confidence:** SUPPORTED (based on import analysis)

---

## 1. Workflow Module Decomposition

### 1.1 Scale

```
Total files: 391
Total imports: 2,389
Runtime imports: 2,371  ⚠️ CRITICAL: Do not include in MVP extraction
Type-only imports: 18
Engine dependencies: ~100+
```

> **⚠️ CRITICAL WARNING:** Workflow has 2,371 runtime imports — the highest of any module.
> This module is DEFERRED from MVP extraction. Any future attempt to include it
> must account for this massive coupling. Post-MVP refactoring required.

### 1.2 Responsibility Map

```
WORKFLOW ENGINE (391 files)
│
├── CORE AUTOMATION (essential)
│   ├── trigger evaluation
│   ├── condition evaluation
│   ├── action execution
│   ├── workflow state management
│   ├── execution history
│   └── scheduling
│
├── PLATFORM (infrastructure)
│   ├── queue/message-queue
│   ├── cache/cache-storage
│   ├── events/workspace-event-emitter
│   ├── auth/workspace context
│   └── metadata/object-metadata
│
├── CRM-SPECIFIC (domain)
│   ├── record-crud actions
│   ├── record-position actions
│   └── CRM entity triggers
│
├── OPTIONAL FEATURES (can defer)
│   ├── AI tools (ai-agent)
│   ├── calendar tools
│   ├── email tools
│   ├── HTTP tools
│   ├── logic functions
│   └── command-menu-item
│
├── COMMERCIAL (must remove)
│   ├── billing checks (6 imports)
│   ├── usage tracking (4 imports)
│   └── billing module
│
└── ENTERPRISE (must remove)
    └── (none found in workflow itself)
```

### 1.3 Dependency Breakdown

| Category | Imports | Action |
|----------|---------|--------|
| Engine API (graphql, query-runner) | ~50 | KEEP (CRM needs GraphQL) |
| Engine Core (auth, workspace, config) | ~30 | KEEP (CRM needs auth) |
| Engine ORM (twenty-orm, workspace-*) | ~20 | KEEP (CRM needs ORM) |
| Engine Metadata (object-metadata, etc.) | ~30 | KEEP (CRM needs metadata) |
| Engine Billing | 6 | REMOVE |
| Engine Usage | 4 | REMOVE |
| Engine AI | ~10 | DEFER |
| Engine Logic Function | ~5 | DEFER |
| Engine Tool | ~10 | DEFER |
| Engine Command Menu | ~3 | DEFER |
| Engine Application | ~5 | DEFER |
| Engine Event Logs | ~3 | KEEP (audit) |
| **TOTAL** | **~2,371** | — |

---

## 2. Three Architecture Options

### Architecture A: Twenty Workflow Mostly Intact

**Description:** Keep workflow as-is, remove only billing/usage.

**Pros:**
- Maximum feature preservation
- Minimum code changes
- Upstream compatible

**Cons:**
- Massive closure (2,371 runtime imports)
- Deep engine coupling
- Commercial dependencies must be removed
- Hard to maintain

**Complexity:** LOW (just remove billing)
**Maintainability:** LOW (deep coupling)
**Upstream compatibility:** HIGH
**Feature completeness:** HIGH

### Architecture B: Workflow Core + BioAcupunt Platform Ports

**Description:** Extract workflow core, replace infrastructure with ports.

**Pros:**
- Cleaner architecture
- Better separation of concerns
- Easier to maintain

**Cons:**
- More code to write
- May lose some features
- Needs port implementations

**Complexity:** MEDIUM
**Maintainability:** MEDIUM
**Upstream compatibility:** MEDIUM
**Feature completeness:** MEDIUM

### Architecture C: Lightweight BioAcupunt Automation Kernel

**Description:** Build minimal automation from scratch, inspired by Twenty.

**Pros:**
- Cleanest architecture
- Minimal coupling
- Maximum control

**Cons:**
- Most code to write
- May miss features
- No upstream compatibility

**Complexity:** HIGH
**Maintainability:** HIGH
**Upstream compatibility:** LOW
**Feature completeness:** LOW

---

## 3. Recommendation

**Architecture A (with modifications)** — Keep workflow mostly intact, remove billing/usage, defer AI/logic-function/tools.

**Rationale:**
1. Workflow is the most complex module (391 files)
2. Rewriting it would take months
3. The billing/usage removal is straightforward (10 imports)
4. AI/logic-function/tools can be deferred to later phases
5. Upstream compatibility is preserved

**Implementation:**
1. Remove billing imports (6 files)
2. Remove usage imports (4 files)
3. Stub out AI tool providers
4. Stub out logic function providers
5. Keep core automation intact

---

## 4. Confidence

**SUPPORTED** — Based on import analysis and responsibility mapping.

**Status:** RECONSTRUCTED
