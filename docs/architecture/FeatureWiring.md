# FeatureWiring.md — Integração UI → Domínio → Infra

## Regra de Ouro
Sem regra 6 do prompt: nunca bypassar Use Cases.  
Sem regra 1 do prompt: UI não acessa domínio/infra diretamente.  

Padrão obrigatório:
```
UI (tela + componentes)
 -> ViewModel (estado + eventos de usuário + side-effects)
 -> UseCase (regra de negócio + validação + orquestração)
 -> Repository (interface de domínio)
 -> Infra (Room / Retrofit / SyncEngine / Media)
 -> EventProcessor (se houver mudança com efeito colateral)
```

Todos os nomes aqui usam os mesmos termos do DomainModel e do EventModel.

---

## Convenções transversais

- Estado de UI em UiState (data class imutável).
- Eventos de usuário em UiEvent (sealed interface).  
- Efeitos colaterais em UiEffect (sealed interface, consumidos uma vez).
- Repositórios sempre no domínio (`domain/repository`).  
- Implementações sempre em infra (`infra/repository`).  
- Cada UseCase faz apenas uma coisa (SRP).  
- Erros viram eventos/efeitos; nunca crasham a UI.  
- Erros clínicos NÃO são genéricos: têm código, mensagem amigável e contexto.

---

## 1. Patient

### 1.1 UI
- Tela principal: `PatientsScreen`
- Ações:
  - buscar paciente
  - criar paciente
  - ver detalhe
  - ver histórico
- Estado: `PatientsUiState(patients, isLoading, errorMessage)`
- Eventos: `SearchQueryChanged`, `CreatePatientClicked`, `PatientSelected`

### 1.2 ViewModel
- Expõe `patients`
- Recebe eventos, chama use cases
- Consome `PatientsUiEffect.ToPatientDetail(patientId)`

### 1.3 Use Cases
- `SearchPatients(query: String) : List<Patient>`
- `CreatePatient(input: CreatePatientInput) : Patient`
- `GetPatient(patientId: UUID) : Patient`

### 1.4 Domain
- Aggregate: `Patient`
- Entidade associada: `PatientProfile` (VO), `Consent` (para LGPD)
- Regras:
  - `tenantId` sempre preenchido
  - CPF/RG cifrados quando aplicável
  - criação exige consentimento mínimo de tratamento

### 1.5 Eventos
- `PatientRegistered(patientId, tenantId, createdAt, userId)`
- `PatientContactUpdated(...)`
- `PatientMerged(...)`
- `PatientIdentityLinked(...)`

### 1.6 Sync ChangeSet
- Tipo: `Patient`
- Operações: `create`, `update`
- Bloqueio offline:
  - merge assistido em `PatientProfile`
  - campos civis: last local wins se clinic offline

---

## 2. Appointment

### 2.1 UI
- Tela: `AppointmentsScreen`
- Ações:
  - listar por data
  - agendar
  - confirmar
  - cancelar
  - remarcar
  - configurar disponibilidade
- Estado: `AppointmentsUiState(items, selectedDate, isSlotLoading)`

### 2.2 ViewModel
- Orquestra:
  - `ScheduleAppointment`
  - `ConfirmAppointment`
  - `CancelAppointment`
  - `RescheduleAppointment`

### 2.3 Use Cases
- `ScheduleAppointment(input) : Appointment`
- `ConfirmAppointment(appointmentId) : Appointment`
- `CancelAppointment(appointmentId, reason?) : Appointment`
- `RescheduleAppointment(appointmentId, newStart, newEnd) : Appointment`
- `GetAvailability(professionalId, date) : List<Slot>`

### 2.4 Domain
- Aggregate: `Appointment`
- Regras:
  - não permitir double-booking (por profissional, por intervalo)
  - status inicial: `scheduled`
  - cancelamento gera justificativa obrigatória
  - Remark = novo Appointment; original vira histórico

### 2.5 Eventos
- `AppointmentScheduled(...)`
- `AppointmentConfirmed(...)`
- `AppointmentCancelled(...)`
- `AppointmentRescheduled(...)`
- `AvailabilityCreated(...)`
- `ReminderScheduled(...)`
- `ReminderSent(...)`

### 2.6 Sync
- Estratégia: last-write-wins com aviso UX
- idempotência: por `professionalId + startTime + endTime`
- merge: se conflito, preferir backend; UI atualiza após sync

---

## 3. Consultation

### 3.1 UI
- Tela: `ConsultationScreen`
- Ações:
  - iniciar consulta
  - registrar anamnese
  - registrar língua/pulso
  - registrar síndrome
  - finalizar consulta
- Estado: `ConsultationUiState(currentConsultation, isSaving, clinicalData)`

### 3.2 ViewModel
- Chama use cases específicos para anamnese, análise e síndrome
- Mantém rascunho local
- Persiste apenas via UseCase

### 3.3 Use Cases
- `StartConsultation(input) : Consultation`
- `RecordAnamnesis(consultationId, input) : Anamnesis`
- `RecordSyndrome(consultationId, syndrome) : Syndrome`
- `FinishConsultation(consultationId) : Consultation`

### 3.4 Domain
- Aggregate raiz: `Consultation`
- Entidades/VOs:
  - `Anamnesis`
  - `TongueAnalysis`
  - `PulseAnalysis`
  - `Syndrome`
- Regras:
  - consulta não pode ser finalizada sem anamnese mínima
  - síndrome não pode ser alterada após consulta fechada (imutabilidade clínica)
  - versionamento para evitar overwrite em sync

### 3.5 Eventos
- `ConsultationStarted(...)`
- `AnamnesisPerformed(...)`
- `TongueAnalyzed(...)`
- `PulseAnalyzed(...)`
- `SyndromeCreated(...)`
- `ConsultationFinished(...)`

### 3.6 Sync
- Tipo 3 clínico crítico:
  - bloqueio por merge assistido/online first
  - se offline, salva local e marca `pending`
  - conflito exibe tela de resolução antes de permitir nova ação clínica

---

## 4. TreatmentPlan

### 4.1 UI
- Tela: `TreatmentPlanScreen`
- Ações:
  - criar plano
  - anexar protocolo
  - atualizar frequência/duração
  - pausar/concluir
- Estado: `TreatmentPlanUiState(plan, sessions, status)`

### 4.2 ViewModel
- Chama use cases de criação, atualização e conclusão
- Mantém lista local de sessões associadas

### 4.3 Use Cases
- `CreateTreatmentPlan(input) : TreatmentPlan`
- `UpdateTreatmentPlan(planId, changes) : TreatmentPlan`
- `PauseTreatmentPlan(planId, reason) : TreatmentPlan`
- `CompleteTreatmentPlan(planId) : TreatmentPlan`

### 4.4 Domain
- Aggregate: `TreatmentPlan`
- Regras:
  - plano nasce vinculado a uma `Consultation`
  - não pode existir plano ativo duplicado para mesmo paciente em período sobreposto
  - transição `active -> completed/paused/cancelled` permitida
  - campos imutáveis após `completed`

### 4.5 Eventos
- `TreatmentPlanCreated(...)`
- `TreatmentPlanUpdated(...)`
- `TreatmentPlanPaused(...)`
- `TreatmentPlanCompleted(...)`
- `ProtocolReferenced(...)`

### 4.6 Sync
- Tipo 2 semi-automático:
  - merge assistido por campo (campo a campo)
  - prioridade: backend quando ambos online
  - quando conflito, UI oferece opções preservando campos do plano atual

---

## 5. ClinicalSession

### 5.1 UI
- Tela: `ClinicalSessionScreen`
- Ações:
  - criar sessão do plano
  - iniciar/finalizar sessão
  - registrar evolução
  - registrar procedimentos
  - gerar prescrição
- Estado: `ClinicalSessionUiState(session, evolution, prescription, isSaving)`

### 5.2 ViewModel
- Orquestra:
  - `ScheduleSession`
  - `StartSession`
  - `FinishSession`
  - `AddEvolutionNote`
  - `RegisterProcedures`
  - `IssuePrescription`

### 5.3 Use Cases
- `ScheduleSession(input) : ClinicalSession`
- `StartSession(sessionId) : ClinicalSession`
- `FinishSession(sessionId, notes, procedures) : ClinicalSession`
- `IssuePrescription(sessionId, items) : Prescription`

### 5.4 Domain
- Aggregate: `ClinicalSession`
- Entidades relacionadas:
  - `Prescription`
  - `PrescriptionItem` (VO)
- Regras:
  - sessão não pode iniciar sem plano ativo
  - sessão não pode ser finalizada sem evolução mínima
  - prescrição só pode ser emitida em sessão finalizada
  - sessão fechada é imutável

### 5.5 Eventos
- `ClinicalSessionScheduled(...)`
- `ClinicalSessionStarted(...)`
- `ClinicalSessionFinished(...)`
- `ClinicalSessionCancelled(...)`
- `ClinicalSessionNoShow(...)`
- `PrescriptionIssued(...)`

### 5.6 Sync
- Tipo 3:
  - bloqueio clínico
  - conflito exige reconciliação explícita
  - em offline, sessão salva local e depois reconcilia com backend

---

## 6. ClinicalRecord

### 6.1 UI
- Tela: `ClinicalRecordScreen`
- Ações:
  - ver timeline clínica
  - exportar/resumo
  - vincular exames/documentos externos
- Estado: `ClinicalRecordUiState(record, timeline, relatedDocs)`

### 6.2 ViewModel
- Consome `GetClinicalRecord(patientId)`
- Dispara exportação via use case

### 6.3 Use Cases
- `GetClinicalRecord(patientId) : ClinicalRecord`
- `ConsolidateClinicalRecord(appointmentId) : ClinicalRecord`

### 6.4 Domain
- Aggregate: `ClinicalRecord`
- Regras:
  - visão consolidada, não fonte primária
  - pode ser recalculado a partir de aggregates fonte
  - se divergente, recalcular via backend

### 6.5 Eventos
- `ClinicalRecordConsolidated(...)`
- `ClinicalRecordUpdated(...)`

### 6.6 Sync
- derivado; não editável diretamente na UI
- sincronização eventual após alterações clínicas relevantes

---

## 7. CRM

### 7.1 UI
- Telas:
  - `LeadsScreen`
  - `OpportunitiesScreen`
  - `CampaignsScreen`
- Ações:
  - capturar lead
  - converter lead em paciente
  - avançar oportunidade
  - registrar interação
- Estado: `CrmUiState(leads, opportunities, interactions)`

### 7.2 ViewModel
- Orquestra fluxo lead → opportunity → patient

### 7.3 Use Cases
- `CreateLead(input) : Lead`
- `ConvertLeadToPatient(leadId, patientId) : Lead`
- `AdvanceOpportunity(opportunityId, stage) : Opportunity`
- `RecordInteraction(input) : Interaction`

### 7.4 Domain
- Aggregates:
  - `Lead`
  - `Opportunity`
  - `Campaign`
  - `Interaction`
- Regras:
  - duplicidade por email/telefone deve ser prevenida
  - conversão deve gerar vínculo clínico `LeadConvertedToPatient`

### 7.5 Eventos
- `LeadCaptured(...)`
- `LeadConvertedToPatient(...)`
- `OpportunityAdvanced(...)`
- `InteractionRecorded(...)`

### 7.6 Sync
- Tipo 1:
  - last-write-wins com aviso
  - reconciliável facilmente

---

## 8. AI Clinical Query

### 8.1 UI
- Componente: `AiQueryPanel`
- Entradas:
  - texto livre
  - modo (`diagnosis_support`, `protocol_search`, `explanation`, `general`)
  - contexto de paciente opcional
- Saídas:
  - resposta
  - referências
  - confidence
  - trace id
- Estado: `AiQueryUiState(isLoading, answer, references, traceId, errorMessage)`

### 8.2 ViewModel
- Chama `ClinicalQueryUseCase`
- Expõe histórico de consultas
- Nunca monta prompt diretamente

### 8.3 Use Cases
- `SubmitClinicalQuery(input) : ClinicalAnswer`
- `GetReasoningTrace(traceId) : ClinicalReasoningTrace`

### 8.4 Domain
- Aggregates:
  - `ClinicalQuery`
  - `ClinicalAnswer`
  - `ClinicalReasoningTrace`
- Regras:
  - IA não altera estado clínico
  - toda resposta deve ter referência e nível de confiança
  - respostas sem fontes/baixa confiança devem ser sinalizadas

### 8.5 Eventos
- `ClinicalQueryRequested(...)`
- `ClinicalAnswerGenerated(...)`
- `EvidenceReferenced(...)`
- `ClinicalReasoningTraceRecorded(...)`
- `KnowledgeGapDetected(...)`

### 8.6 Sync
- apenas consultas/respostas locais (cache)
- trace id usado como idempotência

---

## 9. Knowledge Base

### 9.1 UI
- Tela: `KnowledgeScreen`
- Ações:
  - busca textual
  - filtros por tipo/tags
  - importar PDF
  - adicionar nota/referência
- Estado: `KnowledgeUiState(results, selectedNode, filters)`

### 9.2 ViewModel
- Chama use cases de busca e importação

### 9.3 Use Cases
- `SearchKnowledge(query, filters) : List<KnowledgeNodeHit>`
- `ImportKnowledge(input) : KnowledgeNode`
- `AddCitation(knowledgeNodeId, citation) : Citation`

### 9.4 Domain
- Aggregates:
  - `KnowledgeNode`
  - `Citation`
- Regras:
  - conteúdo imutável após indexado a menos que reprocessado
  - importação sempre gera status inicial `processing`

### 9.5 Eventos
- `KnowledgeNodeImported(...)`
- `KnowledgeNodeIndexed(...)`
- `KnowledgeNodeUpdated(...)`
- `KnowledgeEmbeddingGenerated(...)`
- `CitationAdded(...)`

### 9.6 Sync
- Tipo 1:
  - last-write-wins para knowledge; reindexação ocorre após sync
  - embeddings recalculados se `updatedAt > embeddedAt`
