# MKIS — Especificação Executável para Implementação

**Date:** 2026-07-22
**Status:** ✅ PRONTO PARA IMPLEMENTAÇÃO — 6/6 blockers resolvidos
**ARB Target:** Pontuação 8/10 (pronto para implementação, não produção plena)
**Location:** `mkis-backend/` (backend dedicado, FastAPI)

---

## Sumário Executivo

O **MKIS (Medical Knowledge Intelligence System)** é o motor de conhecimento científico do BioAcupunt. Ele ingere, estrutura, busca e governa conteúdo clínico de MTC e medicina ocidental.

**Stack:** Self-hosted 100% open-source — BGE-M3 (embedding) + Qwen 2.5 7B (LLM) + PostgreSQL/pgvector

**6 blockers resolvidos:**

| # | Blocker | Documento de Resolução |
|---|--------|----------------------|
| 1 | Stack: BGE-M3 + Qwen 2.5 7B | `docs/mkis-blocker-1-vendor-decision.md` |
| 2 | Enums canônicos (16) | `docs/mkis-blocker-2-enums-canonical.md` |
| 3 | pgvector migration (dual-write) | `docs/mkis-blocker-3-pgvector-migration.md` |
| 4 | Partitioning (range mensal) | `docs/mkis-blocker-4-partitioning.md` |
| 5 | Pipeline states (2 state machines) | `docs/mkis-blocker-5-pipeline-states.md` |
| 6 | Deep delete LGPD (cascade) | `docs/mkis-blocker-6-deep-delete-lgpd.md` |

---

## Índice

1. [Stack & Infraestrutura](#1-stack--infraestrutura)
2. [Banco de Dados — Schema Completo](#2-banco-de-dados--schema-completo)
3. [Enums Canônicos](#3-enums-canônicos)
4. [Pipeline — Duas Máquinas de Estado](#4-pipeline--duas-máquinas-de-estado)
5. [Deep Delete LGPD](#5-deep-delete-lgpd)
6. [API — Endpoints Principais](#6-api--endpoints-principais)
7. [Eventos do Domínio](#7-eventos-do-domínio)
8. [Roadmap de Implementação](#8-roadmap-de-implementação)

---

## 1. Stack & Infraestrutura

### 1.1 Componentes

| Componente | Escolha | Versão | Licença | RAM |
|-----------|---------|--------|---------|-----|
| Embedding | **BGE-M3** (BAAI) | `BAAI/bge-m3` | Apache 2.0 | ~4 GB |
| LLM Pipeline | **Qwen 2.5 7B Instruct** | Q4_K_M quantized | Apache 2.0 | ~6 GB |
| Re-ranking | **BGE-reranker-v2-m3** | Q8 quantized | Apache 2.0 | ~3 GB |
| Vector DB | **pgvector** on PostgreSQL 16 | v0.8+ | PostgreSQL license | — |
| LLM Runtime | **llama.cpp** (CPU) via Ollama | latest | MIT | — |
| Embedding Runtime | **sentence-transformers** (CPU, ONNX) | latest | Apache 2.0 | — |

### 1.2 Infraestrutura Mínima

| Recurso | Mínimo | Recomendado |
|---------|--------|-------------|
| CPU | 4 cores | 8+ cores |
| RAM | 16 GB | 32 GB |
| Storage | 50 GB SSD | 200 GB+ SSD |
| GPU | Não necessária | Opcional (10x LLM speed) |
| OS | Ubuntu 22.04+ | Ubuntu 24.04 |
| Custo estimado | ~R$ 250/mês (Hetzner AX102) | — |

### 1.3 Arquitetura de Integração

```
┌──────────┐    ┌──────────────┐    ┌──────────────────┐
│ Document  │───▶│   BGE-M3     │───▶│    pgvector      │
│ uploaded  │    │  (embedding)  │    │  (vector search) │
└──────────┘    └──────────────┘    └────────┬─────────┘
                                              │
                 ┌────────────────────────────▼────────────┐
                 │      Hybrid Search (BM25 + vector)       │
                 │  → RRF fusion → BGE-reranker → top-10    │
                 └────────────────────────────┬────────────┘
                                              │
                 ┌────────────────────────────▼────────────┐
                 │       Qwen 2.5 7B (LLM Pipeline)        │
                 │  Sumarização, scoring, extração, RAG    │
                 └─────────────────────────────────────────┘
```

---

## 2. Banco de Dados — Schema Completo

### 2.1 Extensions

```sql
CREATE EXTENSION IF NOT EXISTS vector;     -- pgvector
CREATE EXTENSION IF NOT EXISTS pgcrypto;   -- gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS pg_partman; -- partitioning (future)
```

### 2.2 `knowledge_nodes` — Tabela Principal

```sql
CREATE TABLE knowledge_nodes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    title TEXT NOT NULL,
    summary TEXT NOT NULL,
    content TEXT NOT NULL,

    -- === Enums canônicos (TEXT + CHECK) ===
    knowledge_type TEXT NOT NULL CHECK (knowledge_type IN (
        'artigo', 'revisao', 'guideline', 'capitulo', 'livro',
        'tese', 'caso_clinico', 'ensaio_clinico', 'protocolo',
        'nota', 'relatorio', 'educacional'
    )),
    status TEXT NOT NULL DEFAULT 'rascunho' CHECK (status IN (
        'rascunho', 'em_revisao', 'aguardando_aprovacao',
        'aprovado', 'rejeitado', 'descontinuado', 'substituido'
    )),
    evidence_level TEXT CHECK (evidence_level IN (
        'cebm_1a', 'cebm_1b', 'cebm_2a', 'cebm_2b',
        'cebm_3a', 'cebm_3b', 'cebm_4', 'cebm_5',
        'grade_alta', 'grade_moderada', 'grade_baixa', 'grade_muito_baixa'
    )),
    bias_risk TEXT NOT NULL DEFAULT 'nao_avaliado' CHECK (bias_risk IN (
        'nao_avaliado', 'baixo', 'moderado', 'alto'
    )),
    clinical_evidence TEXT CHECK (clinical_evidence IN (
        'muito_alta', 'alta', 'moderada', 'baixa', 'insuficiente'
    )),
    category TEXT NOT NULL CHECK (category IN (
        'mtc', 'medicina_ocidental', 'saude_publica',
        'farmacologia', 'fisiologia', 'psicologia',
        'nutricao', 'biotecnologia', 'educacao'
    )),
    source TEXT NOT NULL CHECK (source IN (
        'pubmed', 'europe_pmc', 'semantic_scholar', 'crossref',
        'openalex', 'scielo', 'bvs', 'who_iris', 'paho_iris',
        'doaj', 'clinical_trials', 'wikimedia', 'manual', 'pacote'
    )),
    specialty TEXT NOT NULL DEFAULT 'geral' CHECK (specialty IN (
        'acupuntura', 'auriculoterapia', 'fitoterapia', 'moxabustao',
        'ventosaterapia', 'tui_na', 'eletroacupuntura',
        'craniopuntura', 'geral'
    )),
    language TEXT NOT NULL DEFAULT 'pt' CHECK (language ~ '^[a-z]{2}$'),
    data_classification TEXT NOT NULL DEFAULT 'restrito' CHECK (data_classification IN (
        'publico', 'interno', 'restrito', 'pii'
    )),

    -- === Embedding (BGE-M3, 1024d) ===
    embedding_version TEXT NOT NULL DEFAULT 'bge-m3-v1',
    embedding vector(1024),
    embedding_v2 vector(1024),       -- dual-write migration

    -- === Scores ===
    scientific_score NUMERIC CHECK (scientific_score >= 0 AND scientific_score <= 1),
    ai_score NUMERIC CHECK (ai_score >= 0 AND ai_score <= 1),
    reliability_score NUMERIC CHECK (reliability_score >= 0 AND reliability_score <= 1),

    -- === Metadados de publicação ===
    doi TEXT,
    pmid TEXT,
    pmcid TEXT,
    source_url TEXT,
    published_at DATE,
    imported_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_reviewed_at TIMESTAMPTZ,

    -- === Governança ===
    checksum TEXT NOT NULL,
    created_by UUID NOT NULL,
    reviewed_by UUID,
    approved_by UUID,
    approved_at TIMESTAMPTZ,
    version TEXT NOT NULL DEFAULT '0.1.0',
    superseded_by UUID,

    -- === JSON flexíveis ===
    metadata JSONB DEFAULT '{}',
    authors JSONB DEFAULT '[]',
    conflicts JSONB DEFAULT '[]',

    -- === Timestamps ===
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,

    -- === Constraints ===
    CONSTRAINT unique_active_doi UNIQUE (tenant_id, doi) WHERE status = 'aprovado' AND doi IS NOT NULL,
    CONSTRAINT unique_active_pmid UNIQUE (tenant_id, pmid) WHERE status = 'aprovado' AND pmid IS NOT NULL,
    CONSTRAINT checksum_unique UNIQUE (checksum, tenant_id, version),
    CONSTRAINT evidence_completeness CHECK (
        (status = 'aprovado')::int +
        (evidence_level IS NOT NULL)::int +
        (approved_by IS NOT NULL)::int +
        (approved_at IS NOT NULL)::int +
        (embedding IS NOT NULL)::int
        NOT IN (1,2,3,4)
    )
);

-- Indexes
CREATE INDEX idx_nodes_tenant_status ON knowledge_nodes (tenant_id, status);
CREATE INDEX idx_nodes_tenant_created ON knowledge_nodes (tenant_id, created_at DESC);
CREATE INDEX idx_nodes_checksum ON knowledge_nodes (checksum, tenant_id);
CREATE INDEX idx_nodes_fulltext ON knowledge_nodes USING gin(to_tsvector('portuguese', title || ' ' || summary));

-- HNSW index for vector search
CREATE INDEX CONCURRENTLY idx_nodes_embedding
    ON knowledge_nodes USING hnsw (embedding vector_cosine_ops)
    WHERE status IN ('aprovado', 'descontinuado');
```

### 2.3 `knowledge_node_versions`

```sql
CREATE TABLE knowledge_node_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    node_id UUID NOT NULL REFERENCES knowledge_nodes(id),
    version TEXT NOT NULL,
    snapshot JSONB NOT NULL,
    content_hash TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID NOT NULL,
    change_reason TEXT,
    previous_version UUID REFERENCES knowledge_node_versions(id)
);

CREATE INDEX idx_node_versions_node ON knowledge_node_versions (node_id, version);
```

### 2.4 `knowledge_artifacts`

```sql
CREATE TABLE knowledge_artifacts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    filename TEXT NOT NULL,
    mime_type TEXT NOT NULL,
    size_bytes BIGINT NOT NULL,
    storage_key TEXT NOT NULL,
    source_url TEXT,
    source_package TEXT,
    sha256 TEXT NOT NULL,
    ingestion_job_id UUID,
    ocr_required BOOLEAN NOT NULL DEFAULT false,
    parsed_text TEXT,
    validation_status TEXT NOT NULL DEFAULT 'pendente' CHECK (validation_status IN (
        'pendente', 'valido', 'invalido', 'rejeitado'
    )),
    security_scan_status TEXT NOT NULL DEFAULT 'nao_escanado' CHECK (security_scan_status IN (
        'nao_escanado', 'limpo', 'infectado', 'quarentena', 'falhou'
    )),
    virus_scan_result JSONB,
    uploader_id UUID,
    blocked_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_artifacts_tenant ON knowledge_artifacts (tenant_id);
CREATE UNIQUE INDEX idx_artifacts_storage ON knowledge_artifacts (tenant_id, storage_key);
```

### 2.5 `knowledge_graph_edges`

```sql
CREATE TABLE knowledge_graph_edges (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    subject_id UUID NOT NULL,
    predicate TEXT NOT NULL CHECK (predicate IN (
        'trata', 'indica', 'contraindica', 'referencia',
        'suporta', 'contradiz', 'parte_de', 'tem_parte',
        'relacionado_a', 'tem_alvo'
    )),
    object_id UUID NOT NULL,
    weight NUMERIC CHECK (weight >= 0 AND weight <= 1),
    evidence_refs JSONB DEFAULT '[]',
    source_version_id UUID,
    status TEXT NOT NULL DEFAULT 'ativa' CHECK (status IN (
        'ativa', 'substituida', 'descontinuada', 'em_quarentena'
    )),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID NOT NULL
);

CREATE INDEX idx_edges_subject ON knowledge_graph_edges (subject_id, predicate);
CREATE INDEX idx_edges_object ON knowledge_graph_edges (object_id, predicate);
CREATE INDEX idx_edges_tenant ON knowledge_graph_edges (tenant_id);
```

### 2.6 `ingestion_jobs`

```sql
CREATE TABLE ingestion_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    artifact_id UUID REFERENCES knowledge_artifacts(id),
    node_id UUID REFERENCES knowledge_nodes(id),

    status TEXT NOT NULL DEFAULT 'na_fila' CHECK (status IN (
        'na_fila', 'baixando', 'validacao_falhou',
        'escanando', 'scan_falhou', 'em_quarentena',
        'ocr_na_fila', 'ocr_rodando', 'ocr_falhou',
        'parse_na_fila', 'parse_rodando', 'parse_falhou',
        'chunk_na_fila', 'chunk_rodando', 'chunk_falhou',
        'embedding_na_fila', 'embedding_rodando', 'embedding_falhou',
        'indexacao_na_fila', 'indexacao_rodando', 'indexacao_falhou',
        'criando_no', 'criacao_falhou',
        'revisao_necessaria',
        'concluido', 'falhou', 'bloqueado_manualmente', 'cancelado'
    )),

    attempt_id UUID NOT NULL DEFAULT gen_random_uuid(),
    attempt_count INT NOT NULL DEFAULT 1,
    max_attempts INT NOT NULL DEFAULT 3,
    current_stage TEXT,
    error_code TEXT,
    error_message TEXT,
    quarantine_reason TEXT,
    review_notes TEXT,
    source_url TEXT,
    priority INT NOT NULL DEFAULT 0,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_jobs_tenant_status ON ingestion_jobs (tenant_id, status);
CREATE INDEX idx_jobs_quarantine ON ingestion_jobs (created_at) WHERE status = 'em_quarentena';
```

### 2.7 `audit_trail`

```sql
CREATE TABLE audit_trail (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    actor_id UUID NOT NULL,
    action TEXT NOT NULL,
    resource_type TEXT NOT NULL,
    resource_id UUID,
    request_id UUID NOT NULL,
    ip_address TEXT,
    user_agent TEXT,
    method TEXT,
    path TEXT,
    status_code INT,
    payload_hash TEXT,
    outcome TEXT,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    metadata JSONB DEFAULT '{}'
);

CREATE INDEX idx_audit_tenant ON audit_trail (tenant_id, occurred_at DESC);
CREATE INDEX idx_audit_resource ON audit_trail (resource_type, resource_id);
```

### 2.8 `purge_certificates`

```sql
CREATE TABLE purge_certificates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    target_type TEXT NOT NULL CHECK (target_type IN ('knowledge_node', 'knowledge_artifact', 'user_data', 'tenant_data')),
    target_id UUID NOT NULL,
    cascade_scope TEXT NOT NULL CHECK (cascade_scope IN ('full', 'metadata_only', 'storage_only')),
    target_details JSONB DEFAULT '{}',
    requested_by UUID NOT NULL,
    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ,
    checkpoint TEXT NOT NULL DEFAULT 'pending' CHECK (checkpoint IN (
        'pending', 'nodes_anonymized', 'versions_anonymized', 'artifacts_deleted',
        'edges_anonymized', 'jobs_anonymized', 'audit_anonymized',
        'cache_invalidated', 'storage_purged', 'completed', 'failed'
    )),
    steps_log JSONB DEFAULT '[]',
    certificate_hash TEXT NOT NULL,
    legal_hold_verified_at TIMESTAMPTZ,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_certificates_tenant ON purge_certificates (tenant_id, created_at DESC);
```

### 2.9 `tenants`

```sql
CREATE TABLE tenants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slug TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    region TEXT NOT NULL DEFAULT 'BR',
    evidence_min_score NUMERIC DEFAULT 0.8,
    features JSONB DEFAULT '{}',
    retention_policy JSONB DEFAULT '{}',
    legal_hold BOOLEAN NOT NULL DEFAULT false,
    legal_hold_reason TEXT,
    legal_hold_activated_at TIMESTAMPTZ,
    legal_hold_activated_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

---

## 3. Enums Canônicos

### 3.1 Tabela Resumo

| Enum | Coluna | Valores | Total |
|------|--------|---------|-------|
| `knowledge_type` | `knowledge_nodes.knowledge_type` | artigo, revisao, guideline, capitulo, livro, tese, caso_clinico, ensaio_clinico, protocolo, nota, relatorio, educacional | 12 |
| `status` (node) | `knowledge_nodes.status` | rascunho, em_revisao, aguardando_aprovacao, aprovado, rejeitado, descontinuado, substituido | 7 |
| `evidence_level` | `knowledge_nodes.evidence_level` | cebm_1a..5 + grade_alta..muito_baixa | 12 |
| `bias_risk` | `knowledge_nodes.bias_risk` | nao_avaliado, baixo, moderado, alto | 4 |
| `clinical_evidence` | `knowledge_nodes.clinical_evidence` | muito_alta, alta, moderada, baixa, insuficiente | 5 |
| `category` | `knowledge_nodes.category` | mtc, medicina_ocidental, saude_publica, farmacologia, fisiologia, psicologia, nutricao, biotecnologia, educacao | 9 |
| `source` | `knowledge_nodes.source` | pubmed, europe_pmc, semantic_scholar, crossref, openalex, scielo, bvs, who_iris, paho_iris, doaj, clinical_trials, wikimedia, manual, pacote | 14 |
| `specialty` | `knowledge_nodes.specialty` | acupuntura, auriculoterapia, fitoterapia, moxabustao, ventosaterapia, tui_na, eletroacupuntura, craniopuntura, geral | 9 |
| `data_classification` | `knowledge_nodes.data_classification` | publico, interno, restrito, pii | 4 |
| `predicate` | `knowledge_graph_edges.predicate` | trata, indica, contraindica, referencia, suporta, contradiz, parte_de, tem_parte, relacionado_a, tem_alvo | 10 |
| `validation_status` | `knowledge_artifacts.validation_status` | pendente, valido, invalido, rejeitado | 4 |
| `security_scan_status` | `knowledge_artifacts.security_scan_status` | nao_escanado, limpo, infectado, quarentena, falhou | 5 |
| `edge_status` | `knowledge_graph_edges.status` | ativa, substituida, descontinuada, em_quarentena | 4 |
| `language` | `knowledge_nodes.language` | Regex `^[a-z]{2}$` (ISO 639-1) | ∞ |

### 3.2 Regra de Implementação

- **TODOS os enums são `TEXT` + `CHECK` constraint** — não PostgreSQL `ENUM` types
- O CHECK é defesa em profundidade; a validação principal é na aplicação
- Para adicionar valores: `ALTER TABLE ... DROP CONSTRAINT ... ADD CONSTRAINT ...` (zero downtime)

---

## 4. Pipeline — Duas Máquinas de Estado

O MKIS tem **duas máquinas de estado separadas** que se conectam no boundary:

```
IngestionJob (processamento do documento)
    na_fila → baixando → escanando → [ocr|parse] → chunk → embedding → indexacao → criando_no → concluido
        ↓                                       ↓
   em_quarentena                          revisao_necessaria
        ↓                                       ↓
   (security_reviewer)                    (human reviewer)

KnowledgeNode (ciclo de vida do conteúdo)
    rascunho → em_revisao → aguardando_aprovacao → aprovado → descontinuado/substituido
        ↑                             ↓
        └─────── rejeitado ──────────┘
```

### 4.1 Regras de Transição (Resumo)

**IngestionJob:**
- Cada stage tem `{stage}_na_fila` → `{stage}_rodando` → sucesso (próximo stage) / falha (`{stage}_falhou`)
- Máx. 3 retries por stage; após 3 falhas consecutivas → `falhou` (terminal)
- Qualquer estado → `em_quarentena` (malware, policy, data_steward)
- `revisao_necessaria` → pode reprocessar, cancelar, ou elevar à quarentena
- `em_quarentena` retenção 90 dias; depois auto-purge

**KnowledgeNode:**
- `rascunho` → `em_revisao` (clinical_validator submete)
- `em_revisao` → `aguardando_aprovacao` (revisor recomenda) ou `rejeitado`
- `aguardando_aprovacao` → `aprovado` (senior clinical_validator) ou `rejeitado`
- `rejeitado` → `rascunho` (pode ser corrigido e reenviado)
- `aprovado` → `descontinuado` ou `substituido`
- Timeout gera alerta, **nunca** auto-aprova

### 4.2 Actor Permissions

| Actor | Ações |
|-------|-------|
| system (worker) | Todas transições automáticas de stage |
| clinical_validator | rascunho→em_revisao, em_revisao→aguardando_aprovacao/rejeitado, rejeitado→rascunho |
| clinical_validator_senior | aguardando_aprovacao→aprovado, aprovado→descontinuado/substituido |
| security_reviewer | em_quarentena→escanando/revisao_necessaria/bloqueado_manualmente |
| data_steward | Desbloqueio de jobs, legal hold, purge |

---

## 5. Deep Delete LGPD

### 5.1 Escopo

Apenas metadados de pessoa são removidos (conteúdo científico é preservado).

| Tabela | Ação |
|--------|------|
| `knowledge_nodes` | created_by, reviewed_by, approved_by → sentinela `0000...0de1` |
| `knowledge_node_versions` | created_by → sentinela; authors no snapshot → '[anonimizado]' |
| `knowledge_artifacts` | Soft delete + **apagar arquivo físico do storage** |
| `knowledge_graph_edges` | created_by → sentinela |
| `ingestion_jobs` | review_notes, error_message → regex redact |
| `audit_trail` | actor_id → sentinela; ip, user_agent → NULL |
| Cache | Invalidação por chave de nó via evento |

### 5.2 Fluxo

```
Data steward solicita → Verifica legal_hold → Cria purge_certificate
→ Executa cascade (8 passos com checkpoint) → Emite HardDeleteCompleted
→ Registra certificado imutável
```

### 5.3 Legal Hold

- `tenants.legal_hold = true` → deep delete bloqueado
- Apenas `data_steward` pode ativar/desativar
- Toda mudança registrada em audit_trail

---

## 6. API — Endpoints Principais

### 6.1 Knowledge Nodes

| Method | Path | Descrição |
|--------|------|-----------|
| `GET` | `/api/v1/nodes` | Listar nós (filtros: tenant, status, category, specialty, q, page) |
| `GET` | `/api/v1/nodes/{id}` | Detalhe do nó |
| `POST` | `/api/v1/nodes` | Criar nó manualmente |
| `PATCH` | `/api/v1/nodes/{id}` | Atualizar metadados |
| `PATCH` | `/api/v1/nodes/{id}/status` | Transição de estado (rascunho→em_revisao, etc.) |
| `DELETE` | `/api/v1/nodes/{id}` | Soft delete |

### 6.2 Search

| Method | Path | Descrição |
|--------|------|-----------|
| `GET` | `/api/v1/search` | Busca híbrida (BM25 + vetorial + reranking) |
| `GET` | `/api/v1/search/rag` | RAG: busca + síntese via LLM |

### 6.3 Pipeline

| Method | Path | Descrição |
|--------|------|-----------|
| `POST` | `/api/v1/pipeline/upload` | Upload de documento para ingestão |
| `GET` | `/api/v1/pipeline/jobs` | Listar ingestion_jobs |
| `GET` | `/api/v1/pipeline/jobs/{id}` | Status do job |
| `POST` | `/api/v1/pipeline/jobs/{id}/retry` | Reprocessar job falho |

### 6.4 Graph

| Method | Path | Descrição |
|--------|------|-----------|
| `GET` | `/api/v1/graph/nodes/{id}/edges` | Aristas de um nó |
| `POST` | `/api/v1/graph/edges` | Criar aresta manualmente |

### 6.5 Purge & LGPD

| Method | Path | Descrição |
|--------|------|-----------|
| `POST` | `/api/v1/purge` | Solicitar deep delete |
| `GET` | `/api/v1/purge/{cert_id}` | Status do purge |
| `PUT` | `/api/v1/tenants/{id}/legal-hold` | Ativar/desativar legal hold |

---

## 7. Eventos do Domínio

Emitidos via outbox (transacional com o banco):

| Evento | Trigger | Payload Chave |
|--------|---------|---------------|
| `KnowledgeNodeCreated` | Pipeline concluído | node_id, tenant_id, knowledge_type |
| `KnowledgeNodeApproved` | Senior validator aprova | node_id, approved_by, approved_at |
| `KnowledgeNodeRejected` | Revisor rejeita | node_id, rejection_reason |
| `KnowledgeNodeDeprecated` | Editor depreca | node_id, deprecation_reason |
| `IngestionJobCompleted` | Pipeline conclui | job_id, node_id, duration_ms |
| `QuarantineCreated` | Malware detectado | job_id, reason, sha256 |
| `QuarantineResolved` | Security reviewer decide | job_id, resolution |
| `HardDeleteRequested` | Data steward solicita | certificate_id, target_id |
| `HardDeleteCompleted` | Purge concluído | certificate_id, steps_log |
| `CacheInvalidationRequired` | DB steps completos | node_id, tenant_id, patterns |
| `LegalHoldActivated` | Tenant bloqueado | tenant_id, reason |

---

## 8. Roadmap de Implementação

### Fase 1 — Fundação (Semanas 1-3)

| Tarefa | Dependência |
|--------|-------------|
| Setup PostgreSQL + pgvector + pg_partman | Nenhuma |
| Criar schema completo (7 tabelas + enums + CHECK) | Acima |
| Implementar endpoints básicos CRUD de nodes | Schema pronto |
| Setup BGE-M3 + Qwen 7B (docker-compose ou shell) | Servidor Linux |
| Implementar ingestão manual (upload + parse + embedding) | Modelos rodando |

### Fase 2 — Pipeline (Semanas 4-6)

| Tarefa | Dependência |
|--------|-------------|
| Implementar state machine do IngestionJob | Schema ingestion_jobs |
| Reconciliação de OCR (Tesseract) + parsing | Pipeline base |
| Chunking + embedding batch | BGE-M3 rodando |
| Indexação HNSW automática por partition | Schema + pipeline |
| Quarantine workflow (scan + revisão) | Pipeline |

### Fase 3 — Busca & Graph (Semanas 7-9)

| Tarefa | Dependência |
|--------|-------------|
| Busca híbrida (BM25 + vector + RRF) | Embeddings populados |
| Re-ranking via BGE-reranker-v2-m3 | Busca base |
| RAG endpoint (Qwen 7B synthesizes answer) | Busca + LLM |
| Knowledge Graph (CRUD de edges + queries) | Nodes populados |
| Endpoints /search e /search/rag | Busca + Graph |

### Fase 4 — LGPD & Compliance (Semanas 10-11)

| Tarefa | Dependência |
|--------|-------------|
| Deep delete cascade (função PostgreSQL + job) | Todas tabelas |
| Purge certificate (certificado imutável) | Deep delete |
| Legal hold enforcement | Deep delete |
| Cache invalidation por evento | Sistema de eventos |
| Verification queries + reports | Deep delete |

### Fase 5 — Governança (Semanas 12-13)

| Tarefa | Dependência |
|--------|-------------|
| RBAC + RLS por tenant | Schema completo |
| Métricas de pipeline (latência, erros, DLQ) | Pipeline rodando |
| Timeouts + alertas (em_revisão, aguardando_aprovacao, quarentena) | Pipeline |
| Documentação de runbooks | Tudo acima |

---

## Referências

| Documento | Conteúdo |
|-----------|----------|
| `docs/mkis-blocker-1-vendor-decision.md` | Stack decision: BGE-M3 + Qwen 7B |
| `docs/mkis-blocker-2-enums-canonical.md` | 16 canonical enums with CHECK |
| `docs/mkis-blocker-3-pgvector-migration.md` | Dual-write + CONCURRENTLY + HNSW tuning |
| `docs/mkis-blocker-4-partitioning.md` | Range partition monthly + pg_partman |
| `docs/mkis-blocker-5-pipeline-states.md` | Two state machines (IngestionJob + KnowledgeNode) |
| `docs/mkis-blocker-6-deep-delete-lgpd.md` | Deep delete cascade + purge certificate + legal hold |
| `docs/mkis/ARB-architecture-review-report.md` | Original ARB review with 6 blockers |
| `docs/mkis/ARB-architecture-remainder-plan.md` | Remediation plan per blocker |
| `database-spec.md` | Full database audit from initial analysis |

---

*End of document — MKIS specification v1.0 — Ready for implementation 🚀*
