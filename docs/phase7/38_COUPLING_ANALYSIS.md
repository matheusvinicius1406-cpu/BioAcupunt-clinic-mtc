# 38 — COUPLING ANALYSIS

## God Modules (High Fan-In)

| Module | Fan-In | Fan-Out | Centrality | Risk |
|--------|--------|---------|------------|------|
| CoreEngineModule | 79 | 79 | VERY HIGH | GOD MODULE |
| MetadataEngineModule | 40+ | 40+ | VERY HIGH | GOD MODULE |
| WorkspaceModule | 15+ | 20+ | HIGH | HIGH COUPLING |
| AuthModule | 10+ | 15+ | HIGH | HIGH COUPLING |

### CoreEngineModule Analysis

```
CoreEngineModule imports: 79 modules
CoreEngineModule imported by: virtually everything
```

**This is the central nervous system of Twenty.** It aggregates:
- Auth
- Billing
- File storage
- Search
- Record CRUD
- GraphQL
- And 70+ more modules

**Risk:** Any change to CoreEngineModule affects everything.

**Mitigation:** Do NOT extract CoreEngineModule as-is. Instead, extract only the CRM-relevant submodules.

### MetadataEngineModule Analysis

```
MetadataEngineModule imports: 40+ modules
MetadataEngineModule imported by: all CRM modules
```

**This is the data definition layer.** It defines:
- What objects exist
- What fields exist
- What relations exist
- What views exist
- What permissions exist

**Risk:** Essential for CRM. Cannot be removed.

**Mitigation:** Extract as a Platform component, not a CRM component.

## Low-Coupling Modules (Good Extraction Candidates)

| Module | Fan-In | Fan-Out | Centrality | Risk |
|--------|--------|---------|------------|------|
| Person | 1 | 2 | LOW | LOW |
| Company | 1 | 2 | LOW | LOW |
| Opportunity | 1 | 3 | LOW | LOW |
| Attachment | 1 | 2 | LOW | LOW |
| Search | 2 | 3 | LOW | LOW |

**These are the best extraction candidates** — minimal coupling, clear responsibility.

## Change Hotspots

| Area | Change Frequency | Risk |
|------|-----------------|------|
| Metadata engine | HIGH | Schema changes ripple everywhere |
| Auth module | MEDIUM | Security-sensitive |
| Workspace module | LOW | Stable |
| CRM entities | LOW | Stable |

### Confidence: HIGH

The coupling analysis confirms that CRM entity modules are well-isolated and suitable for extraction. The metadata engine is heavily coupled but essential.
