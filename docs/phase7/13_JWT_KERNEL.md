# Phase 7.0.10 — JWT Kernel

**Date:** 2026-08-21
**Confidence:** SUPPORTED

---

## 1. JWT Module Structure

```
jwt/ (12 files total)
├── constants/
│   └── rotate-signing-keys-cron-pattern.constant.ts  ← ENTERPRISE
├── crons/
│   ├── commands/
│   │   └── rotate-signing-keys.cron.command.ts       ← ENTERPRISE
│   └── jobs/
│       └── rotate-signing-keys.cron.job.ts           ← ENTERPRISE
├── services/
│   └── signing-key-rotation.service.ts               ← ENTERPRISE
└── [core JWT files]
```

## 2. Classification

### Core JWT (Required)
- JWT creation
- JWT validation
- Key storage
- Token handling

### Enterprise JWT (Must Remove)
- Key rotation cron
- Rotation command
- Rotation job
- Rotation service

## 3. Minimum Non-Enterprise JWT Runtime

The core JWT functionality (creation, validation, key storage) does NOT depend on key rotation. Rotation is a compliance/operational feature that can be:
- Deferred to Platform implementation
- Replaced with manual key rotation
- Implemented as a separate service

## 4. Answer

| Question | Answer |
|----------|--------|
| Can JWT work without rotation? | YES |
| Minimum JWT runtime | Core JWT files (creation, validation, storage) |
| Rotation belongs to | Platform (operational concern) |
| Confidence | HIGH |

**Status:** RECONSTRUCTED
