# DatabaseModel.md — Modelo de Dados Oficial

## 1. Objetivo
Definir regras de persistência que atendam a:
- Dados operacionais relacionais (PostgreSQL)
- Dados clínicos com auditoria e imutabilidade
- Offline-first (Room local)
- Vetorial/semântica como cidadão de primeira classe
- Event sourcing/histórico futuro

---

## 2. Estratégias de Persistência por Contexto

| Contexto | Estratégia |
|----------|------------|
| Clinical, Patients, Appointments, Finance, CRM, Education, Protocols | PostgreSQL relacional + Room local (offline) |
| Knowledge | PostgreSQL + Qdrant (vetorial) + storage |
| Analytics | PostgreSQL + materialized views + cache |
| Authentication | PostgreSQL |
| Sync & Offline | Room local (ChangeSet, Conflict, SyncSession) + PostgreSQL remoto |
| AI | Qdrant + PostgreSQL para rastreabilidade |

---

## 3. PostgreSQL — Modelo Relacional Oficial

### 3.1 Convenções
- Tabelas em `snake_case` no plural para operacionais: `patients`, `appointments`, `clinical_sessions`.
- Tabelas em `snake_case` no singular para configuração: `tenant`, `plan`, `feature_flag`.
- Toda tabela operacional contém:
  - `id UUID PRIMARY KEY`
  - `tenant_id UUID NOT NULL`
  - `created_at TIMESTAMPTZ NOT NULL DEFAULT now()`
  - `updated_at TIMESTAMPTZ NOT NULL DEFAULT now()`
  - `deleted_at TIMESTAMPTZ` (soft delete)
- Chaves estrangeiras sempre nomeadas: `<referencia>_id`.
- Índices compostos por `tenant_id` + `created_at` quando houver listagens.
- FTS (tsvector) em colunas textuais relevantes.

### 3.2 Schema por Contexto

#### Authentication & Authorization
- `users` (id, tenant_id, email, phone, status, email_verified, last_login_at, created_at, updated_at, deleted_at)
- `roles` (id, tenant_id?, name, scope)
- `permissions` (id, code, description)
- `user_roles` (user_id, role_id)
- `role_permissions` (role_id, permission_id)
- `sessions` (id, user_id, refresh_token_hash, expires_at, device_info, ip_address, created_at)
- `audit_logs` (id, tenant_id, user_id, action, resource, resource_id, changes JSONB, timestamp, ip_address)

#### Patients
- `patients` (id, tenant_id, full_name, birth_date, gender, cpf_hash, rg_hash, address JSONB, contacts JSONB, emergency_contact JSONB, status, created_at, updated_at, deleted_at)
- `patient_meta` (id, patient_id, tenant_id, crm_record JSONB, tags text[], lifetime_value, acquisition_source, risk_score, created_at, updated_at)
- `consents` (id, tenant_id, patient_id, type, granted, granted_at, revoked_at, document_url, ip_address, user_agent, created_at, updated_at)

#### Clinical
- `appointments` (id, tenant_id, patient_id, professional_id, status, start_time, end_time, type, room, notes, version int, created_at, updated_at, deleted_at)
- `consultations` (id, tenant_id, patient_id, professional_id, appointment_id, status, started_at, finished_at, created_at, updated_at, deleted_at)
- `anamnesis` (id, consultation_id, chief_complaint, history_of_present_illness, past_history, family_history, lifestyle, emotional_state, observations, created_at)
- `tongue_analysis` não deve ser tabela separada na MVP; armazenar como JSONB em `consultations.tongue_analysis`.
- `pulse_analysis` idem, em `consultations.pulse_analysis`.
- `syndromes` (id, consultation_id, name, pattern, theory_basis, references UUID[], created_at)
- `treatment_plans` (id, consultation_id, diagnosis_id, objective, frequency, duration, protocol_id, protocol_snapshot JSONB, status, created_at, updated_at, deleted_at)
- `clinical_sessions` (id, treatment_plan_id, professional_id, status, scheduled_at, started_at, finished_at, evolution_notes, procedures_performed JSONB, created_at, updated_at, deleted_at)
- `clinical_records` (id, tenant_id, patient_id, consultation_id, anamnesis_id, diagnosis_ids UUID[], treatment_plan_id, session_ids UUID[], summary, created_at, updated_at, deleted_at)
- `acupuncture_points` (id, code, name, meridian, location, indications, contraindications, references UUID[])
- `prescriptions` (id, clinical_session_id, professional_id, items JSONB, observations, created_at, updated_at, deleted_at)

#### Protocols
- `protocols` (id, tenant_id, title, description, category, tags text[], status, current_version_id, created_at, updated_at, deleted_at)
- `protocol_versions` (id, protocol_id, version, semver, steps JSONB, change_notes, published_at, created_by, created_at)
  - `steps JSONB` segue modelo forte:
```json
[
  {
    "order": 1,
    "title": "string",
    "description": "string",
    "points": [{ "code": "E36", "name": "Zusanli", "technique": "string?" }],
    "duration": "string?",
    "observations": "string?",
    "contraindications": "string?"
  }
]
```

#### Knowledge
- `knowledge_nodes` (id, tenant_id?, type, title, content, summary, tags text[], version, metadata JSONB, embedding_id?, status, created_at, updated_at, deleted_at)
- `citations` (id, knowledge_node_id, source, page, excerpt, context)
- FTS em `knowledge_nodes` sobre `title`, `content`, `summary`, `tags`.

#### Education
- `courses` (id, tenant_id, title, description, level, category, instructor_id, is_public, status, created_at, updated_at)
- `modules` (id, course_id, title, "order", content_ref, created_at)
- `lessons` (id, module_id, title, type, content_ref, duration_minutes, created_at)
- `quizzes` (id, module_id, questions JSONB, passing_score, created_at, updated_at)
- `flashcards` (id, tenant_id, front, back, tags text[], knowledge_refs UUID[], next_review_at, interval, ease_factor, created_at, updated_at)
- `certifications` (id, course_id, student_id, issued_at, expires_at, code, metadata, created_at)
- `enrollments` (id, course_id, student_id, status, progress, enrolled_at, completed_at)

#### Finance
- `invoices` (id, tenant_id, patient_id, issuer_id, status, due_date, paid_at, total_amount, currency, items JSONB, pdf_url, created_at, updated_at, deleted_at)
- `payments` (id, invoice_id, method, amount, status, paid_at, gateway_id, metadata JSONB, created_at, updated_at, deleted_at)
- `transactions` (id, tenant_id, payment_id, type, amount, category, related_entity JSONB, occurred_at, created_at)
- `charges` (id, invoice_id, installment_number, total_installments, amount, due_date, status, payment_id, created_at, updated_at)

#### CRM
- `leads` (id, tenant_id, name, email, phone, source, interest, status, assigned_to, converted_to_patient_id, created_at, updated_at, deleted_at)
- `opportunities` (id, lead_id, stage, value, probability, expected_close_date, lost_reason, created_at, updated_at)
- `campaigns` (id, tenant_id, name, channel, type, target_audience JSONB, scheduled_at, status, created_at, updated_at)
- `interactions` (id, lead_id, patient_id, channel, direction, summary, occurred_at, created_by, created_at)

#### AI
- `clinical_queries` (id, tenant_id, user_id, patient_context_id, query, mode, filters JSONB, created_at)
- `clinical_answers` (id, query_id, answer, summary, confidence, references JSONB, model_version, latency_ms, created_at)
- `clinical_reasoning_traces` (id, query_id, professional_id, steps JSONB, final_decision, confidence, created_at)
- `knowledge_gaps` (id, query_id, description, suggested_acquisition, status, assigned_to, created_at, updated_at)
- `prompt_templates` (id, name, mode, template, variables text[], version, is_active, created_at, updated_at)

#### Analytics
- `metrics` (id, tenant_id, name, value, dimensions JSONB, period, calculated_at, valid_from, valid_to)
- `dashboards` (id, tenant_id, owner_id, name, layout JSONB, is_public, created_at, updated_at)
- `reports` (id, tenant_id, name, type, parameters JSONB, generated_at, file_url, created_by, created_at)

#### Notifications
- `notifications` (id, tenant_id, user_id, channel, template_id, variables JSONB, status, scheduled_at, sent_at, delivered_at, read_at, error_message, created_at)
- `templates` (id, tenant_id?, channel, name, subject, body, variables text[], is_system, created_at, updated_at)
- `preferences` (id, user_id, channel, enabled, opt_in_types text[], updated_at)

#### Administration & Settings
- `tenants` (id, name, domain, plan_id, status, settings JSONB, billing_config JSONB, created_at, updated_at)
- `plans` (id, name, price, currency, features text[], limits JSONB, is_active)
- `feature_flags` (id, tenant_id?, key, enabled, rollout_percentage, variants JSONB, updated_at)

#### Sync & Offline
- `device_registrations` (id, tenant_id, user_id, device_id, platform, last_sync_at, status, created_at, updated_at)
- `sync_sessions` (id, device_id, started_at, finished_at, status, changes_processed, conflicts_detected, conflicts_resolved, error_message)
- `change_sets` (id, device_id, entity_type, entity_id, operation, payload JSONB, version, timestamp, synced_at, conflict_id, status)
- `sync_conflicts` (id, local_change_set_id, remote_change_set_id, winner_strategy, winner_change_set_id, resolved_by, resolved_at, resolution_data JSONB, status)

---

## 4. Room (Android Offline)

### 4.1 Estrutura oficial
- Banco: `bioacupunt_offline.db`
- Entidades principais espelham agregados necessários para funcionamento offline.
- Não há replicação total; apenas entidades com acesso offline obrigatório.

### 4.2 Entidades Room iniciais
- `PatientEntity` (id, tenant_id, full_name, birth_date, gender, cpf_hash, status, updated_at)
- `ClinicalRecordEntity` (id, tenant_id, patient_id, summary, updated_at)
- `AppointmentEntity` (id, tenant_id, patient_id, professional_id, start_time, end_time, status, version)
- `TreatmentPlanEntity` (id, consultation_id, objective, frequency, duration, status, updated_at)
- `ClinicalSessionEntity` (id, treatment_plan_id, professional_id, status, scheduled_at, started_at, finished_at, updated_at)
- `SyncMetaEntity` (id, device_id, last_sync_at, schema_version)

### 4.3 Regras
- Toda escrita offline primeiro persiste em Room, depois gera `ChangeSet` e marca `pending`.
- Room é a única fonte local; UI lê de repositórios que expõem `Flow<T>`.
- Nenhuma consulta SQL bruta na UI; acesso sempre por DAO tipado.
- FTS4 em `ClinicalRecordEntity.summary` e tags de conhecimento.

---

## 5. Modelo Vetorial (Qdrant)

### 5.1 Coleções
- `clinical_knowledge` — embeddings de `KnowledgeNode.title + content + summary`
- `protocols` — embeddings de `ProtocolVersion.steps` estruturados
- `clinical_answers` — embeddings das perguntas/respostas para reuso

### 5.2 Campos obrigatórios no payload
- `knowledgeNodeId`
- `tenantId?`
- `type`
- `tags`
- `createdAt`

### 5.3 Estratégia de atualização
- Geração de embedding assíncrona após `KnowledgeNodeIndexed`.
- Reprocessamento em caso de alteração.

---

## 6. Indexação e Performance

### 6.1 PostgreSQL
- Índices por `tenant_id` e `created_at` como padrão.
- Índices únicos por `email` (por tenant) e códigos quando houver.
- `tsvector` em campos de busca textual (nome, título, resumo, tags).
- JSONB indexado apenas quando houver query real comprovada.

### 6.2 Android/Room
- Índices em chaves usadas em `WHERE`.
- FTS4 em campos de busca offline principal.

---

## 7. Versionamento e Evolução

- Toda tabela nova ou alteração de schema nasce como migration versionada.
- Migrations são imutáveis; novas alterações geram nova versão.
- Nenhuma mudança de schema sem ADR + migration + teste.

---

## 8. Riscos e Mitigações

| Risco | Mitigação |
|-------|-----------|
| Schema evoluindo sem rastreabilidade | Exigir migration + ADR em cada mudança |
| JSONB como refúgio para entidades importantes | Validar com modelo forte + testes de schema |
| Dados clínicos crescendo muito | Particionamento por `tenant_id` + `created_at` com avalias após 1M de linhas |
| Inconsistência local/remota | Sync idempotente + change sets + versionamento otimista |
| Vetor desatualizado | Reprocessamento automático quando `updated_at > embedded_at` |

---

## 9. Plano de Validação
1. Revisar schema com profissional clínico para garantir campos MTC essenciais.
2. Validar estrutura de `protocol_versions.steps` com equipe de conteúdo.
3. Aprovar particionamento e índices antes da primeira migração de produção.
4. Validar modelo offline Room com time mobile.
5. Aprovar coleções e payloads do Qdrant antes da implementação de IA.
