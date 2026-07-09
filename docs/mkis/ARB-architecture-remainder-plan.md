# MKIS — Plano Consolidado de Remediação
## Fonte principal: `ARB-architecture-review-report.md`

**Data:** 2026-07-06  
**Owner:** Principal Architect  
**Objetivo:** Eliminar todos os bloqueadores do ARB até atingir implementação com risco baixo.  
**Regra:** simplicidade > funcionalidade. Sem overengineering.

---

## 1. Plano por bloqueador

### 1.1 Dimensão vetorial fixa e migração sem downtime
- **Problema:** `vector(1536)` fixo sem path documentado para troca de modelo.
- **Impacto técnico:** mudança de embedding model exige reescrita completa da tabela até 10M+ linhas.
- **Impacto operacional:** janela de indisponibilidade ou inconsistência de busca.
- **Impacto financeiro:** compute massivo em reindex + possível downtime.
- **Risco:** degradação da busca por horas; risco de corromper relações se vetores antigos forem usados.
- **Solução:** introduzir `embedding_version` no `KnowledgeNode`; manter coabitação temporária de `embedding` e `embedding_v2` durante migração; usar job paralelo com idempotência; depois swap atômico; nunca remover antiga coluna antes de validação.
- **Alternativas:** migration direta (descartada: lock e downtime); cache de resultados negativos (descartada: baixa eficácia em vetores).
- **Justificativa:** coabitação é a única operação segura para produção.
- **Documentos afetados:** `03-domain-model.md`, `04-database.md`, `05-pipelines.md`
- **Critério de sucesso:** migração documentada termina < 1h de janela planejada, sem lock relevante para escrita.

### 1.2 Reindexação paralela sem degradação
- **Problema:** HNSW degrada em inserções contínuas sem rotina de reindex.
- **Impacto operacional:** recall cai ao longo de semanas.
- **Solução:** índice shadow `embedding_idx_next` com HNSW em fase de build; validação por amostragem; swap via `ALTER INDEX ... RENAME`; feature flag para rollback.
- **Documentos afetados:** `04-database.md`, `10-audit.md`, `15-observability.md`
- **Critério de sucesso:** swap < 30s; P95 search sem aumento mensurável; rollback testado.

### 1.3 Estados do pipeline/completos
- **Problema:** estados `chunking_queued` sem parsed_text; ausência de `quarantined` e `awaiting_approval`.
- **Solução:** máquina de estados completa; curinga `pipeline_failed` para erros não mapeados.
- **Documentos afetados:** `05-pipelines.md`, `11-api-contracts.md`, `12-events.md`
- **Critério de sucesso:** todas transições possuem origem válida explícita; onboarding clínico é bloqueado até approval humana.

### 1.4 Quarantine workflow
- **Problema:** sem estado de quarentena, documentos maliciosos não têm destino forense.
- **Solução:** upload poisoned -> geração de storage key isolado -> scan falho -> `quarantined` -> apenas revisor segurança pode rejeitar/enviar para análise -> action `reprocess` ou `purge` com log imutável.
- **Documentos afetados:** `05-pipelines.md`, `14-security.md`
- **Critério de sucesso:** payload malicioso não modifica fila normal; não há deleção silenciosa; retenção 90 dias para análise.

### 1.5 Awaiting approval workflow
- **Solução:** `approved` é gate humano exclusivo; timeout opcional sem auto-reject; revisor tem papel `clinical_validator`; rejeição mantém draft; rejected pode reabrir com notas.
- **Documentos afetados:** `03-domain-model.md`, `11-api-contracts.md`, `14-security.md`
- **Critério de sucesso:** nenhuma publicação sem `approved_by` e `approved_at`; evento auditável.

### 1.6 Deep delete LGPD
- **Problema:** exclusão lógica remove só a linha visível; vetores, edges, cache e audit permanecem.
- **Solução:** `hard_delete_request` -> job assíncrono com remoção em cascata documentada (node, versions, edges, vectors por job_id, cache entries, audit entries resumidos retidos por política); confirmação + certificado de purge.
- **Documentos afetados:** `03-domain-model.md`, `04-database.md`, `10-audit.md`
- **Critério de sucesso:** query por identificadores antigos retorna zero; certificado gerado; log imutável.

### 1.7 Retenção de dados
- **Solução:** tiering explicito:
  - `audit_trail`: 7 anos quente, archive a frio opcional com retenção legal
  - `knowledge_node_versions`: retenção ativa por política científica, purge cronológico por tenant com flag legal_hold
  - `knowledge_graph_edges`: seguir ciclo do node origem
- **Documentos afetados:** `04-database.md`, `10-audit.md`
- **Critério de sucesso:** política única por tipo; sem apagar dependente sem node pai.

### 1.8 Audit trail e lock contention
- **Problema:** `previous_hash` serializa inserts.
- **Solução:** simplificação:
  - remover encadeamento linha-a-linha;
  - usar hash por bloco/request id;
  - integridade assegurada por `request_id` único + hash do evento + agrupamento externo para forward integrity;
  - manter `outbox` como registro canônico.
- **Documentos afetados:** `04-database.md`, `03-domain-model.md`, `12-events.md`
- **Critério de sucesso:** insert latency não degrada com 1k eventos/s por tenant.

### 1.9 Normalização dos related_* e integração com Knowledge Graph
- **Problema:** arrays em `KnowledgeNode` duplicam grafo e criam inconsistência.
- **Solução:** remover `related_nodes`, `related_treatments`, `related_acupuncture_points`, `related_syndromes`, `related_pathologies` de `KnowledgeNode`; relações são exclusivamente `KnowledgeGraphEdge`.
- **Documentos afetados:** `03-domain-model.md`, `04-database.md`, `08-knowledge-graph.md`, `11-api-contracts.md`
- **Critério de sucesso:** queries de relações usam apenas edge table; controllers retornam arestas filtradas.

### 1.10 Política de dimensionamento vetorial
- **Problema:** políticas dependem de modelo fixo 1536.
- **Solução:** `embedding_version` define dimensão por coluna dedicada; tolerância a vetor nulo durante seeding; cache desajeitado com chave `{tenant}:{version}`.
- **Documentos afetados:** `04-database.md`, `05-pipelines.md`, `28-scalability-matrix.md`
- **Critério de sucesso:** troca de modelo não quebra schema nem busca híbrida.

### 1.11 Orçamento e limites de custo para LLM e OCR
- **Problema:** sem limites, custo explode.
- **Solução:**
  - `tenant_cost_budget.daily_usd_cap`
  - LLM only para etapas que agregam valor científico; parser e chunking sem LLM.
  - OCR: Tesseract self-hosted como default; API paga excede orçamento.
  - job enfileira por custo quando budget estourar; modo degraded sem IA.
- **Documentos afetados:** `06-ai-services.md`, `05-pipelines.md`
- **Critério de sucesso:** custo por documento documentado; burst limitado por tenant configurável.

### 1.12 Controller de custo com thresholds numéricos
- **Problema:** thresholds genéricos.
- **Solução:** fixar em `07-search.md`:
  - cosine similarity mínimo 0.75
  - BM25 mínimo 6
  - reranking só para top-150
  - fusion por RRF
- **Documentos afetados:** `07-search.md`, `11-api-contracts.md`
- **Critério de sucesso:** relevance test set com precision@10 >= baseline médio; experimentos documentados.

### 1.13 Vendor strategy simplificada
- **Problema:** sem vendor definido nenhuma etapa fecha.
- **Solução:** estratégia simplificada por showdown:
  - LLM/Embedding: vendor primary + secondary com fallback automático.
  - OCR: Tesseract self-hosted primeiro; API como fallback caro para PDFs ruins.
  - Antivírus: ClamAV em sidecar isolado; sem opção comercial cara no inicio.
  - Fila: Redis Streams + consumer groups; simples, barato, operável em 1 node.
  - Object Storage: S3-compatível local; separação de bucket por tenant-like prefix; evitar multi-cloud no início.
- **Documentos afetados:** `05-pipelines.md`, `06-ai-services.md`, `16-jobs.md`
- **Critério de sucesso:** cada componente tem fallback + vendor default documentado; sem dependência única.

### 1.14 Classificação regulatória
- **Problema:** LGPD parcial, ANVISA/CFM sem posicionamento.
- **Solução:**
  - LGPD: workflow técnico de deep delete legal e consentimento por processamento.
  - CFM/ANVISA: classificação inicial como software de apoio à decisão com documented disclaimer; apenas classificação, não certificação.
  - circular documento legal padrão para revisor clínico.
- **Documentos afetados:** `10-audit.md`, `14-security.md`
- **Critério de sucesso:** parecer jurídico inicial coletado; riscos mitigados por disclaimer e acesso restrito.

### 1.15 Simplificações consideradas
- Remover `Relationship Engine` como motor separado; usar `KnowledgeGraphService` como serviço único.
- Remover `Recommendation` do escopo inicial; adicionar apenas se `Citation` sair primeiro.
- Eliminar `ValidationEngine` standalone; inserir validação como stages no Ingestion Pipeline.

## 2. Execução e progresso

Para cada item acima:
- Implementar o documento como solução canônica
- Marcar checklist no próprio item
- Nao expandir documentação por volume

## 3. Nova classificação ARB após remediação

Espera-se chegar em:

- Pronto para implementação (requer ainda ajustes pontuais)
- com maturidade em torno de 8/10

O requisito final é "Pronto para implementação", não produção plena.