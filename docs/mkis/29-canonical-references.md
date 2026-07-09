# MKIS — Referências Canônicas
## Nomenclaturas, enums, eventos, estados, códigos, thresholds e políticas normalizadas

---

## 1. Papéis

`admin`, `editor`, `revisor`, `clinician`, `auditor`, `service`, `security_reviewer`, `data_steward`, `clinical_validator`

---

## 2. Enums de domínio

### 2.1 knowledge_type
artigo, guideline, revisao, capitulo, tese, caso_clinico, nota

### 2.2 category
MTC, medicina_ocidental, saude_publica, epidemiologia, farmacologia, fisiologia, psicologia

### 2.3 specialty
acupuntura, auriculoterapia, fitoterapia, ventosas, moxabustao, quiropraxia

### 2.4 source
pubmed, europepmc, semantic_scholar, crossref, openalex, clinicaltrials, scielo, bvs, who, doaj, wikimedia

### 2.5 status
draft, pending_review, approved, rejected, deprecated, superseded

### 2.6 clinical_evidence
baixa, moderada, alta, muito_alta

### 2.7 evidence_level
O:CEBM_G1, O:CEBM_G2, O:CEBM_G3, O:CEBM_G4
GRADE_HIGH, GRADE_MODERATE, GRADE_LOW, GRADE_VERY_LOW

### 2.8 bias_risk
baixo, moderado, alto, nao_avaliado

---

## 3. Estado do Ingestion Job
queued, downloading, validation_failed, scan_failed, scan_quarantined,
ocr_queued, ocr_running, ocr_failed, ocr_needs_human_review,
parsing_queued, parsing_running, parsing_failed,
chunking_queued, chunking_running, chunking_failed,
embedding_queued, embedding_running, embedding_failed,
indexing_queued, indexing_running, indexing_failed,
awaiting_approval, approved_to_index,
completed, failed, manually_blocked, cancelled

---

## 4. Estado do Knowledge Graph Edge
active, superseded, deprecated, quarantined

---

## 5. Eventos canônicos
knowledge.node.created.v1
knowledge.node.versioned.v1
knowledge.node.approved.v1
knowledge.node.deprecated.v1
knowledge.node.rejected.v1
knowledge.artifact.uploaded.v1
knowledge.artifact.quarantined.v1
ingestion.job.status_changed.v1
ingestion.job.failed.v1
ingestion.job.completed.v1
ingestion.job.cancelled.v1
embedding.generated.v1
embedding.migration.completed.v1
graph.edge.created.v1
evidence.score.updated.v1
hard_delete.completed.v1

---

## 6. Códigos de erro canônicos
KnowledgeNotFound
InvalidDocument
DuplicateKnowledge
ImportFailed
OCRFailed
EmbeddingFailed
ValidationFailed
ScientificSourceUnavailable
VectorIndexError
GraphConflict
QuarantineRequired
ApprovalRequired
HardDeleteFailed
RateLimited
Unauthorized
Forbidden
Conflict
Timeout
ServiceUnavailable

---

## 7. Thresholds e timeouts canônicos
JWT Access Token: 15 minutos
Refresh Token: 30 dias
Chunk Size: 1024 tokens
Chunk Overlap: 128 tokens
Embedding Dimensions: 1536
Top K busca: 20
Similarity mínima vetorial: 0.75
BM25 mínimo: 6
Top candidatos para reranking: 150
Boost mínimo evidence: 1.15
Penalty bias alto: 0.85
Cache TTL busca: 300 segundos
Max query length: 2048 caracteres
Retry: 3 tentativas com backoff exponencial + jitter
Circuit Breaker: 50% de erro por 30 segundos
OCR Timeout: 120 segundos
LLM Timeout padrão: 45 segundos
Database Timeout: 10 segundos
Search Timeout: 5 segundos
Health Check: 30 segundos
Backup: diário
Snapshot: 6 horas
Log Retention: 365 dias
Audit Retention: 7 anos quentes; archive a frio permitido

Obs: valores dependem de fatores externos (contratação de fornecedores, budget aprovado, parecer jurídico) devem ser registrados explicitamente:
- premissa adotada;
- critérios de decisão;
- impacto caso a premissa mude.