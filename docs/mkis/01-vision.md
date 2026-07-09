# MKIS — Visão e Princípios

## 1. Propósito do sistema

O Medical Knowledge Intelligence System (MKIS) é a camada de inteligência científica do ecossistema BioAcupunt.  
Ele existe para:

- adquirir conhecimento científico de fontes confiáveis;
- estruturar, higienizar e enriquecer esse conhecimento;
- indexá-lo para busca híbrida, semântica e vetorial;
- disponibilizá-lo de forma auditável, versionada e rastreável para apps, agentes e relatórios;
- prover um grafo de conhecimento clínico usado por tomada de decisão, recomendação e compliance.

O MKIS não é um CRUD de texto. É um sistema de inteligência científica que combina ingestão, processamento, IA, busca eKnowledge Graph, com forte ênfase em segurança, LGPD, auditoria, performance e rastreabilidade probatória.

---

## 2. Princípios arquiteturais obrigatórios

- **Independência:** backend dedicado, desacoplado do Android. Contratos HTTP são a única fronteira oficial.
- **Persistence ignorance no core:** o domínio não referencia frameworks, ORMs ou clientes externos.
- **Dados são imutáveis:** nunca sobrescrita; toda alteração gera nova versão.
- **Auditabilidade inerente:** toda mudança relevante gera evento, log e, quando aplicável, hash de conteúdo.
- **Observabilidade from day one:** toda operação relevante emite métricas, logs estruturados e spans.
- **Fail fast e segurança:** proteções contra SSRF, XXE, path traversal, prompt injection e upload malicioso desde o primeiro endpoint.
- **Escalabilidade horizontal:** estado fora do processo, workers como consumidores, cache como camada explícita.
- **Latência controlada:** operações síncronas devem ser bounded; operações pesadas são assíncronas com webhook/evento.

---

## 3. Objetivos não funcionionais

| NFR | Target | Medição |
|------|--------|---------|
| Disponibilidade | 99,95% no backend | health checks + probes |
| Latência P95 search | < 300 ms | métricas Prometheus |
| Latência P95 ingest | assíncrono com backpressure | batch size, queue depth |
| Consistência | strong para leituras por handle; eventual para índices | políticas documentadas |
| Segurança | LGPD, RBAC, ABAC, rate limit, proteções OWASP Top 10 | pentest + verificação contínua |
| Observabilidade | tracing 100% nas operações críticas | OpenTelemetry |
| Auditabilidade | 100% das mutações com autor, origem e hash | tabela de auditoria consolidada |
| Testabilidade | cobertura de contrato > 90%; integração e caos | quality gates |
| Escalabilidade | suporte a 10.000 usuários e milhões de documentos | índices + cache + workers + sharding |
| Residência | dados no Brasil por padrão; multirregião opcional | politicas por tenant/região |
| Portabilidade | backend deployable em containers/nuvem | Docker/K8s manifests |

---

## 4. Fronteiras explícitas

**Dentro do MKIS:**
- Ingestão, processamento, validação, chunking, embedding, indexação, busca, Knowledge Graph, versionamento, auditoria, eventos, segurança, jobs.

**Fora do MKIS:**
- App Android (consumidor via contratos públicos).
- Serviços médicos auxiliares auxiliares existentes tratados como apoio, não bloqueiam runtime app.

**Regra de integração:**
- O app nunca deve depender de comportamento interno do MKIS.
- Contratos públicos são REST/DTOs/Eventos versionados.
- O backend evoluirá independentemente; versões de API devem conviver por período mínimo de deprecação.

---

## 5. Princípios de qualidade textual e semântica

- Nomes de módulos, tabelas, APIs, eventos e DTOs devem ser semanticamente corretos para MTC/medicina.
- Políticas de nomenclatura: snake_case para banco/tabelas/colunas, kebab-case para endpoints, PascalCase para entidades DTOs quando aplicável.
- IDs públicos devem ser UUIDv7 sempre que possível.
- Não usar nomes genéricos como "data", "value", "type" sem namespace semântico explícito.

---

## 6. Não-objetivos (fora de escopo)

- Interface clínica de usuário final.
- Modificações no app Android além de contratos bem definidos.
- Processamento de dados pessoais fora de políticas LGPD explícitas.
- Funcionalidades clínicas com risco de prejuízo ao paciente sem validação ética e governança.

---

## 7. Glossário mínimo

- KnowledgeNode: unidade atômica de conhecimento científico estruturada.
- KnowledgeGraph: grafo de relacionamentos entre KnowledgeNodes e entidades clínicas.
- IngestJob: job assíncrono completo de ingestão de documento.
- HybridSearch: combinação lexical + vetorial + re-ranking.
- ScientificEvidenceScore: score consolidado de evidência, risco de viés e metodologia.
