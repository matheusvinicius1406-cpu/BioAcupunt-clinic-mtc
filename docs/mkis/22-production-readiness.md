# MKIS — Prontidão para Produção
## Este documento foi substituído pelo relatório oficial do ARB
## Consulte: `ARB-architecture-review-report.md`

**Data:** 2026-07-06  
**Status:** NÃO PRONTO — Revisão significativa requerida antes da implementação  
**Maturidade estimada:** 5.2 / 10

---

> Use o ARB-architecture-review-report.md como documento oficial de prontidão. Este arquivo permanece como referência para compatibilidade, mas não reflete a auditoria rigorosa realizada.

## 1. Pendências (do ARB)
- [ ] Selecionar vendors/stack para LLM, antivírus, OCR, filas
- [ ] Fechar enums canônicos: knowledge_type, status, evidence_level, bias_risk
- [ ] Estratégia de migração pgvector: dual-write, reindex paralelo, shadow index
- [ ] Estratégia de particionamento de knowledge_nodes
- [ ] Estados de pipeline: quarantined, needs_human_review, awaiting_approval
- [ ] Deep delete LGPD em cascata (vectors, KG, audit, cache)
- [ ] Workflow de hard-purge aprovado por DPO/advogado
- [ ] Escrever ADRs para 7 decisões faltantes
- [ ] Exemplos request/response por endpoint
- [ ] Métricas por stage de pipeline
- [ ] Runbooks com roles acionáveis
- [ ] Definição de ownership por contexto

## 2. Riscos Críticos Bloqueantes
| # | Risco | Status |
|---|-------|--------|
| 1 | Downtime em troca de embedding model | BLOQUEADO |
| 2 | Lock contention em audit_trail | BLOQUEADO |
| 3 | Documento malicioso escapa sandbox | BLOQUEADO |
| 4 | Custo LLM/embedding explode | BLOQUEADO |
| 5 | Deep delete incompleto (LGPD) | BLOQUEADO |
| 6 | ANVISA classifica MKIS como dispositivo médico | BLOQUEADO |
| 7 | Index vetorial degrada sem rotina | BLOQUEADO |

## 3. Critérios de Aceite Mensuráveis (revisados pelo ARB)
- [ ] Todos docs com status >= Validado; docs novos de auditoria Especificado+
- [ ] Nenhuma inconsistência de nomenclatura canônica em contratos públicos
- [ ] Toda tabela mutável com tenant_id + RLS aplicado
- [ ] Toda mutação gerando evento + audit_trail
- [ ] API com contrato OpenAPI versionado e testes de schema aprovando
- [ ] SLOs com valores numéricos, janela e erro-budget definido
- [ ] Execução de AO menos um teste E2E ponta a ponta
- [ ] Resource limits por tenant configurados e testados
- [ ] Deep delete validado em ambiente de teste com LGPD check
- [ ] Reindexação paralela testada com 1M+ vetores simulados

## 4. Decisões Abertas
- Modelo de embedding definitivo e política de troca/versionamento
- Tamanho de chunk e overlap padrão por tipo
- Modelo de re-ranking (interno x externo x open)
- Número de partições e frequência de vacuum/analyze
- TTL do cache de busca e invalidação precisa
- Temperatura máxima para extração científica (fixar 0)
- Estratégia de feature flags (LaunchDarkly, Flagsmith, custom)

## 5. Dependências Externas (revisadas pelo ARB)
- Fornecedor LLM/embedding com SLA, custo e residência — OBRIGATÓRIO antes de Sprint 1
- Fornecedor antivírus/quarentena com isolation forte — OBRIGATÓRIO
- Fornecedor de filas (Redis Streams ou broker) — OBRIGATÓRIO
- Fornecedor de OCR (Tesseract self-hosted ou API paga)
- Aprovação legal ANVISA/CFM pela assessoria jurídica — OBRIGATÓRIA

## 6. Ordem Recomendada de Implementação (revisada pelo ARB)
1. Sprint 0: Definições executáveis (2 semanas)
2. Sprint 1: Consistência arquitetural (3 semanas)
3. Sprint 2: Segurança e compliance (3 semanas)
4. Fase 2 core: Domínio + contratos + índice (8-12 semanas)
5. Fase 3: Busca híbrida e pipeline de ingestão manual (10-14 semanas)
6. Fase 4: Workers, IA, grafo (8-12 semanas)
7. Fase 5: Observabilidade, escala, go-live (6-8 semanas)

## 7. Checklist de Prontidão (revisado pelo ARB)
| Item | Pronto | Responsável |
|------|--------|-------------|
| Documentação validada pelo ARB | NÃO | Arquitetura |
| ADRs aprovados (11+ docs) | NÃO | Arquitetura |
| Vendor LLM/embedding contratado | NÃO | Engenharia + Finanças |
| Antivírus/quarentena definido | NÃO | Segurança |
| Modelo de migração pgvector | NÃO | DBA |
| Particionamento de knowledge_nodes | NÃO | DBA |
| Nomenclatura canônica em contratos | PARCIAL | Backend |
| Enum fechado para campos-chave | NÃO | Backend |
| Deep delete workflow aprovado | NÃO | Compliance |
| Feature flags e rollout planejados | NÃO | DevOps |
| ORçamento de LLM/OCR aprovado | NÃO | Finanças |
| Classificação ANVISA/CFM definida | NÃO | Jurídico |
