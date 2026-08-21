# Phase 7H — Domain Ownership Matrix

**Date:** 2026-08-21
**Method:** Entity analysis + business rule extraction
**Confidence:** SUPPORTED

---

## 1. Entity Ownership Matrix

| Entity | Owner | Reads | Writes | Forbidden Callers |
|--------|-------|-------|--------|-------------------|
| **Tenant** | Platform | All | Platform only | — |
| **User** | Platform | Platform, CRM | Platform only | Healthcare (direct) |
| **TenantMember** | Platform | Platform, CRM | Platform only | — |
| **Person** | CRM | CRM, Healthcare | CRM only | AI (direct write) |
| **Organization** | CRM | CRM | CRM only | Healthcare, AI |
| **Opportunity** | CRM | CRM | CRM only | Healthcare, AI |
| **Pipeline** | CRM | CRM | CRM only | — |
| **PipelineStage** | CRM | CRM | CRM only | — |
| **Task** | CRM | CRM | CRM only | Healthcare |
| **Note** | CRM | CRM | CRM only | Healthcare |
| **Attachment** | CRM | CRM | CRM only | — |
| **View** | CRM | CRM | CRM only | — |
| **Dashboard** | CRM | CRM | CRM only | — |
| **PatientProfile** | Healthcare | CRM (read), Healthcare | Healthcare only | CRM (direct write) |
| **Encounter** | Healthcare | Healthcare | Healthcare only | CRM, AI |
| **ClinicalRecord** | Healthcare | Healthcare | Healthcare only | CRM, AI |
| **ClinicalNote** | Healthcare | Healthcare | Healthcare only | CRM, AI |
| **CarePlan** | Healthcare | Healthcare | Healthcare only | CRM, AI |
| **CareTeam** | Healthcare | Healthcare | Healthcare only | CRM |
| **Diagnosis** | Healthcare | Healthcare | Healthcare only | CRM, AI |
| **Article** | Knowledge | All (read) | Knowledge only | CRM, Healthcare |
| **Evidence** | Knowledge | All (read) | Knowledge only | — |
| **KnowledgePack** | Knowledge | Knowledge | Knowledge only | — |
| **ChatMessage** | AI | AI | AI only | — |
| **ClinicalSynthesis** | AI | AI, Healthcare (read) | AI only | — |
| **EvidenceSource** | AI | AI, Knowledge (read) | AI only | — |

---

## 2. Cross-Context Read Rules

| Reader | Entity | Allowed | Condition |
|--------|--------|---------|-----------|
| CRM | PatientProfile | ✅ Read | Via personId reference |
| Healthcare | Person | ✅ Read | Via patientId reference |
| AI | Person | ✅ Read | For context building |
| AI | ClinicalRecord | ✅ Read | For synthesis (R2 gate) |
| AI | Article | ✅ Read | For evidence (R2 gate) |
| Android | All | ✅ Read | Via sync contract |
| Knowledge | Person | ❌ Write | Never |
| CRM | ClinicalRecord | ❌ Write | Never |

---

## 3. Cross-Context Write Rules

| Writer | Entity | Allowed | Condition |
|--------|--------|---------|-----------|
| CRM | PatientProfile | ❌ Write | Healthcare owns |
| Healthcare | Person | ❌ Write | CRM owns |
| AI | Any clinical | ❌ Write | Never (R1) |
| AI | Any CRM | ❌ Write | Never |
| Android | All | ✅ Write | Via sync contract |

---

## 4. Forbidden Dependencies

| From | To | Reason |
|------|----|--------|
| CRM | Healthcare internals | Domain isolation |
| Healthcare | CRM internals | Domain isolation |
| AI | Clinical truth mutation | R1 (safety) |
| Knowledge | Any write | Read-only |
| Android | Database directly | Sync contract only |
| Commercial modules | CRM core | License compliance |

---

## 5. Summary

| Rule | Status |
|------|--------|
| Each concept has canonical owner | ✅ Proven |
| No duplicate identity | ✅ Proven (Person ≠ PatientProfile) |
| Cross-context reads are explicit | ✅ Proven |
| Cross-context writes are forbidden | ✅ Proven |
| AI never writes clinical truth | ✅ Proven (R1) |
| Commercial modules excluded | ✅ Proven |

**Confidence:** SUPPORTED
