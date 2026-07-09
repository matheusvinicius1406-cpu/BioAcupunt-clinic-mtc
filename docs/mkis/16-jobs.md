# MKIS — Jobs assíncronos, Workers, Filas, batches, DLQ e filas prioritárias
## Estratégia operacional para ingestão e processamento custoso

---

## 1. Princípios operacionais

- Nenhum processamento longo bloqueia requisições.
- Workers são desacoplados, idempotentes, com `tenant_id` e `job_id`.
- Filas são por prioridade e domínio; nunca uma fila única poluída.
- Cada job gera agregado com máquina de estados e event log próprio.

---

## 2. Classes de jobs

- `IngestionJob`: upload/download/parse/embedding/index/relate.
- `OCRJob`: aplicável a artefatos específicos.
- `EmbeddingJob`: gera vetores quando embeddings faltarem ou forem reindexados.
- `ReindexJob`: rebuild de índices por domínio.
- `GraphBatchJob`: atualiza relações periodicamente.
- `CleanupJob`: purge/archive com base em política de retenção e exclusão lógica.
- `DualWriteMigrationJob`: migração dual-write de embeddings.

---

## 3. Filas por prioridade

- `priority.realtime`: busca e leituras sensíveis.
- `priority.high`: ingestões clínicas.
- `priority.normal`: enriquecimento e atualizações de grafo.
- `priority.batch`: reindexação, otimização, relatórios.

Workers respeitam prioridade e limitam concorrência por domínio.

---

## 4. Retry, backoff, circuit breakers

- Retry: `3` tentativas com backoff exponencial + jitter.
- Dead-letter queue com motivo, payload mínimo e ação sugerida.
- Circuit breaker por integração externa e serviço de IA: `50% de erro por 30 segundos`.
- Estados: closed -> open -> half-open -> closed.

---

## 5. Batch e backpressure

- Tamanho de batch configurável por domínio.
- Limites de paralelismo, taxa e memória.
- Backpressure a clientes via headers e códigos quando limite for atingido.
- DLQ com reprocessamento manual único; antes, aprovação `security_reviewer`.