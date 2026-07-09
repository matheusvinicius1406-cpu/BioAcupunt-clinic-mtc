# MKIS — Motor de Busca Científica
## Busca híbrida, ranking, facetas, filtros, paginação, cache e observabilidade

---

## 1. Objetivo

Procurar conhecimento científico com precisão clínica, combinando:

- Busca lexical (BM25)
- Busca vetorial (similaridade semântica)
- Re-ranking com modelo especializado
- Filtros estruturais por metadados científicos
- Paginação cursor eficiente
- Cache respeitando tenant e escopo clínico
- Fusão por Reciprocal Rank Fusion (RRF)

---

## 2. API de busca

A busca aceita multi-modalidade:

- `q`: texto livre
- `filters`: JSON com filtros por `knowledge_type`, `category`, `specialty`, `year`, `language`, `evidence_level`
- `codes`: array de `icd_10`, `icd_11`, `ciap_2`
- `ordering`: `relevance`, `date`, `evidence`, `reliability`
- `cursor`: string opcional para paginação
- `limit`: 1-50; default 20

---

## 3. Pipeline da busca

1. Normalização de input e canonicalização de filtros.
2. Validação de escopo por `tenant_id` via RBAC/ABAC.
3. Aplicação de filtros iniciais por metadados (`status = approved`, `embedding_version = v1` quando suportado).
4. Busca lexical (PostgreSQL `tsvector` com BM25).
5. Busca vetorial (pgvector HNSW com `embedding_version` selecionado).
6. Fusão RRF: lista única por documento respeitando posição nas duas listas.
7. Re-ranking: top-150 candidatos reponderados por modelo pequeno.
8. Regras clínicas: boost mínimo por `evidence_level` e `scientific_score`.
9. Paginação cursor.
10. Cache quando aplicável.

---

## 4. Thresholds numéricos canônicos

| Threshold | Valor | Motivo |
|-----------|-------|--------|
| Similarity mínima vetorial | 0.75 | reduz ruído semântico genérico |
| BM25 mínimo | 6 | remove matches triviais |
| Top candidatos para reranking | 150 | custo aceitável; recall suficiente |
| Fusão | RRF | robustez; sem calibração contínua |
| Boost mínimo evidence | 1.15 | prioriza fontes melhores |
| Penalty bias alto | 0.85 | evita documentos de evidência fraca no topo |
| TTL cache | 300s | evita stale em publicações |
| Max query length | 2048 chars | proteção de custo e DOS |

---

## 5. Fusão híbrida

- Estratégia oficial: **RRF**.
- Sem pesos calibrados por heurística.
- Re-ranking é opcional; se indisponível, busca retorna RRF puro.
- Se reranking falhar, timeout curto cai para RRF sem bloqueio.

---

## 6. Cache

- Redis particionado por tenant, filtros_hash, `embedding_version`.
- TTL configurado por ambiente; DEFAULT 300s.
- Invalidação por eventos: `KnowledgeNodeApproved`, `EvidenceScoreUpdated`, `GraphEdgeCreated`.
- Nunca cachear PII; dados sensíveis mascarados.
- Cache miss não bloqueia fluxo; busca assíncrona persiste resultado futuro.

---

## 7. Observabilidade

- Métricas obrigatórias: P95/P99 por tenant, cache hit/miss, reranking usage, reranking timeout count.
- Alertas:
  - relevância média cai abaixo do baseline por 1h
  - P95 > 400ms por 15min para approved docs
  - aumento anômalo de reranking não executado
- Logs sem PII; incluem `request_id`, `tenant_id`, `embedding_version`, filtros Hash, resultado count.
