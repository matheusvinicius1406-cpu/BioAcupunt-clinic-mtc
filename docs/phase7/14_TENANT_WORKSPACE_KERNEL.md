# 14 — TENANT/WORKSPACE KERNEL RECONSTRUCTION

## Discovery

### Twenty's Workspace Model

Twenty uses a `workspace` table in the `core` schema as the primary isolation boundary. Key findings from source:

```typescript
// workspace.entity.ts
@Entity({ name: 'workspace', schema: 'core' })
export class WorkspaceEntity {
  @PrimaryGeneratedColumn('uuid') id: string;
  displayName?: string;
  activationStatus: WorkspaceActivationStatus;
  databaseSchema?: string;  // ← per-workspace schema name
  // ...
}

// get-workspace-schema-name.util.ts
export const getWorkspaceSchemaName = (workspaceId: string): string => {
  return `workspace_${uuidToBase36(workspaceId)}`;
};
```

**Twenty already uses schema-per-workspace.** Each workspace gets a unique PostgreSQL schema (`workspace_<base36_id>`). CRM data lives in workspace-specific schemas, not shared tables.

### The Mapping Question

| Twenty Concept | BioAcupunt Concept | Relationship |
|---------------|-------------------|-------------|
| Workspace | Tenant (Clinic) | **≈ 1:1** |
| User | User | Different entity |
| UserWorkspace | TenantMembership | Junction table |
| WorkspaceMember | Professional | Different entity |

### Evidence

- `WorkspaceEntity` lives in `core` schema (shared)
- CRM data entities live in `workspace_<id>` schemas (isolated)
- `UserWorkspaceEntity` is the junction (many-to-many: users ↔ workspaces)
- A user can belong to multiple workspaces
- A workspace has its own metadata, views, roles, permissions

### Critical Finding: Twenty's Schema Is Already Tenant-Isolated

```
core schema (shared)
  └── workspace table (one row per tenant)
  └── user table (shared)
  └── user_workspace table (junction)

workspace_<id> schema (per-tenant)
  └── person table
  └── company table
  └── opportunity table
  └── task table
  └── note table
  └── ... all CRM entities
  └── ... all metadata
  └── ... all views/filters/sorts
```

### Workspace Service Dependencies (from workspace.service.ts)

The `WorkspaceService` imports:
- `BillingSubscriptionService` — **COMMERCIAL**
- `BillingService` — **COMMERCIAL**
- `DnsManagerService` — platform-specific
- `CustomDomainManagerService` — platform-specific
- `SubdomainManagerService` — platform-specific
- `SdkClientGenerationService` — platform-specific
- `FeatureFlagService` — platform
- `UpgradeMigrationService` — platform
- `PermissionsService` — platform
- `MessageQueueService` — infrastructure

**This means WorkspaceService is heavily coupled to platform/commercial concerns.**

### Minimal Workspace Runtime

For CRM isolation, the minimum needed is:
1. `WorkspaceEntity` (table definition)
2. Schema name resolution (`getWorkspaceSchemaName`)
3. Database connection switching (SchemaRoutingDatasource)
4. Workspace context propagation (from JWT → request → service → repository)

NOT needed from WorkspaceService:
- Billing
- DNS management
- Custom domains
- SDK generation
- Upgrade migrations

### Proposed Architecture

```
BIOACUPUNT PLATFORM
├── TenantService (new, replaces WorkspaceService for our use)
│   ├── TenantResolver (JWT → tenant ID)
│   ├── SchemaRouter (tenant → PostgreSQL schema)
│   └── TenantContext (propagated through request)
│
├── CRM Domain
│   └── uses TenantContext for all queries
│
└── Schema-per-tenant (same as Twenty's model)
    ├── workspace_core (shared: users, tenants, memberships)
    ├── workspace_<tenant_1> (CRM data)
    └── workspace_<tenant_2> (CRM data)
```

### Confidence: HIGH

Twenty's schema-per-workspace model is already proven in production. BioAcupunt needs to:
1. Keep the schema routing mechanism
2. Replace WorkspaceService with a simpler TenantService
3. Remove billing/DNS/SDK dependencies from the workspace resolution path

### Decision: Map Twenty Workspace → BioAcupunt Tenant

- Workspace ID = Tenant ID
- Schema routing = Tenant isolation
- UserWorkspace = TenantMembership
- Remove billing coupling from workspace resolution
