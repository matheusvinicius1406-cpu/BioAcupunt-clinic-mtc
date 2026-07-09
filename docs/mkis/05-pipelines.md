# MKIS — Estado do Pipeline e Jobs
## Estados válidos, transições, eventos, observabilidade e simplificações

---

## 1. Premissas

- O pipeline trata apenas artefatos/dados; publicação requer stato `approved` humano.
- Qualquer stage pode transitar para `quarantined` ou `failed`.
- Atores principais: `worker`, `system`, `reviewer`, `clinical_validator`, `data_steward`, `security_reviewer`.
- Nenhuma transição pode pular auditoria quando `tenant_id != system`.

---

## 2. Estados do Ingestion Job

Estados canônicos:
- `queued`
- `downloading`
- `validation_failed`
- `scan_failed`
- `scan_quarantined`
- `ocr_queued`
- `ocr_running`
- `ocr_failed`
- `ocr_needs_human_review`
- `parsing_queued`
- `parsing_running`
- `parsing_failed`
- `chunking_queued`
- `chunking_running`
- `chunking_failed`
- `embedding_queued`
- `embedding_running`
- `embedding_failed`
- `indexing_queued`
- `indexing_running`
- `indexing_failed`
- `awaiting_approval`
- `approved_to_index`
- `completed`
- `failed`
- `manually_blocked`
- `cancelled`

Transições válidas selecionadas:
- `queued -> downloading`
- `downloading -> validation_failed` ou `ocr_queued`
- `validation_failed -> failed`
- `scan_failed -> failed`
- `scan_quarantined -> manually_blocked` ou `ocr_queued`
- `ocr_running -> ocr_failed` ou `ocr_needs_human_review` ou `parsing_queued`
- `ocr_needs_human_review -> parsing_queued` ou `scan_quarantined`
- `parsing_running -> parsing_failed` ou `chunking_queued`
- `chunking_running -> chunking_failed` ou `embedding_queued`
- `embedding_running -> embedding_failed` ou `indexing_queued`
- `indexing_running -> indexing_failed` ou `awaiting_approval`
- `awaiting_approval -> approved_to_index` ou `rejected`
- `approved_to_index -> completed`
- `failed -> queued` apenas com novo `attempt_id`, nunca automático infinito
- `any -> cancelled` via `data_steward`

Regras:
- `chunking_queued` exige `parsed_text` válido; senão, transição é `parsing_failed`.
- `embedding_queued` exige chunks válidos; senão, `chunking_failed`.
- Nenhuma transição após `completed` ou `cancelled` é permitida.

---

## 3. Estados do Documento / Knowledge Node

- `draft`
- `pending_review`
- `approved`
- `rejected`
- `deprecated`
- `superseded`

Transições relevantes:
- `draft -> pending_review` via `pipeline.completed`
- `pending_review -> approved` via `clinical_validator`
- `pending_review -> rejected` via `clinical_validator`
- `approved -> deprecated` via `editor`
- `rejected -> draft` via `pipeline re-run` com novo `attempt_id`
- `superseded` aplicado por jobs versionados; nunca manual direto.

---

## 4. Eventos associados

- `IngestionJobQueued`
- `IngestionJobStatusChanged`
- `IngestionJobFailed`
- `IngestionJobCompleted`
- `QuarantineCreated`
- `QuarantineResolved`
- `KnowledgeNodeCreated`
- `KnowledgeNodeApproved`
- `KnowledgeNodeRejected`
- `EmbeddingGenerated`
- `EmbeddingMigrationCompleted`
- `GraphEdgeCreated`
- `EvidenceScoreUpdated`
- `HardDeleteRequested`
- `HardDeleteCompleted`

Todos eventos incluem `stream_id`, `occurred_at`, `actor_id`, `tenant_id`, `request_id`, `payload_hash`, `schema_version`. Nenhum evento obriga `previous_hash`.

---

## 5. Quarantine workflow

Entrada:
- scan fail com `malware`, `sensitive_content`, `packer`, `password_locked`.
- re-exame humano suspeito.
- policy `ClassificationPolicy` rejeita tipo não suportado.

Saída:
- `quarantined -> approved_to_index` se `security_reviewer` aprovar.
- `quarantined -> manually_blocked` se rejeitado.
- Ação adicional `purge` com certificado para LGPD sanitário.

---

## 6. Awaiting approval workflow

Após pipeline e indexação:
- `indexing_running -> indexing_failed` permite retry.
- `indexing_running -> awaiting_approval` prepara node em `pending_review`.
- Timeout opcional: alerta; nunca auto-approve.
- Apenas papel `clinical_validator` pode `approve` ou `reject`.
- `rejected` retorna a `draft` com `change_reason` e mantém histórico por versão.

---

## 7. Rollback / Retry / DLQ

- Retry deve gerar novo `attempt_id` e preservar job antigo para auditoria.
- DLQ aceita jobs com 3 falhas consecutivas por stage.
- DLQ pode ser reprocessado com `max_attempts = 1` após ação humana.
- Rollback técnico: reindexação usa shadow index + swap; não há rollback por linha.

---

## 8. Observabilidade mínima

- Contador de jobs por state por tenant.
- Latência por stage.
- DLQ depth por tenant.
- Tempo em `awaiting_approval`.
- Proveniência de versão: `embedding_version` ativo por query.

Sem esses sinais, a operação não tem como evitar degradação silenciosa.