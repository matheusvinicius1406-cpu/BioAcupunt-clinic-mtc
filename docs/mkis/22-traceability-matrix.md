# MKIS — Matriz de Rastreabilidade

Mapeamento completo: requisito/entidade/endpoint/evento/tabela/política -> documentos onde é coberto.

---

## 1. Conceitos -> cobertura documental

- **KnowledgeNode**: 01-vision.md, 02-architecture.md, 03-domain-model.md, 04-database.md, 05-pipelines.md, 08-knowledge-graph.md, 09-versioning.md, 10-audit.md, 11-api-contracts.md, 20-appendix.md, 21-architecture-validation-and-risk-register.md
- **KnowledgeGraph**: 01-vision.md, 02-architecture.md, 03-domain-model.md, 05-pipelines.md, 06-ai-services.md, 08-knowledge-graph.md, 18-roadmap.md, 20-appendix.md, 21-architecture-validation-and-risk-register.md
- **KnowledgeArtifact**: 03-domain-model.md, 04-database.md
- **Hybrid Search**: 00-index.md, 01-vision.md, 02-architecture.md, 04-database.md, 05-pipelines.md, 07-search.md, 11-api-contracts.md, 15-observability.md, 18-roadmap.md, 20-appendix.md, 21-architecture-validation-and-risk-register.md
- **Audit Trail**: 00-index.md, 01-vision.md, 02-architecture.md, 03-domain-model.md, 04-database.md, 05-pipelines.md, 06-ai-services.md, 08-knowledge-graph.md, 09-versioning.md, 10-audit.md, 14-security.md, 18-roadmap.md, 19-quality.md, 21-architecture-validation-and-risk-register.md
- **RBAC/ABAC**: 00-index.md, 01-vision.md, 02-architecture.md, 11-api-contracts.md, 14-security.md, 18-roadmap.md, 19-quality.md, 20-appendix.md
- **Embeddings**: 00-index.md, 01-vision.md, 02-architecture.md, 03-domain-model.md, 04-database.md, 05-pipelines.md, 06-ai-services.md, 07-search.md, 10-audit.md, 12-events.md, 13-errors.md, 15-observability.md, 16-jobs.md, 18-roadmap.md, 20-appendix.md, 21-architecture-validation-and-risk-register.md
- **OCR/Parser/Chunk**: 00-index.md, 01-vision.md, 02-architecture.md, 03-domain-model.md, 04-database.md, 05-pipelines.md, 13-errors.md, 14-security.md, 15-observability.md, 16-jobs.md, 18-roadmap.md, 19-quality.md, 20-appendix.md
- **Versionamento**: 00-index.md, 01-vision.md, 02-architecture.md, 03-domain-model.md, 04-database.md, 06-ai-services.md, 08-knowledge-graph.md, 09-versioning.md, 10-audit.md, 11-api-contracts.md, 12-events.md, 19-quality.md, 20-appendix.md, 21-architecture-validation-and-risk-register.md
- **Observabilidade**: 00-index.md, 01-vision.md, 02-architecture.md, 10-audit.md, 15-observability.md, 18-roadmap.md, 20-appendix.md, 21-architecture-validation-and-risk-register.md
- **Jobs/Filas**: 00-index.md, 01-vision.md, 02-architecture.md, 05-pipelines.md, 06-ai-services.md, 09-versioning.md, 14-security.md, 15-observability.md, 16-jobs.md, 17-integrations.md, 18-roadmap.md, 19-quality.md, 20-appendix.md, 21-architecture-validation-and-risk-register.md
- **LGPD**: 00-index.md, 01-vision.md, 02-architecture.md, 10-audit.md, 14-security.md, 16-jobs.md, 18-roadmap.md, 21-architecture-validation-and-risk-register.md
- **Segurança**: 01-vision.md, 02-architecture.md, 05-pipelines.md, 06-ai-services.md, 14-security.md, 15-observability.md, 19-quality.md, 21-architecture-validation-and-risk-register.md
- **Cache**: 00-index.md, 01-vision.md, 02-architecture.md, 07-search.md, 11-api-contracts.md, 15-observability.md, 17-integrations.md, 18-roadmap.md, 21-architecture-validation-and-risk-register.md
- **Integrações externas**: 03-domain-model.md, 05-pipelines.md, 17-integrations.md, 20-appendix.md

## 2. Entidades de domínio -> banco / API / eventos / pipeline

- **KnowledgeNode**: docs=['01-vision.md', '02-architecture.md', '03-domain-model.md', '04-database.md', '05-pipelines.md', '08-knowledge-graph.md', '10-audit.md', '11-api-contracts.md', '20-appendix.md', '21-architecture-validation-and-risk-register.md']
- **KnowledgeArtifact**: docs=['03-domain-model.md']
- **KnowledgeGraphEdge**: docs=['03-domain-model.md']
- **AuditTrailEntry**: docs=['03-domain-model.md']

## 3. Endpoints -> documentos

- `POST /api/v1/knowledge/nodes`: 11-api-contracts.md
- `GET /api/v1/knowledge/nodes/{id}`: 11-api-contracts.md
- `GET /api/v1/knowledge/nodes`: 11-api-contracts.md
- `GET /api/v1/knowledge/search`: 11-api-contracts.md
- `POST /api/v1/knowledge/nodes/{id}/reviews/approve`: 11-api-contracts.md
- `POST /api/v1/knowledge/artifacts/upload`: 11-api-contracts.md
- `GET /api/v1/knowledge/graph/edges`: 11-api-contracts.md
- `GET /api/v1/knowledge/evidence/{node_id}`: 11-api-contracts.md

## 4. Eventos -> documentos

- `knowledge.node.created.v1`: 12-events.md
- `knowledge.node.approved.v1`: 12-events.md
- `knowledge.node.deprecated.v1`: 12-events.md
- `knowledge.artifact.uploaded.v1`: 12-events.md
- `ingestion.job.status_changed.v1`: 12-events.md
- `embedding.generated.v1`: 12-events.md
- `graph.edge.created.v1`: 12-events.md
- `evidence.score.updated.v1`: 12-events.md
- `review.requested.v1`: 12-events.md

## 5. Tabelas -> documentos

- `knowledge_nodes`: 04-database.md
- `knowledge_node_versions`: 04-database.md, 09-versioning.md
- `knowledge_artifacts`: 04-database.md
- `knowledge_graph_edges`: 04-database.md
- `audit_trail`: 04-database.md, 10-audit.md

## 6. Políticas -> documentos

- `PromotionPolicy`: 03-domain-model.md
- `DeprecationPolicy`: 03-domain-model.md
- `EvidencePolicy`: 03-domain-model.md
- `PrivacyPolicy`: 03-domain-model.md