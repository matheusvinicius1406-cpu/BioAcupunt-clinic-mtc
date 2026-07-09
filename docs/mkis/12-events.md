# MKIS — Eventos
## Schema, canais, contratos, publishers/consumers e políticas de evento

---

## 1. Princípios

- Mudanças relevantes no domínio geram eventos.
- Eventos são a comunicação oficial entre contextos limite.
- Eventos são imutáveis; nunca editar ou excluir eventos emitidos.
- Toda operação de negócio tenta emitir evento via outbox antes do commit transacional.
- Eventos não carregam PII quando possível; usam `request_id` e referências.

---

## 2. Eventos centrais

- `knowledge.node.created.v1`
- `knowledge.node.versioned.v1`
- `knowledge.node.approved.v1`
- `knowledge.node.deprecated.v1`
- `knowledge.node.rejected.v1`
- `knowledge.artifact.uploaded.v1`
- `knowledge.artifact.quarantined.v1`
- `ingestion.job.status_changed.v1`
- `ingestion.job.failed.v1`
- `ingestion.job.completed.v1`
- `ingestion.job.cancelled.v1`
- `embedding.generated.v1`
- `embedding.migration.completed.v1`
- `graph.edge.created.v1`
- `evidence.score.updated.v1`
- `hard_delete.completed.v1`

---

## 3. Schema e versionamento

- Eventos possuem `schema_version` explícita.
- Campos obrigatórios: `event_id`, `occurred_at`, `stream_id`, `correlation_id`, `causation_id`, `tenant_id`, `request_id`, `actor_id`, `payload_hash`, `schema_version`.
- `correlation_id` agrupa fluxo de ingestão completo.
- `causation_id` aponta evento causal anterior imediato.
- Eventos antigos continuam consumíveis; nunca marshall obrigatório a campo novo.

---

## 4. Garantias

- Exactly-once delivery via outbox pattern + consumidores idempotentes por `idempotency_key`.
- Dead-letter queue por consumidor com motivo e payload mínimo.
- Reprocessamento manual via admin com marcação e justificativa.
- Observabilidade: publish rate, DLQ depth, consumer lag, failure reason.