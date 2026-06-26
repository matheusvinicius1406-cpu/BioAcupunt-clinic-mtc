# BoundedContexts.md — Mapa de Contextos Delimitados

## 1. Princípio Norte

O BioAcupunt Supremo é tratado inicialmente como **Modular Monolith** com preparação para evolução futura para **microsserviços**. Cada contexto abaixo representa:

- Um agregado raiz e suas entidades/vO
- Regras de negócio específicas
- Eventos de domínio próprios
- Limites explícitos contra outros contextos
- Anticorrupção quando a fronteira for cruzada

> **Regra:** cada contexto evolui com autonomia. Integrações ocorrem por contratos explÍcitos (events, APIs, repository interfaces), jamais por acoplamento direto.

---

## 2. Contextos Delimitados (Bounded Contexts)

### 2.1 Authentication & Authorization
- **Responsabilidade:** Identidade, autenticação, sessão, RBAC (e futuramente ABAC), auditoria de acesso.
- **Agregados principais:** User, Role, Permission, Session, AuditLog.
- **Eventos iniciais:** UserRegistered, UserLoggedIn, UserLoggedOut, RoleAssigned.
- **Integrações:** Consumido por todos os outros contextos via token/claims. Fornece identidade centralizada.

**Decisão arquitetural:** Contexto centralizado. Usuário é entidade compartilhada, mas contexto mantém modelo próprio de identidade. Evita espalhar auth por dezenas de módulos.

**Alternativa considerada:** Auth distribuída por bounded context.
**Por que não:** duplicação de lógica, inconsistência de sessões, dobra de work. Centralizar reduz custo e aumenta segurança.

**Riscos/impactos:**
- Ponto único, exige alta disponibilidade.
- Justifica investimento desde a MVP.

---

### 2.2 Clinical (Core)
- **Responsabilidade:** Modelagem clínica especializada de MTC, prontuário, anamnese, diagnóstico energético, plano terapêutico e sessões.
- **Agregados principais:** Patient, Consultation, Anamnesis, Diagnosis, TreatmentPlan, Session, ClinicalRecord, TongueAnalysis, PulseAnalysis, Syndrome, AcupuncturePoint, Prescription.
- **Eventos iniciais:** PatientRegistered, ConsultationScheduled, AnamnesisPerformed, DiagnosisCreated, TreatmentPlanCreated, SessionExecuted, ClinicalRecordUpdated.
- **Integrações:** Consome Authentication; referencia Knowledge (protocolos); emite eventos para Finance e Notifications.

**Decisão arquitetural:** Contexto mais estável e crítico. Define invariantes clínicas imutáveis (ex.: sessão encerrada não pode ser alterada; apenas evolução).

**Alternativa considerada:** Modelo genérico de prontuário com extensões MTC.
**Por que não:** MTC tem terminologia, fluxo e regras específicas (Zang Fu, meridianos, pontos, sinais). Genericidade empobrece dados clínicos e invalida IA especialista.

**Riscos/impactos:**
- Complexidade alta (dados clínicos regulados).
- Requer migrations versionadas e auditoria obrigatória.
- Mudanças aqui afetam praticamente todos os demais contextos.

---

### 2.3 Patients
- **Responsabilidade:** Cadastro civil, identificação, relacionamentos familiares, contatos, consentimentos LGPD.
- **Agregados principais:** PatientProfile, Contact, Identifier, Consent, PatientTag.
- **Eventos iniciais:** PatientCreated, PatientUpdated, ConsentGranted, ConsentRevoked.
- **Integrações:** Referenciado por Clinical, Finance, CRM, Notifications.

**Decisão arquitetural:** Contexto separado do Clinical. Motivo: paciente existe independentemente de consultas (CRM, financeiro, educação). Desacopla ciclo de vida civil do ciclo clínico.

**Riscos/impactos:**
- Consistência eventual entre PatientProfile e Clinical.Patient.
- Baixo risco: sincronização por eventos corrige divergências.

---

### 2.4 Appointments
- **Responsabilidade:** Agenda, disponibilidade, agendamentos, retornos, confirmações, lembretes, conflitos.
- **Agregados principais:** Appointment, Slot, Availability, Reminder, RecurrenceRule.
- **Eventos iniciais:** AppointmentScheduled, AppointmentCancelled, AppointmentRescheduled, ReminderSent.
- **Integrações:** Consome Clinical (para regras de retorno); emite para Notifications e CRM.

**Decisão arquitetural:** Separado de Clinical para evitar contaminar modelo clínico com problemas de agendamento. Agenda é dominio de disponibilidade/conflito; Clinical é dominio de assistência.

**Riscos/impactos:**
- Duplicação mínima de dados entre Appointment e Consultation.
- Justifica-se por clareza arquitetural.

---

### 2.5 Protocols
- **Responsabilidade:** Protocolos clínicos, condutas, sequências terapêuticas, bibliotecas de pontos.
- **Agregados principais:** Protocol, ProtocolVersion, Step, PointSet, Contraindication.
- **Eventos iniciais:** ProtocolCreated, ProtocolUpdated, ProtocolDeprecated, ProtocolReviewed.
- **Integrações:** Referenciado por Clinical e Knowledge; IA usa como base de conhecimento estruturado.

**Decisão arquitetural:** Contexto próprio porque protocolos são ativos de conhecimento com ciclo de vida editorial (revisão, versão, obsolescência) distinto do prontuário.

**Riscos/impactos:**
- Versões de protocolos precisam ser snapshots no prontuário (Clinical) no momento da sessão.
- Isso é intencional: prática clínica não pode ser alterada retroativamente.

---

### 2.6 Knowledge (Scientific Library)
- **Responsabilidade:** Biblioteca científica (livros, artigos, PDFs, imagens, vídeos, casos clínicos), indexação, busca híbrida, referências.
- **Agregados principais:** KnowledgeNode, Book, Article, PdfDocument, ImageAsset, VideoAsset, ClinicalCase, Citation.
- **Eventos iniciais:** KnowledgeImported, KnowledgeIndexed, KnowledgeUpdated, KnowledgeDeleted.
- **Integrações:** Alimenta IA (RAG); referenciado por Clinical e Education; depende de infraestrutura vetorial (Qdrant).

**Decisão arquitetural:** Separado de Clinical porque conhecimento tem regras de autoria, licenciamento e indexação diferentes. Em Clinical o dado é operacional; em Knowledge o dado é referencial.

**Riscos/impactos:**
- Volume grande de mídia exige storage dedicado.
- Indexação assíncrona via workers (Celery) será necessária.

---

### 2.7 Education
- **Responsabilidade:** Conteúdo educacional: cursos, módulos, aulas, quizzes, flashcards, exercícios, certificados.
- **Agregados principais:** Course, Module, Lesson, Quiz, Flashcard, Exercise, Certification, Enrollment.
- **Eventos iniciais:** CoursePublished, StudentEnrolled, QuizCompleted, FlashcardReviewed, CertificationIssued.
- **Integrações:** Usa Knowledge como repositório de conteúdo; consome Authentication para perfis aluno/professor.

**Decisão arquitetural:** Contexto independente porque ciclo de vida pedagógico é distinto do ciclo clínico. Educação pode existir sem instância de Clinical.

**Riscos/impactos:**
-复杂性 pedagógica pode crescer muito.
- Decisão manter separado permite evoluir módulo educacional com autonomia.

---

### 2.8 Finance
- **Responsabilidade:** Faturamento, cobranças, pagamentos, repasses, Fluxo de caixa, Nota Fiscal e relatórios financeiros.
- **Agregados principais:** Invoice, Payment, Transaction, Charge, Refund, FinancialReport.
- **Eventos iniciais:** InvoiceGenerated, PaymentReceived, PaymentFailed, RefundIssued.
- **Integrações:** Consome Clinical (sessões concluídas); consome Patient; emite para Notifications.

**Decisão arquitetural:** Contexto isolado para segregar obrigações contá tributárias do fluxo clínico.

**Riscos/impactos:**
- Requer conformidade fiscal futura (NF-e/NFS-e).
- Eventos de pagamento devem ser idempotentes.

---

### 2.9 CRM
- **Responsabilidade:** Relacionamento com paciente, leads, campanhas, funil, comunicação multicanal.
- **Agregados principais:** Lead, Opportunity, Campaign, Interaction, Communication.
- **Eventos iniciais:** LeadCaptured, OpportunityOpened, CampaignSent, InteractionRecorded.
- **Integrações:** Consome Patient e Appointments; emite para Notifications.

**Decisão arquitetural:** CRM separado de Clinical para desacoplar aquisição/relacionamento da assistência. CRM trata do relacionamento; Clinical trata do tratamento.

**Riscos/impactos:**
- Possível duplicação de nomes/contatos com Patient.
- Solução: Patient é autoridade para dados civis; CRM mantém metadados de relacionamento.

---

### 2.10 AI (Clinical Intelligence)
- **Responsabilidade:** Motor especialista, RAG, classificação, síntese clínica, sugestões, justificativas.
- **Agregados principais:** ClinicalQuery, ClinicalAnswer, EvidenceReference, ConfidenceScore, KnowledgeGap.
- **Eventos iniciais:** ClinicalQueryRequested, ClinicalAnswerGenerated, EvidenceReferenced, KnowledgeGapDetected.
- **Integrações:** Consome Knowledge (busca vetorial + textual); referencia Protocols; opera sobre Clinical sem modificar diretamente (read-only nas consultas).

**Decisão arquitetural:** Contexto autônomo porque IA tem pipeline próprio (embeddings, vector DB, LLM, prompt engineering, avaliação). Não deve ser misturado com orquestração clínica.

**Riscos/impactos:**
- Dependência de infraestrutura vetorial (Qdrant) e embeddings.
- Isolamento intencional: Clinical não depende de AI para funcionar.

---

### 2.11 Analytics
- **Responsabilidade:** Métricas operacionais, clínicas, financeiras e de produto; dashboards; relatórios.
- **Agregados principais:** Metric, Dashboard, Report, KPI, Cohort.
- **Eventos iniciais:** MetricComputed, ReportGenerated, DashboardUpdated.
- **Integrações:** Consome eventos de Clinical, Finance, Appointments, CRM e Notifications via stream/event bus.

**Decisão arquitetural:** Read-model/materialized view sobre dados operacionais. Separação evita impacto em escritas clínicas.

**Riscos/impactos:**
- Baixo risco operacional.
- Consistência eventual é aceitável para dashboards.

---

### 2.12 Notifications
- **Responsabilidade:** Mensageria (e-mail, SMS, push, WhatsApp), templates, preferências de usuário.
- **Agregados principais:** Notification, Template, Preference, DeliveryLog.
- **Eventos iniciais:** NotificationSent, NotificationDelivered, NotificationFailed, PreferenceChanged.
- **Integrações:** Reativo a eventos de Clinical, Appointments, Finance, CRM.

**Decisão arquitetural:** Contexto dedicado para evitar espalhar lógica de envio por dezenas de módulos. Facilita troca de provedor (Twilio, Firebase, etc).

**Riscos/impactos:**
- Baixo risco.
- Entrega assíncrona pode causar delay.

---

### 2.13 Administration & Settings
- **Responsabilidade:** Configurações globais do tenant, recursos, planos, features flags, administração de usuários e políticas.
- **Agregados principais:** Tenant, Plan, FeatureFlag, Setting, AdminAction, BillingConfig.
- **Eventos iniciais:** TenantCreated, PlanChanged, FeatureFlagUpdated.
- **Integrações:** Afeta todos os contextos por configuração global.

**Decisão arquitetural:** Contexto separado porque configuração é transversal e deve ser auditada.

**Riscos/impactos:**
- Mudanças de configuração afetam múltiplos módulos.
- Requer propagação eficiente.

---

### 2.14 Sync & Offline
- **Responsabilidade:** Sincronização offline-first, versionamento, merge, resolução de conflitos, fila, DeviceId.
- **Agregados principais:** SyncSession, SyncLog, Conflict, ChangeSet, DeviceRegistration.
- **Eventos iniciais:** SyncStarted, SyncCompleted, ConflictDetected, ConflictResolved.
- **Integrações:** Transversal — intercepta mudanças de Clinical, Patients, Appointments e Finance antes de persistir e envia ao backend quando online.

**Decisão arquitetural:** Contexto transversal tratado como módulo de infraestrutura compartilhada. Não é bounded context de domínio puro, mas é isolado arquiteturalmente.

**Riscos/impactos:**
- Maior custo de implementação.
- Se falhar, causa perda de dados ou conflitos.
- Requer idempotência e transações atômicas locais.

---

## 3. Mapa de Relacionamentos (Context Map)

```
[Authentication] ----- identifica -----> [todos os contexts]

[Patients] <----- consumido por ----- [Clinical | Finance | CRM | Notifications]

[Clinical] <----- referencia ----- [Protocols]
[Clinical] ----- consulta -----> [AI]  (read-only)
[Clinical] ----- emite -----> [Finance | Notifications | Analytics]

[Appointments] ----- emite -----> [Notifications | CRM | Analytics]

[Knowledge] <----- alimenta ----- [AI]
[Knowledge] ----- referencia ----- [Education]

[CRM] <----- usa ----- [Patients]
[CRM] ----- emite -----> [Notifications | Analytics]

[Finance] <----- consumido ----- [Analytics]

[Analytics] <------ eventos ------ [Clinical | Finance | Appointments | CRM | Notifications]

[Notifications] <---- eventos ---- [Clinical | Appointments | Finance | CRM | AI]

[Administration] --- configura ----> [todos]

[Sync] --- transversal ----> [Clinical | Patients | Appointments | Finance]
```

---

## 4. Decisões Arquiteturais

| Decisão | Escolha | Consequência |
|---------|---------|--------------|
| Modular Monolith inicial | SIM | Custo menor de operação na MVP. Preparado para extrair bounded contexts em serviços futuramente. |
| Separação Patient x Clinical | SIM | Dados civis e clínicos com ciclos de vida diferentes. Evita acoplamento indevido. |
| AI como contexto separado | SIM | Pipeline especialista é complexo e evolui rápido. Isolamento protege core clínico. |
| Notificações centralizadas | SIM | Troca de provedor sem tocar em regras de negócio. |
| Sync transversal | SIM | Offline first não é um bounded context tradicional; é camada de infraestrutura compartilhada. |

---

## 5. Alternativas Consideradas

### 5.1 “Tudo em um bounded context clínico”
- **Por que não:** Cria massa única impossível de evoluir. Mistura agenda, financeiro, CRM e educação com prontuário. Violenta SRP e acopla lançamentos.

### 5.2 “Microsserviços desde MVP”
- **Por que não:** Custo operacional alto, overhead de orquestração, complexidade de deploy e observabilidade incompatível com fase inicial. Escolhemos modular monolith para aprender o domínio antes de particionar.

### 5.3 “AI acoplada ao Clinical”
- **Por que não:** Clinical ficaria dependente de prompts, modelos e vetores. IA pode ser substituída/atualizada sem tocar no core clínico. Independência viabiliza experimentação contínua.

### 5.4 “Auth descentralizada”
- **Por que não:** Cada contexto implementando login/session/permissão gera inconsistência, retrabalho e risco de segurança.

---

## 6. Riscos e Mitigações

| Risco | Mitigação |
|-------|-----------|
| Crescimento desordenado de contextos | Política de criação: só criar novo contexto se houver agregado raiz próprio, eventos próprios e timeout de consistência > 2s. |
| Comunicação excessiva entre contextos | Eventos assíncronos sempre que possível. APIs síncronas apenas para consultas. |
| Inconsistência eventual | Aceitar consistência eventual por boundary. Usar eventos de reconciliação e audit log. |
| Dificuldade de evolução para microsserviços | Manter limites claros, evitar queries transversais, usar interfaces/anti-corruption. |
| Over-engineering inicial | Documentar, mas não implementar mais do que a MVP exige. Evitar abstrações prematuras. |

---

## 7. Plano de Validação

- Revisar nomes de agregados com profissional clínico.
- Validar eventos suficientes para cobrir jornada do paciente.
- Confirmar separação Patient x Clinical com caso real de uso (ex.: paciente vira lead sem consulta).
- Aprovar integração AI → Knowledge (RAG) antes de implementar.
- Validar que contexto Sync não vaza regras clínicas.
