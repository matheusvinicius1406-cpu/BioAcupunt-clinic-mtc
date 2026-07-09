# MKIS — Matriz de Dependências

Dependências entre documentos e grafo de efeito cascata.

---

## 1. Dependências por documento

- **00-index.md** -> 01-vision.md, 02-architecture.md, 03-domain-model.md, 04-database.md, 05-pipelines.md, 06-ai-services.md, 07-search.md, 08-knowledge-graph.md, 09-versioning.md, 10-audit.md, 11-api-contracts.md, 12-events.md, 13-errors.md, 14-security.md, 15-observability.md, 16-jobs.md, 17-integrations.md, 18-roadmap.md, 19-quality.md
- **01-vision.md** -> nenhuma explícita
- **02-architecture.md** -> nenhuma explícita
- **03-domain-model.md** -> nenhuma explícita
- **04-database.md** -> nenhuma explícita
- **05-pipelines.md** -> nenhuma explícita
- **06-ai-services.md** -> nenhuma explícita
- **07-search.md** -> nenhuma explícita
- **08-knowledge-graph.md** -> nenhuma explícita
- **09-versioning.md** -> nenhuma explícita
- **10-audit.md** -> nenhuma explícita
- **11-api-contracts.md** -> nenhuma explícita
- **12-events.md** -> nenhuma explícita
- **13-errors.md** -> nenhuma explícita
- **14-security.md** -> nenhuma explícita
- **15-observability.md** -> nenhuma explícita
- **16-jobs.md** -> nenhuma explícita
- **17-integrations.md** -> nenhuma explícita
- **18-roadmap.md** -> nenhuma explícita
- **19-quality.md** -> nenhuma explícita
- **20-appendix.md** -> nenhuma explícita
- **21-architecture-validation-and-risk-register.md** -> 00-index.md, 01-vision.md, 02-architecture.md, 03-domain-model.md, 04-database.md, 05-pipelines.md, 06-ai-services.md, 07-search.md, 08-knowledge-graph.md, 09-versioning.md, 10-audit.md, 11-api-contracts.md, 12-events.md, 13-errors.md, 14-security.md, 15-observability.md, 16-jobs.md, 17-integrations.md, 18-roadmap.md, 19-quality.md, 20-appendix.md
- **22-traceability-matrix.md** -> nenhuma explícita

## 2. Documentos que influenciam outros (efeito cascata)

- **00-index.md**: afeta 21-architecture-validation-and-risk-register.md
- **01-vision.md**: afeta 00-index.md, 21-architecture-validation-and-risk-register.md
- **02-architecture.md**: afeta 00-index.md, 21-architecture-validation-and-risk-register.md
- **03-domain-model.md**: afeta 00-index.md, 21-architecture-validation-and-risk-register.md
- **04-database.md**: afeta 00-index.md, 21-architecture-validation-and-risk-register.md
- **05-pipelines.md**: afeta 00-index.md, 21-architecture-validation-and-risk-register.md
- **06-ai-services.md**: afeta 00-index.md, 21-architecture-validation-and-risk-register.md
- **07-search.md**: afeta 00-index.md, 21-architecture-validation-and-risk-register.md
- **08-knowledge-graph.md**: afeta 00-index.md, 21-architecture-validation-and-risk-register.md
- **09-versioning.md**: afeta 00-index.md, 21-architecture-validation-and-risk-register.md
- **10-audit.md**: afeta 00-index.md, 21-architecture-validation-and-risk-register.md
- **11-api-contracts.md**: afeta 00-index.md, 21-architecture-validation-and-risk-register.md
- **12-events.md**: afeta 00-index.md, 21-architecture-validation-and-risk-register.md
- **13-errors.md**: afeta 00-index.md, 21-architecture-validation-and-risk-register.md
- **14-security.md**: afeta 00-index.md, 21-architecture-validation-and-risk-register.md
- **15-observability.md**: afeta 00-index.md, 21-architecture-validation-and-risk-register.md
- **16-jobs.md**: afeta 00-index.md, 21-architecture-validation-and-risk-register.md
- **17-integrations.md**: afeta 00-index.md, 21-architecture-validation-and-risk-register.md
- **18-roadmap.md**: afeta 00-index.md, 21-architecture-validation-and-risk-register.md
- **19-quality.md**: afeta 00-index.md, 21-architecture-validation-and-risk-register.md
- **20-appendix.md**: afeta 21-architecture-validation-and-risk-register.md

## 3. Top influenciadores (alteração causaria maior efeito cascata)

- 01-vision.md: 2 dependentes -> 00-index.md, 21-architecture-validation-and-risk-register.md
- 02-architecture.md: 2 dependentes -> 00-index.md, 21-architecture-validation-and-risk-register.md
- 03-domain-model.md: 2 dependentes -> 00-index.md, 21-architecture-validation-and-risk-register.md
- 04-database.md: 2 dependentes -> 00-index.md, 21-architecture-validation-and-risk-register.md
- 05-pipelines.md: 2 dependentes -> 00-index.md, 21-architecture-validation-and-risk-register.md
- 06-ai-services.md: 2 dependentes -> 00-index.md, 21-architecture-validation-and-risk-register.md
- 07-search.md: 2 dependentes -> 00-index.md, 21-architecture-validation-and-risk-register.md
- 08-knowledge-graph.md: 2 dependentes -> 00-index.md, 21-architecture-validation-and-risk-register.md