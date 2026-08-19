# Sync Foundation — Multi-Device Preparation

## Overview

Phase 5 prepares entities for future multi-device sync without implementing
distributed sync. Every new entity includes the fields needed for sync.

## Sync-Ready Fields

Every Phase 5 entity includes:

| Field | Type | Purpose |
|---|---|---|
| `id` | Long (auto) | Local primary key |
| `tenantId` | Long | Clinic isolation |
| `createdAt` | String (ISO-8601) | Creation timestamp |
| `updatedAt` | String (ISO-8601) | Last modification |
| `deleted` | Boolean | Soft delete |

## Future Sync Fields (not yet added)

When sync is implemented, entities will need:

| Field | Type | Purpose |
|---|---|---|
| `serverId` | String? | Server-side identifier |
| `version` | Int | Optimistic locking |
| `syncStatus` | Enum | SYNCED / PENDING / CONFLICT |
| `deletedAt` | String? | When soft-deleted |

## Conflict Strategy (Future)

```
LOCAL_NEWER    → accept local
REMOTE_NEWER   → accept remote
CONFLICT       → manual resolution required
MERGE_REQUIRED → automatic merge possible
```

**Never overwrite silently.** Conflict detection requires version comparison.

## Current Sync Architecture

The existing `SyncEngine` handles:
- `Patient`, `CrmPatient`, `Appointment`, `Prontuario`, `MtcAssessment`
- `SyncQueueEntity` for pending operations
- `SyncStateEntity` for sync tracking
- `SyncConflictEntity` for conflicts

Phase 5 entities are **not yet wired** to the SyncEngine. This is intentional —
the sync architecture needs a design decision before adding more entity types.

## Recommendation

Before implementing sync for Phase 5 entities:
1. Audit existing SyncEngine for capacity
2. Decide: optimistic vs pessimistic locking
3. Decide: last-write-wins vs conflict detection
4. Add `serverId`, `version`, `syncStatus` to entities
5. Wire to SyncEngine
6. Test with multi-device scenario
