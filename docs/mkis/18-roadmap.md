# MKIS — Roadmap
## Fases, marcos, critérios de aceitação, rollout, feature flags e reversão

---

## 1. Visão macro

Evoluir o MKIS de um “CRUD restrito” para uma plataforma de inteligência científica médica, sempre desacoplada do app Android.

---

## 2. Fase 1 — Aprovação arquitetural MVP

- Documentação final aprovada pelo ARB.
- Dependências externas contratadas ou com fallback operacional documentado.
- Decisões tecnicamente fechadas: banco, busca, embeddings, pipeline, IA, segurança, compliance.

Critérios:
- Classificação ARB = Aprovado para implementação da Fase 1 (MVP)
- Sem bloqueador técnico pendente
- Dependências externas isoladas em documento próprio

---

## 3. Fase 2 — Núcleo de Domínio e Segurança (8–12 semanas)

- Implementações contextuais core com PostgreSQL, RLS, pgvector, auth/ABAC, audit.
- Exceções canônicas, eventos mínimo, observabilidade básica.
- Primeiro dataset aceitável via ingest manual/staging sem filas pesadas.

Critérios:
- Testes de segurança e contrato passam baseline.
- Métricas básicas disponíveis.

---

## 4. Fase 3 — Motores e Busca Híbrida (10–14 semanas)

- Busca lexical/vetorial/híbrida com ranking básico + RRF.
- Filtros, facetas, cursor, cache.
- Primeira API de busca pública coesa.

Critérios:
- P95 dentro de SLO inicial em ambiente isolado.
- Relevance@10 avaliada por especialistas clínicos.

---

## 5. Fase 4 — IA, Graph e Importação Automática (8–12 semanas)

- OCR/parsing embeddings/pipeline assíncrono.
- Knowledge Graph primário.
- Auto-relacionamentos iniciais por entidades extraídas.

Critérios:
- Pipeline E2E com taxa de sucesso aceitável.
- Nós plausíveis aparecem no grafo com justificativa explícita.

---

## 6. Fase 5 — Governança, Observabilidade e Escala (6–8 semanas)

- Dashboards, SLOs, alertas, tracing, retenção legal.
- Políticas de approver, DLQ operada.
- Rollouts por tenant com feature flags.

Critérios:
- Redução de ruído de alertas, rollback automático sob falha de health check.
- Teste de carga cruzando p90, p99 e erro budget.

---

## 7. Rollout e reversão

- Cada mudança relevante usa feature flag por tenant/sigla.
- Piloto fechado com 1 tenant; graduação por capacidade de observabilidade.
- Rollback reversível em menos de 15 minutos.