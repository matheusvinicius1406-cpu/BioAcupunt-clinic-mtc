# MKIS — Validação de fluxos

Fluxos com estados válidos, transições, eventos gerados, persistência, auditoria, rollback e recuperação.

---

## 1. Fluxo: Ingestão completa

Estados: queued -> downloading -> validation_failed/scan_failed -> ocr_queued -> ocr_running -> ocr_failed -> parsing_queued -> parsing_running -> parsing_failed -> chunking_queued -> chunking_running -> embedding_queued -> embedding_running -> indexing_queued -> enrichment_running -> completed/failed/manually_blocked.
Eventos: ArtifactUploaded, IngestionJobStatusChanged, EmbeddingGenerated.
Persistência: knowledge_artifacts, knowledge_node_versions, audit_trail.
Auditoria: obrigatória em cada transição.
Rollback: reprocessar job do estado falha; não descarta artefato validado.
Recuperação: reprocessamento idempotente por job_id e chunk hash. Transições inválidas proibidas pelos limites de estado; falhas -> DLQ com ação sugerida.

## 2. Fluxo: Aprovação científica

Estados: draft -> pending_review -> approved/rejected/deprecated.
Eventos: KnowledgeNodeCreated, KnowledgeNodeApproved.
Políticas: PromotionPolicy, EvidencePolicy.
Persistência: knowledge_nodes, knowledge_node_versions.
Auditoria: antes/depois do approve.
Rollback: versão anterior permanece aprovada; nova versão pode ser proposta. Não rejeita aprovada publicada.

## 3. Fluxo: Busca híbrida

Estados: query input -> normalized -> lexical+vector search -> fuse -> rerank -> filter/order -> paginate -> response.
Eventos: Search Performed (telemetria opcional se consentido).
Rollback: não aplicável; leituras são idempotentes.
Recuperação: fallback lexical quando re-ranking indisponível.

## 4. Fluxo: Manutenção e reindexação

Estados: scheduled -> preparing -> index building -> swap/sync -> completed/failed.
Backpressure: limita paralelismo por partição e tamanho de batch.
Rollback: manter índice antigo até validação de amostra. Não remover antes do swap assinado.
