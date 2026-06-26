# DomainModel.md — Modelo de Domínio Oficial

## 1. Objetivo
Este documento define o vocabulário canônico do BioAcupunt Supremo. Ele é a fonte da verdade para nomes de classes, tabelas, APIs e eventos. Não pode haver termos conflitantes entre código, documentação e contratos de integração.

---

## 2. Representação
- **Entidade**: identidade própria e ciclo de vida.
- **Value Object (VO)**: imutável, definido por valores, sem identidade.
- **Agregado (Aggregate)**: cluster de entidades/VO com uma Aggregate Root que controla invariantes.
- **Evento de domínio**: fato passado, imutável.

---

## 3. Agregados e Entidades por Contexto

### 3.1 Authentication & Authorization
- **User** (Agregate Root)
  - id, email, phone?, status, emailVerified, lastLoginAt?, createdAt, updatedAt
- **Role**
  - id, name, permissions, scope
- **Permission**
  - id, code, description
- **Session**
  - id, userId, refreshToken (hash), expiresAt, deviceInfo?, ipAddress?
- **AuditLog**
  - id, userId, action, resource, resourceId, changes (JSON), timestamp, ipAddress?

---

### 3.2 Clinical
- **Patient** (Agregate Root)
  - id, tenantId, profile (PatientProfile VO), status, createdAt, updatedAt
- **PatientProfile** (VO)
  - fullName, birthDate, gender, cpf? (criptografado), rg? (criptografado), address (VO), contacts (List<ContactInfo>), emergencyContact (ContactInfo)?
- **Appointment** (Agregate Root)
  - id, tenantId, patientId, professionalId, status, startTime, endTime, type, room?, notes?, createdAt, updatedAt, version (sync otimista)
- **Consultation** (Agregate Root)
  - id, tenantId, patientId, professionalId, appointmentId?, status, startedAt?, finishedAt?, createdAt, updatedAt
- **Anamnesis** (entidade, pertence a Consultation)
  - id, consultationId, chiefComplaint, historyOfPresentIllness, pastHistory, familyHistory, lifestyle, emotionalState, observations?, createdAt
- **TongueAnalysis** (VO)
  - color, coating, shape, imageUrl?, notes?
- **PulseAnalysis** (VO)
  - left, right, quality, notes?
- **Syndrome** (entidade)
  - id, consultationId, name, theoryBasis, references (KnowledgeNode ids), createdAt
- **TreatmentPlan** (Agregate Root)
  - id, consultationId, diagnosisId, objective, frequency, duration, status, createdAt, updatedAt
- **ClinicalSession** (Agregate Root)
  - id, treatmentPlanId, professionalId, status, scheduledAt, startedAt?, finishedAt?, evolutionNotes, proceduresPerformed (JSON), createdAt, updatedAt
- **ClinicalRecord** (Agregate Root — visão consolidada)
  - id, patientId, consultationId, anamnesisId?, diagnosisIds, treatmentPlanId?, sessionIds, summary, createdAt, updatedAt
- **AcupuncturePoint** (entidade)
  - id, code, name, meridian, location, indications, contraindications?, references
- **Prescription** (Agregate Root)
  - id, clinicalSessionId, professionalId, items (List<PrescriptionItem>), observations?, createdAt
- **PrescriptionItem** (VO)
  - point (AcupuncturePoint.id ou código livre), name, technique, duration, notes?

---

### 3.3 Patients (Contexto separado de Clinical)
- **PatientIdentity** (Agregate Root)
  - id (mesmo id de Clinical.Patient), tenantId, crmRecord (JSON), tags, lifetimeValue?, acquisitionSource?, riskScore?, createdAt, updatedAt
- **Consent** (Agregate Root — LGPD)
  - id, patientId, type, granted, grantedAt?, revokedAt?, documentUrl?, ipAddress?, userAgent?

---

### 3.4 Protocols
- **Protocol** (Agregate Root)
  - id, tenantId?, title, description, category, tags, status, currentVersion (ProtocolVersion.id), createdAt, updatedAt
- **ProtocolVersion** (Agregate Root)
  - id, protocolId, version, semver, steps (List<Step>), changeNotes?, publishedAt?, createdBy, createdAt
- **Step** (VO)
  - order, title, description, points (List<PointReference>), duration?, observations?, contraindications?
- **PointReference** (VO)
  - code, name, technique?, notes?
- **Contraindication** (VO)
  - absolute, description

---

### 3.5 Knowledge (Scientific Library)
- **KnowledgeNode** (Agregate Root)
  - id, tenantId?, type, title, content, summary, tags, version, metadata (JSON), status, embedding?, createdAt, updatedAt
- **Citation** (entidade)
  - id, knowledgeNodeId, source, page?, excerpt, context?

---

### 3.6 Education
- **Course** (Agregate Root)
  - id, tenantId, title, description, level, category, instructorId, isPublic, status, createdAt, updatedAt
- **Module**
  - id, courseId, title, order, contentRef?
- **Lesson**
  - id, moduleId, title, type, contentRef?, durationMinutes?
- **Quiz**
  - id, moduleId, questions (List<Question>), passingScore
- **Flashcard** (Agregate Root)
  - id, tenantId?, front, back, tags, knowledgeRefs, nextReviewAt?, interval?, easeFactor?
- **Certification**
  - id, courseId, studentId, issuedAt, expiresAt?, code, metadata
- **Enrollment**
  - id, courseId, studentId, status, progress, enrolledAt, completedAt?

---

### 3.7 Finance
- **Invoice** (Agregate Root)
  - id, tenantId, patientId, issuerId, status, dueDate, paidAt?, totalAmount, currency, items (List<InvoiceItem>), pdfUrl?, createdAt, updatedAt
- **InvoiceItem** (VO)
  - description, quantity, unitPrice, total
- **Payment** (Agregate Root)
  - id, invoiceId, method, amount, status, paidAt?, gatewayId?, metadata
- **Transaction** (Agregate Root — espelho contábil)
  - id, tenantId, paymentId?, type, amount, category, relatedEntity (JSON), occurredAt
- **Charge** (Agregate Root — parcelas)
  - id, invoiceId, installmentNumber, totalInstallments, amount, dueDate, status, paymentId?

---

### 3.8 CRM
- **Lead** (Agregate Root)
  - id, tenantId, name, email?, phone?, source, interest?, status, assignedTo?, convertedToPatientId?, createdAt, updatedAt
- **Opportunity** (Agregate Root)
  - id, leadId, stage, value?, probability?, expectedCloseDate?, lostReason?, createdAt, updatedAt
- **Campaign** (Agregate Root)
  - id, tenantId, name, channel, type, targetAudience (JSON), scheduledAt?, status, createdAt
- **Interaction** (entidade)
  - id, leadId?, patientId?, channel, direction, summary, occurredAt, createdBy

---

### 3.9 AI (Clinical Intelligence)
- **ClinicalQuery** (Agregate Root)
  - id, tenantId, userId, patientContextId?, query, mode, filters (JSON), createdAt
- **ClinicalAnswer** (Agregate Root)
  - id, queryId, answer, summary, confidence, references (List<EvidenceReference>), modelVersion, latencyMs, createdAt
- **EvidenceReference** (VO)
  - knowledgeNodeId?, title, source, excerpt, url?, relevanceScore?
- **ClinicalReasoningTrace** (Agregate Root)
  - id, queryId, professionalId, steps (List<ReasoningStep>), finalDecision, confidence, createdAt
- **ReasoningStep** (VO)
  - order, action, input, output, references, evidenceIds, observation
- **KnowledgeGap** (Agregate Root)
  - id, queryId, description, suggestedAcquisition, status, assignedTo?, createdAt

---

### 3.10 Analytics
- **Metric** (Agregate Root — materialized)
  - id, tenantId, name, value, dimensions (JSON), period, calculatedAt, validFrom, validTo?
- **Dashboard** (Agregate Root)
  - id, tenantId, ownerId, name, layout (JSON), isPublic, createdAt, updatedAt
- **Report** (Agregate Root)
  - id, tenantId, name, type, parameters (JSON), generatedAt?, fileUrl?, createdBy, createdAt

---

### 3.11 Notifications
- **Notification** (Agregate Root)
  - id, tenantId, userId, channel, templateId, variables (JSON), status, scheduledAt?, sentAt?, deliveredAt?, readAt?, errorMessage?, createdAt
- **Template**
  - id, tenantId?, channel, name, subject?, body, variables, isSystem
- **Preference** (Agregate Root)
  - id, userId, channel, enabled, optInTypes, updatedAt

---

### 3.12 Administration & Settings
- **Tenant** (Agregate Root)
  - id, name, domain?, planId, status, settings (JSON), billingConfig (JSON), createdAt, updatedAt
- **Plan**
  - id, name, price, currency, features, limits (JSON), isActive
- **FeatureFlag**
  - id, tenantId?, key, enabled, rolloutPercentage?, variants?, updatedAt

---

### 3.13 Sync & Offline
- **DeviceRegistration** (Agregate Root)
  - id, tenantId, userId, deviceId, platform, lastSyncAt?, status, createdAt
- **SyncSession** (Agregate Root)
  - id, deviceId, startedAt, finishedAt?, status, changesProcessed, conflictsDetected, conflictsResolved, errorMessage?, createdAt
- **ChangeSet** (Agregate Root)
  - id, deviceId, entityType, entityId, operation, payload (JSON), version, timestamp, syncedAt?, conflictId?, status
- **SyncConflict** (Agregate Root)
  - id, deviceLocalChangeSetId, remoteChangeSetId, winnerStrategy, winnerChangeSetId?, resolvedBy?, resolvedAt?, resolutionData (JSON), status
- **WinnerStrategy**
  - enum: LOCAL, REMOTE, MERGE, MANUAL

---

## 4. Regras de Merge e Resolução por Entidade

| Entidade | Estratégia Padrão | Observação |
|----------|-------------------|------------|
| ClinicalRecord, TreatmentPlan, ClinicalSession | Bloqueio clínico | Alterações conflitantes exigem merge assistido |
| Appointment | Last-Write-Wins com aviso | Conflito raro e de baixo risco clínico |
| Patient / PatientIdentity | Merge assistido | Dados civis nunca sobrescrever; priorizar fluxo LGPD |
| Invoice / Payment | Remoto vence | Regra fiscal/operacional |
| Protocol | Local proibido | Profissional não edita protocolo global offline; apenas referência |
| KnowledgeNode | Last-Write-Wins indexado | Reindexação acontece após sincronização |

---

## 5. Regras de Nomenclatura
- Entidades/Agregados: PascalCase, singular
- Value Objects: PascalCase, singular
- Enums: PascalCase, valores em snake_case
- Campos: camelCase no código; snake_case em banco/JSON/APIs
- IDs: sempre UUID v4 em string

---

## 6. Anti-Corrupção
Nenhum detalhe de infraestrutura pode vazar para o domínio:
- Domínio não depende de Retrofit, Room, WorkManager, Firebase, JWT, HTTP ou JSON.
- Domínio define interfaces e modelos imutáveis.
- Infra implementa repositórios, mapeadores e clients.

---

## 7. Regras de Ouro do Domínio
- Imutabilidade clínica: sessões encerradas, prontuários fechados e pagamentos confirmados não são alterados, apenas evoluem.
- Tenant isolation: toda entidade operacional carrega tenantId.
- Idempotência: chaves de idempotência em todas as escritas.
- Soft delete: nenhuma entidade é deletada fisicamente.
- Auditoria obrigatória: toda mutação gera AuditLog.

---

## 8. Mapa Conceitual do Fluxo Clínico (sem ambiguidade)

Appointment (Agendamento)
→ Consultation (Atendimento/Consulta)
→ Anamnesis + TongueAnalysis + PulseAnalysis
→ Syndrome
→ TreatmentPlan
→ ClinicalSession (Sessões do plano)
→ ClinicalRecord (Registro clínico consolidado)

Regras de separação:
- Appointment = compromisso de agenda
- Consultation = instância clínica do atendimento
- TreatmentPlan = plano terapêutico do paciente
- ClinicalSession = execução de uma sessão do plano
- ClinicalRecord = visão consolidada e navegável do paciente
