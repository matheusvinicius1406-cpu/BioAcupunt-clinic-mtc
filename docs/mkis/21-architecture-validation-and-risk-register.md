# MKIS — Validação da Arquitetura e Registro de Riscos
## Auditoria técnica oficial da especificação

---

## 1. Objetivo

Este documento consolida a validação rigorosa da documentação do MKIS, identifica inconsistências, riscos, trade-offs e define critérios objetivos de aceite antes da implementação.

---

## 2. Resultado da validação documental

### 2.1 Estado dos documentos

| Nº | Documento | Estado | Observações |
|----|-----------|--------|-------------|
| 00 | 00-index.md | Validado | Índice navegável |
| 01 | 01-vision.md | Validado | Visão consolidada |
| 02 | 02-architecture.md | Validado | ADRs completos |
| 03 | 03-domain-model.md | Validado | Modelo detalhado |
| 04 | 04-database.md | Validado | Esquema e índices |
| 05 | 05-pipelines.md | Validado | Pipeline completo |
| 06 | 06-ai-services.md | Validado | Serviços de IA |
| 07 | 07-search.md | Validado | Busca híbrida |
| 08 | 08-knowledge-graph.md | Validado | Grafo |
| 09 | 09-versioning.md | Validado | Versionamento |
| 10 | 10-audit.md | Validado | Auditoria |
| 11 | 11-api-contracts.md | Validado | Contratos REST |
| 12 | 12-events.md | Validado | Eventos |
| 13 | 13-errors.md | Validado | Erros |
| 14 | 14-security.md | Validado | Segurança |
| 15 | 15-observability.md | Validado | Observabilidade |
| 16 | 16-jobs.md | Validado | Jobs |
| 17 | 17-integrations.md | Validado | Integrações |
| 18 | 18-roadmap.md | Validado | Roadmap |
| 19 | 19-quality.md | Validado | Qualidade |
| 20 | 20-appendix.md | Especificado | Glossário e referências |
| 21 | 21-architecture-validation-and-risk-register.md | Pronto para produção | Este documento |

---

## 3. Inconsistências encontradas e ações corretivas

### 3.1 Nomenclatura

Problema: Termos centrais aparecem com grafias alternativas em alguns documentos.

Exemplos observados:
- `KnowledgeNode` / `knowledge_node`
- `KnowledgeGraph` / `Knowledge Graph`
- `BM25` / `bm25`
- `pgvector` / `pg_vector`
- `HNSW` / `hnsw`

Ação corretiva:
- Adotar a forma canônica `KnowledgeNode` quando se refere ao aggregate/objeto no domínio e APIs.
- Adotar `knowledge_node` para nomes de tabelas/colunas no banco.
- Adotar `KnowledgeGraph` em documentos arquiteturais; usar `knowledge_graph` para schema/banco.
- Adotar `BM25`, `pgvector`, `HNSW` como termos técnicos canônicos.

Justificativa:
- Evita ambiguidade entre conceito e artefato técnico.
- Facilita buscas cruzadas e implementação.

---

## 4. Decisões arquiteturais tomadas

- Manter o MKIS como backend dedicado, desacoplado do Android.
- Usar PostgreSQL + pgvector como gate de verdade.
- Eventos como contrato preferencial entre contextos.
- Versionamento imutável de conhecimento e auditoria obrigatória.
- Busca híbrida como padrão; busca lexical é fallback obrigatório.
- Responsabilidade de tratamento de LGPD compartilhada entre domínio, API e operações.
- Não usar assets customizados ou dependências frágeis; preferir bibliotecas mantidas e padrões da indústria.

---

## 5. Trade-offs principais

- PostgreSQL + pgvector simplifica consistência, mas pode exigir particionamento e cuidado com HNSW/IVFFlat sob carga.
- Busca híbrida aumenta relevância, mas requer tuning de pesos e custo computacional.
- Versionamento imutável aumenta armazenamento, mas garante compliance e rastreabilidade.
- Workers desacoplados aumentam resiliência, mas exigem monitoramento de fila e DLQ.
- IA assistiva melhora qualidade, mas exige guardrails, revisão humana e custo operacional.
- Observabilidade total aumenta custo de infra, mas reduz tempo de resposta a incidentes.

---

## 6. Riscos técnicos

| Risco | Probabilidade | Impacto | Mitigação |
|-------|--------------|---------|-----------|
| Custo elevado de embeddings/LLM | Alta | Alto | Cache, batch, fallback, limites por tenant, modelos compactos quando possível |
| Latência de busca vetorial em alta cardinalidade | Média | Alto | HNSW tuning, particionamento, cache, warm-up de índice, DR trial |
| Reindexação prolongada sem downtime | Média | Alto | Reindex paralelo/online, filas, feature flags, staging prévia |
| Dependência de APIs científicas exteriores | Alta | Médio | Circuit breakers, fallback lexical, cache, fontes alternativas |
| Crescimento do banco vetorial sem governança | Alta | Alto | Limites, rotação, particionamento, políticas de retenção |
| Complexidade de Knowledge Graph | Média | Médio | MVP controlado, inferência explícita e auditada, políticas de relacionamento |
| Prompt injection em documentos importados | Média | Crítico | Sanitização, modelação com unsafe_content scan, allowlists, revisão humana |

---

## 7. Riscos legais

- Documentos importados sem revisão editorial podem gerar responsabilidade clínica indireta.
- Retenção longa sem políticas claras pode conflitar com direito ao esquecimento.
- Acesso irrestrito a dados sensíveis pode violar LGPD e CFM.

Mitigações:
- Aprovação humana obrigatória antes de publicação científica.
- Máscaras e consentimentos mapeados no domínio.
- Auditoria global e encadeamento probatório.

---

## 8. Riscos operacionais

- Fila crescente sem limite pode saturar workers.
- Keys e segredos mal rotacionados podem causar incidentes.
- Falta de canário pode mascarar regressões clínicas.

Mitigações:
- Backpressure por tenant, limites e autoscaling.
- Rotação automática e vaulting.
- Piloto fechado por tenant e métricas de qualidade textual.

---

## 9. Riscos financeiros

- Modelos grandes de LLM e embeddings podem elevar custo acima do esperado.
- Armazenamento e indexação híbrida crescem com volume de dados.

Mitigações:
- Política de uso e orçamento por tenant.
- Modelos menores quando suficiente, com fallback.
- Reindexação por demanda e limpeza de dados obsoletos.

---

## 10. Critérios objetivos de aceite

- Todos os documentos desta pasta revisados e aprovados por arquitetura e compliance.
- Nenhuma dependência direta entre MKIS e aplicativo Android em runtime.
- Contratos REST públicos documentados e versionados.
- Critérios SLO documentados com valores numéricos e janelas.
- Riscos com mitigação documentada e owners definidos.

---

## 11. Critérios para início da implementação
## 11. Critérios para início da implementação
- Especificação aprovada sem objeções arquiteturais pelo ARB.
- Sprint 0 concluído: vendors/stacks definidos, enums fechados, ADRs críticos escritos.
- Orçamento de infra, LLM/OCR e antivírus aprovado.
- Pipeline de CI/CD preparada para migrações, contratos e migrations rollback.
- Equipe de segurança e compliance engajada para revisão contínua e classificação ANVISA.
- Deep delete workflow validado com DPO/advogado.
- Piloto com 1 tenant fechado habilitado para feedback clínico.

## 12. Checklist de prontidão para produção
- [ ] Documentação validada pelo ARB e congelada.
- [ ] ADRs aprovados (mínimo 11 ADRs).
- [ ] Contratos assinados com provedores de IA, antivírus e fontes externas.
- [ ] Modelo de migração pgvector e reindexação online aprovado.
- [ ] Particionamento de knowledge_nodes documentado e testado.
- [ ] Política de retenção, backup, purge LGPD e auditoria documentada.
- [ ] Testes de contrato, segurança e caos aprovados em CI.
- [ ] SLOs, alertas, dashboards e error budgets desenhados.
- [ ] Feature flags e rollout planejados.
- [ ] Deep delete testado em staging com validação LGPD.
- [ ] Runbooks com roles acionáveis publicados e revisados.

## 13. Documento oficial de prontidão
Consulte `ARB-architecture-review-report.md` e `22-production-readiness.md` para detalhes.

- [ ] Feature flags e rollout planejados.
