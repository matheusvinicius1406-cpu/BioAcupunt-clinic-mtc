# 22 — DASHBOARD DECISION

## Discovery

### Twenty's Dashboard Module

```
src/modules/dashboard/
├── dashboard.module.ts
├── dashboard.module-definition.ts
├── standard-objects/
│   ├── dashboard/                ← dashboard entity
│   ├── dashboard-card/           ← dashboard cards
│   └── dashboard-datasource/     ← data sources
├── services/
│   └── dashboard-query-builder   ← query construction
└── utils/
```

**File count: ~154 files**

### Dashboard Dependencies

- `ObjectMetadataModule` — knows what objects exist
- `FieldMetadataModule` — knows what fields exist
- `ViewModule` — reuses view definitions
- `PermissionsModule` — authorization
- `WorkspaceCacheModule` — caching

### Analysis

Dashboard is a **presentation layer** feature that:
- Aggregates data from multiple CRM objects
- Renders charts, tables, metrics
- Supports customization (add/remove cards)
- Is metadata-driven (reads object/field metadata)

### Is Dashboard Required for MVP?

**No.** Dashboard is:
- High complexity (154 files)
- Presentation-heavy (visualization logic)
- Not core CRM functionality
- Can be added later without architectural changes

### Decision: DEFER

```
Dashboard = DEFERRED
Reason:
  - 154 files = large blast radius
  - Presentation layer, not domain
  - Can be added after core CRM is stable
  - No architectural dependency on CRM core

Future approach:
  - Build custom dashboard in BioAcupunt web
  - Use CRM API for data
  - Reuse metadata for field definitions
  - Don't extract Twenty's dashboard module
```

### Confidence: HIGH

Dashboard deferral is low-risk. Core CRM operations (CRUD, search, views, permissions) work without dashboard.
