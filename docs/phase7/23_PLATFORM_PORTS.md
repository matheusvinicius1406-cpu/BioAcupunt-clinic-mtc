# 23 — PLATFORM PORTS

## Definition

Platform Ports are the interfaces between the CRM domain and platform infrastructure. The CRM should depend on PORTS (interfaces), not on specific implementations.

## Required Ports

### 1. IdentityPort
```
interface IdentityPort {
  getCurrentUserId(context: RequestContext): string;
  getUser(userId: string): User;
  getUsers(ids: string[]): User[];
}
```
**Twenty implementation:** UserEntity, UserService
**BioAcupunt implementation:** UserService (existing)

### 2. AuthenticationPort
```
interface AuthenticationPort {
  validateToken(token: string): AuthContext;
  createSession(user: User): Session;
  destroySession(sessionId: string): void;
}
```
**Twenty implementation:** AuthService, JwtService
**BioAcupunt implementation:** AuthService (existing)

### 3. TenantPort
```
interface TenantPort {
  getCurrentTenantId(context: RequestContext): string;
  getTenantSchema(tenantId: string): string;
  switchTenantSchema(dataSource: DataSource, tenantId: string): void;
}
```
**Twenty implementation:** WorkspaceService, WorkspaceDataSourceService
**BioAcupunt implementation:** TenantService (new)

### 4. AuthorizationPort
```
interface AuthorizationPort {
  checkPermission(user: User, resource: string, action: string): boolean;
  getUserRoles(userId: string, tenantId: string): Role[];
  evaluateRowPermission(userId: string, resource: string, rowId: string): boolean;
}
```
**Twenty implementation:** PermissionsService, RoleService
**BioAcupunt implementation:** AuthorizationService (new)

### 5. StoragePort
```
interface StoragePort {
  upload(file: File, path: string): FileReference;
  download(fileId: string): ReadableStream;
  delete(fileId: string): void;
  getSignedUrl(fileId: string, expiry: number): string;
}
```
**Twenty implementation:** FileStorageService (S3/MinIO/local)
**BioAcupunt implementation:** StorageService (new)

### 6. CachePort
```
interface CachePort {
  get<T>(key: string): T | null;
  set<T>(key: string, value: T, ttl?: number): void;
  invalidate(pattern: string): void;
  invalidateWorkspace(workspaceId: string): void;
}
```
**Twenty implementation:** WorkspaceCacheStorageService (Redis)
**BioAcupunt implementation:** CacheService (new)

### 7. EventPort
```
interface EventPort {
  publish(event: DomainEvent): void;
  subscribe(eventType: string, handler: EventHandler): void;
}
```
**Twenty implementation:** EventEmitterModule
**BioAcupunt implementation:** EventService (new)

### 8. AuditPort
```
interface AuditPort {
  log(entry: AuditEntry): void;
  query(filter: AuditFilter): AuditEntry[];
}
```
**Twenty implementation:** EventLogService
**BioAcupunt implementation:** AuditService (new)

### 9. JobPort
```
interface JobPort {
  enqueue<T>(job: Job<T>): void;
  schedule<T>(job: Job<T>, cron: string): void;
}
```
**Twenty implementation:** MessageQueueService
**BioAcupunt implementation:** JobService (new)

## Architecture Rule

```
CRM Domain
    ↓
PORT (interface)
    ↓
Platform Implementation
```

CRM code must NEVER import:
- Twenty-specific infrastructure directly
- BioAcupunt-specific infrastructure directly
- Any concrete implementation of platform services

This ensures:
- CRM can be tested in isolation (mock ports)
- Platform can be swapped (S3 → MinIO → local)
- Tenancy can be changed (schema → RLS → DB-per-tenant)
- No domain leakage
