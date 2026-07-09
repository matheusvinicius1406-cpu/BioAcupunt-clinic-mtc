# MKIS — Dados e Banco
## PostgreSQL, pgvector, estratégia de dados, migrações e governança

---

## 1. Diretrizes e não-negociáveis

- `tenant_id` é obrigatório em toda tabela mutável e todos os índices relevantes devem isolá-lo.
- Todo campo sensível deve ser classificado: **PUBLIC**, **INTERNAL**, **RESTRICTED**, **PII**.
- Toda representação de sigilo médico deve ser mascarada para perfis não autorizados.
- `deleted_at` marca exclusão lógica; tabelas de auditoria complementam a veracidade histórica.
- Toda tabela com mais de 10M de linhas cresce com particionamento planejado e acesso via chave de partição.

---

## 2. Esquema relacional organizado por domínio

### 2.1 knowledge_nodes

Cabeça atual e auditável da entidade KnowledgeNode.

| Coluna | Tipo | Restrição |
|-------|------|--------|
| id | UUIDv7 | PK |
| version | text | NOT NULL |
| tenant_id | UUID | NOT NULL, default partition key |
| title | text | NOT NULL |
| summary | text | NOT NULL |
| content | text | NOT NULL |
| knowledge_type | text | NOT NULL |
| category | text | NOT NULL |
| subcategory | text | indexado |
| specialty | text | NOT NULL |
| icd_10 | text[] | indexado |
| icd_11 | text[] | indexado |
| ciap_2 | text[] | indexado |
| tags | text[] | indexado |
| authors | jsonb |  |
| doi | text | UNIQUE parcial com status approved |
| pmid | text | UNIQUE parcial |
| pmcid | text |  |
| source | text |  |
| source_url | text |  |
| language | text | ISO-639-1, obrigatório |
| published_at | date |  |
| imported_at | timestamptz |  |
| last_reviewed_at | timestamptz |  |
| created_by | UUID |  |
| reviewed_by | UUID |  |
| approved_by | UUID |  |
| approved_at | timestamptz |  |
| version | text | semver X.Y.Z |
| status | text |  |
| scientific_score | numeric |  |
| ai_score | numeric |  |
| reliability_score | numeric |  |
| references | jsonb |  |
| clinical_evidence | text |  |
| evidence_level | text | valores canônicos fechados |
| bias_risk | text | valores canônicos fechados |
| conflicts | jsonb |  |
| checksum | text | SHA-256, único por versão |
| embedding_version | text | versão do modelo utilizada, default `v1` |
| embedding | vector(1536) |  |
| embedding_v2 | vector | opcional; apenas durante migração dual-write |
| metadata | jsonb |  |
| created_at | timestamptz |  |
| updated_at | timestamptz |  |
| deleted_at | timestamptz |  |

Regras importantes:
- `checksum` sempre representando conteúdo canônico normalizado.
- índice único composto por `doi` + `tenant_id` + `status = approved`.
- `embedding` só pode ser NULL em drafts controlados.

---

### 2.2 knowledge_node_versions

Histórico completo, imutável.

| Coluna | Tipo | Restrição |
|-------|------|---------|
| id | UUIDv7 | PK |
| node_id | UUID | FK para knowledge_nodes |
| version | text |  |
| snapshot | jsonb | conteúdo completo desta versão |
| content_hash | text |  |
| created_at | timestamptz |  |
| created_by | UUID |  |
| change_reason | text |  |
| previous_version | UUID | FK auto-relativa |

Regras:
- Nunca atualizar nem excluir linhas.
- Mantém encadeamento de versão para rollback ou reconstrução.

---

### 2.3 knowledge_artifacts

Arquivos físicos/digitais em staging.

| Coluna | Tipo | Restrição |
|-------|------|---------|
| id | UUIDv7 | PK |
| tenant_id | UUID |  |
| filename | text |  |
| mime_type | text |  |
| size_bytes | bigint |  |
| storage_key | text | único por tenant |
| source_url | text |  |
| source_package | text |  |
| sha256 | text |  |
| ingestion_job_id | UUID |  |
| ocr_required | boolean |  |
| parsed_text | text |  |
| validation_status | text |  |
| security_scan_status | text |  |
| virus_scan_result | jsonb |  |
| uploader_id | UUID |  |
| blocked_reason | text |  |
| created_at | timestamptz |  |
| deleted_at | timestamptz |  |

Velocidade e segurança:
- `mime_type` validado contra lista-controlada.
- rejeitar arquivos acima do limite configurado.

---

### 2.4 knowledge_graph_edges

Relações direcionadas do grafo.

| Coluna | Tipo | Restrição |
|-------|------|---------|
| id | UUIDv7 | PK |
| tenant_id | UUID |  |
| subject_id | UUID |  |
| predicate | text |  |
| object_id | UUID |  |
| weight | numeric |  |
| evidence_refs | jsonb |  |
| source_version_id | UUID |  |
| status | text |  |
| created_at | timestamptz |  |
| created_by | UUID |  |

Regras:
- proibir autoloops sem sinalização explícita de revisão; documentar exceções em auditoria.

---

### 2.5 audit_trail

Nunca atualizar, excluir ou atualizar em massa.

| Coluna | Tipo | Restrição |
|-------|------|---------|
| id | UUIDv7 | PK |
| tenant_id | UUID |  |
| actor_id | UUID |  |
| action | text |  |
| resource_type | text |  |
| resource_id | UUID |  |
| request_id | UUID |  |
| ip_address | text |  |
| user_agent | text |  |
| method | text |  |
| path | text |  |
| status_code | int |  |
| payload_hash | text |  |
| outcome | text |  |
| occurred_at | timestamptz |  |
| previous_hash | text | encadeamento de integridade |
| row_hash | text |  |
| metadata | jsonb |  |

Regras:
- `previous_hash` encadeia todos os eventos para provar integridade histórica.

---

## 3. Índices e particionamento

- B-tree compostos para `tenant_id` + `created_at`/`status` em tabelas grandes.
- GIN em `tags`, `references`, `icd_10`, `ciap_2`, `authors`.
- HNSW em `embedding` e IVFFlat como fallback em volumes extremos.
- Particionamento por mês/ano em `audit_trail` e `knowledge_node_versions`.

---

## 4. pgvector e busca híbrida

- `vector(1536)` para modelos padrão de embeddings clínicos.
- Manter `inner_product` e `cosine` disponíveis com mecanismo de escolha por dominio.
- Combinar BM25 e vetor via CTEs performáticas sem perdas arquiteturais.

---

## 5. RLS e multitenancy

- Habilitar RLS por padrão em tabelas sensíveis.
- Políticas por `tenant_id`.
- Auditoria global sempre registra tenant efetivo e subject do acesso.
