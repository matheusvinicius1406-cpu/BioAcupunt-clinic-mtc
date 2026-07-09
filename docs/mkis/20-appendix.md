# MKIS — Apêndice
## Glossário, abreviações, referências, convenções e revisão histórica

---

## 1. Glossário

- **KnowledgeNode**: unidade atômica de conhecimento científico estruturado.
- **KnowledgeArtifact**: instância física/digital da fonte antes do processamento.
- **KnowledgeGraphEdge**: relação direcionada e versionada entre entidades.
- **IngestionJob**: agregado de job com estados canônicos e event log.
- **embedding_version**: versão do modelo/bundle de embedding aplicado ao vetor.
- **purge_certificate**: certificado imutável que prova execução de hard delete LGPD.
- **clinical_validator**: papel humano com poder exclusivo de aprovar/rejeitar publicação.
- **data_steward**: papel humano com poder de aprovar purge e remover legal_hold.

---

## 2. Abreviações

- RRF: Reciprocal Rank Fusion
- RLS: Row Level Security
- AV: Antivírus
- SLO/SLI: Service Level Objective/Indicator
- DLQ: Dead-Letter Queue
- IOC: Indicador de comprometimento
- PII/PHI: Informação pessoal/saúde
- LGPD: Lei Geral de Proteção de Dados
- ANVISA: Agência Nacional de Vigilância Sanitária
- CFM: Conselho Federal de Medicina
- OTel: OpenTelemetry
- BM25: Best Matching 25

---

## 3. Referências canônicas

- Nomenclaturas e enums: `29-canonical-references.md`
- Critérios de maturidade e decisões finais: `ARB-architecture-review-report.md`
- Plano de remediação executado: `ARB-architecture-remainder-plan.md`
- Thresholds numéricos: `29-canonical-references.md`

---

## 4. Convenções

- Identificadores públicos sempre UUIDv7.
- Eventos em snake_case com sufixo `.v1`.
- Códigos de erro em PascalCase estável.
- Campos sensíveis marcados em maiúsculas quando aplicável.
- Documentos afetados por mudança devem ser atualizados antes do deploy.