# Privacy & Security — Phase 5

## Overview

Phase 5 maintains the existing security model with new considerations for
clinical workflow data, voice input, and FHIR export.

## Core Principles

1. **100% on-device** — no cloud AI (per 2026-07-29 decision)
2. **Tenant isolation** — every entity scoped to clinic via `tenantId`
3. **Soft delete** — data never physically removed (LGPD audit trail)
4. **Professional review** — AI never auto-finalizes clinical data
5. **Source tracking** — every observation knows its origin

## Data Classification

| Data | Sensitivity | Storage | Export |
|---|---|---|---|
| Patient identity | HIGH | Room/SQLite | FHIR (user-initiated) |
| Clinical observations | HIGH | Room/SQLite | FHIR (user-initiated) |
| MTC Assessment | HIGH | Room/SQLite | FHIR (user-initiated) |
| Clinical notes | HIGH | Room/SQLite | FHIR (user-initiated) |
| Questionnaire responses | MEDIUM | Room/SQLite | FHIR (user-initiated) |
| Voice transcripts | HIGH | Transient → Room | Never exported raw |
| AI draft observations | MEDIUM | Room/SQLite | FHIR (after confirmation) |

## Voice/STT Privacy

- Audio stays local (LOCAL provider)
- Transcripts are processed on-device
- No audio is transmitted without explicit permission
- Cloud STT requires explicit configuration and policy

## FHIR Export Privacy

- Export is user-initiated, never automatic
- Bundle includes only data the user selects
- No automatic transmission to external systems
- warnings logged for missing identifiers (CPF)

## AI Data Handling

- AI-generated observations start as `AI_EXTRACTED_DRAFT`
- Never auto-promoted to `CONFIRMED`
- Professional must review and confirm
- AI draft content not included in FHIR export until confirmed

## Audit Trail

Phase 5 entities support audit via:
- `createdAt` / `updatedAt` timestamps
- `createdBy` / `finalizedBy` user tracking
- `status` lifecycle (DRAFT → CONFIRMED)
- `source` attribution (who/what created the observation)
