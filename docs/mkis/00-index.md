# MKIS — Índice oficial
## Navegação documental do Medical Knowledge Intelligence System

---

## 1. Estrutura

- `00-index.md` — Índice oficial e navegação
- `01-vision.md` — Visão, propósito e escopo estratégico do MKIS
- `02-architecture.md` — Arquitetura hexagonal, bounded contexts e ADRs
- `03-domain-model.md` — Entidades, agregados, value objects, invariantes e eventos
- `04-database.md` — PostgreSQL, pgvector, índices, particionamento, retenção e governança
- `05-pipelines.md` — Estados do pipeline, jobs, quarantine, approval, rollback, DLQ
- `06-ai-services.md` — Vendor strategy, orçamento, limites, anti-alucinação, fallback, auditoria
- `07-search.md` — Busca híbrida, RRF, thresholds canônicos, cache, observabilidade
- `08-knowledge-graph.md` — Grafo científico, predicados, consistência, performance, observabilidade
- `09-versioning.md` — Semântica, workflow, diff, rollback e relação com busca/grafo
- `10-audit.md` — Auditoria, retenção, incidentes, masking, purge e compliance
- `11-api-contracts.md` — REST, DTOs, códigos HTTP, OpenAPI, cache, endpoints
- `12-events.md` — Schema, canais, publishers/consumers, garantias, observabilidade
- `13-errors.md` — Taxonomia de erros, formato canônico, mapeamentos por contexto
- `14-security.md` — RBAC, ABAC, rate limit, LGPD, classificação regulatória
- `15-observability.md` — Logs, métricas, tracing, SLOs/SLIs, alertas, dashboards, runbooks
- `16-jobs.md` — Classes de jobs, filas, retry, backoff, circuit breaker, batch, backpressure
- `17-integrations.md` — Adaptadores externos, vendor strategy, resiliência e compliance
- `18-roadmap.md` — Fases, marcos, critérios de aceitação, rollout e reversão
- `19-quality.md` — Testes, gates, métricas textuais, amostragem e runbooks
- `20-appendix.md` — Glossário, abreviações, referências e convenções
- `21-architecture-validation-and-risk-register.md` — Validação arquitetural inicial e registro de riscos
- `22-production-readiness.md` — Status NÃO PRONTO até remediação final
- `ARB-architecture-review-report.md` — Parecer crítico formal do Architecture Review Board
- `ARB-architecture-remainder-plan.md` — Plano executado de remediação por bloqueador
- `executive-summary.md` — Resumo executivo pré-remoção de bloqueadores
- `29-canonical-references.md` — Nomenclaturas, enums, eventos, estados, erros, thresholds e políticas normalizadas