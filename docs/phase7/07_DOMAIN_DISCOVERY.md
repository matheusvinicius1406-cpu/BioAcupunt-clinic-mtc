# Phase 7G — Domain Discovery / Bounded Contexts

**Date:** 2026-08-21
**Method:** Entity analysis + dependency tracing + business rule extraction
**Confidence:** SUPPORTED

---

## 1. Identified Bounded Contexts

### 1.1 PLATFORM Context

| Aspect | Details |
|--------|---------|
| **Purpose** | System-wide infrastructure: auth, tenancy, billing, config |
| **Owned Entities** | Tenant, User, TenantMember, Subscription, ApiKey, FeatureFlag |
| **Owned Business Rules** | Authentication, authorization, tenant resolution, rate limiting |
| **Public API** | /auth, /users, /tenants, /config |
| **Incoming Dependencies** | All other contexts |
| **Outgoing Dependencies** | None (foundational) |
| **Persistence** | PostgreSQL (platform schema) |
| **Events** | UserCreated, TenantCreated, TenantSuspended |
| **Security Boundary** | JWT validation, tenant resolution |
| **Data Classification** | PII (user emails, names) |

### 1.2 CRM Context

| Aspect | Details |
|--------|---------|
| **Purpose** | Relationship management, sales pipeline, tasks, notes |
| **Owned Entities** | Person, Organization, Opportunity, Pipeline, PipelineStage, Task, Note, Attachment, View, Dashboard |
| **Owned Business Rules** | Pipeline stages, task status, view filters, permissions |
| **Public API** | GraphQL (primary), REST (compatibility) |
| **Incoming Dependencies** | Platform (auth, tenancy), Healthcare (patient reference) |
| **Outgoing Dependencies** | Platform (auth), Healthcare (patientId reference) |
| **Persistence** | PostgreSQL (tenant schema) |
| **Events** | PersonCreated, OpportunityStageChanged, TaskCompleted |
| **Security Boundary** | Tenant isolation, role-based permissions |
| **Data Classification** | Business data (contacts, deals, tasks) |

### 1.3 HEALTHCARE Context

| Aspect | Details |
|--------|---------|
| **Purpose** | Clinical records, encounters, care plans, diagnoses |
| **Owned Entities** | Patient, Encounter, ClinicalRecord, ClinicalNote, CarePlan, CareTeam, Diagnosis, Procedure |
| **Owned Business Rules** | Clinical safety (R1), evidence gating (R2), content generation (R4) |
| **Public API** | REST (FastAPI) |
| **Incoming Dependencies** | Platform (auth, tenancy), CRM (personId reference) |
| **Outgoing Dependencies** | Platform (auth), Knowledge (evidence), AI (synthesis) |
| **Persistence** | PostgreSQL (tenant schema) + Room/SQLite (Android) |
| **Events** | EncounterCreated, ClinicalNoteAdded, CarePlanUpdated |
| **Security Boundary** | Clinical data isolation, LGPD compliance |
| **Data Classification** | PHI (Protected Health Information) |

### 1.4 KNOWLEDGE Context

| Aspect | Details |
|--------|---------|
| **Purpose** | Library articles, evidence, knowledge packs |
| **Owned Entities** | Article, Evidence, KnowledgePack, InstalledPack |
| **Owned Business Rules** | R4 (no AI-generated content), evidence verification, pack validation |
| **Public API** | REST (FastAPI) |
| **Incoming Dependencies** | Platform (auth, tenancy) |
| **Outgoing Dependencies** | None (read-only by others) |
| **Persistence** | PostgreSQL (tenant schema) + FTS4 (search) |
| **Events** | ArticleApproved, PackInstalled |
| **Security Boundary** | Read-only access, tenant isolation |
| **Data Classification** | Public/curated knowledge |

### 1.5 AI Context

| Aspect | Details |
|--------|---------|
| **Purpose** | Copilot, RAG, clinical synthesis, evidence-grounded AI |
| **Owned Entities** | ChatMessage, ClinicalSynthesis, EvidenceSource |
| **Owned Business Rules** | R1 (no LLM in safety), R2 (no evidence = no model call), R4 (no AI-generated clinical content) |
| **Public API** | REST (FastAPI) |
| **Incoming Dependencies** | Platform (auth, tenancy), Healthcare (clinical data), Knowledge (evidence) |
| **Outgoing Dependencies** | None (consumer only) |
| **Persistence** | Room/SQLite (Android) |
| **Events** | ChatMessageSent, ClinicalSynthesisGenerated |
| **Security Boundary** | Tenant isolation, evidence gating |
| **Data Classification** | PHI (clinical context), business (chat history) |

### 1.6 ANDROID Context

| Aspect | Details |
|--------|---------|
| **Purpose** | Offline-first mobile client, clinical capture, sync |
| **Owned Entities** | Room entities (local), sync queue |
| **Owned Business Rules** | Offline-first, conflict resolution, sync protocol |
| **Public API** | Sync contract (REST) |
| **Incoming Dependencies** | Platform (auth), CRM (sync), Healthcare (sync), Knowledge (sync) |
| **Outgoing Dependencies** | All server contexts (via sync) |
| **Persistence** | Room/SQLite (local) |
| **Events** | SyncCompleted, SyncConflict |
| **Security Boundary** | Local auth, sync token validation |
| **Data Classification** | PHI (local), business (local) |

---

## 2. Context Relationships

```
                    PLATFORM
                   (Auth/Tenant)
                   /     |     \
                  /      |      \
                CRM   HEALTHCARE  KNOWLEDGE
                 \      |      /
                  \     |     /
                    ANDROID
                   (Offline)
                       |
                    AI/COPILOT
                   (Cross-cutting)
```

### 2.1 Integration Rules

| From | To | Protocol | Data |
|------|----|----------|------|
| CRM | Healthcare | REST API | personId → patientId |
| Healthcare | CRM | REST API | patientId → personId |
| CRM | Android | Sync contract | CRM entities |
| Healthcare | Android | Sync contract | Clinical entities |
| Knowledge | Android | Sync contract | Articles, packs |
| AI | Healthcare | Internal call | Clinical context |
| AI | Knowledge | Internal call | Evidence |
| Platform | All | Internal call | Auth, tenancy |

---

## 3. Context Map

### 3.1 Shared Kernel

```
Platform Context (shared by all)
├── Tenant (shared entity)
├── User (shared entity)
├── TenantMember (shared entity)
└── Auth (shared service)
```

### 3.2 Customer-Supplier

```
CRM (customer) ← Healthcare (supplier)
CRM needs patientId from Healthcare
Healthcare needs personId from CRM
```

### 3.3 Conformist

```
Android (conformist) ← All server contexts
Android must conform to server API contracts
```

### 3.4 Anti-Corruption Layer

```
CRM ← ACL → Healthcare
Twenty Person ≠ BioAcupunt Patient
ACL translates between models
```

---

## 4. Summary

| Context | Entities | Business Rules | Integration |
|---------|----------|----------------|-------------|
| Platform | 4 | Auth, tenancy | Foundational |
| CRM | 10 | Pipeline, tasks, views | GraphQL + REST |
| Healthcare | 8 | Clinical safety, evidence | REST |
| Knowledge | 3 | R4, pack validation | REST |
| AI | 3 | R1, R2, R4 | Internal |
| Android | Local | Offline-first, sync | Sync contract |

**Confidence:** SUPPORTED (based on entity analysis and dependency tracing)
