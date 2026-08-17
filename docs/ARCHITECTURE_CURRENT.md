# BioAcupunt — Auditoria da arquitetura atual

**Data da auditoria:** 2026-08-17  
**Escopo:** Fase 0 do programa BioAcupunt Clinical Intelligence 2.0  
**Regra:** esta auditoria não altera comportamento do produto.

## 1. Resumo executivo

O repositório não é apenas um prontuário com chatbot. Ele já é um monorepo com:

- aplicativo Android nativo em Kotlin/Jetpack Compose;
- backend FastAPI assíncrono com PostgreSQL/SQLAlchemy/Alembic;
- frontend web Next.js;
- Room offline-first com sincronização e resolução de conflitos;
- biblioteca local, Knowledge Nodes, packs e busca híbrida/MKIS on-device;
- contratos de IA com roteamento por capacidade, privacidade e fallback;
- Gemma/MediaPipe para inferência local;
- prontuário, MTC estruturada, agenda, CRM, relatórios, educação e farmacologia;
- autenticação, biometria, armazenamento seguro, auditoria e backup Google Drive.

O principal problema atual não é ausência de módulos; é a falta de uma superfície canônica única para conhecimento clínico MTC, evidência, graph retrieval, diagnóstico diferencial e copiloto contextual. Há também dívida de organização e documentação que precisa ser resolvida antes de ampliar o domínio.

## 2. Estado observado

### Android

- Módulo único `:app`, namespace `com.bioacupunt`.
- Kotlin/JVM 17, compile/target SDK 36, Compose, Room, WorkManager, DataStore, Retrofit/OkHttp, Moshi/Serialization, Biometric e MediaPipe Tasks GenAI.
- Organização predominantemente por domínio/pacote, mas com uma camada ampla de telas em `ui/screens` e alguns contextos duplicados entre `data`, `patient`, `prontuario`, `biblioteca`, `sync` e `ai`.
- `AppDatabase` está na versão 24 e reúne entidades de clinic core, prontuário, MTC assessment, biblioteca, Knowledge Nodes/MKIS, sincronização, educação, farmacologia e traduções.
- Conteúdo embarcado existente em `app/src/main/assets/packs`, incluindo packs ANVISA e PCDT. O conteúdo MTC precisa ser separado e governado sem misturar conteúdo farmacológico ou editorial sem proveniência.

### IA e inteligência

Existem contratos e infraestrutura para:

- `AiRequest`, `AiResult`, `AiProvider`, `AiRepository`;
- capabilities, privacidade, custo, latência, fallback e telemetria;
- provider registry/orchestrator;
- modelo local e download por WorkManager;
- embedding, busca/retrieval, biblioteca e casos de uso de síntese/estruturação clínica.

Ainda não há evidência, nesta auditoria, de um pipeline único e verificável que imponha a sequência `plan → retrieve → rules → evidence → verify → explain` para todos os fluxos clínicos. Essa será uma evolução de contratos e orquestração, não uma substituição da IA existente.

### Conhecimento e busca

- `KnowledgeNodeEntity` e `KnowledgeNodeDao` já suportam tipos, versão, metadados, tenant, soft delete e projeções de busca.
- Há infraestrutura de FTS5 e referência a sqlite-vec no histórico do banco.
- `BibliotecaViewModel`, `HybridSearchService`, `MtcSearchEngine` e `MtcRetriever` indicam busca local e híbrida, mas a experiência e os contratos ainda estão divididos entre biblioteca legada e MKIS.
- O backend expõe `library_nodes` com busca textual simples em título/resumo; ele não é ainda o Knowledge Graph/GraphRAG canônico.
- O diretório `mkis-backend` aparece no histórico/arquitetura como sistema separado e não deve ser reintegrado por cópia. Primeiro é necessário decidir se seus contratos serão substituídos, adaptados ou consumidos por uma camada anticorrupção.

### Clinic Core

Já existem entidades e telas para pacientes, agenda, prontuário, sessões/avaliações MTC, documentos, CRM, finanças, relatórios e timeline parcial. O domínio documentado também prevê Consultation, Anamnesis, TreatmentPlan, ClinicalSession, ClinicalRecord e Consent, mas parte desse modelo ainda está apenas na documentação ou representada por JSON/entidades legadas.

### Segurança, privacidade e operação

- Secure preferences usam `EncryptedSharedPreferences`.
- Login e desbloqueio biométrico estão presentes.
- Há trilha de auditoria, hardening, backup/restore e integração opcional com Google Drive.
- O próprio código da tela de ajustes registra que o banco Room ainda não está criptografado em repouso. Isso é uma lacuna de segurança relevante para dados clínicos sensíveis.
- A política atual de IA local/cloud precisa ser exposta ao profissional com clareza no fluxo de cada operação, incluindo escopo de dados enviado.

### Backend e web

- Backend oficial documentado: autenticação, clínicas, pacientes, agendamentos, biblioteca e auditoria LGPD, com migrations e testes unitários/integrados/e2e.
- Frontend Next.js possui dashboard, pacientes, agenda, biblioteca, CRM, analytics, financeiro e relatórios.
- Não há ainda uma camada pública consolidada para Knowledge Graph, evidências, busca híbrida federada, FHIR ou copilot patient-aware.

## 3. Patrimônio a preservar

1. Room como fonte local e fila de mudanças para offline-first.
2. Contratos de IA por capability/privacy, sem acoplamento a nome de modelo.
3. `KnowledgeNode` e packs como base de conteúdo local, com evolução para `KnowledgePack` assinado.
4. Prontuário e MTC assessment existentes, incluindo revisão humana antes da persistência de conteúdo estruturado.
5. Audit trail, purge/deep-delete e separação de dados clínicos da telemetria.
6. Testes de sincronização, segurança, Room e backend.
7. Backend FastAPI oficial e frontend web como superfícies futuras, sem forçar a migração do app para web.

## 4. Lacunas prioritárias

### P0 — segurança e confiabilidade

- Criptografia do banco clínico em repouso.
- Contrato explícito de consentimento e política de saída de dados para IA externa.
- Validação de migrations, restauração e deep delete com testes de regressão.
- Corrigir divergências entre documentação antiga e estado real antes de usar a documentação como fonte de verdade.

### P1 — modelo canônico de conhecimento

- Definir `KnowledgeEntity`, `KnowledgeRelation`, `Source`, `Citation`, `Evidence` e `Version` no domínio, com IDs e proveniência estáveis.
- Unificar `KnowledgeNode`, `BibliotecaNode` e o modelo remoto por uma camada anticorrupção, sem apagar compatibilidade.
- Separar claramente tradição MTC, conteúdo editorial, literatura moderna, observação clínica e inferência de IA.

### P1 — inteligência explicável

- Criar contratos de `RetrievalResult`, `RuleResult`, `EvidenceTrace`, `DifferentialResult` e `MissingDataResult`.
- Fazer o copilot consumir contexto autorizado do paciente e retornar evidências, incertezas e origem.
- Manter o LLM como camada de explicação; regras e retrieval devem produzir a base da resposta.

### P2 — evolução do produto

- Knowledge Graph local/remoto e GraphRAG.
- Módulos estruturados de língua, pulso, pontos, meridianos, fórmulas, ervas e protocolos.
- Modo Atendimento com STT → extração → revisão → persistência.
- FHIR por adaptadores, sem remodelar o banco local à força.
- Atlas, Research Lab e Academy sobre os mesmos IDs de conhecimento e evidência.

## 5. Riscos arquiteturais

| Risco | Impacto | Mitigação incremental |
|---|---|---|
| Um banco Room crescente concentra muitos bounded contexts | Alto | manter migrações aditivas agora; definir limites e DAOs por contexto; modularizar só após contratos estáveis |
| Biblioteca/MKIS/Knowledge Node têm sobreposição | Alto | criar modelo canônico + adapters; migrar leituras primeiro, escritas depois |
| Busca híbrida sem provenance uniforme | Alto | IDs de fonte/versão obrigatórios em todo resultado |
| JSON clínico sem esquema forte | Alto | introduzir DTOs versionados e validação antes de normalizar tabelas |
| IA local, backend e web divergem em políticas | Alto | capability/privacy policy central e testes de contrato |
| Conteúdo externo ou datasets com licença incerta | Alto | inventário jurídico antes de clonar/incorporar; nenhum dataset entra no produto por padrão |
| Worktree já contém alterações não relacionadas | Alto | não sobrescrever nem formatar arquivos modificados; commits/fases isolados |
| Documentação anterior contém afirmações obsoletas | Médio | este documento passa a registrar fatos observados; atualizar documentos canônicos por etapa |

## 6. Plano de migração incremental

### Fase 0 — concluída nesta auditoria

- mapa de superfícies e patrimônio;
- identificação de sobreposições;
- inventário inicial de riscos;
- definição de ordem de migração.

### Fase 1 — Knowledge Core compatível

Adicionar apenas contratos/domain models e testes para entidade, relação, fonte, citação, evidência e versão. Adaptar o `KnowledgeNode` existente; não duplicar banco nem substituir a busca atual.

### Fase 2 — proveniência e busca unificada

Normalizar resultados de FTS, semantic retrieval e graph retrieval em um contrato comum. Começar com FTS + grafo local; deixar embeddings remotos opcionais.

### Fase 3 — motor determinístico MTC

Implementar regras puras, scoring, diferencial e missing data sobre observações estruturadas. Nenhuma chamada de LLM no núcleo determinístico; cobertura com testes de casos.

### Fase 4 — Evidence Engine e GraphRAG

Construir trace de claim → evidence → source → citation/version e contexto híbrido. O verificador deve rejeitar citações inexistentes ou conteúdo sem origem.

### Fase 5 — Copilot e Modo Atendimento

Conectar os contratos anteriores ao contexto autorizado do paciente, STT/extração opcional e revisão humana obrigatória antes da escrita clínica.

### Fase 6 — atlas, academy, research e interoperabilidade

Reusar os IDs canônicos de conhecimento; adicionar FHIR por adapters e conteúdo multimodal apenas após o núcleo ter métricas de qualidade.

## 7. Critérios de aceite da próxima fase

- build/testes atuais preservados;
- nenhuma API pública interna removida;
- entidade/relação/evidência serializáveis e versionadas;
- resultado de busca capaz de apontar fonte e versão;
- teste demonstrando que conteúdo sem evidência não é apresentado como evidência;
- nenhuma dependência/dataset externo incorporado sem licença registrada.

## 8. Estado do worktree

Na data da auditoria havia modificações locais pré-existentes em Android, sync, segurança, navegação, telas e testes, além de arquivos não rastreados. Esta auditoria não reverteu, formatou ou sobrescreveu esses arquivos. A continuidade deve tratar essas mudanças como pertencentes ao usuário e revisar conflitos antes de qualquer alteração sobreposta.

## 9. Descobertas da Fase 1

- `BibliotecaNodeEntity` (`biblioteca_nodes`) e `KnowledgeNodeEntity` (`knowledge_nodes`) são tabelas distintas; nenhuma relação canônica persistida existia.
- `MtcArticle` e os packs de curadoria são modelos de conteúdo adicionais e não foram importados automaticamente por falta de uma decisão de proveniência/licença por item.
- O Room recebeu uma camada aditiva `knowledge_core_*` na versão 25, com migration 24→25, sem apagar estruturas legadas.
- O backend oficial passou a ter uma fronteira `app.knowledge` e endpoint canônico inicial de busca; o adapter atual lê a biblioteca remota e mantém provenance.
- A migração de dados em massa ainda não foi executada; o relatório registra zero por desenho, evitando perda silenciosa.
