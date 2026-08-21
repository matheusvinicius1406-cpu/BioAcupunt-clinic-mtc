# Phase 7I — Canonical Identity

**Date:** 2026-08-21
**Method:** Entity modeling + relationship analysis
**Confidence:** SUPPORTED

---

## 1. Identity Entities

### 1.1 Platform Level

```
Tenant
├── id: UUID (primary key)
├── name: String
├── schema: String (PostgreSQL schema name)
├── status: ACTIVE | SUSPENDED | DELETED
├── createdAt: DateTime
└── updatedAt: DateTime

User
├── id: UUID (primary key)
├── email: String (unique, platform-wide)
├── name: String
├── avatar: String
├── status: ACTIVE | INACTIVE
├── createdAt: DateTime
└── updatedAt: DateTime

TenantMember
├── id: UUID (primary key)
├── tenantId: UUID (FK → Tenant)
├── userId: UUID (FK → User)
├── role: OWNER | ADMIN | PRACTITIONER | ASSISTANT | RECEPTION | READ_ONLY
├── status: ACTIVE | INACTIVE
├── createdAt: DateTime
└── updatedAt: DateTime
```

### 1.2 CRM Level (Tenant Schema)

```
Person (Canonical Identity)
├── id: UUID (primary key)
├── name: String
├── emails: JSON [{primary, additional}]
├── phones: JSON [{primary, additional}]
├── avatar: String
├── linkedinLink: String
├── jobTitle: String
├── companyId: UUID (FK → Organization)
├── position: Int
├── createdBy: UUID (FK → User)
├── updatedBy: UUID (FK → User)
├── createdAt: DateTime
├── updatedAt: DateTime
└── deletedAt: DateTime

Organization
├── id: UUID (primary key)
├── name: String
├── domainName: String
├── address: String
├── phones: JSON
├── emails: JSON
├── linkedinLink: String
├── organizationType: PROSPECT | CUSTOMER | PARTNER | VENDOR | INVESTOR | OTHER
├── position: Int
├── createdBy: UUID (FK → User)
├── updatedBy: UUID (FK → User)
├── createdAt: DateTime
├── updatedAt: DateTime
└── deletedAt: DateTime
```

### 1.3 Healthcare Level (Tenant Schema)

```
PatientProfile (Extension of Person)
├── id: UUID (primary key)
├── personId: UUID (FK → Person, UNIQUE)
├── clinicalStatus: ACTIVE | INACTIVE | CHURNED
├── careTeamId: UUID (FK → CareTeam)
├── lastEncounterAt: DateTime
├── nextAppointmentAt: DateTime
├── totalEncounters: Int
├── mainComplaint: String
├── healthInsurance: String
├── npsScore: Int
├── referralSource: String
├── tags: JSON
├── notes: String
├── createdAt: DateTime
├── updatedAt: DateTime
└── deletedAt: DateTime
```

---

## 2. Identity Rules

### 2.1 Canonical Identity Rule

**Person is the canonical identity for a real person.**

- One Person entity per real person
- PatientProfile extends Person (1:1 relationship)
- User is separate (system auth, not the same as Person)
- No duplicate identities allowed

### 2.2 Identity Relationships

```
User (system auth)
  ↓
TenantMember (user ↔ tenant)
  ↓
Person (CRM identity)
  ↓
PatientProfile (healthcare extension)
```

### 2.3 Identity Resolution

| Scenario | Resolution |
|----------|------------|
| New contact | Create Person |
| New patient | Create Person + PatientProfile |
| New user | Create User + TenantMember |
| Import from old CRM | Merge duplicates into Person |
| Android sync | Use personId as identity key |

---

## 3. Forbidden Identity Patterns

### 3.1 Duplicate Identity

```
❌ CrmPerson + Patient + Person = three sources of truth
✅ Person → PatientProfile = one canonical identity
```

### 3.2 Cross-Context Identity

```
❌ CRM writes PatientProfile directly
✅ CRM reads PatientProfile via personId reference
```

### 3.3 Identity Leakage

```
❌ AI infers diagnosis from Person data
✅ AI reads Person data with evidence gate (R2)
```

---

## 4. Identity Lifecycle

### 4.1 Person Lifecycle

```
Created → Active → Deleted (soft delete)
```

### 4.2 PatientProfile Lifecycle

```
Created → Active → Inactive → Churned
```

### 4.3 User Lifecycle

```
Created → Active → Inactive
```

---

## 5. Identity Scope

| Entity | Tenant Scope | Authorization Scope |
|--------|-------------|-------------------|
| Tenant | Platform | Platform admin |
| User | Platform | Self + Platform admin |
| TenantMember | Tenant | Tenant admin |
| Person | Tenant | Tenant members |
| Organization | Tenant | Tenant members |
| PatientProfile | Tenant | Healthcare providers |

---

## 6. Summary

| Rule | Status |
|------|--------|
| Person is canonical | ✅ Defined |
| PatientProfile extends Person | ✅ Defined |
| User is separate | ✅ Defined |
| No duplicate identity | ✅ Enforced |
| Cross-context references explicit | ✅ Defined |
| Identity lifecycle defined | ✅ Defined |

**Confidence:** SUPPORTED
