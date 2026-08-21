# PHASE 7 — COMPLETE ARCHITECTURE RECOVERY & CRM INTEGRATION

## STATUS: PASS (with conditions)

## ARCHITECTURE CONFIDENCE: MEDIUM-HIGH

---

## 1. Baseline

### BioAcupunt
- Branch: main
- Commit: latest uncommitted changes
- Tests: 171+ passing
- Build: compiles successfully
- CRM entities: CrmPerson, CrmOrganization, CrmLead, CrmPipeline, CrmTask, CrmActivity, CrmTag

### Twenty
- Commit: e5dd07b22
- Server: NestJS/TypeScript
- Frontend: React/TypeScript
- Database: PostgreSQL with schema-per-workspace
- Enterprise files: ~283
- Total files: ~16,276

---

## 2. Architecture Recovered

### Platform
- Auth (core: 35 files, SSO: 16 files)
- JWT (core: 8 files, Enterprise: 4 files)
- Tenant/Workspace (7 files, billing-coupled)
- Metadata engine (40+ modules, essential)
- Record CRUD (10 services)
- Search (1 service, 1 resolver)
- File storage (S3/MinIO/local)

### CRM Domain
- Person (1 file)
- Company (1 file)
- Opportunity (1 file)
- Attachment (1 file)
- Task (8 files)
- Note (8 files)
- Timeline (simplified: activity feed only)

### Deferred
- Workflow (391 files, billing coupling)
- Dashboard (154 files, presentation layer)
- Calendar (integration layer)
- Messaging (integration layer)
- AI modules (separate concern)
- SSO (Enterprise)
- Billing (replaced)
- Usage (Enterprise)

---

## 3. Runtime Closure

**Final CRM Runtime:**
- Platform: ~100 providers (Auth, JWT, Tenant, Metadata, CRUD, Search, Storage, Cache, Jobs, Events)
- CRM: ~30 providers (Person, Company, Opportunity, Task, Note, Timeline, Views, Filters, Permissions)
- Total: ~130 providers

**Previous estimates were inflated due to type-only imports being counted as runtime dependencies.**

---

## 4. Identity

**Decision: Shared Identity Context**

```
Platform
  └── User (authentication identity)
  └── Tenant (organization)
  └── TenantMembership (user ↔ tenant)

CRM
  └── Person (CRM contact) → references User/Tenant
  └── Organization (CRM company) → references Tenant

Healthcare
  └── PatientProfile (extends Person, 1:1)
  └── ProfessionalProfile (extends User, 1:1)
```

**Person is NOT canonical identity.** User is canonical for authentication. Person is CRM-specific contact representation.

---

## 5. Authentication

**Core auth works without SSO.**

```
Auth Core (35 files)
  ├── CredentialGuard
  ├── JwtAuthGuard
  ├── SessionGuard
  ├── AuthResolver (core)
  └── AuthService (core)

SSO (16 files) → REMOVED from imports
  ├── SSOAuthController
  ├── SamlAuthStrategy
  ├── AuthSsoService
  └── CreateSSOConnectedAccountService
```

**JWT Core works without Enterprise key rotation.**

```
JWT Core (8 files)
  ├── JwtService
  ├── TokenService
  ├── JwtStrategy
  └── TokenModule

Enterprise (4 files) → DEFERRED
  └── KeyRotationService
```

---

## 6. Tenant/Workspace

**Twenty Workspace ≈ BioAcupunt Tenant (1:1 mapping)**

```
Workspace Entity
  └── id (UUID) → maps to Tenant ID
  └── databaseSchema → workspace_<base36_id>
  └── activationStatus → tenant status

Tenant Service (new, replaces WorkspaceService)
  └── TenantResolver (JWT → tenant context)
  └── SchemaRouter (tenant → PostgreSQL schema)
  └── TenantContext (propagated through request)
```

**WorkspaceService billing coupling → REPLACED with simplified TenantService**

---

## 7. CRM Entity Kernel

**Entity-only modules (1 file each):**
- Person
- Company
- Opportunity
- Attachment

**Small runtime modules (8 files each):**
- Task
- Note

**Simplified runtime:**
- Timeline (activity feed only, messaging/calendar deferred)

---

## 8. Metadata Kernel

**Metadata engine is ESSENTIAL. Cannot be removed.**

```
MetadataEngineModule
  ├── ObjectMetadataModule (table definitions)
  ├── FieldMetadataModule (column definitions)
  ├── ViewModule (views)
  ├── ViewFieldModule (view fields)
  ├── ViewFilterModule (filters)
  ├── ViewGroupModule (grouping)
  ├── ViewSortModule (sorting)
  ├── RoleModule (RBAC)
  ├── PermissionsModule (authorization)
  ├── ObjectPermissionModule (per-object permissions)
  ├── SearchFieldMetadataModule (search config)
  ├── WorkspaceMetadataVersionModule (cache invalidation)
  └── FlatEntity modules (performance cache)
```

**~80+ providers for metadata kernel alone.**

---

## 9. Search

**CRM search ≠ Knowledge search (separate systems)**

```
CRM Search (Twenty-derived)
  → searches Person, Company, Opportunity, Task, Note
  → metadata-driven
  → workspace-scoped
  → PostgreSQL full-text

Knowledge Search (BioAcupunt existing)
  → searches articles, evidence, clinical knowledge
  → FTS4 on Android, PostgreSQL FTS on backend
  → domain-specific (MTC terminology)
```

**Rule: CRM search must NOT import Knowledge search infrastructure.**

---

## 10. Automation

**Workflow DEFERRED (too complex for initial extraction)**

```
Workflow (391 files)
  ├── 443 runtime imports
  ├── 140 engine dependencies
  ├── billing coupling (6 imports)
  ├── AI tools
  ├── calendar tools
  ├── email tools
  └── messaging tools
```

**Future approach:**
- Build simplified automation in BioAcupunt
- Use CRM API for data
- Reuse metadata for field definitions
- Don't extract Twenty's workflow module

---

## 11. Platform Ports

```
CRM Domain
    ↓
PORT (interface)
    ↓
Platform Implementation
```

**Ports:**
- IdentityPort
- AuthenticationPort
- TenantPort
- AuthorizationPort
- StoragePort
- CachePort
- EventPort
- AuditPort
- JobPort

**CRM code must NEVER import platform implementations directly.**

---

## 12. Domain Boundaries

```
BIOACUPUNT HEALTHCARE SAAS
├── PLATFORM (Auth/Tenant/Storage/Cache/Jobs/Events)
├── CRM (Twenty-derived: Entities/Metadata/Search/Views/Permissions)
├── HEALTHCARE (FastAPI/Python: Patient/Encounter/Clinical/Safety)
├── KNOWLEDGE (FastAPI/Python: Graph/Evidence/Retrieval)
├── AI (FastAPI/Python + Local LLM: Copilot/Orchestration)
└── ANDROID (Kotlin/Room/Compose: Offline-first client)
```

---

## 13. Healthcare Boundary

**Healthcare owns clinical truth. CRM does not.**

```
ALLOWED:
  CRM → PatientProfile lookup (read-only)
  Healthcare → Person reference (read-only)

FORBIDDEN:
  CRM → Clinical persistence
  CRM → SafetyEngine internals
  CRM → Clinical Intelligence internals
```

---

## 14. Knowledge Boundary

**Knowledge owns clinical evidence. CRM does not.**

```
ALLOWED:
  CRM → Knowledge search (via API contract)
  CRM → Evidence summary (read-only)

FORBIDDEN:
  CRM → Knowledge graph internals
  CRM → EvidenceResolver internals
```

---

## 15. AI Boundary

**AI is consumer, never source of truth.**

```
ALLOWED:
  AI → Read CRM data (for context)
  AI → Read Healthcare data (for context)
  AI → Suggest actions (never auto-execute)

FORBIDDEN:
  AI → Auto-create records
  AI → Bypass authorization
  AI → Cross tenant boundaries
```

---

## 16. Commercial/Enterprise

**Enterprise components excluded from CRM:**

| Component | Status | Action |
|-----------|--------|--------|
| SSO (16 files) | EXCLUDED | Remove from imports |
| Billing (15+ files) | EXCLUDED | Replace with BioAcupunt |
| Usage (5+ files) | EXCLUDED | Remove |
| 2FA (3 files) | DEFERRED | Optional security |
| Impersonation (2 files) | EXCLUDED | Remove |
| SDK Client (3 files) | DEFERRED | Code generation |

---

## 17. License

**AGPL-3.0 with Application Use Exception**

- BioAcupunt is SaaS → Application Exception applies
- Must make source available upon request
- Must preserve copyright notices
- Must include AGPL license text
- May keep proprietary Healthcare/AI/Android code private

---

## 18. Provenance

**Documented in:**
- docs/TWENTY_PROVENANCE_AND_LICENSES.md
- docs/CRM_COMPONENT_PROVENANCE.md
- docs/BIOACUPUNT_CRM_ARCHITECTURE.md

---

## 19. Cycles

**No forbidden cross-domain cycles found.**

```
Type-only cycles: 2+ (benign)
Infrastructure cycles: 3+ (expected)
Architectural cycles: 1 (billing ↔ workspace, addressed)
Forbidden cycles: 0 ✅
```

---

## 20. Coupling

**God modules identified:**
- CoreEngineModule (79 imports) — DO NOT extract as-is
- MetadataEngineModule (40+ imports) — extract as Platform component
- WorkspaceModule (15+ imports) — replace with simplified TenantService

**Low-coupling modules (good extraction candidates):**
- Person (1 file)
- Company (1 file)
- Opportunity (1 file)
- Attachment (1 file)
- Search (1 service)

---

## 21. Upstream

**Strategy: Selective cherry-pick**

```
Monthly: Check releases
Quarterly: Evaluate changes
Annually: Major version assessment
```

**Track closely:**
- Security patches
- Metadata engine changes
- Auth changes

**Ignore:**
- Enterprise features
- Billing changes
- UI changes

---

## 22. Behavioral Contracts

**Documented in:**
- docs/phase7/40_BEHAVIORAL_CONTRACTS.md

**Covers:**
- Person CRUD
- Company CRUD
- Opportunity CRUD
- Task CRUD
- Search operations
- View operations
- Authorization checks
- Tenant isolation

---

## 23. Extraction Design

**Documented in:**
- docs/phase7/50_EXTRACTION_DESIGN.md

**Layers:**
1. Platform Infrastructure (Auth, JWT, Tenant, Metadata, CRUD, Search, Storage)
2. CRM Domain (Person, Company, Opportunity, Task, Note, Timeline)
3. CRM Application (Views, Filters, Permissions, Roles)

---

## 24. First Implementation Slice

**Documented in:**
- docs/phase7/51_FIRST_IMPLEMENTATION_SLICE.md

**Slice: Person/Company CRUD**

```
Value × Reversibility ÷ Risk = HIGHEST SCORE
```

**Proves:**
- Metadata engine works
- Schema-per-tenant works
- Auth works without SSO
- Record CRUD works
- Search works
- Views work
- Permissions work
- Tenant isolation works

---

## 25. Remaining Risks

| Risk | Severity | Mitigation |
|------|----------|------------|
| Upstream changes break extraction | MEDIUM | Pin to version, monitor releases |
| License changes | LOW | Pin to version, annual audit |
| Metadata engine complexity | MEDIUM | Extensive testing |
| Migration data loss | HIGH | Behavioral contracts, rollback plan |
| Tenant isolation gaps | HIGH | Runtime verification, fitness functions |

---

## 26. Final Decision

**EXTRACTION MAY PROCEED** with the following conditions:

1. **Phase 7.0.2.1 (Executable Validation)** must PASS before any production implementation
2. **First slice** must be validated in sandbox before proceeding
3. **Architecture fitness functions** must be in place before integration
4. **License compliance** must be verified before deployment

---

## 27. Next Steps

```
1. Phase 7.0.2.1 — Executable Architecture Validation (sandbox)
2. Phase 7.0.3 — Architecture Freeze (after validation)
3. Phase 7.1 — Extraction Design (after freeze)
4. Phase 7.2 — Sandbox Extraction (after design)
5. Phase 7.3 — Runtime Validation (after extraction)
6. Phase 7.4 — BioAcupunt Integration (after validation)
7. Phase 7.5 — Migration (after integration)
8. Phase 7.6 — Decommission (after migration)
```

---

## 28. Documentation Index

All Phase 7 documents are in `docs/phase7/`:

```
01_BASELINE.md                    ← baseline state
03_DEPENDENCY_GRAPH.md            ← dependency analysis
04_DEPENDENCY_CLOSURE.md          ← closure calculation
05_REACHABILITY_ANALYSIS.md       ← reachability from entrypoints
07_DOMAIN_DISCOVERY.md            ← domain boundaries
08_DOMAIN_OWNERSHIP.md            ← ownership matrix
09_CANONICAL_IDENTITY.md          ← identity model
10_TENANCY_ANALYSIS.md            ← tenancy options
11_IDENTITY_KERNEL.md             ← identity reconstruction
13_JWT_KERNEL.md                  ← JWT analysis
13_THREAT_MODEL.md                ← security threats
14_TENANT_WORKSPACE_KERNEL.md     ← tenant/workspace mapping
18_METADATA_KERNEL.md             ← metadata analysis
19_SEARCH_KERNEL.md               ← search analysis
20_TIMELINE_KERNEL.md             ← timeline analysis
22_DASHBOARD_DECISION.md          ← dashboard deferral
23_PLATFORM_PORTS.md              ← platform port interfaces
24_ARCHITECTURAL_INVARIANTS.md    ← invariants
25_ARCHITECTURE_FITNESS_FUNCTIONS.md ← fitness functions
25_HEALTHCARE_BOUNDARY.md         ← healthcare boundary
26_KNOWLEDGE_BOUNDARY.md          ← knowledge boundary
27_AI_BOUNDARY.md                 ← AI boundary
27_GATE_REPORT.md                 ← previous gate report
28_CLAIM_REGISTER.md              ← adversarial claims
29_STORAGE_ISOLATION.md           ← storage isolation
30_CACHE_ISOLATION.md             ← cache isolation
31_WORKER_ISOLATION.md            ← worker isolation
32_DATABASE_BOUNDARY.md           ← database boundary
33_COMMERCIAL_ENTERPRISE_MATRIX.md ← commercial analysis
34_LICENSE_MATRIX.md              ← license analysis
37_CYCLE_ANALYSIS.md              ← cycle detection
38_COUPLING_ANALYSIS.md           ← coupling analysis
38_ADVERSARIAL_GATE_REPORT.md     ← adversarial review
39_UPSTREAM_STRATEGY.md           ← upstream strategy
40_BEHAVIORAL_CONTRACTS.md        ← behavioral contracts
40_CLAIM_RESET.md                 ← claim reset
45_AUTHENTICATION_KERNEL.md       ← auth reconstruction
49_CRM_ENTITY_KERNEL.md           ← CRM entity analysis
50_EXTRACTION_DESIGN.md           ← extraction plan
51_FIRST_IMPLEMENTATION_SLICE.md  ← first slice design
52_AUTOMATION_KERNEL.md           ← workflow analysis
52_PHASE7_FINAL_GATE_REPORT.md    ← THIS DOCUMENT
57_RECONSTRUCTED_ARCHITECTURE.md  ← reconstructed architecture
58_BLOCKER_RESOLUTION_GATE_REPORT.md ← blocker resolution
60_V4_STATUS.md                   ← v4 status tracking
ADR-007-001_CRM_CORE.md           ← ADR: CRM Core
ADR-007-005_TENANCY.md            ← ADR: Tenancy
```

---

**Generated with Codebuff 🤖**
Co-Authored-By: Codebuff <noreply@codebuff.com>
