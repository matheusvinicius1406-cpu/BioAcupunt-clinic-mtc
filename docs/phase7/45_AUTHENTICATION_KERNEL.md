# Phase F — Authentication Kernel Reconstruction

**Date:** 2026-08-21
**Confidence:** SUPPORTED (based on source inspection)

---

## 1. Auth Module Structure

```
auth/ (158 files total)
├── controllers/ (6 files)
│   ├── google-auth.controller.ts
│   ├── google-apis-auth.controller.ts
│   ├── microsoft-auth.controller.ts
│   ├── microsoft-apis-auth.controller.ts
│   ├── oauth-propagator.controller.ts
│   └── sso-auth.controller.ts              ← ENTERPRISE
│
├── strategies/ (13 files)
│   ├── jwt.auth.strategy.ts                ← CORE
│   ├── google.auth.strategy.ts             ← OPTIONAL (OAuth)
│   ├── google-apis-oauth-*.auth.strategy.ts (3) ← OPTIONAL
│   ├── microsoft.auth.strategy.ts          ← OPTIONAL (OAuth)
│   ├── microsoft-apis-oauth-*.auth.strategy.ts (3) ← OPTIONAL
│   ├── oidc.auth.strategy.ts               ← ENTERPRISE
│   └── saml.auth.strategy.ts               ← ENTERPRISE
│
├── guards/ (14 files)
│   ├── is-user-auth-context.guard.ts       ← CORE
│   ├── is-api-key-auth-context.guard.ts    ← CORE
│   ├── is-application-auth-context.guard.ts ← CORE
│   ├── is-system-auth-context.guard.ts     ← CORE
│   ├── enterprise-features-enabled.guard.ts ← ENTERPRISE
│   ├── google-oauth.guard.ts               ← OPTIONAL
│   ├── google-provider-enabled.guard.ts    ← OPTIONAL
│   ├── google-apis-oauth-*.guard.ts (2)    ← OPTIONAL
│   ├── microsoft-oauth.guard.ts            ← OPTIONAL
│   ├── microsoft-provider-enabled.guard.ts ← OPTIONAL
│   ├── microsoft-apis-oauth-*.guard.ts (2) ← OPTIONAL
│   ├── oidc-auth.guard.ts                  ← ENTERPRISE
│   └── saml-auth.guard.ts                  ← ENTERPRISE
│
├── services/ (16 files)
│   ├── auth.service.ts                     ← CORE
│   ├── sign-in-up.service.ts               ← CORE
│   ├── reset-password.service.ts           ← CORE
│   ├── auth-sso.service.ts                 ← ENTERPRISE
│   ├── create-sso-connected-account.service.ts ← ENTERPRISE
│   ├── create-calendar-channel.service.ts  ← OPTIONAL
│   ├── create-connected-account.service.ts ← OPTIONAL
│   ├── create-message-channel.service.ts   ← OPTIONAL
│   ├── google-apis-scopes.service.util.ts  ← OPTIONAL
│   ├── google-apis-service-availability.service.ts ← OPTIONAL
│   ├── google-apis.service.ts              ← OPTIONAL
│   ├── microsoft-apis.service.ts           ← OPTIONAL
│   └── update-connected-account-on-reconnect.service.ts ← OPTIONAL
│
├── dto/ (12 files)
├── constants/ (3 files)
├── filters/ (1 file)
├── middlewares/ (2 files)
├── storage/ (1 file)
├── token/ (4 files)
├── types/ (5 files)
└── utils/ (8 files)
```

---

## 2. Classification

### 2.1 CORE Authentication (Required)

| Component | Purpose | Files |
|-----------|---------|-------|
| jwt.auth.strategy.ts | JWT validation | 1 |
| is-user-auth-context.guard.ts | User auth guard | 1 |
| is-api-key-auth-context.guard.ts | API key guard | 1 |
| is-application-auth-context.guard.ts | App auth guard | 1 |
| is-system-auth-context.guard.ts | System auth guard | 1 |
| auth.service.ts | Core auth logic | 1 |
| sign-in-up.service.ts | Sign in/up | 1 |
| reset-password.service.ts | Password reset | 1 |
| token/ | JWT token handling | 4 |
| storage/ | Session storage | 1 |
| types/ | Auth types | 5 |
| utils/ | Auth utilities | 8 |
| constants/ | Auth constants | 3 |
| dto/ | Auth DTOs | ~8 |
| **TOTAL CORE** | | **~35 files** |

### 2.2 OPTIONAL OAuth (Google/Microsoft)

| Component | Purpose | Files |
|-----------|---------|-------|
| google.auth.strategy.ts | Google OAuth | 1 |
| google-apis-oauth-*.auth.strategy.ts | Google APIs | 3 |
| microsoft.auth.strategy.ts | Microsoft OAuth | 1 |
| microsoft-apis-oauth-*.auth.strategy.ts | Microsoft APIs | 3 |
| google-oauth.guard.ts | Google guard | 1 |
| google-provider-enabled.guard.ts | Google guard | 1 |
| google-apis-oauth-*.guard.ts | Google APIs guards | 2 |
| microsoft-oauth.guard.ts | Microsoft guard | 1 |
| microsoft-provider-enabled.guard.ts | Microsoft guard | 1 |
| microsoft-apis-oauth-*.guard.ts | Microsoft APIs guards | 2 |
| google-auth.controller.ts | Google controller | 1 |
| google-apis-auth.controller.ts | Google APIs controller | 1 |
| microsoft-auth.controller.ts | Microsoft controller | 1 |
| microsoft-apis-auth.controller.ts | Microsoft APIs controller | 1 |
| oauth-propagator.controller.ts | OAuth propagator | 1 |
| google-apis-scopes.service.util.ts | Google scopes | 1 |
| google-apis-service-availability.service.ts | Google availability | 1 |
| google-apis.service.ts | Google service | 1 |
| microsoft-apis.service.ts | Microsoft service | 1 |
| create-calendar-channel.service.ts | Calendar channel | 1 |
| create-connected-account.service.ts | Connected account | 1 |
| create-message-channel.service.ts | Message channel | 1 |
| update-connected-account-on-reconnect.service.ts | Reconnect | 1 |
| **TOTAL OPTIONAL** | | **~28 files** |

### 2.3 ENTERPRISE (Must Remove)

| Component | Purpose | Files |
|-----------|---------|-------|
| sso-auth.controller.ts | SSO controller | 1 |
| auth-sso.service.ts | SSO service | 1 |
| create-sso-connected-account.service.ts | SSO connected account | 1 |
| oidc.auth.strategy.ts | OIDC strategy | 1 |
| saml.auth.strategy.ts | SAML strategy | 1 |
| oidc-auth.guard.ts | OIDC guard | 1 |
| saml-auth.guard.ts | SAML guard | 1 |
| enterprise-features-enabled.guard.ts | Enterprise guard | 1 |
| get-authorization-url-for-sso.dto.ts | SSO DTO | 1 |
| get-authorization-url-for-sso.input.ts | SSO input | 1 |
| get-auth-tokens-from-sso-exchange-token.input.ts | SSO input | 1 |
| sso-exchange-token.service.ts | SSO token service | 1 |
| social-sso-state.type.ts | SSO type | 1 |
| saml.auth.strategy.spec.ts | SAML test | 1 |
| oidc-auth.spec.ts | OIDC test | 1 |
| auth-sso.spec.ts | SSO test | 1 |
| **TOTAL ENTERPRISE** | | **~16 files** |

---

## 3. Can Authentication Work Without SSO?

**YES.**

The core authentication (JWT, sign-in-up, password reset, guards) does NOT import SSO files.

The SSO files are imported by:
- `auth.module.ts` (imports SSOAuthController, SamlAuthStrategy)
- `auth-sso.service.ts` (SSO-specific service)
- `create-sso-connected-account.service.ts` (SSO-specific)

**To remove SSO:**
1. Remove 16 SSO files
2. Remove SSO imports from `auth.module.ts`
3. Verify build passes

**Authentication core (~35 files) remains functional.**

---

## 4. Answer to Key Questions

| Question | Answer |
|----------|--------|
| Can auth operate without SSO? | YES |
| Which files are required? | ~35 core files |
| Which Enterprise code is structurally coupled? | SSO controller, SSO service, SSO guards |
| Can coupling be replaced by interface? | YES — SSO can be behind optional provider |
| What is minimum auth runtime? | JWT + guards + sign-in-up + token handling |
| What belongs to Platform? | Auth is Platform concern, not CRM |

---

## 5. Architecture

```
Authentication Core (Platform)
├── JWT Strategy
├── Guards (user, api-key, application, system)
├── Sign-in/Sign-up
├── Password Reset
├── Token Handling
└── Session Storage

Optional OAuth Adapters (Platform)
├── Google OAuth
├── Google APIs
├── Microsoft OAuth
└── Microsoft APIs

Enterprise Adapters (EXCLUDED)
├── SSO (OIDC, SAML)
├── Enterprise Features Guard
└── SSO Connected Account
```

---

## 6. Confidence

**HIGH** — Based on source file inspection and import analysis.

**Status:** RECONSTRUCTED
