# 40 — BEHAVIORAL CONTRACTS

## Purpose

Before extraction, catalog the expected behavior of each CRM operation. These contracts serve as golden-master tests to prevent regressions.

## Person Operations

### Create Person
```
Input: { firstName, lastName, email, phone?, companyId? }
Output: { id, firstName, lastName, email, phone, companyId, createdAt, updatedAt }
Side effects:
  - Person record created in workspace schema
  - Timeline activity recorded
  - Search index updated
  - If companyId: CompanyPerson relation created
Authorization: CREATE on person object
Tenant: scoped to current workspace
Errors:
  - Duplicate email → 409 Conflict
  - Invalid companyId → 400 Bad Request
  - Unauthorized → 401/403
```

### Read Person
```
Input: { id }
Output: { id, firstName, lastName, email, phone, companyId, createdAt, updatedAt }
Side effects: none
Authorization: READ on person object
Tenant: scoped to current workspace
Errors:
  - Not found → 404
  - Unauthorized → 401/403
  - Cross-tenant → 404 (not 403, to prevent enumeration)
```

### Update Person
```
Input: { id, firstName?, lastName?, email?, phone?, companyId? }
Output: { id, firstName, lastName, email, phone, companyId, createdAt, updatedAt }
Side effects:
  - Person record updated
  - Timeline activity recorded
  - Search index updated
Authorization: UPDATE on person object
Tenant: scoped to current workspace
Errors:
  - Not found → 404
  - Duplicate email → 409
  - Unauthorized → 401/403
```

### Delete Person
```
Input: { id }
Output: { success: true }
Side effects:
  - Person record soft-deleted
  - Related CompanyPerson relations removed
  - Timeline activity recorded
  - Search index updated
Authorization: DELETE on person object
Tenant: scoped to current workspace
Errors:
  - Not found → 404
  - Unauthorized → 401/403
```

## Company Operations

### Create Company
```
Input: { name, domain?, address?, employees? }
Output: { id, name, domain, address, employees, createdAt, updatedAt }
Side effects:
  - Company record created
  - Timeline activity recorded
  - Search index updated
Authorization: CREATE on company object
Tenant: scoped to current workspace
Errors:
  - Invalid input → 400
  - Unauthorized → 401/403
```

## Opportunity Operations

### Create Opportunity
```
Input: { name, pipelineId, stageId, amount?, currency?, personId?, companyId? }
Output: { id, name, pipelineId, stageId, amount, currency, personId, companyId, createdAt, updatedAt }
Side effects:
  - Opportunity record created
  - Pipeline stage count updated
  - Timeline activity recorded
Authorization: CREATE on opportunity object
Tenant: scoped to current workspace
Errors:
  - Invalid pipelineId → 400
  - Invalid stageId → 400
  - Unauthorized → 401/403
```

## Task Operations

### Create Task
```
Input: { title, body?, dueAt?, assigneeId?, entityType?, entityId? }
Output: { id, title, body, dueAt, assigneeId, entityType, entityId, createdAt, updatedAt }
Side effects:
  - Task record created
  - Timeline activity recorded
Authorization: CREATE on task object
Tenant: scoped to current workspace
```

## Search Operations

### Search Records
```
Input: { query, objectTypes?, limit?, offset? }
Output: { results: [{ objectType, id, title, subtitle, url }], totalCount }
Side effects: none
Authorization: READ on each result's object type
Tenant: scoped to current workspace
```

## View Operations

### Create View
```
Input: { name, objectType, type (list|kanban|calendar) }
Output: { id, name, objectType, type, createdAt }
Side effects:
  - View record created
  - Default view fields created
Authorization: CREATE on view object
Tenant: scoped to current workspace
```

## Timeline Operations

### List Timeline Events
```
Input: { entityType?, entityId?, startDate?, endDate?, limit?, offset? }
Output: { events: [{ id, entityType, entityId, type, title, createdAt }], totalCount }
Side effects: none
Authorization: READ on each event's entity type
Tenant: scoped to current workspace
```

## Authorization Operations

### Check Permission
```
Input: { userId, resource, action }
Output: { allowed: boolean }
Side effects: none
Authorization: N/A (this IS the authorization check)
Tenant: scoped to current workspace
```

## Tenant Isolation Operations

### Cross-Tenant Read
```
Input: Tenant A request → Tenant B resource ID
Output: 404 Not Found (NOT 403 Forbidden)
Side effects: none
Authorization: N/A (tenant mismatch)
```

## Migration Contracts

### Data Integrity
```
Input: Existing CRM data in BioAcupunt
Output: Equivalent data in Twenty-derived CRM
Verification:
  - All records migrated
  - All relationships preserved
  - All timestamps preserved
  - All permissions preserved
  - No data loss
```

### Behavioral Equivalence
```
Input: Same operations on old and new CRM
Output: Identical results
Verification:
  - CRUD operations produce same results
  - Search returns same results
  - Views show same data
  - Permissions enforce same rules
```
