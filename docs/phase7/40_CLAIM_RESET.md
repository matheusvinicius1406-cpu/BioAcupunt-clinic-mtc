# Phase 7.0.2 — Claim Reset

**Date:** 2026-08-21
**Status:** RECONSTRUCTION IN PROGRESS

---

## Claim Reclassification

| ID | Previous Claim | Previous Status | New Evidence | New Status |
|----|---------------|-----------------|--------------|------------|
| C-01 | Twenty provides correct CRM foundation | CONFIRMED | CRM modules are entity-only or small runtime | **MODIFIED** — Foundation is thin entities + large workflow/dashboard |
| C-02 | CRM Core contains ~63 modules | REJECTED | Type-only imports inflated count | **RECALCULATED** — See runtime graph |
| C-03 | 18 commercial modules can be excluded | MODIFIED | Auth has SSO Enterprise files | **EXPANDED** — Auth SSO + JWT rotation = 14 Enterprise files in closure |
| C-04 | CoreEngineModule can be decomposed | UNPROVEN | Not yet analyzed | **PENDING** |
| C-05 | Workflow needs billing adaptation | CONFIRMED | 2,371 runtime imports | **STRENGTHENED** — Workflow is deeply coupled, not just billing |
| C-06 | Schema-per-tenant is optimal | CONFIRMED | See Tenancy Analysis (10): 137/190 + Tenant/Workspace Kernel (14) |
| C-07 | Person is canonical identity | CONFIRMED | Entity-only, no business logic | **REJECTED** — User is canonical for auth; Person is CRM contact |
| C-08 | PatientProfile extends Person | CONFIRMED | 1:1 relationship | **CONFIRMED** |
| C-09 | Enterprise can be removed | UNDERCOUNTED | Auth SSO + JWT rotation missed | **EXPANDED** |
| C-10 | CRM isolated from Healthcare | UNPROVEN | Need verification | **PENDING** |
| C-11 | CRM isolated from Knowledge | UNPROVEN | Need verification | **PENDING** |
| C-12 | AI outside CRM | UNPROVEN | Need verification | **PENDING** |
| C-13 | Android via contracts | UNPROVEN | Need verification | **PENDING** |
| C-14 | Twenty upstream-compatible | UNPROVEN | Need analysis | **PENDING** |
| C-15 | 63-module closure sufficient | REJECTED | Runtime graph shows different picture | **RECALCULATED** |
| C-16 | No hidden dependencies | CONTRADICTED | Auth SSO, JWT rotation, workflow usage | **FALSE** |
| C-17 | No licensing issues | UNPROVEN | AGPL compliance needs verification | **PENDING** |
| C-18 | Migration boundary complete | UNPROVEN | Need verification | **PENDING** |

---

## New Claims (From Adversarial Review)

| ID | New Claim | Evidence | Status |
|----|-----------|----------|--------|
| N-01 | Person/Company/Opportunity/Attachment are entity-only (1 file each) | File count analysis | **PROVEN** |
| N-02 | Task/Note have small runtime (8 files each) | File count analysis | **PROVEN** |
| N-03 | Timeline has small runtime (11 files) | File count analysis | **PROVEN** |
| N-04 | Workflow has massive runtime (391 files, 2371 runtime imports) | Import analysis | **PROVEN** |
| N-05 | Dashboard has large runtime (154 files, 452 runtime imports) | Import analysis | **PROVEN** |
| N-06 | Auth has 158 files, 22 SSO-related files | File analysis | **PROVEN** |
| N-07 | Auth non-SSO runtime = 254 imports | Import analysis | **PROVEN** |
| N-08 | Auth SSO runtime = 71 imports | Import analysis | **PROVEN** |
| N-09 | `EntityRelation<T>` is pure type alias (zero runtime) | Source inspection | **PROVEN** |
| N-10 | Type-only imports inflate closure by ~90% for entity modules | Import analysis | **PROVEN** |

---

## Recalculated Runtime Graph

| Module | Compile Imports | Runtime Imports | Type-Only | Classification |
|--------|----------------|-----------------|-----------|----------------|
| person | 12 | 1 | 11 | ENTITY-ONLY |
| company | 8 | 0 | 8 | ENTITY-ONLY |
| opportunity | 9 | 1 | 8 | ENTITY-ONLY |
| attachment | 12 | 1 | 11 | ENTITY-ONLY |
| task | 47 | 27 | 20 | SMALL-RUNTIME |
| note | 46 | 27 | 19 | SMALL-RUNTIME |
| timeline | 54 | 37 | 17 | SMALL-RUNTIME |
| workflow | 2389 | 2371 | 18 | MASSIVE-RUNTIME |
| dashboard | 628 | 452 | 176 | LARGE-RUNTIME |
| auth | 803 | 658 | 145 | LARGE-RUNTIME (with SSO) |

---

## Key Findings

1. **Entity modules are trivial** — person, company, opportunity, attachment have 0-1 runtime imports
2. **Workflow is dominant** — 2371 runtime imports, deeply coupled to engine
3. **Dashboard is large** — 452 runtime imports
4. **Auth has SSO coupling** — 71 SSO runtime imports, 22 SSO files
5. **Type-only imports were the root cause** of inflated closure counts

---

## Status

**RECONSTRUCTION IN PROGRESS**

More kernels needed before final verdict.
