# Phase 7X — Architectural Invariants

**Date:** 2026-08-21
**Status:** APPROVED
**Purpose:** Permanent rules that cannot be violated

---

## 1. Platform Invariants

| ID | Invariant | Enforcement |
|----|-----------|-------------|
| I-01 | Platform owns authentication | Auth module in Platform context only |
| I-02 | Platform owns tenant resolution | Tenant middleware in Platform context only |
| I-03 | Platform owns user management | User CRUD in Platform context only |

## 2. Domain Invariants

| ID | Invariant | Enforcement |
|----|-----------|-------------|
| I-04 | Healthcare owns clinical truth | Clinical entities written by Healthcare only |
| I-05 | CRM owns CRM truth | CRM entities written by CRM only |
| I-06 | Knowledge owns knowledge truth | Knowledge entities written by Knowledge only |
| I-07 | AI never becomes source of truth | AI reads only, never writes clinical/CRM data |

## 3. Security Invariants

| ID | Invariant | Enforcement |
|----|-----------|-------------|
| I-08 | Cross-tenant access is forbidden | Tenant filter in all queries |
| I-09 | Clinical internals are not CRM dependencies | CRM → Healthcare via reference only |
| I-10 | Commercial modules cannot be hidden dependencies | No billing/SSO in CRM closure |
| I-11 | Identity duplication is minimized | User is canonical (auth), Person is canonical (CRM contact) |
| I-12 | Domain boundaries are enforced automatically | Architecture fitness functions |

## 4. Data Invariants

| ID | Invariant | Enforcement |
|----|-----------|-------------|
| I-13 | PHI never leaves tenant boundary | Schema-per-tenant isolation |
| I-14 | AI never generates clinical content | R4 rule (automated test) |
| I-15 | RAG without evidence = no model call | R2 rule (automated test) |
| I-16 | Safety engine is deterministic | R1 rule (no LLM in safety) |

## 5. Integration Invariants

| ID | Invariant | Enforcement |
|----|-----------|-------------|
| I-17 | Android never accesses database directly | Sync contract only |
| I-18 | Cross-context writes are forbidden | ACL enforcement |
| I-19 | CRM ↔ Healthcare via reference only | personId/patientId references |
| I-20 | Sync contract is versioned | API versioning |

## 6. License Invariants

| ID | Invariant | Enforcement |
|----|-----------|-------------|
| I-21 | AGPL obligations are fulfilled | Provenance tracking |
| I-22 | Commercial modules are excluded | Build-time exclusion |
| I-23 | MIT attribution is preserved | Copyright notices |
| I-24 | Trademarks are not used | BioAcupunt branding only |

---

## 7. Summary

| Category | Count |
|----------|-------|
| Platform | 3 |
| Domain | 4 |
| Security | 5 |
| Data | 4 |
| Integration | 4 |
| License | 4 |
| **TOTAL** | **24** |

**Status:** APPROVED
**Enforcement:** Architecture fitness functions (automated tests)
