# 18 — METADATA KERNEL RECONSTRUCTION

## Discovery

### Twenty's Metadata Engine

The `MetadataEngineModule` (src/engine/metadata-modules/metadata-engine.module.ts) aggregates ~40+ metadata modules:

```
MetadataEngineModule
├── FieldMetadataModule        ← field definitions
├── ObjectMetadataModule       ← object/table definitions
├── ViewModule                 ← views (list, kanban, calendar)
├── ViewFieldModule            ← view field configuration
├── ViewFilterModule           ← view filters
├── ViewFilterGroupModule      ← filter groups
├── ViewGroupModule            ← grouping
├── ViewSortModule             ← sorting
├── RoleModule                 ← RBAC roles
├── PermissionsModule          ← permissions
├── PermissionFlagModule       ← permission flags
├── ObjectPermissionModule     ← per-object permissions
├── SearchFieldMetadataModule  ← search configuration
├── WebhookModule              ← webhooks
├── LogicFunctionModule        ← server-side logic functions
├── LogicFunctionLayerModule   ← logic function layers
├── CommandMenuItemModule      ← command palette items
├── NavigationMenuItemModule   ← navigation items
├── FrontComponentModule       ← frontend component registry
├── AiAgentModule              ← AI agents
├── AiChatModule               ← AI chat
├── AiGenerateTextModule       ← AI text generation
├── AiWorkspaceStatsModule     ← AI usage stats
├── SkillModule                ← AI skills
├── CalendarChannelModule      ← calendar integration
├── ConnectedAccountModule     ← connected accounts
├── MessageChannelModule       ← messaging channels
├── MessageFolderModule        ← message folders
├── WorkspaceMetadataVersionModule ← version tracking
├── MinimalMetadataModule      ← lightweight metadata
├── ApplicationConnectionsModule ← app connections
├── ApplicationJobModule       ← app jobs
├── ApplicationKeyValueModule  ← app key-value
└── RouteTriggerModule         ← route triggers
```

### What Is Truly Required for CRM

The metadata engine is the **brain** of Twenty's extensibility. It powers:
- Dynamic table creation (object → schema DDL)
- Dynamic field management (field → column)
- Dynamic relationship management (relation → FK/join)
- View construction (what fields, filters, sorts, groups)
- Permission evaluation (who can see/edit what)
- Search configuration (which fields are searchable)

**Without the metadata engine, Twenty's CRM does not function.**

### Minimum Required for CRM Entity CRUD

| Metadata Module | Required? | Why |
|----------------|-----------|-----|
| ObjectMetadata | **YES** | Defines what tables exist |
| FieldMetadata | **YES** | Defines columns |
| ViewModule | **YES** | CRM needs views |
| ViewField | **YES** | Which fields in which views |
| ViewFilter | **YES** | Filtering |
| ViewGroup | **YES** | Kanban grouping |
| ViewSort | **YES** | Sorting |
| RoleModule | **YES** | RBAC |
| PermissionsModule | **YES** | Authorization |
| ObjectPermission | **YES** | Per-object permissions |
| SearchFieldMetadata | **YES** | Searchable fields |
| WorkspaceMetadataVersion | **YES** | Cache invalidation |
| FlatEntity modules | **YES** | Performance cache layer |
| LogicFunction | DEFER | Server-side logic |
| Webhook | DEFER | Event notifications |
| AiAgent/AiChat | DEFER | AI features |
| CalendarChannel | DEFER | Calendar |
| ConnectedAccount | DEFER | Integrations |
| MessageChannel | DEFER | Messaging |
| CommandMenuItem | DEFER | UX polish |
| NavigationMenuItem | DEFER | UX polish |
| FrontComponent | DEFER | Frontend registry |

### Key Insight: Metadata Is the Heart of Twenty

Unlike a traditional CRM where tables are hardcoded, Twenty's CRM is **entirely metadata-driven**:
- Person/Company/Opportunity are NOT hardcoded SQL tables
- They are metadata definitions that generate SQL tables at runtime
- The metadata engine creates actual PostgreSQL schemas/columns from metadata
- Views, filters, permissions are all metadata records

**This means: extracting CRM entities without the metadata engine is impossible.**

### Runtime Closure

```
MetadataEngineModule
  ├── FieldMetadataModule (10+ providers)
  ├── ObjectMetadataModule (10+ providers)
  ├── ViewModule (5+ providers)
  ├── ViewFieldModule (3+ providers)
  ├── ViewFilterModule (3+ providers)
  ├── ViewGroupModule (3+ providers)
  ├── ViewSortModule (3+ providers)
  ├── RoleModule (5+ providers)
  ├── PermissionsModule (5+ providers)
  ├── ObjectPermissionModule (3+ providers)
  ├── SearchFieldMetadataModule (2+ providers)
  ├── WorkspaceMetadataVersionModule (2+ providers)
  ├── FlatEntity modules (20+ providers)
  └── Supporting services (10+ providers)
```

**Estimated: ~80+ providers for the metadata kernel alone.**

### Conclusion

The metadata engine is NOT optional. It is the core of Twenty's architecture. Any CRM extraction must include:
1. The full metadata engine (or a compatible replacement)
2. The flat entity cache layer (performance)
3. The schema generation pipeline
4. The permission evaluation system

### Confidence: HIGH

Twenty's metadata-driven architecture is proven and well-documented in the codebase. The dependency is real, not optional.

### Implication for Extraction

This means the CRM extraction is larger than previously estimated. The metadata engine is not just "CRM" — it's the foundation for all dynamic data management. The extraction must include it as a Platform component, not just a CRM component.
