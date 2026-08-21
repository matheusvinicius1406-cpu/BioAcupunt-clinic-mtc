# 50 — EXTRACTION DESIGN

## Scope

Design the extraction of Twenty components into BioAcupunt CRM.

## Extraction Layers

### Layer 1: Platform Infrastructure (extract first)

| Component | Source | Action | Blast Radius |
|-----------|--------|--------|-------------|
| Auth core | twenty-server/auth (35 core files) | ADAPT | HIGH |
| JWT core | twenty-server/jwt (8 core files) | ADAPT | HIGH |
| Workspace/Tenant | twenty-server/workspace (7 files) | ADAPT | HIGH |
| Metadata engine | twenty-server/metadata-modules | REUSE | VERY HIGH |
| Record CRUD | twenty-server/record-crud | REUSE | HIGH |
| File storage | twenty-server/file-storage | REUSE | MEDIUM |
| Search | twenty-server/search | REUSE | LOW |

### Layer 2: CRM Domain (extract second)

| Component | Source | Action | Blast Radius |
|-----------|--------|--------|-------------|
| Person | twenty-server/modules/person (1 file) | REUSE | LOW |
| Company | twenty-server/modules/company (1 file) | REUSE | LOW |
| Opportunity | twenty-server/modules/opportunity (1 file) | REUSE | LOW |
| Attachment | twenty-server/modules/attachment (1 file) | REUSE | LOW |
| Task | twenty-server/modules/task (8 files) | REUSE | LOW |
| Note | twenty-server/modules/note (8 files) | REUSE | LOW |
| Timeline | twenty-server/modules/timeline (simplified) | ADAPT | MEDIUM |

### Layer 3: CRM Application (extract third)

| Component | Source | Action | Blast Radius |
|-----------|--------|--------|-------------|
| Views | twenty-server/metadata-modules/view* | REUSE | MEDIUM |
| Filters | twenty-server/metadata-modules/view-filter* | REUSE | MEDIUM |
| Permissions | twenty-server/metadata-modules/permissions* | REUSE | HIGH |
| Roles | twenty-server/metadata-modules/role* | REUSE | HIGH |

### Deferred (NOT extracted)

| Component | Reason |
|-----------|--------|
| Workflow | Too complex (391 files, billing coupling) |
| Dashboard | Presentation layer (154 files) |
| Calendar | Integration layer (deferred) |
| Messaging | Integration layer (deferred) |
| AI modules | Separate concern |
| SSO | Enterprise |
| Billing | Replaced |
| Usage | Enterprise |

## Extraction Order

```
Phase 7.2.1: Auth Core + JWT Core + Tenant Service
    ↓
Phase 7.2.2: Metadata Engine + Record CRUD
    ↓
Phase 7.2.3: Person + Company + Opportunity + Attachment
    ↓
Phase 7.2.4: Task + Note + Timeline (simplified)
    ↓
Phase 7.2.5: Views + Filters + Permissions + Roles
    ↓
Phase 7.2.6: Search + File Storage
    ↓
Phase 7.2.7: Integration testing
    ↓
Phase 7.2.8: BioAcupunt platform integration
```

## Anti-Corruption Layers

### Twenty Person → BioAcupunt Person

```typescript
// ACL: maps Twenty person to BioAcupunt person
interface PersonAdapter {
  toBioAcupunt(twentyPerson: TwentyPerson): BioAcupuntPerson;
  toTwenty(bioAcupuntPerson: BioAcupuntPerson): TwentyPerson;
}
```

### Twenty Workspace → BioAcupunt Tenant

```typescript
// ACL: maps Twenty workspace to BioAcupunt tenant
interface TenantAdapter {
  toBioAcupunt(twentyWorkspace: WorkspaceEntity): Tenant;
  workspaceIdToTenantId(workspaceId: string): string;
}
```

## Files to Modify

### Auth Module
```
REMOVE from imports:
  - SSOAuthController
  - SamlAuthStrategy
  - AuthSsoService
  - CreateSSOConnectedAccountService

KEEP:
  - CredentialGuard
  - JwtAuthGuard
  - SessionGuard
  - AuthResolver (core)
  - AuthService (core)
```

### JWT Module
```
REMOVE from imports:
  - KeyRotationService (Enterprise)

KEEP:
  - JwtService (core)
  - TokenService (core)
  - JwtStrategy (core)
```

### Workspace Module
```
CREATE NEW:
  - TenantService (replaces WorkspaceService for BioAcupunt)
  - TenantResolver (JWT → tenant context)

REMOVE:
  - BillingSubscriptionService imports
  - BillingService imports
  - DnsManagerService imports
  - CustomDomainManagerService imports
  - SdkClientGenerationService imports
```

## Validation Checklist

For each extraction phase:
- [ ] Build succeeds
- [ ] Unit tests pass
- [ ] Integration tests pass
- [ ] No Enterprise imports
- [ ] No commercial imports
- [ ] Tenant isolation verified
- [ ] Auth works without SSO
- [ ] Metadata engine starts
- [ ] CRUD operations work
- [ ] Search works
- [ ] Views work
- [ ] Permissions work
