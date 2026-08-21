# 33 — COMMERCIAL/ENTERPRISE DEPENDENCY MATRIX

## Methodology

This matrix was produced by:
1. Searching for `@license Enterprise` annotations in Twenty source
2. Tracing runtime imports from CRM entrypoints to Enterprise files
3. Classifying each dependency as: DIRECT, TRANSITIVE, OPTIONAL, BUILD-ONLY

## Enterprise Files in Twenty

Total Enterprise files: **~283** (from previous audit)

### Enterprise in CRM Runtime Closure

| Module | Enterprise Files | Runtime Impact | Classification |
|--------|-----------------|----------------|----------------|
| **auth** | 10 (SSO-related) | HIGH — auth cannot start without SSO providers registered | DIRECT |
| **jwt** | 4 (key rotation) | MEDIUM — core JWT works, rotation is add-on | OPTIONAL |
| **billing** | 15+ | HIGH — WorkspaceService imports BillingService | DIRECT in WorkspaceService |
| **usage** | 5+ | MEDIUM — usage tracking for billing | OPTIONAL |
| **sso** | 20+ | HIGH — entire module is Enterprise | EXCLUDED |
| **2fa** | 3 | LOW — optional security feature | DEFER |
| **impersonation** | 2 | LOW — admin-only feature | DEFER |
| **upgrade** | 5+ | MEDIUM — upgrade commands | PLATFORM |
| **sdk-client** | 3 | LOW — code generation | DEFER |

### Critical Finding: Auth ↔ SSO Coupling

```
AuthModule imports:
  ├── SSOAuthController        ← Enterprise
  ├── SamlAuthStrategy         ← Enterprise
  ├── AuthSsoService           ← Enterprise
  └── CreateSSOConnectedAccountService ← Enterprise
```

**The auth module has hard imports to SSO Enterprise files.**

However, analysis shows:
- SSO controllers are registered via NestJS module imports
- Core auth (credentials, JWT, session) does NOT depend on SSO
- SSO is an OPTIONAL module that can be removed from module imports

**Solution:** Remove SSO module from AuthModule imports. Core auth continues to work.

### Critical Finding: WorkspaceService ↔ Billing Coupling

```
WorkspaceService imports:
  ├── BillingSubscriptionService  ← Commercial
  ├── BillingService              ← Commercial
  └── FeatureFlagService          ← Platform (not commercial)
```

**WorkspaceService has hard imports to billing services.**

This means:
- Workspace provisioning requires billing check
- Workspace deletion requires billing cleanup
- Feature flags may gate billing features

**Solution:** Create a simplified TenantService that does NOT import billing. Workspace/Billing coupling remains in Twenty's WorkspaceService but is not used by BioAcupunt.

### Enterprise in Platform Closure

| Module | Enterprise Files | Required? | Action |
|--------|-----------------|-----------|--------|
| auth (SSO) | 10 | NO | Remove from imports |
| jwt (rotation) | 4 | NO | Defer |
| billing | 15+ | NO | Replace with BioAcupunt billing |
| usage | 5+ | NO | Remove |
| sso | 20+ | NO | Remove entirely |
| 2fa | 3 | DEFER | Optional security |
| impersonation | 2 | NO | Remove |

### Decision Matrix

| Component | Enterprise? | Can Remove? | Replacement |
|-----------|-------------|-------------|-------------|
| Core auth (credentials) | NO | N/A | Keep |
| SSO auth | YES | YES | Remove from imports |
| JWT core | NO | N/A | Keep |
| JWT key rotation | YES | YES | Defer |
| Workspace core | NO | N/A | Keep |
| Workspace billing | YES | YES | BioAcupunt billing |
| Metadata engine | NO | N/A | Keep |
| Record CRUD | NO | N/A | Keep |
| Search | NO | N/A | Keep |
| Views | NO | N/A | Keep |
| Permissions | NO | N/A | Keep |
| File storage | NO | N/A | Keep |

### Confidence: MEDIUM

The analysis is based on static imports. Runtime behavior may differ. Executable validation (Phase 7.0.2.1) is needed to confirm.
