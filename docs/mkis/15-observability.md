# MKIS — Observabilidade
## Logs, métricas, tracing, SLOs, SLIs, alertas e health checks

---

## 1. Objetivo

Toda operação crítica deve ser observável de ponta a ponta: latência medida, taxa de erro, saturação e estado de saúde interno/externo.
Valores canônicos: `29-canonical-references.md`.

---

## 2. Pilares

- **Logs estruturados**: JSON com `tenant_id`, `request_id`, `trace_id`, `actor_id`, `component`, `severity`.
- **Tracing**: OpenTelemetry com propagação obrigatória em chamadas internas e externas.
- **Métricas**: contadores, histogramas, gauges por operação relevante.
- **Health checks**: `alive`, `ready`, dependências externas, filas, caches, índice vetorial.

---

## 3. SLOs/SLIs

| SLO | Alvo | Janela |
|------|------|--------|
| Disponibilidade do serviço | 99.95% | 30 dias |
| Latência P95 busca pública | < 300ms | 5 minutos |
| Latência P95 ingestão completa | worker p99 < 5 min | 5 minutos |
| Erros 5xx | < 0.1% | 1 hora |
| Erros 4xx por policy violation | não conta contra SLO de confiabilidade | N/A |
| Search timeout | 5s | requisição |
| OCR timeout | 120s | job |
| LLM timeout | 45s | requisição |
| Database timeout | 10s | requisição |
| Health check timeout | 30s | probe |

SLIs:
- Disponibilidade = requests com 2xx e 4xx por policy / total
- Latência = histograma P95/P99 por endpoint
- Erros = 5xx e timeouts por minuto
- Saturação = fila lag, tamanho de batch pendente, conexões DB ocupadas

---

## 4. Alertas

- SLO breach imminent
- fila com lag acima de limite por tenant
- OCR/embedding com taxa de erro acima de limiar
- índice vetorial com queda de performance
- aumento súbito de 404s por idempotência inválida
- autenticação malsucedida em massa
- DLQ depth maior que baseline em 2x por 10min

---

## 5. Métricas obrigatórias

| Métrica | Tipo | Uso |
|---------|------|-----|
| ingestion.job.status | contador | throughput por status/tenant |
| search.latency | histograma | P95/P99 |
- cache hit/miss | contador | eficiência de cache
- search.filtered.count | contador | popularidade
- ia.cost | histograma/sum | custo por request
- security.blocked | contador | SSRF/XXE/injection/minuto |
- audit.retention.violation | contador | alerta compliance

---

## 6. Dashboards

- Ingestão por status, throughput por hora, tamanho médio.
- Busca: popularidade, filtros, cache hit ratio, latência.
- IA: custo por request, taxa de fallback, taxa de rejeição.
- Segurança: bloqueios SSRF/XXE/Injection por minuto.
- Falhas de vendor: rate e tempo de abertura de circuit breaker.

---

## 7. Runbooks

- Degradação de busca: isolar tenant, trocar para fallback lexical, abrir incidente.
- Índice vetorial lento: reindex shadow + swap; manter busca lexical viva.
- DLQ crescendo: escalar consumer, inspecionar motivo e payload mínimo.
- Pico de custo LLM: ativar modo degraded por tenant, limitar batch.