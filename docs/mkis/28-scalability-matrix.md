# MKIS — Matriz de Escalabilidade

Análise por escala: 10k/100k/1M/10M/100M documentos.

---

## 1. Premissas de referência
- Modelo padrão embedding 1536 dims, float32 (~6KB por vetor + overhead).
- Uma ingestão típica gera ~1-50 chunks por documento; média: 8 chunks -> 8 vetores.
- Custo estimado de storage PostgreSQL com pgvector: ~3x o tamanho puro em disco devido a índices e metadados (ordem de grandeza).
- Fator de compressão/redundância: auditoria e versionamento crescem ~2-4x em relação aos dados principais em horizontes longos.

## 2. Cenários

| Escala | Vetores estimados | Storage (ordem) | Gargalos principais | Estratégias de contenção |
|---|---|---|---|---|
| 10k docs | 100k | 20-40 GB | Sem gargalo relevante | HNSW padrão; cache Redis normal; réplica leitura |
| 100k docs | 1M | 200-400 GB | Latência de busca vetorial em HNSW sem tuning | HNSW tunado (m=32, ef_construction=200); indexação por tenant; cache por query; read replica |
| 1M docs | 10M | 2-4 TB | Partições e reindexação | Particionamento por tenant/período; IVFFlat segmentado; cache distribuído e filas priorizadas; sharding se multi-tenant massivo |
| 10M docs | 100M | 20-40 TB | Escala de workers/CPU/IOPS | Sharding por tenant/id; separação de índices em nós dedicados; cache por query em Redis Cluster; reindexação contínua e incremental |
| 100M docs | 1B+ | 200+ TB | Infra dedicada, cold storage | Sharding + índices dedicados; embeddings em object store com particionamento por namespace; cache multi-camada; fallback para BM25 quando necessário; limpeza/archive por política |

## 3. Comportamento do pgvector por escala

- HNSW tem latência aceitável até ~10M vetores com tuning; acima, prefira sharding e segmentação por domínio/tenant.
- IVFFlat cai melhor para recall não crítico em >50M vetores, exige treino periódico e precisa reindexar para mudança de distribuição.
- Combinação BM25 + vetorial consome CPU proporcional ao volume; após ~5M docs, isolar buscadores em nós separados.


## 4. Decisões de particionamento

- Particionar por created_at + tenant_id quando houver predominância de acesso recente.
- Manter índice vetorial “hot” separado de índice lexical quando o volume cruzar 5M docs.


## 5. Necessidade de sharding

- 1M docs: só se multi-tenant massivo (>500 tenants). Caso contrário, vertical scaling com réplicas.
- 10M docs: recomendado sharding por tenant para evitar hotspot.
- 100M docs: sharding obrigatório; separação de cluster e cold tiers.
