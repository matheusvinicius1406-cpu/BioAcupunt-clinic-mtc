# EventModel.md — Modelo de Eventos de Domínio

## 1. Princípio Norte
Eventos são fatos que **já aconteceram**. Eles são imutáveis, nomeados no passado e representam a única fonte de verdade para comunicação entre contextos.

### Regras de Ouro
- Nome sempre no **passado** (ex.: `PatientCreated`, não `CreatePatient`).
- Payload completo e imutável (nunca atualizar um evento depois de publicado).
- Todo aggregado que executa uma operação de negócio deve emitir pelo menos um evento.
- Eventos são a única forma de **comunicação entre bounded contexts**.
- Eventos devem ser versionados desde o nascimento (`eventVersion`).

---

## 2. Nomenclatura Oficial
| Padrão | Exemplo |
|--------|---------|
| Evento | `PatientRegistered` |
| Evento de domínio | `ConsultationStarted` |
| Evento de integração | `AppointmentConfirmed` |
| Evento de sistema | `SyncConflictDetected` |

---

## 3. Eventos por Contexto

### 3.1 Authentication & Authorization
- `UserRegistered`
- `UserLoggedIn`
- `UserLoggedOut`
- `UserSuspended`
- `RoleAssigned`
- `PermissionGranted`
- `SessionRefreshed`
- `SessionRevoked`
- `PasswordChanged`

**Quando emitir:**
- No momento exato em que a ação de segurança acontece.
- Nunca antecipar ou postergar (auditoria depende da ordem real).

---

### 3.2 Clinical (Core)

#### Ciclo do Paciente
- `PatientRegistered`
- `PatientProfileUpdated`
- `PatientArchived`

#### Ciclo da Consulta
- `AppointmentScheduled`
- `AppointmentConfirmed`
- `AppointmentCancelled`
- `AppointmentRescheduled`
- `ConsultationStarted`
- `ConsultationFinished`
- `ConsultationCancelled`

#### Ciclo Clínico
- `AnamnesisPerformed`
- `TongueAnalyzed`
- `PulseAnalyzed`
- `SyndromeCreated`
- `SyndromeUpdated`

#### Plano e Sessões
- `TreatmentPlanCreated`
- `TreatmentPlanUpdated`
- `TreatmentPlanPaused`
- `TreatmentPlanCompleted`
- `ClinicalSessionScheduled`
- `ClinicalSessionStarted`
- `ClinicalSessionFinished`
- `ClinicalSessionCancelled`
- `ClinicalSessionNoShow`

#### Prescrição
- `PrescriptionIssued`
- `PrescriptionUpdated`

#### Prontuário
- `ClinicalRecordConsolidated`
- `ClinicalRecordUpdated`

---

### 3.3 Patients
- `PatientContactUpdated`
- `PatientTagAdded`
- `PatientTagRemoved`
- `ConsentGranted`
- `ConsentRevoked`
- `PatientMerged` — quando dois registros são unificados
- `PatientIdentityLinked` — usado para ligar Clinical.Patient com Patients.PatientIdentity

**Regra:** `PatientIdentity` nunca é criado diretamente; sempre surge por vinculação com Clinical.

---

### 3.4 Appointments
- `SlotBooked`
- `SlotReleased`
- `AvailabilityCreated`
- `AvailabilityUpdated`
- `AvailabilityDeactivated`
- `ReminderScheduled`
- `ReminderSent`
- `ReminderFailed`

---

### 3.5 Protocols
- `ProtocolCreated`
- `ProtocolVersionPublished`
- `ProtocolUpdated`
- `ProtocolDeprecated`
- `ProtocolReviewed`
- `ProtocolReferenced` — quando usado em TreatmentPlan

---

### 3.6 Knowledge (Scientific Library)
- `KnowledgeNodeImported`
- `KnowledgeNodeIndexed`
- `KnowledgeNodeUpdated`
- `KnowledgeNodeDeleted`
- `KnowledgeEmbeddingGenerated`
- `CitationAdded`
- `CitationRemoved`

---

### 3.7 Education
- `CoursePublished`
- `CourseArchived`
- `StudentEnrolled`
- `StudentCompleted`
- `QuizCompleted`
- `FlashcardReviewed`
- `CertificationIssued`
- `CertificationRevoked`

---

### 3.8 Finance
- `InvoiceGenerated`
- `InvoiceCancelled`
- `PaymentReceived`
- `PaymentFailed`
- `PaymentRefunded`
- `TransactionRecorded`
- `ChargeOverdue`

**Regra:** Todos os eventos financeiros são idempotentes por `invoiceId` + `installmentNumber`.

---

### 3.9 CRM
- `LeadCaptured`
- `LeadContacted`
- `LeadQualified`
- `LeadConvertedToPatient`
- `LeadLost`
- `OpportunityOpened`
- `OpportunityAdvanced`
- `OpportunityWon`
- `OpportunityLost`
- `CampaignCreated`
- `CampaignSent`
- `InteractionRecorded`

---

### 3.10 AI (Clinical Intelligence)
- `ClinicalQueryRequested`
- `ClinicalAnswerGenerated`
- `EvidenceReferenced`
- `KnowledgeGapDetected`
- `ClinicalReasoningTraceRecorded`
- `PromptTemplateUsed`
- `ModelEvaluationRecorded`

---

### 3.11 Analytics
- `MetricComputed`
- `DashboardCreated`
- `DashboardUpdated`
- `ReportGenerated`

---

### 3.12 Notifications
- `NotificationQueued`
- `NotificationSent`
- `NotificationDelivered`
- `NotificationFailed`
- `NotificationRead`
- `PreferenceChanged`

---

### 3.13 Administration & Settings
- `TenantCreated`
- `TenantSuspended`
- `TenantReactivated`
- `PlanChanged`
- `FeatureFlagUpdated`
- `AdminActionPerformed`

---

### 3.14 Sync & Offline
- `DeviceRegistered`
- `DeviceRevoked`
- `SyncStarted`
- `SyncCompleted`
- `SyncFailed`
- `ChangeSetCreated`
- `ChangeSetSynced`
- `ConflictDetected`
- `ConflictResolved`
- `SyncConflictAutoResolved`

---

## 4. Eventos Críticos e sua Cadeia Orquestrada

### Jornada do Paciente (Happy Path)

```
PatientRegistered
  → AppointmentScheduled
    → AppointmentConfirmed
      → ConsultationStarted
        → AnamnesisPerformed
          → SyndromeCreated
            → TreatmentPlanCreated
              → ClinicalSessionScheduled
                → ClinicalSessionStarted
                  → ClinicalSessionFinished
                    → PrescriptionIssued
                      → ClinicalRecordConsolidated
                        → PaymentReceived
                          → NotificationSent
```

### Jornada do Paciente (Offline → Online)

```
[Offline]
  ChangeSetCreated
    (múltiplas alterações acumuladas)

[Online]
  SyncStarted
    → ConflictDetected? → (MERGE | MANUAL)
    → ChangeSetSynced
    → SyncCompleted
```

### Jornada da IA

```
ClinicalQueryRequested
  → KnowledgeSearched (implicit)
  → ReasoningTraceRecorded
    → ClinicalAnswerGenerated
      → EvidenceReferenced
        → KnowledgeGapDetected? → (backlog para equipe clínica)
```

---

## 5. Eventos Compensatórios

Em sistemas distribuídos eventualmente consistentes, erros acontecem. BioAcupunt Supremo usa **Saga por compensação** apenas quando necessário.

| Evento Original | Evento de Compensação | Quando Disparar |
|-----------------|-----------------------|-----------------|
| `InvoiceGenerated` | `InvoiceCancelled` | Pagamento não confirmado em X horas |
| `ClinicalSessionStarted` | `ClinicalSessionCancelled` | Início inválido (ex.: sem anamnese) |
| `TreatmentPlanCreated` | `TreatmentPlanPaused` | Revisão obrigatória |
| `PaymentReceived` | `PaymentRefunded` | Chargeback ou estorno |

**Regra:** Compensação não apaga eventos; adiciona eventos novos documentando a correção.

---

## 6. Eventos e Event Sourcing (Visão Futura)

Para entidades com requisitos históricos rigorosos, event sourcing será considerado:

- `ClinicalSession` (cada etapa da sessão é um evento)
- `Consent` (cada concessão/revogação)
- `AuditLog` (todo evento também é log)
- `Finance` (cada status de pagamento)

Demais entidades usam snapshot + log de auditoria, não event sourcing completo.

---

## 7. Map Event → Aggregate

| Evento | Aggregate Raiz | Contexto |
|--------|----------------|----------|
| PatientRegistered | Patient | Clinical |
| PatientIdentityLinked | PatientIdentity | Patients |
| AppointmentScheduled | Appointment | Appointments |
| ConsultationStarted | Consultation | Clinical |
| AnamnesisPerformed | Anamnesis | Clinical |
| TreatmentPlanCreated | TreatmentPlan | Clinical |
| ClinicalSessionFinished | ClinicalSession | Clinical |
| ClinicalRecordConsolidated | ClinicalRecord | Clinical |
| InvoiceGenerated | Invoice | Finance |
| PaymentReceived | Payment | Finance |
| ProtocolPublished | ProtocolVersion | Protocols |
| KnowledgeNodeIndexed | KnowledgeNode | Knowledge |
| ClinicalAnswerGenerated | ClinicalAnswer | AI |
| SyncCompleted | SyncSession | Sync |

---

## 8. Estratégia de Versionamento de Eventos

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `eventId` | UUID | Identificador único do evento |
| `eventType` | string | Nome do evento (ex.: `PatientRegistered`) |
| `eventVersion` | string | Versão do contrato (ex.: `1.0.0`) |
| `occurredAt` | datetime | Quando aconteceu |
| `aggregateId` | UUID | ID do aggregate raiz |
| `aggregateType` | string | Tipo do aggregate (ex.: `Patient`) |
| `tenantId` | UUID | Multitenancy |
| `payload` | object | Dados do evento |
| `metadata` | object | userId, deviceId, correlationId, causationId |

---

## 9. Riscos e Mitigações

| Risco | Mitigação |
|-------|-----------|
| Evento "gordo" demais | Versionar campos via `eventVersion`, evitar breaking changes |
| Ordem de eventos garantida apenas por aggregate | Usar `occurredAt` + `aggregateId` como criterio de ordenação, não posição na fila |
| Retry infinito por evento inválido | Dead-letter queue com alerta para time clínico |
| Schema mudando sem consumidores saberem | Consumer-driven contracts + testes de integração por evento |

---

## 10. Plano de Validação
1. Revisar jornada do paciente com profissional de MTC.
2. Validar se todos os passos clínicos obrigatórios têm evento correspondente.
3. Confirmar com time mobile/web se eventos escolhidos cobrem as necessidades de atualização de UI.
4. Validar com time backend se volume de eventos por sessão clínica é aceitável para o broker escolhido.
5. Aprovar versionamento de eventos antes de criar o primeiro producer/consumer.
