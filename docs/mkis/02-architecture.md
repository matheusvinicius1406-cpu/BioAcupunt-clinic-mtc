# MKIS — Arquitetura Geral e ADRs

## 1. Visão arquitetônica

O sistema usa arquitetura hexagonal limpa com contextos explícitos.  
 Cada bounded context tem sua camada:  
`API/HTTP -> Application -> Domain <- Infrastructure`

Routers são finos.  
Regras de negócio moram em services/domain.  
Repositories são interfaces no domínio; implementações ficam na infraestrutura.

Além disso, usamos "pipes and filters" para pipelines de ingestão/processamento.  
Fila/Queue garante desacoplamento, resiliência e escala.

```
App Android
   |
   v
API Gateway / Router v1
   |
   +--> API Handlers (thin)
   |
   +--> Application Layer
            |
            +--> Domain (Entities, Aggregates, Value Objects, Policies)
            |
            +--> Application Services
                    |
                    +--> Repository Interfaces
                            |
                            +--> Infrastructure
                                    |
                                    +--> PostgreSQL + pgvector
                                    +--> Redis
                                    +--> Workers / Streams
                                    +--> Embedding Service
                                    +--> OCR Parser
                                    +--> Vector Index
                                    +--> Event Bus
                                    +--> External Sources
```

---

## 2. Módulos e responsabilidades

- **Knowledge Context**: agregados de conhecimento, validações de negócio, relacionamentos core.
- **Search Context**: motor de busca híbrida, ranking, facetas, filtros, paginação cursor.
- **Ingestion Context**: pipelines de ingestão, OCR, parsing, chunking, normalização.
- **AI Context**: geração de embeddings, resumos, keywords, scores e enriquecimento.
- **Graph Context**: Knowledge Graph, relacionamentos, inferências, consistência, path queries.
- **Versioning Context**: versionamento semântico, policy set, rollback, diffing.
- **Audit Context**: trilhas, imutabilidade, logs estruturados, encadeamento probatório.
- **Events Context**: evento de domínio/integração, schema registry, idempotência.
- **Security Context**: RBAC, ABAC, rate limit, SSRF, XXE, path traversal, prompt injection protections.
- **Notifications Context**: webhooks, eventos assíncronos, notificações por conclusão/erro/deploy.
- **Observability Context**: métricas, tracing, alerting, SLOs e health checks.

---

## 3. ADRs

### ADR-001 — Backend dedicado e comunicação via HTTP público

**Decisão:** MKIS não será uma library Android. Haverá um backend dedicado com endpoints REST versionados.

**Razão:** Isolamento, evolução independente, pipeline especializada, compliance e performance. Android evoluirá por contrato.

**Consequência:** requer deploy, pilha própria e monitoramento separado.

---

### ADR-002 — PostgreSQL + pgvector como banco primário

**Decisão:** Usar PostgreSQL com pgvector para dados vetoriais e dados relacionais no mesmo sistema.

**Razão:** simplifica consistência transacional e obtenção de dados; reduz drift; permite atomicidade em operações de ingestão.

**Consequência:** índice vetorial deve ser particionado e com HNSW/IVFFlat com configuração explícita; monitorar latência/throughput.

---

### ADR-003 — Arquitetura de filas/workers com prioridade e DLQ

**Decisão:** Toda ingestão e processamento custoso roda em workers com filas.

**Razão:** API não pode bloquear; escala horizontal; separação de workload.

**Consequência:** Exigirá broker disponível, monitoramento de lag e DLQ operada.

---

### ADR-004 — Eventos como contrato de integração primário

**Decisão:** Mudanças relevantes geram eventos; consumidores reagem via handlers idempotentes.

**Razão:** desacoplamento, rastreabilidade, possibilidade de reprocessamento, observabilidade.

**Consequência:** schema registry e política de evolução obrigatórios.

---

### ADR-005 — Versionamento semântico e durabilidade do dado

**Decisão:** Nunca sobrescrever uma KnowledgeNode; emitir uma nova versão; diff e rollback são primitivos do sistema.

**Razão:** obrigações legais, LGPD, rastreabilidade e reprodutibilidade científica.

**Consequência:** crescimento de tabelas; precisa de rotação/archive documentada.

---

### ADR-006 — IAs como engines com fallback e guardrails

**Decisão:** IA não é uma função monolítica. Existem motores especializados: embedding, resumo, keywords, scoring, relacionamentos. Todos com fallback e circuit breaker.

**Razão:** resiliência, custo, latência, observabilidade e responsabilidade clínica.

**Consequência:** políticas de retry, muting, fallback e alertas.

---

### ADR-007 — Busca híbrida com re-ranking e cache contextual

**Decisão:** Busca combina lexical (BM25), vetorial (cosine/ip) e re-ranking com modelo pequeno especializado; cache isolado por query/filtro.

**Razão:** relevância clínica superior, desempenho, redução de custo de LLMs.

**Consequência:** necessidade de camada de pré-processamento de query e modelo de re-ranking treinado ou supervisionado.

---

## 4. Decomposição em contextos

Cada contexto:

- tem seu próprio pacote/namespace com `Domain`, `Application`, `Infrastructure`, `Api` (quando necessário);
- define aggregate roots, entity invariants, events e repository interfaces;
- não deve importar detalhes de infraestrutura de outros contextos diretamente;
- comunicação entre contextos acontece via eventos ou serviços de aplicação coordenados.

---

## 5. Decisões arquiteturais adicionais

- **Ids:** UUIDv7; quando precisar de ordenação temporal, usar embedded timestamp.
- **Transacionalidade:** preferir outbox pattern para eventos garantidos; nunca enviar evento antes do commit.
- **Particionamento:** KnowledgeNodes e Audit particionados por created_at; índices híbridos com HNSW em coluna vetorial.
- **Cache:** Redis por usuário, query e contexto; nunca cachear PII sem mascaramento consentido.
- **Feature flags:** isoladas por contexto e endpoint; permitem reversão rápida.
- **Multitenancy:** isolamento por policy set; schema/recurso isolado quando necessário; tenant em coluna + RLS.
