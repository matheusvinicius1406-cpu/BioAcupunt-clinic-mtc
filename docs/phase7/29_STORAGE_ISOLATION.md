# 29 — STORAGE ISOLATION

## Twenty's File Storage Architecture

```
FileStorageModule
├── drivers/
│   ├── s3.driver.ts        ← AWS S3 / MinIO
│   ├── local.driver.ts     ← local filesystem
│   └── google-cloud.driver.ts ← GCS
├── services/
│   └── file-storage.service.ts
└── entities/
    └── file.entity.ts
```

### Storage Path Model

Twenty uses workspace-scoped storage paths:
```
workspace/<workspace_id>/<file_id>
```

### Isolation Requirements

| Requirement | Status | Notes |
|------------|--------|-------|
| Tenant-scoped paths | ENFORCED | workspace_id in path |
| Cross-tenant access prevention | ENFORCED | path includes workspace_id |
| Signed URL tenant scoping | NEEDED | Must verify |
| Deletion tenant scoping | NEEDED | Must verify |
| Backup tenant scoping | NEEDED | Must verify |

### Cross-Tenant Attack Vector

```
ATTACK: Tenant A requests Tenant B's file
  → Request includes file_id
  → Storage service resolves path
  → Path includes workspace_id
  → If workspace_id doesn't match current tenant → DENY
```

**Verification needed:** The file lookup must include workspace_id validation, not just file_id.

### Decision: REUSE Twenty's FileStorageModule

- Clean architecture (driver pattern)
- No commercial dependencies
- Tenant-scoped by design
- Supports S3/MinIO/local (flexible)

**Adaptation needed:**
- Ensure workspace_id is always validated
- Add audit logging for file access
- Test cross-tenant isolation

### Confidence: HIGH
