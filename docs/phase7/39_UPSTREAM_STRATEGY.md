# 39 — UPSTREAM STRATEGY

## Current State

- Twenty repository: `research/open_source/crm/twenty/`
- Version: commit `e5dd07b22` (from baseline)
- Branch: `main` (upstream)

## Fork Strategy

### Recommended: Selective Cherry-Pick

Instead of maintaining a full fork, selectively incorporate Twenty improvements:

```
Upstream Twenty
    ↓
BioAcupunt monitors releases
    ↓
Identifies relevant changes
    ↓
Cherry-picks into BioAcupunt
    ↓
Adapts to BioAcupunt architecture
    ↓
Tests & validates
```

### Why Not Full Fork?

1. **Maintenance burden** — full fork requires continuous merge with upstream
2. **License compliance** — AGPL requires source availability for modifications
3. **Feature bloat** — full fork includes Enterprise/commercial code we don't use
4. **Divergence risk** — fork diverges from upstream over time

### What to Track from Upstream

| Area | Track? | Why |
|------|--------|-----|
| Security patches | YES | Critical |
| Bug fixes | SELECTIVE | Only if affects our extraction |
| New CRM features | SELECTIVE | Only if valuable |
| Metadata changes | YES | Core dependency |
| Auth changes | YES | Security-sensitive |
| Enterprise features | NO | Not used |
| Billing changes | NO | Replaced |
| UI changes | NO | We build our own |

## Update Cadence

```
Monthly: Check Twenty releases
Quarterly: Evaluate relevant changes
Annually: Major version assessment
```

## Compatibility Surface

### High Compatibility (track closely)
- Metadata engine (ObjectMetadata, FieldMetadata)
- Auth core (credentials, JWT)
- Record CRUD
- Schema management

### Medium Compatibility (monitor)
- Search
- Views
- Permissions
- File storage

### Low Compatibility (ignore)
- UI components
- Enterprise features
- Billing
- SSO

## Risk: Upstream Divergence

**Risk:** BioAcupunt's extraction diverges from Twenty's architecture over time.

**Mitigation:**
1. Keep extraction changes minimal
2. Document all modifications
3. Design for portability (use interfaces/ports)
4. Monitor upstream for breaking changes

## Risk: License Changes

**Risk:** Twenty changes license from AGPL to something incompatible.

**Mitigation:**
1. Pin to current version (commit hash)
2. License audit on each update
3. Have exit strategy (replace with BioAcupunt implementation)

### Confidence: MEDIUM

The strategy is sound but depends on Twenty's release cadence and license stability. Annual review is essential.
