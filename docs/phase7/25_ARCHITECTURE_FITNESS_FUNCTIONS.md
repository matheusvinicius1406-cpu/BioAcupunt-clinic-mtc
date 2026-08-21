# Phase 7N — Architecture Fitness Functions

**Date:** 2026-08-21
**Status:** DEFINED
**Purpose:** Automated rules to prevent architectural degradation

---

## 1. Dependency Rules

### 1.1 Forbidden Dependencies

| Rule | Violation | Test |
|------|-----------|------|
| CRM must not import Healthcare internals | `import.*healthcare.*` in CRM | Build-time lint |
| CRM must not import Knowledge internals | `import.*knowledge.*` in CRM | Build-time lint |
| CRM must not import AI internals | `import.*ai.*` in CRM | Build-time lint |
| Healthcare must not depend on CRM internals | `import.*crm.*` in Healthcare | Build-time lint |
| AI must not depend on CRM internals | `import.*crm.*` in AI | Build-time lint |
| Android must not access persistence directly | Direct DB access in Android | Runtime check |

### 1.2 Commercial Exclusion

| Rule | Violation | Test |
|------|-----------|------|
| CRM must not import billing | `import.*billing.*` in CRM | Build-time lint |
| CRM must not import SSO | `import.*sso.*` in CRM | Build-time lint |
| CRM must not import usage | `import.*usage.*` in CRM | Build-time lint |
| CRM must not import enterprise | `import.*enterprise.*` in CRM | Build-time lint |
| CRM must not import impersonation | `import.*impersonation.*` in CRM | Build-time lint |

### 1.3 Domain Boundary

| Rule | Violation | Test |
|------|-----------|------|
| No domain cycles | Circular dependency between contexts | Build-time lint |
| Platform is foundational | Any context importing Platform internals | Build-time lint |
| AI never writes clinical truth | AI mutating clinical entities | Runtime check |

---

## 2. Security Rules

| Rule | Violation | Test |
|------|-----------|------|
| Cross-tenant access forbidden | Query without tenant filter | Runtime test |
| Tenant escape forbidden | Schema switching | Runtime test |
| PHI leakage forbidden | Clinical data in non-healthcare context | Runtime test |
| R2 gate enforced | Model call without evidence | Unit test |
| R4 gate enforced | AI-generated clinical content | Unit test |
| R1 gate enforced | LLM in safety engine | Unit test |

---

## 3. Identity Rules

| Rule | Violation | Test |
|------|-----------|------|
| Person is canonical | Duplicate identity creation | Integration test |
| PatientProfile extends Person | Direct patient creation without person | Integration test |
| User is separate | User == Person assumption | Unit test |

---

## 4. Implementation

### 4.1 Build-Time Checks

```bash
# Dependency lint (ESLint plugin)
npx eslint --rule 'no-restricted-imports: error' .

# Architecture test (custom ESLint rules)
npx eslint --config .eslintrc.architecture.js .
```

### 4.2 Runtime Checks

```typescript
// Tenant isolation test
describe('Tenant Isolation', () => {
  it('should not allow cross-tenant access', async () => {
    const result = await queryAsTenantA(tenantBData);
    expect(result).toBeNull();
  });
});
```

### 4.3 Unit Tests

```typescript
// R2 gate test
describe('AskLibraryUseCase', () => {
  it('should not call model without evidence', async () => {
    const result = await useCase.execute({ query: 'unknown' });
    expect(result.hasEvidence).toBe(false);
    expect(modelCallCount).toBe(0);
  });
});
```

---

## 5. Summary

| Category | Rules | Enforcement |
|----------|-------|-------------|
| Dependency | 11 | Build-time lint |
| Security | 6 | Runtime + unit tests |
| Identity | 3 | Integration tests |
| **TOTAL** | **20** | — |

**Status:** DEFINED
**Enforcement:** Automated in CI/CD pipeline
