# 34 — LICENSE MATRIX

## Root License

Twenty uses `AGPL-3.0` with an `Application Use Exception` (see LICENSE file).

### Application Use Exception

```
For the avoidance of doubt:

1. If you combine or link the AGPL Software with your own software
   ("Your Application"), Your Application may not be considered a
   "modified version" of the AGPL Software if it does not include
   any of the AGPL Software's source code.

2. You may use the AGPL Software in the backend of Your Application
   without triggering the AGPL's copyleft requirements, provided that:
   a) Your Application is a SaaS application;
   b) You do not distribute the AGPL Software;
   c) You make available the source code of Your Application upon request.
```

## Package-Level Licenses

| Package | License | Can Use? | Conditions |
|---------|---------|----------|------------|
| twenty-server | AGPL-3.0 + App Exception | YES | SaaS only, source on request |
| twenty-front | AGPL-3.0 + App Exception | YES | SaaS only, source on request |
| twenty-ui | MIT | YES | Preserve copyright |
| twenty-shared | MIT | YES | Preserve copyright |
| twenty-utils | MIT | YES | Preserve copyright |

## Component-Level License Classification

### MIT (use freely)

| Component | License | Copyright |
|-----------|---------|-----------|
| twenty-ui | MIT | Twenty PBC |
| twenty-shared | MIT | Twenty PBC |
| twenty-utils | MIT | Twenty PBC |

### AGPL-3.0 + Application Exception (use with conditions)

| Component | License | Conditions |
|-----------|---------|------------|
| twenty-server core | AGPL-3.0 + App Exc | SaaS, source on request |
| twenty-server modules | AGPL-3.0 + App Exc | SaaS, source on request |
| twenty-front | AGPL-3.0 + App Exc | SaaS, source on request |

### Enterprise/Commercial (excluded)

| Component | License | Action |
|-----------|---------|--------|
| SSO modules | Enterprise | EXCLUDE |
| Billing modules | Enterprise | EXCLUDE (replace) |
| Usage modules | Enterprise | EXCLUDE |
| 2FA modules | Enterprise | DEFER |
| Impersonation | Enterprise | EXCLUDE |
| SDK client | Enterprise | DEFER |

## Obligations for AGPL Components

1. **Preserve copyright notices** — keep all `Copyright (c) Twenty PBC` notices
2. **License text** — include AGPL-3.0 license text in distribution
3. **Source availability** — make source code available upon request (SaaS deployment)
4. **Modifications** — document any modifications made
5. **No additional restrictions** — cannot add restrictions beyond AGPL

## BioAcupunt Obligations

As a SaaS application:
1. BioAcupunt MUST make source code available upon user request
2. BioAcupunt MUST preserve Twenty's copyright notices
3. BioAcupunt MUST include AGPL-3.0 license text
4. BioAcupunt MUST document modifications to Twenty code
5. BioAcupunt MAY keep proprietary Healthcare/AI/Android code private

## Third-Party Dependencies

Twenty uses many third-party packages. Key ones:

| Package | License | Compatible? |
|---------|---------|-------------|
| NestJS | MIT | YES |
| TypeORM | MIT | YES |
| GraphQL | MIT | YES |
| React | MIT | YES |
| TypeScript | Apache-2.0 | YES |
| PostgreSQL | PostgreSQL License | YES |
| Redis | BSD-3-Clause | YES |

### Confidence: HIGH

The license structure is clear. The Application Use Exception makes AGPL usable for SaaS. Enterprise components are excluded. Third-party dependencies are compatible.
