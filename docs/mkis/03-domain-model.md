# MKIS — Modelo de Domínio Científico
## Entities, Value Objects, Aggregates e Invariantes

---

## 1. Conceito central

O objeto principal do MKIS é a **KnowledgeNode**, uma unidade atômica de conhecimento científico estruturada.  
Ela é a base para busca, relacionamentos, IA e construção do grafo científico.

Além dela, existe o **KnowledgeArtifact**, que representa a instância física/digital de uma fonte (PDF, artigo, capítulo, guideline, vídeo transcrição, URL). KnowledgeNode é interpretação estruturada; KnowledgeArtifact é a origem material.

---

## 2. KnowledgeNode (Aggregate Root)

Representa conhecimento científico estruturado e enriquecido.

### Campos

| Campo | Tipo | Regras |
|-------|------|--------|
| `id` | UUIDv7 | obrigatório, imutável, público via API |
| `title` | text | obrigatório, indexado |
| `summary` | text | obrigatório |
| `content` | text | obrigatório, pode ser longo |
| `knowledge_type` | enum | artigo, guideline, revisão, capítulo, tese, caso-clínico, nota |
| `category` | enum | MTC, medicina_ocidental, saude_publica, epidemiologia, farmacologia, fisiologia, psicologia |
| `subcategory` | text | livre, indexado |
| `specialty` | text | acupuntura, auriculoterapia, fitoterapia, ventosas, moxabustao, quiropraxia |
| `icd_10` | text[] | opcional |
| `icd_11` | text[] | opcional |
| `ciap2` | text[] | opcional |
| `tags` | text[] | indexado |
| `authors` | json | nome, afiliacao, orcid |
| `doi` | text | opcional, unico quando presente |
| `pmid` | text | opcional |
| `pmcid` | text | opcional |
| `source` | enum | pubmed, europepmc, semantic_scholar, crossref, openalex, clinicaltrials, scielo, bvs, who, doaj, wikimedia |
| `source_url` | text | opcional |
| `language` | text | ISO-639-1, obrigatorio |
| `published_at` | date | obrigatorio quando aplicavel |
| `imported_at` | timestamptz | obrigatorio, gerado pelo sistema |
| `last_reviewed_at` | timestamptz | atualizado em cada revisao profunda |
| `created_by` | UUID | obrigatorio |
| `reviewed_by` | UUID | opcional |
| `approved_by` | UUID | opcional |
| `version` | semver X.Y.Z | obrigatorio, crescente |
| `status` | enum | draft, pending_review, approved, deprecated, rejected, superseded |
| `scientific_score` | numeric | obrigatorio apos scoring |
| `ai_score` | numeric | confianca da IA no enriquecimento |
| `reliability_score` | numeric | score de confiabilidade composta |
| `references` | json | apenas referencias citadas compostas (doi, pmid, label) |
| `clinical_evidence` | enum | baixa, moderada, alta, muito_alta |
| `evidence_level` | enum | O:CEBM_G1, O:CEBM_G2, O:CEBM_G3, O:CEBM_G4, GRADE_HIGH, GRADE_MODERATE, GRADE_LOW, GRADE_VERY_LOW |
| `bias_risk` | enum | baixo, moderado, alto, nao_avaliado |
| `conflicts` | json | registros de conflitos e resolucoes |
| `checksum` | text | SHA-256 do conteudo canonico, indexado para dedup |
| `embedding_version` | text | versao do modelo utilizado (`v1` default) |
| `embedding` | vector(1536) | obrigatorio apos ingest; NULL permitido apenas em draft |
| `embedding_v2` | vector | opcional; usado durante migracao dual-write |
| `metadata` | json | campos livres normalizados pelo parser |

### Invariantes

- `doi` + `tenant_id` devem ser únicos quando presente e em status `approved`.
- `pmid` + `tenant_id` devem ser únicos quando presente e em status `approved`.
- `checksum` é imutável; para nova versão gera-se novo `checksum` e novo registro de versionamento.
- `status == approved` exige `approved_by`, `approved_at`, `evidence_level` valida e `scientific_score` válido.
- `published_at` nao pode ser maior que `imported_at`.
- Quando `status != draft`, `embedding` deve estar presente.
- Relacionamentos clinicos sao exclusivos em `KnowledgeGraphEdge`; nao ha arrays `related_*` neste aggregate.

---

## 3. KnowledgeArtifact (Aggregate Root)

Representa o documento físico/digital antes do processamento e enriquecimento.

### Campos

- `id` UUIDv7
- `tenant_id` UUID, obrigatorio
- `filename` text
- `mime_type` text
- `size_bytes` bigint
- `storage_key` text, unico por tenant
- `source_url` text
- `source_package` text
- `sha256` text
- `ingestion_job_id` UUID
- `ocr_required` boolean
- `parsed_text` text (opcional)
- `validation_status` enum
- `security_scan_status` enum
- `virus_scan_result` json
- `uploader_id` UUID
- `blocked_reason` text

### Invariantes

- `sha256` não pode ser vazio; deve ser calculado após upload.
- `mime_type` deve estar em uma lista-controlada de tipos permitidos.
- `storage_key` deve ser único por tenant permitir resolução estável.

---

## 4. Tenant (Entity)

Unidade de isolamento operacional e comercial.

### Campos

- `id` UUIDv7
- `slug` text único
- `name` text
- `region` text
- `evidence_min_score` numeric, default 0.8
- `features` json contendo flags ativas
- `retention_policy` json com janelas por tipo de dado
- `legal_hold` boolean, default false
- `created_at` timestamptz
- `updated_at` timestamptz

### Regras

- `legal_hold = true` bloqueia purge mesmo para LGPD enquanto ativo.
- `Tenant` não comanda dados clínicos; comanda isolamento, retenção e features.

---

## 5. Value Objects

- **ScientificEvidenceLevel**: nível de evidência (Oxford/GRADE) com regras de transição documentadas.
- **ReliabilityRange**: [0,1] normalizado para scores.
- **ParsedDocument**: campo estruturado + raw + parsing metadata.
- **EmbeddingVector**: dimensão checada por `embedding_version`, normalizada, indexada.
- **Citation**: estilo formal, com DOI/PMID como referência validada.
- **AuditTrailEntry**: evento imutável usado em construção de integridade agregada.

---

## 6. Agregados e raízes

- **KnowledgeNode**: raiz para todo conhecimento estruturado.
- **KnowledgeArtifact**: raiz para o documento original.
- **IngestionJob**: raiz do fluxo de entrada; único owner de worker states.
- **KnowledgeGraphEdge**: relação direcionada entre entidades compatíveis (KnowledgeNode ou entidades clínicas).
- **Tenant**: raiz de isolamento organizacional; nao cria dominio clinico.

---

## 7. Políticas (Policies)

São objetos de domínio puro, sem infraestrutura:

- **PromotionPolicy**: decide se o node pode ser aprovado.
- **DeprecationPolicy**: define critérios de deprecação e substituição.
- **EvidencePolicy**: define como `evidence_level`, `bias_risk` e `scientific_score` devem ser preenchidos.
- **ClassificationPolicy**: define mapeamento autorizado para `knowledge_type`, `category`, `specialty`.
- **PrivacyPolicy**: define se elementos podem ser expostos a público restrito ou apenas auditores.

Políticas recebem contexto, entidades e retornam boolean ou justificativa textual para auditoria.

---

## 8. Eventos de domínio essenciais

- `KnowledgeNodeCreated`
- `KnowledgeNodeVersioned`
- `KnowledgeNodeApproved`
- `KnowledgeNodeDeprecated`
- `KnowledgeNodeRejected`
- `ArtifactUploaded`
- `IngestionJobQueued`
- `IngestionJobStatusChanged`
- `IngestionJobCompleted`
- `IngestionJobFailed`
- `EmbeddingGenerated`
- `EmbeddingMigrationCompleted`
- `GraphEdgeCreated`
- `EvidenceScoreUpdated`
- `QuarantineCreated`
- `QuarantineResolved`
- `HardDeleteRequested`
- `HardDeleteCompleted`

Todos eventos devem ter `stream_id`, `occurred_at`, `actor_id`, `tenant_id`, `payload_hash`, `schema_version` e `request_id`.
Nenhum evento obriga `previous_hash`. O encadeamento de integridade desejado é cumprido pelo outbox bloqueado e auditoria agregada.