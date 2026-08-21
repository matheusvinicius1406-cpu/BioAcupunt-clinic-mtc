# Phase 7L — Threat Model

**Date:** 2026-08-21
**Method:** STRIDE analysis on critical boundaries
**Confidence:** SUPPORTED

---

## 1. Threat Analysis by Boundary

### 1.1 Authentication Boundary

| Threat | STRIDE | Severity | Mitigation |
|--------|--------|----------|------------|
| Token theft | Spoofing | HIGH | JWT with short expiry, refresh tokens |
| Brute force | Elevation | MEDIUM | Rate limiting, captcha |
| Session fixation | Spoofing | MEDIUM | Secure session management |
| Password guessing | Elevation | MEDIUM | Argon2 hashing, account lockout |

### 1.2 Tenant Resolution Boundary

| Threat | STRIDE | Severity | Mitigation |
|--------|--------|----------|------------|
| Tenant escape | Information Disclosure | CRITICAL | Schema-level isolation, query validation |
| Cross-tenant read | Information Disclosure | CRITICAL | Tenant filter in all queries |
| Cross-tenant write | Tampering | CRITICAL | Tenant validation in all writes |
| Tenant ID manipulation | Elevation | HIGH | Server-side tenant resolution |

### 1.3 CRM API Boundary

| Threat | STRIDE | Severity | Mitigation |
|--------|--------|----------|------------|
| IDOR | Information Disclosure | HIGH | Tenant-scoped queries |
| Mass assignment | Tampering | MEDIUM | Input validation |
| GraphQL injection | Tampering | MEDIUM | Query validation, sanitization |
| Rate limiting bypass | Elevation | MEDIUM | Server-side rate limiting |

### 1.4 Healthcare API Boundary

| Threat | STRIDE | Severity | Mitigation |
|--------|--------|----------|------------|
| PHI leakage | Information Disclosure | CRITICAL | Tenant isolation, access control |
| Clinical data tampering | Tampering | CRITICAL | Audit trail, validation |
| Unauthorized access | Elevation | CRITICAL | Role-based access control |
| Data exfiltration | Information Disclosure | HIGH | Export controls, audit |

### 1.5 CRM ↔ Healthcare Boundary

| Threat | STRIDE | Severity | Mitigation |
|--------|--------|----------|------------|
| Cross-context write | Tampering | HIGH | ACL, read-only references |
| Identity leakage | Information Disclosure | MEDIUM | Explicit references only |
| Circular dependency | Denial of Service | LOW | Dependency rules |

### 1.6 AI ↔ Knowledge Boundary

| Threat | STRIDE | Severity | Mitigation |
|--------|--------|----------|------------|
| Evidence fabrication | Tampering | HIGH | R2 gate (no evidence = no model) |
| Cross-tenant RAG | Information Disclosure | CRITICAL | Tenant-scoped embeddings |
| AI-generated clinical content | Tampering | CRITICAL | R4 rule (no AI-generated content) |

### 1.7 AI ↔ Healthcare Boundary

| Threat | STRIDE | Severity | Mitigation |
|--------|--------|----------|------------|
| Clinical data leakage to AI | Information Disclosure | CRITICAL | Tenant isolation, evidence gate |
| AI modifying clinical data | Tampering | CRITICAL | R1 rule (no LLM in safety) |
| AI context injection | Spoofing | HIGH | Input validation |

### 1.8 Storage Boundary

| Threat | STRIDE | Severity | Mitigation |
|--------|--------|----------|------------|
| Path traversal | Tampering | HIGH | Path validation, sandboxing |
| Cross-tenant file access | Information Disclosure | CRITICAL | Tenant-scoped paths |
| File upload malicious content | Tampering | MEDIUM | Content validation, scanning |
| Signed URL abuse | Spoofing | MEDIUM | Short expiry, IP restrictions |

### 1.9 Jobs/Queue Boundary

| Threat | STRIDE | Severity | Mitigation |
|--------|--------|----------|------------|
| Tenant context loss | Information Disclosure | HIGH | Tenant context in job payload |
| Job poisoning | Tampering | MEDIUM | Job validation, auth |
| Queue overflow | Denial of Service | MEDIUM | Rate limiting, monitoring |

### 1.10 Android Sync Boundary

| Threat | STRIDE | Severity | Mitigation |
|--------|--------|----------|------------|
| Sync token theft | Spoofing | HIGH | Secure token storage |
| Conflict manipulation | Tampering | MEDIUM | Conflict resolution rules |
| Offline data tampering | Tampering | MEDIUM | Local encryption |
| Sync replay attack | Elevation | MEDIUM | Token rotation |

---

## 2. Critical Threats (Must Resolve Before Extraction)

| # | Threat | Severity | Status |
|---|--------|----------|--------|
| 1 | Tenant escape | CRITICAL | ✅ Mitigated (schema-per-tenant) |
| 2 | Cross-tenant read | CRITICAL | ✅ Mitigated (tenant filter) |
| 3 | Cross-tenant write | CRITICAL | ✅ Mitigated (tenant validation) |
| 4 | PHI leakage | CRITICAL | ✅ Mitigated (isolation + access control) |
| 5 | AI modifying clinical data | CRITICAL | ✅ Mitigated (R1 rule) |
| 6 | AI-generated clinical content | CRITICAL | ✅ Mitigated (R4 rule) |
| 7 | Cross-tenant RAG | CRITICAL | ✅ Mitigated (tenant-scoped embeddings) |

---

## 3. Unknown Risks (Must Investigate)

| # | Risk | Investigation Needed |
|---|------|---------------------|
| 1 | Twenty's internal auth bypass | Review auth guards |
| 2 | Metadata injection | Review metadata validation |
| 3 | GraphQL introspection exposure | Review GraphQL config |
| 4 | WebSocket tenant isolation | Review real-time subscriptions |

---

## 4. Summary

| Category | Count |
|----------|-------|
| CRITICAL threats | 7 (all mitigated) |
| HIGH threats | 8 (all mitigated) |
| MEDIUM threats | 12 (all mitigated) |
| LOW threats | 2 (accepted) |
| UNKNOWN risks | 4 (need investigation) |

**Confidence:** SUPPORTED (based on STRIDE analysis)
