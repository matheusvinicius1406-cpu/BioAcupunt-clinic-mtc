# 20 — TIMELINE KERNEL

## Discovery

### Twenty's Timeline Module

```
src/modules/timeline/
├── timeline.module.ts
├── timeline.module-definition.ts
├── standard-objects/
│   ├── message-channel/          ← messaging integration
│   ├── message-folder/           ← messaging folders
│   ├── calendar-channel/         ← calendar integration
│   └── connected-account/        ← connected accounts
├── messaging/                    ← messaging services
├── calendar/                     ← calendar services
└── utils/                        ← utilities
```

**File count: ~40 files**

### Timeline Runtime Dependencies

The timeline module has significant dependencies:
- `MessagingService` — email/IMAP/SMTP integration
- `CalendarService` — calendar integration
- `ConnectedAccountService` — OAuth-connected accounts
- `MessageQueueService` — background processing

### Is Timeline Required for CRM?

Timeline in Twenty serves as:
1. **Activity feed** — shows what happened with a record
2. **Messaging integration** — email/IMAP sync
3. **Calendar integration** — meeting scheduling
4. **Audit trail** — system events

For a minimal CRM, the activity feed is essential. The messaging/calendar integrations are optional.

### Decomposition

| Capability | Required? | Priority |
|-----------|-----------|----------|
| Activity feed (record changes) | YES | P1 |
| Email sync | DEFER | P3 |
| Calendar sync | DEFER | P3 |
| Audit events | YES | P2 |
| Connected accounts | DEFER | P3 |

### Minimum Timeline Runtime

For MVP:
1. `TimelineActivity` entity (or equivalent)
2. Activity recording (on record create/update/delete)
3. Activity querying (by entity, by date range)
4. Workspace scoping (tenant isolation)

**NOT needed for MVP:**
- Email integration
- Calendar integration
- Connected accounts
- IMAP/SMTP

### Decision: SIMPLIFIED TIMELINE

Create a simplified timeline that:
- Records CRUD activities
- Supports querying by entity
- Is workspace-scoped
- Defers messaging/calendar to later phases

### Confidence: MEDIUM

The timeline module's dependencies on messaging/calendar are a concern. The simplified approach avoids this coupling.
