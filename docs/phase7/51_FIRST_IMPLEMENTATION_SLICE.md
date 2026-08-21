# 51 — FIRST IMPLEMENTATION SLICE

## Selection Criteria

```
VALUE × REVERSIBILITY ÷ RISK
```

## Candidates

| Slice | Value | Reversibility | Risk | Score |
|-------|-------|---------------|------|-------|
| Person/Company CRUD | HIGH | HIGH | LOW | **HIGHEST** |
| Identity Bridge | HIGH | HIGH | LOW | HIGH |
| Auth Core | HIGH | MEDIUM | MEDIUM | MEDIUM |
| Metadata Kernel | VERY HIGH | LOW | HIGH | MEDIUM |
| Tenant Service | HIGH | HIGH | LOW | HIGH |

## Recommended: Person/Company CRUD

### Why This Slice?

1. **Low blast radius** — Person (1 file), Company (1 file)
2. **Clear contract** — CRUD operations are well-defined
3. **No complex dependencies** — minimal metadata, no workflow
4. **High value** — core CRM functionality
5. **High reversibility** — can be removed without side effects
6. **Proves architecture** — validates metadata engine, schema routing, auth

### What This Slice Proves

```
Person/Company CRUD
    ↓
PROVES:
  ✅ Metadata engine works
  ✅ Schema-per-tenant works
  ✅ Auth works without SSO
  ✅ Record CRUD works
  ✅ Search works
  ✅ Views work
  ✅ Permissions work
  ✅ Tenant isolation works
```

### Implementation Plan

#### Phase 7.2.1a: Scaffold

1. Create BioAcupunt CRM package structure
2. Set up NestJS application
3. Configure PostgreSQL connection
4. Configure schema routing
5. Set up auth (without SSO)

#### Phase 7.2.1b: Metadata

1. Extract metadata engine modules
2. Configure object metadata
3. Configure field metadata
4. Test metadata creation

#### Phase 7.2.1c: Person/Company

1. Extract Person entity definition
2. Extract Company entity definition
3. Configure record CRUD
4. Test CRUD operations

#### Phase 7.2.1d: Search

1. Extract search module
2. Configure search fields
3. Test search operations

#### Phase 7.2.1e: Views

1. Extract view modules
2. Configure default views
3. Test view operations

#### Phase 7.2.1f: Permissions

1. Extract permission modules
2. Configure RBAC roles
3. Test permission checks

### Success Criteria

```
- [ ] Build succeeds
- [ ] Server starts
- [ ] Can create Person
- [ ] Can read Person
- [ ] Can update Person
- [ ] Can delete Person
- [ ] Can create Company
- [ ] Can read Company
- [ ] Can update Company
- [ ] Can delete Company
- [ ] Can search Person/Company
- [ ] Can filter/sort
- [ ] Can create views
- [ ] Permissions enforced
- [ ] Tenant isolation verified
- [ ] No Enterprise imports
- [ ] No commercial imports
```

### Rollback Plan

If slice fails:
1. Remove extracted code
2. Restore original BioAcupunt CRM
3. Document failure reasons
4. Adjust extraction plan

### Timeline Estimate

```
Phase 7.2.1a: Scaffold (1 day)
Phase 7.2.1b: Metadata (2 days)
Phase 7.2.1c: Person/Company (1 day)
Phase 7.2.1d: Search (1 day)
Phase 7.2.1e: Views (1 day)
Phase 7.2.1f: Permissions (1 day)
Phase 7.2.1g: Integration testing (1 day)
Total: ~8 days
```

### Confidence: HIGH

Person/Company CRUD is the lowest-risk, highest-value first slice. It proves the core architecture without complex dependencies.
