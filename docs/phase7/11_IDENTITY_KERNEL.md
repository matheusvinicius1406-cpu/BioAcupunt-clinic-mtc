# Phase 7.0.8 — Identity Kernel Reconstruction

**Date:** 2026-08-21
**Confidence:** SUPPORTED

---

## 1. Identity Entities Analysis

### 1.1 User (Platform)

| Aspect | Details |
|--------|---------|
| **Owner** | Platform |
| **Purpose** | System authentication, login |
| **Key** | UUID (platform-wide unique) |
| **Scope** | Cross-tenant (one user can belong to multiple tenants) |
| **Lifecycle** | Created → Active → Inactive |
| **External IDs** | Email (unique), OAuth IDs |
| **Tenant scope** | None (platform-level) |

### 1.2 TenantMember (Platform)

| Aspect | Details |
|--------|---------|
| **Owner** | Platform |
| **Purpose** | User ↔ Tenant relationship |
| **Key** | UUID (tenant-scoped) |
| **Scope** | Tenant |
| **Lifecycle** | Created → Active → Inactive |
| **Roles** | OWNER, ADMIN, PRACTITIONER, ASSISTANT, RECEPTION, READ_ONLY |

### 1.3 Person (CRM)

| Aspect | Details |
|--------|---------|
| **Owner** | CRM |
| **Purpose** | Contact/relationship representation |
| **Key** | UUID (tenant-scoped) |
| **Scope** | Tenant |
| **Lifecycle** | Created → Active → Deleted (soft) |
| **Business logic** | NONE (entity-only, 1 file) |
| **Relationships** | Company, Opportunity, Task, Note, Attachment, Timeline |

### 1.4 PatientProfile (Healthcare)

| Aspect | Details |
|--------|---------|
| **Owner** | Healthcare |
| **Purpose** | Healthcare extension of Person |
| **Key** | UUID (tenant-scoped) |
| **Scope** | Tenant |
| **Lifecycle** | Created → Active → Inactive → Churned |
| **Relationship** | 1:1 with Person (personId FK) |
| **Business logic** | Clinical status, care team, encounters |

### 1.5 Organization (CRM)

| Aspect | Details |
|--------|---------|
| **Owner** | CRM |
| **Purpose** | Company/organization representation |
| **Key** | UUID (tenant-scoped) |
| **Scope** | Tenant |
| **Lifecycle** | Created → Active → Deleted (soft) |
| **Business logic** | NONE (entity-only) |

---

## 2. Identity Model Comparison

### Model A: Platform owns Person

```
Platform
  └── Person (Platform-owned)
        ├── CRM reads Person
        └── Healthcare extends Person
```

**Pros:**
- Single source of truth
- Clean ownership

**Cons:**
- Person has CRM-specific fields (company, linkedin, jobTitle)
- Platform would need to know about CRM domain
- Couples Platform to CRM semantics

### Model B: CRM owns Person (CHOSEN)

```
Platform
  └── User (Platform-owned)
        └── TenantMember
              └── Person (CRM-owned)
                    └── PatientProfile (Healthcare extends)
```

**Pros:**
- CRM owns its domain (person = contact = CRM concept)
- Healthcare extends cleanly (1:1)
- Platform stays generic (User ≠ Person)

**Cons:**
- Two identity types (User ≠ Person)
- Need bridge between User and Person

### Model C: Shared Identity Context

```
Shared Identity Context
  ├── Person (shared)
  ├── User (shared)
  └── TenantMember (shared)
```

**Pros:**
- Single identity layer

**Cons:**
- Over-engineering for current scale
- Adds complexity without clear benefit

### Model D: Separate identities + bridge

```
CRM: Person
Healthcare: Patient
Bridge: personId ↔ patientId
```

**Pros:**
- Complete separation

**Cons:**
- Duplicate data
- Sync complexity
- Already rejected by previous analysis

---

## 3. Decision: Model B — CRM owns Person

**Rationale:**
1. Person is entity-only (no business logic) — belongs to CRM domain
2. PatientProfile extends Person cleanly (1:1 FK)
3. User is separate (system auth, not contact)
4. Platform stays generic (User, TenantMember)
5. No duplicate identity (Person is canonical for contacts)

**Evidence:**
- Person has 1 file, 1 runtime import (twenty-orm)
- PatientProfile has personId FK (1:1 relationship)
- User has platform-wide email (cross-tenant)
- No clinical data in Person entity

---

## 4. Identity Lifecycle

```
User (Platform)
  │
  ├── Created (email, name)
  ├── Active
  └── Inactive
  
TenantMember (Platform)
  │
  ├── Created (user + tenant + role)
  ├── Active
  └── Inactive
  
Person (CRM)
  │
  ├── Created (name, emails, phones)
  ├── Active
  └── Deleted (soft)
  
PatientProfile (Healthcare)
  │
  ├── Created (linked to Person)
  ├── Active
  ├── Inactive
  └── Churned
```

---

## 5. External Identifiers

| Entity | External IDs | Source |
|--------|-------------|--------|
| User | Email, Google ID, Microsoft ID | OAuth providers |
| Person | Email, Phone, LinkedIn | CRM data |
| PatientProfile | Medical record number | Healthcare system |
| Organization | CNPJ, domain | Brazilian business registry |

---

## 6. Merge Semantics

| Scenario | Resolution |
|----------|------------|
| Duplicate Person (same email) | Merge into one Person |
| Duplicate Person (different emails) | Keep separate unless confirmed same |
| Person → PatientProfile | Create PatientProfile linked to Person |
| User → Person | Manual link (User ≠ Person) |

---

## 7. Deletion/Deactivation

| Entity | Deletion | Deactivation |
|--------|----------|--------------|
| User | Soft delete | Inactive status |
| TenantMember | Soft delete | Inactive status |
| Person | Soft delete (deletedAt) | N/A |
| PatientProfile | Soft delete (deletedAt) | Inactive/Churned |

---

## 8. Cross-Tenant Membership

- **User:** Can belong to multiple tenants (cross-tenant)
- **TenantMember:** Belongs to one tenant
- **Person:** Belongs to one tenant (no cross-tenant)
- **PatientProfile:** Belongs to one tenant

---

## 9. Confidence

**SUPPORTED** — Based on entity analysis and domain modeling.

**Status:** RECONSTRUCTED
