# Phase 7.0.1 — Claim Register

**Date:** 2026-08-21
**Status:** ADVERSARIAL REVIEW IN PROGRESS

---

## Claim Register

| ID | Previous Claim | Evidence | Confidence | Falsification Test | Result | Final Status |
|----|---------------|----------|------------|-------------------|--------|--------------|
| C-01 | Twenty provides the correct CRM foundation | Twenty has complete CRM implementation | HIGH | Verify module sizes and dependencies | **MODIFIED** | See findings |
| C-02 | CRM Core contains approximately 63 modules | Static import graph analysis | LOW | Independent closure calculation | **REJECTED** | See C-02a |
| C-02a | CRM Core: person(1), company(1), opportunity(1), task(8), note(8), timeline(11), attachment(1), workflow(391), dashboard(154) | File count analysis | HIGH | Verify file counts | **CONFIRMED** | Module sizes vary dramatically |
| C-03 | 18 commercial modules can be excluded | grep for `@license Enterprise` | MEDIUM | Verify runtime vs file-level | **MODIFIED** | See C-03a |
| C-03a | 283 Enterprise files exist | grep count | HIGH | Verify they are not in CRM closure | **UNPROVEN** | Need to check if Enterprise files are in CRM closure |
| C-04 | CoreEngineModule can be decomposed safely | It imports 79 modules | MEDIUM | Verify CRM doesn't need CoreEngineModule | **UNPROVEN** | Need to verify |
| C-05 | Workflow requires adaptation because of billing | 6 billing imports found | HIGH | Remove billing, check what breaks | **CONFIRMED** | Billing dependency is real |
| C-06 | Schema-per-tenant is the optimal tenancy model | Multi-criteria analysis (137/190) | MEDIUM | Sensitivity analysis | **UNPROVEN** | Need sensitivity analysis |
| C-07 | Person is canonical identity | Domain modeling | MEDIUM | Verify no duplicate identity | **CONFIRMED** | Person is entity-only, no business logic |
| C-08 | PatientProfile should extend Person | 1:1 relationship design | MEDIUM | Verify healthcare needs | **CONFIRMED** | Pattern is sound |
| C-09 | Enterprise components can be removed from CRM closure | File-level grep | LOW | Verify they are not in transitive closure | **UNPROVEN** | Need to check |
| C-10 | CRM can be isolated from Healthcare | Domain boundary design | MEDIUM | Test cross-context writes | **UNPROVEN** | Need to verify |
| C-11 | CRM can be isolated from Knowledge | Domain boundary design | MEDIUM | Test cross-context writes | **UNPROVEN** | Need to verify |
| C-12 | AI can remain outside CRM | Domain boundary design | MEDIUM | Test AI dependencies | **UNPROVEN** | Need to verify |
| C-13 | Android can interact through contracts | Sync contract design | LOW | Verify sync protocol | **UNPROVEN** | Need to verify |
| C-14 | Twenty can remain reasonably upstream-compatible | Fork strategy | LOW | Analyze upstream churn | **UNPROVEN** | Need to analyze |
| C-15 | The proposed 63-module closure is sufficient | Static analysis | LOW | Runtime reachability test | **REJECTED** | See C-15a |
| C-15a | Workflow module alone has 443 runtime imports | Import analysis | HIGH | Verify counts | **CONFIRMED** | Workflow is massively coupled |
| C-16 | No critical hidden dependency remains | Static analysis | LOW | Runtime tracing | **UNPROVEN** | Need runtime tracing |
| C-17 | No critical licensing issue remains | License audit | MEDIUM | Verify AGPL compliance | **UNPROVEN** | Need to verify |
| C-18 | The migration boundary is complete | Design document | LOW | Verify all entities mapped | **UNPROVEN** | Need to verify |

---

## Critical Findings

### F-01: Type-Only Imports Were Counted as Runtime Dependencies

**Previous Analysis:** Counted ALL imports (including `import { type X }`) as dependencies.

**Actual:** TypeScript `import { type X }` is erased at compile time. `EntityRelation<T>` is defined as `export type EntityRelation<T> = T;` — a pure type alias with ZERO runtime presence.

**Impact:** The CRM closure is significantly smaller than previously calculated for entity definitions. Person, company, opportunity, and attachment have minimal runtime dependencies.

**Evidence:**
- Person: 11 type-only imports, 2 runtime imports
- Company: 8 type-only imports, 1 runtime import
- Opportunity: 9 type-only imports, 1 runtime import

### F-02: CRM Modules Vary Dramatically in Size

**Previous Analysis:** Treated all CRM modules as equal.

**Actual:**
- person: 1 file (entity only)
- company: 1 file (entity only)
- opportunity: 1 file (entity only)
- attachment: 1 file (entity only)
- task: 8 files (entity + query hooks)
- note: 8 files (entity + query hooks)
- timeline: 11 files (entity + services + jobs)
- workflow: 391 files (MASSIVE)
- dashboard: 154 files (LARGE)

**Impact:** The "CRM Core" is dominated by workflow (391 files) and dashboard (154 files). The simple entity modules (person, company, opportunity, attachment) are trivial.

### F-03: Workflow Has Deep Engine Coupling

**Previous Analysis:** Said workflow needs "billing adaptation."

**Actual:** Workflow has 443 runtime imports, including:
- billing (6 imports) - COMMERCIAL
- usage (4 imports) - COMMERCIAL
- AI modules (several)
- logic-function
- application
- tool-provider
- tool (calendar-tool, email-tool, http-tool)
- command-menu-item
- connected-account
- metadata-modules (many)

**Impact:** Workflow is NOT just "billing adaptation." It's deeply coupled to the entire engine infrastructure. Removing billing may not be sufficient — the module may need to be replaced entirely.

### F-04: The "63 Modules" Count is Misleading

**Previous Analysis:** Counted 63 modules in CRM Core.

**Actual:** The count includes:
- 4 trivial entity definitions (1 file each)
- 2 small modules (task, note — 8 files each)
- 1 medium module (timeline — 11 files)
- 1 massive module (workflow — 391 files)
- 1 large module (dashboard — 154 files)
- ~40 engine infrastructure modules
- ~14 metadata modules
- ~8 support services

**Impact:** The "63 modules" number obscures the reality that workflow (391 files) and dashboard (154 files) dominate the closure.

---

## Status

**ADVERSARIAL REVIEW IN PROGRESS**

More checks needed before final verdict.
