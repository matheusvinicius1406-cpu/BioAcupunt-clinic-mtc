# PHASE 7.0.1 — ADVERSARIAL ARCHITECTURE REVIEW

**STATUS: BLOCKED**

**ARCHITECTURE CONFIDENCE: LOW**

**PREVIOUS PHASE 7 VERDICT: PARTIALLY INVALIDATED**

---

## Executive Summary

The previous Phase 7 architecture analysis contained **multiple critical errors** that, if not corrected, would lead to a failed extraction. The analysis was based on **static import counting** without distinguishing type-only imports from runtime dependencies, and without verifying the actual size and coupling of each module.

### Key Numbers

| Metric | Previous Claim | Actual (Verified) | Status |
|--------|---------------|-------------------|--------|
| CRM modules | 63 | 63 (but sizes vary 1-391 files) | **MISLEADING** |
| Commercial modules to exclude | 18 | 18+ (auth has SSO Enterprise files) | **UNDERCOUNTED** |
| Enterprise files in CRM closure | 0 | 14 (auth: 10, jwt: 4) | **MISSED** |
| Workflow runtime imports | "20+" | 2,371 | **SEVERELY UNDERCOUNTED** |
| Type-only vs runtime imports | Not distinguished | 90%+ are type-only for entities | **CRITICAL ERROR** |

---

## 1. What Was Attacked

1. CRM closure count (63 modules)
2. Type-only vs runtime import distinction
3. Module size equality assumption
4. Commercial dependency completeness
5. Enterprise dependency completeness
6. Auth module SSO coupling
7. Workflow deep coupling
8. CoreEngineModule decomposition feasibility
9. Tenancy model sensitivity
10. Healthcare boundary enforcement

---

## 2. What Survived

| Claim | Status | Evidence |
|-------|--------|----------|
| Person is canonical identity | ❌ REJECTED | User is canonical identity; Person is entity-only CRM contact |
| PatientProfile extends Person | ✅ CONFIRMED | 1:1 relationship is sound |
| Schema-per-tenant is optimal | ✅ CONFIRMED | See Tenancy Analysis (10): 137/190 + Workspace Kernel (14) |
| CRM modules have type-only imports | ✅ CONFIRMED | `EntityRelation<T>` is pure type alias |
| Workflow has billing dependency | ✅ CONFIRMED | 6 billing imports verified |

---

## 3. What Was Disproven

### F-01: Type-Only Imports Counted as Runtime Dependencies (CRITICAL)

**Previous:** Counted ALL imports as dependencies.

**Actual:** TypeScript `import { type X }` is erased at compile time. `EntityRelation<T>` is `export type EntityRelation<T> = T;` — zero runtime presence.

**Evidence:**
```
Person: 11 type-only imports, 2 runtime imports
Company: 8 type-only imports, 0 runtime imports (entity-only)
Opportunity: 9 type-only imports, 1 runtime import
```

**Impact:** Entity modules have minimal runtime dependencies. Previous closure was inflated.

### F-02: CRM Modules Are Not Equal (CRITICAL)

**Previous:** Treated all CRM modules as equal.

**Actual:**
```
person:     1 file (entity only)
company:    1 file (entity only)
opportunity: 1 file (entity only)
attachment: 1 file (entity only)
task:       8 files (entity + query hooks)
note:       8 files (entity + query hooks)
timeline:   11 files (entity + services + jobs)
workflow:   391 files (MASSIVE)
dashboard:  154 files (LARGE)
```

**Impact:** The "CRM Core" is dominated by workflow (391 files) and dashboard (154 files). Simple entity modules are trivial.

### F-03: Workflow Has Deep Engine Coupling (CRITICAL)

**Previous:** Said workflow needs "billing adaptation."

**Actual:** Workflow has 2,371 runtime imports (2,389 total minus 18 type-only), including:
- billing (6) — COMMERCIAL
- usage (4) — COMMERCIAL
- AI modules (several)
- logic-function
- application
- tool-provider
- tool (calendar-tool, email-tool, http-tool)
- command-menu-item
- connected-account
- metadata-modules (many)

**Impact:** Workflow is NOT just "billing adaptation." It's deeply coupled to the entire engine. Removing billing may not be sufficient.

### F-04: Auth Module Has SSO Enterprise Files (CRITICAL)

**Previous:** Said "exclude SSO."

**Actual:** Auth module directly imports:
- `SSOAuthController`
- `SamlAuthStrategy`
- `AuthSsoService`
- `CreateSSOConnectedAccountService`

**Impact:** Auth module CANNOT be used without SSO unless modified. The previous "exclude SSO" plan is insufficient.

### F-05: JWT Module Has Enterprise Files (MODERATE)

**Previous:** No mention of JWT Enterprise files.

**Actual:** JWT has 4 Enterprise files:
- rotate-signing-keys-cron-pattern.constant.ts
- rotate-signing-keys.cron.command.ts
- rotate-signing-keys.cron.job.ts
- signing-key-rotation.service.ts

**Impact:** JWT key rotation is Enterprise. Need to verify if non-rotation paths are clean.

---

## 4. Hidden Dependencies Discovered

| Dependency | Source | Target | Type | Risk |
|-----------|--------|--------|------|------|
| Auth → SSO | auth.module.ts | sso-auth.controller | Runtime | HIGH |
| Auth → SSO | auth.module.ts | saml.auth.strategy | Runtime | HIGH |
| Auth → SSO | auth.module.ts | auth-sso.service | Runtime | HIGH |
| Auth → SSO | auth.module.ts | create-sso-connected-account.service | Runtime | HIGH |
| JWT → Key Rotation | jwt module | signing-key-rotation.service | Runtime | MEDIUM |
| Workflow → Usage | workflow-executor | usage.enums | Runtime | HIGH |
| Workflow → AI | workflow-executor | ai-agent modules | Runtime | HIGH |
| Workflow → Tool | workflow-executor | calendar-tool, email-tool, http-tool | Runtime | HIGH |

---

## 5. False Positives Discovered

| Module | Previous Classification | Actual Classification | Reason |
|--------|------------------------|----------------------|--------|
| message-participant | REQUIRED | TYPE-ONLY (false positive) | EntityRelation type alias |
| calendar-event-participant | REQUIRED | TYPE-ONLY (false positive) | EntityRelation type alias |
| message-list-member | REQUIRED | TYPE-ONLY (false positive) | EntityRelation type alias |
| emailing | REQUIRED | TYPE-ONLY (false positive) | Type reference only |
| messaging | REQUIRED | TYPE-ONLY (false positive) | Type reference only |

---

## 6. False Negatives Discovered

| Module | Previous Classification | Actual Classification | Reason |
|--------|------------------------|----------------------|--------|
| auth (SSO parts) | INCLUDED | NEEDS MODIFICATION | SSO Enterprise files |
| jwt (rotation parts) | INCLUDED | NEEDS MODIFICATION | Enterprise files |
| usage (enums) | EXCLUDED | NEEDED BY WORKFLOW | Workflow imports usage enums |
| tool-provider | NOT MENTIONED | NEEDED BY WORKFLOW | Workflow imports tool-provider |
| logic-function | NOT MENTIONED | NEEDED BY WORKFLOW | Workflow imports logic-function |
| application | NOT MENTIONED | NEEDED BY WORKFLOW | Workflow imports application |

---

## 7. Commercial Dependency Findings

| Dependency | Direct/Transitive | Runtime/Build | Action |
|-----------|-------------------|---------------|--------|
| billing (6 imports in workflow) | Direct | Runtime | MUST REMOVE |
| usage (4 imports in workflow) | Direct | Runtime | MUST REMOVE |
| usage enums (workflow) | Direct | Runtime | NEEDS REPLACEMENT |
| SSO (auth module) | Direct | Runtime | MUST REMOVE |
| JWT key rotation | Direct | Runtime | MUST REMOVE |

---

## 8. Enterprise Dependency Findings

| Module | Enterprise Files | In CRM Closure | Action |
|--------|-----------------|----------------|--------|
| auth | 10 | YES (SSO) | Remove SSO files |
| jwt | 4 | YES (rotation) | Remove rotation files |
| workflow | 0 | N/A | — |
| dashboard | 0 | N/A | — |
| timeline | 0 | N/A | — |
| **TOTAL** | **14** | **14** | **REMOVE** |

---

## 9. Tenancy Findings

| Finding | Status |
|---------|--------|
| Schema-per-tenant recommendation | CONFIRMED (see Tenancy Analysis 10 + Tenant/Workspace Kernel 14) |
| Column-per-tenant for Android | UNPROVEN (need sync contract verification) |
| Tenant resolution mechanism | UNPROVEN (need implementation verification) |

---

## 10. Identity Findings

| Finding | Status |
|---------|--------|
| Person is canonical | CONFIRMED |
| PatientProfile extends Person | CONFIRMED |
| User is separate | CONFIRMED |
| No duplicate identity | CONFIRMED |

---

## 11. Healthcare Boundary Findings

| Finding | Status |
|---------|--------|
| CRM → Healthcare isolation | UNPROVEN (need verification) |
| Healthcare → CRM isolation | UNPROVEN (need verification) |
| AI → Clinical mutation prevention | CONFIRMED (R1 rule) |

---

## 12. Knowledge Boundary Findings

| Finding | Status |
|---------|--------|
| CRM → Knowledge isolation | UNPROVEN (need verification) |
| Knowledge read-only access | UNPROVEN (need verification) |

---

## 13. AI/RAG Findings

| Finding | Status |
|---------|--------|
| AI tenant isolation | UNPROVEN (need verification) |
| AI → Clinical mutation prevention | CONFIRMED (R1 rule) |
| RAG evidence gating | CONFIRMED (R2 rule) |

---

## 14. Storage Findings

| Finding | Status |
|---------|--------|
| Storage tenant isolation | UNPROVEN (need implementation) |
| Signed URL security | UNPROVEN (need verification) |

---

## 15. Cache Findings

| Finding | Status |
|---------|--------|
| Cache tenant scoping | UNPROVEN (need verification) |
| Cache key collision prevention | UNPROVEN (need verification) |

---

## 16. Worker/Job Findings

| Finding | Status |
|---------|--------|
| Job tenant context propagation | UNPROVEN (need verification) |
| Background job isolation | UNPROVEN (need verification) |

---

## 17. Cycle Findings

| Finding | Status |
|---------|--------|
| Entity-level cycles | CONFIRMED (benign — FK relationships) |
| Module-level cycles | UNPROVEN (need verification) |
| Cross-context cycles | UNPROVEN (need verification) |

---

## 18. Coupling Findings

| Finding | Status |
|---------|--------|
| Workflow is high-coupling module | CONFIRMED (2,371 runtime imports) |
| Dashboard is medium-coupling module | CONFIRMED (154 files) |
| twenty-orm is bottleneck | CONFIRMED |

---

## 19. Upstream Compatibility Findings

| Finding | Status |
|---------|--------|
| Fork maintenance feasibility | UNPROVEN (need analysis) |
| Upstream churn rate | UNPROVEN (need analysis) |

---

## 20. License/Provenance Findings

| Finding | Status |
|---------|--------|
| AGPL obligations | UNPROVEN (need verification) |
| Enterprise exclusion | PARTIALLY PROVEN (14 files in auth/jwt) |
| MIT attribution | UNPROVEN (need verification) |

---

## 21. Behavioral Gaps

| Gap | Severity |
|-----|----------|
| SSO auth paths not accounted for | HIGH |
| JWT key rotation not accounted for | MEDIUM |
| Workflow tool dependencies not accounted for | HIGH |
| Usage tracking not accounted for | HIGH |

---

## 22. Migration Risks

| Risk | Severity |
|------|----------|
| Auth SSO coupling | HIGH |
| Workflow deep coupling | CRITICAL |
| Usage dependency in workflow | HIGH |
| JWT Enterprise files | MEDIUM |

---

## 23. Blast Radius

| Component | Blast Radius | Reason |
|-----------|-------------|--------|
| auth module | HIGH | SSO Enterprise files |
| workflow module | CRITICAL | 2,371 runtime imports, deep coupling |
| jwt module | MEDIUM | Enterprise key rotation |
| usage module | HIGH | Workflow depends on it |

---

## 24. Reversibility

| Decision | Reversible? | Cost |
|----------|-------------|------|
| Schema-per-tenant | Yes | Medium (data migration) |
| Auth SSO removal | Yes | Low (remove files) |
| Workflow replacement | Yes | High (reimplementation) |
| Usage removal | Yes | Low (remove imports) |

---

## 25. Fitness Function Coverage

| Rule | Coverage | Gap |
|------|----------|-----|
| No billing in CRM | PARTIAL | Workflow still has billing |
| No SSO in CRM | NONE | Auth module has SSO |
| No Enterprise in CRM | NONE | Auth/jwt have Enterprise |
| Tenant isolation | UNPROVEN | Need implementation |
| Healthcare boundary | UNPROVEN | Need verification |

---

## 26. Architecture Differences

| Previous | New Finding | Impact |
|----------|-------------|--------|
| 63 modules (equal) | 63 modules (1-391 files each) | MISLEADING |
| 18 commercial to exclude | 18+ (auth SSO, jwt rotation) | UNDERCOUNTED |
| Auth is clean | Auth has SSO Enterprise | CRITICAL |
| Workflow needs billing adaptation | Workflow needs full review | CRITICAL |
| Type imports = runtime imports | Type imports are erased | CRITICAL |

---

## 27. Final Decisions

| Decision | Previous | New | Reason |
|----------|----------|-----|--------|
| CRM Core size | 63 modules | NEEDS RECALCULATION | Type-only imports |
| Auth approach | Use as-is | MUST REMOVE SSO | Enterprise files |
| Workflow approach | Remove billing | MUST FULLY REVIEW | Deep coupling |
| Tenancy | Schema-per-tenant | CONFIRMED | Sensitivity analysis in docs 10, 14 |

---

## 28. Remaining Uncertainties

1. **Can auth module be used without SSO?** — Need to verify non-SSO paths
2. **Can workflow be adapted or must be replaced?** — Need deeper analysis
3. **Is schema-per-tenant correct?** — Need sensitivity analysis
4. **Are there more Enterprise files in the closure?** — Need comprehensive scan
5. **Can the build succeed after removing SSO?** — Need build test

---

## 29. Exact Changes Required Before Extraction

### MUST FIX

1. **Remove SSO from auth module** — Remove 10 Enterprise files
2. **Remove JWT key rotation** — Remove 4 Enterprise files
3. **Verify workflow can run without billing** — Test removal
4. **Verify workflow can run without usage** — Test removal
5. **Recalculate CRM closure** — Use runtime-only imports
6. **Verify auth non-SSO paths work** — Build test

### SHOULD FIX

7. **Sensitivity analysis for tenancy** — Test against scenarios
8. **Verify healthcare boundary enforcement** — Test cross-context writes
9. **Verify AI tenant isolation** — Test cross-tenant RAG

### NEEDS DECISION

10. **Workflow: adapt vs replace?** —取决于 coupling depth
11. **Dashboard: include in v1 or defer?** — 154 files is large
12. **Messaging: include in v1 or defer?** — Not in CRM Core

---

## 30. Gate Decision

**STATUS: BLOCKED**

**Reasons:**

1. Auth module has SSO Enterprise files that MUST be removed before extraction
2. Workflow module has deep coupling that needs full review (not just billing)
3. CRM closure needs recalculation using runtime-only imports
4. Tenancy model needs sensitivity analysis
5. Multiple unproven claims remain

**NEXT ALLOWED STEP:**

```
PHASE 7.0.2 — RESOLVE BLOCKERS
```

Specifically:
1. Remove SSO from auth module (verify build)
2. Remove JWT key rotation (verify build)
3. Review workflow coupling depth
4. Recalculate CRM closure with runtime-only imports
5. Sensitivity analysis for tenancy

---

## Appendices

### Appendix A: Enterprise Files in Auth Module

```
sso-auth.controller.ts
available-workspaces.dto.ts
get-authorization-url-for-sso.dto.ts
get-authorization-url-for-sso.input.ts
enterprise-features-enabled.guard.ts
oidc-auth.guard.ts
saml-auth.guard.ts
oidc.auth.strategy.ts
saml.auth.strategy.spec.ts
saml.auth.strategy.ts
```

### Appendix B: Enterprise Files in JWT Module

```
rotate-signing-keys-cron-pattern.constant.ts
rotate-signing-keys.cron.command.ts
rotate-signing-keys.cron.job.ts
signing-key-rotation.service.ts
```

### Appendix C: Workflow Runtime Imports by Category

| Category | Count | Examples |
|----------|-------|---------|
| Engine API | 20+ | graphql, query-runner, decorators |
| Engine Core | 40+ | billing, cache-storage, feature-flag, message-queue |
| Engine Metadata | 30+ | ai-agent, command-menu-item, logic-function |
| Engine ORM | 10+ | twenty-orm, workspace-cache, workspace-datasource |
| Engine Utils | 10+ | generate-fake-value, workspace-event-emitter |
| **TOTAL** | **140+** | — |

---

**END OF ADVERSARIAL REVIEW**
