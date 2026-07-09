# MKIS — Qualidade
## Níveis de teste, gate de qualidade, métricas de qualidade textual e compliance

---

## 1. Objetivo

Garantir que cada release do conhecimento preserve correção clínica, segurança, desempenho e explicabilidade.

---

## 2. Níveis de teste

- **Unitário**: purity de regras científicas, policies, transformers, validators.
- **Integração**: contratos API, pipeline stages, eventos, outbox, RLS.
- **Contrato**: OpenAPI auto-validado por mudança.
- **Estresse**: busca híbrida sob carga, embedding job sob paralelismo, DLQ operada.
- **Qualidade textual**: consistency, hallucination rate, citation coverage, drift de embedding.

---

## 3. Gates obrigatórios

- `contract-test`: falha bloqueia deploy.
- `security-scan`: SAST + dependency review + RLS smoke.
- `data-quality-score`: mínimo `0.85` por lote científico; senão, quarentena automática.
- `observability-check`: traces e métricas existem para operações críticas.

---

## 4. Métricas de qualidade textual

| Métrica | Alvo |
|---------|------|
| Hallucination rate | < 2% |
| Citation coverage | 100% em claims clínicas |
| Embedding drift delta | < 5% |
| Approval rejection rate | < 10% |
| DLQ reprocessing success | > 80% uma tentativa |

---

## 5. Amostragem e revisor

- Revisão humana obrigatória em approved inicial até validação automática atingir SLO.
- Revisor automático: `temperature 0`, schema forçado, validadores pós-geração.

---

## 6. Runbooks

- Baixa qualidade textual: pausar batch, voltar para fonte primária, reprocessar após ajuste de prompt.
- Hallucination detectada: marcar node como `rejected`, abrir ticket científico.
- Drift alto: reindex differential com `embedding_v2`.