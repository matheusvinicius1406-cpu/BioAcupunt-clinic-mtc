# CLAUDE.md — BioAcupunt

Sistema Operacional Clínico para Medicina Tradicional Chinesa.
Kotlin · Jetpack Compose · Room · offline-first · backend FastAPI · multi-tenant.

**A usuária final é uma médica. Um bug aqui pode machucar uma paciente.**

---

## REGRAS INVIOLÁVEIS

Estas quatro regras têm teste automatizado que trava violação. Não as contorne.
Se um trade-off aparecer entre "mais features" e "a paciente não se machuca",
escolha a paciente. Sempre.

### R1 — Nenhum LLM no caminho de segurança clínica

`prontuario/domain/safety/ClinicalSafetyEngine.kt` decide se um protocolo pode ser
sugerido (gestação, anticoagulante, marca-passo, oncologia, linfedema). É **Kotlin
puro, determinístico, sem IA**.

- Um LLM pode *sugerir* um protocolo.
- Só o motor determinístico decide se ele **aparece na tela**.
- Um prompt se deixa convencer. Um `when` não.

**Nunca** substitua, complemente ou "melhore" uma regra de segurança com chamada a
modelo. Nunca importe nada de `ai/` dentro de `domain/safety/`.

### R2 — RAG sem evidência = sem chamada ao modelo

Em `biblioteca/domain/usecase/AskLibraryUseCase.kt`:

```kotlin
if (!grounding.hasEvidence) return Answer.NoEvidence
```

Contexto vazio é onde o LLM alucina com mais confiança. Não coloque o modelo nessa
posição. Se a busca não achou nada, o app diz que não achou — não improvisa.

O `SYSTEM_PROMPT` que diz "não invente" é a linha **mais fraca** da defesa.
A garantia é o `if`. Nunca troque um pelo outro.

### R3 — Integridade de modelo falha fechada

`ai/local/ModelIntegrity.kt` só confia num arquivo se o SHA-256 bater com um hash
fixado no código.

- **Nunca invente um SHA-256.** Hash fabricado é pior que nenhum: parece integridade
  e falha aberto.
- Hashes vazios ⇒ `LocalModelCatalog.verifiable` vazio ⇒ nenhum modelo local
  oferecido ⇒ app cai para a nuvem. **Isso é intencional.**
- Para preencher: `export HF_TOKEN=... && ./scripts/pin_models.sh`
- O `.litertlm` é **executado** por runtime nativo C++. Blob não verificado entrando
  em código nativo é superfície de execução arbitrária.

### R4 — Não gere conteúdo clínico a partir dos seus pesos

A biblioteca tem 16 artigos; a visão pede 250+. **Não preencha essa lacuna gerando
artigos de MTC.** Seria conteúdo clínico sem revisão alimentando um sistema que a
médica vai tratar como fonte confiável — exatamente o que R2 existe para impedir.

Conteúdo vem de fonte real revisada por humano (Maciocia, Deadman, diretrizes).
Você constrói o **pipeline de ingestão**, nunca o conteúdo.

---

## Comandos

```bash
./gradlew testDebugUnitTest    # testes unitários — devem passar todos
./gradlew assembleDebug
./scripts/pin_models.sh        # fixa SHA-256 dos modelos (precisa de HF_TOKEN)
```

---

## Armadilhas já corrigidas — não reintroduza

**`lateinit` em singleton acessado por `lazy`.**
`RetrofitInstance` tinha `lateinit var authInterceptor/hostInterceptor`, atribuídos
só em `init()` — que ninguém chamava. O app morria antes da primeira frame com
`UninitializedPropertyAccessException`. Hoje são `@Volatile` com fallback seguro.
Regressão travada em `RetrofitInstanceTest`.

**Filtro de segurança em parâmetro default.**
`LocalModelCatalog.runnableOn()` filtrava modelos não-verificados apenas no *default*
do parâmetro — qualquer chamador com lista própria recebia modelos sem hash.
Fail-open. Hoje o `.filter { it.isVerifiable }` está **dentro** da função.

> Filtro de segurança mora dentro da função, não num parâmetro default que o chamador
> contorna.

**Plugin de serialização não aplicado.**
`kotlinx-serialization-json` estava na dependência mas o plugin de compilador
`org.jetbrains.kotlin.plugin.serialization` não estava aplicado em
`app/build.gradle.kts`. Sem ele, classes `@Serializable` não geram serializer, e
`Json.encodeToString`/`decodeFromString` lançam em runtime — engolidos em silêncio
pelo `runCatching` do mapper (política de falha proposital), fazendo todo campo JSON
do prontuário (língua, pulso, padrões, marcas) voltar vazio depois de salvar. Hoje o
plugin está no bloco `plugins {}`. Regressão pega por
`MtcAssessmentMapperTest.roundTrip_preservesEverything`.

---

## Invariantes de domínio

**Perfil de risco permanente.** `MtcAssessmentRepository.standingFlags()` une **todas**
as flags já registradas, em **qualquer** sessão. Marca-passo anotado em março continua
vetando eletroacupuntura em julho — mesmo que a médica, correndo entre pacientes,
esqueça de re-marcar a caixinha. *A triagem não pode depender de ninguém não esquecer.
Esquecer é o que o software existe para pegar.*

**Flags são coluna SQL (`flagsCsv`), não JSON.** Enterrar contraindicação num blob
JSON a torna invisível para o SQL. Língua/pulso/padrões são JSON (lidos e escritos
junto, nunca consultados sozinhos). Flags, não. A assimetria é proposital.

**Toda edição re-executa a triagem.** Veredito calculado uma vez e envelhecido diz
"seguro" sobre um prontuário que já mudou. Triagem que deu erro **falha alto** — nunca
"limpa" o veredito em silêncio. *Triagem com erro não é paciente segura.*

**Codec degrada, não quebra.** JSON corrompido ⇒ aquele achado vira vazio e o resto do
prontuário **abre**. Médica com paciente na maca não pode levar crash porque um campo
mudou de forma.

---

## Invariantes de UI

**Aba Segurança vem primeiro.** Decisão clínica, não de layout: aviso mostrado depois
do plano pronto é aviso que se **discute**, não que se **obedece**.

**Veto é alto e não dispensável.** Contraindicação que a médica rolou por cima é pior
que software nenhum.

**Override existe, mas dói.** Ela **pode** prosseguir sobre um veto — é a responsável
clínica; o software é apoio, não autoridade. Mas exige justificativa ≥10 caracteres,
gravada com usuário e horário. Decide com autoridade, nunca por acidente.

**Silêncio é ambíguo.** "Sem contraindicações" é mostrado **explicitamente**. "Não tem
alerta" e "não foi checado" jamais podem parecer iguais na tela.

**Estado clínico inválido deve ser irrepresentável.** Ba Gang é eixo com "não
registrado" no meio, não dois toggles — senão dá para marcar Frio **e** Calor.

**Não-selecionado nunca é fantasma.** "Não registrei" e "registrei normal" não podem
parecer iguais. Alvo de toque ≥44dp: prontuário se preenche de pé, com uma mão.

**Completude é informativa, nunca bloqueante.** Formulário que se recusa a salvar
ensina a preencher lixo para passar — pior que registro honestamente incompleto.

---

## Testes que não podem ser deletados nem "simplificados"

- `noPregnancyFlag_li4IsAllowed`, `pacemaker_allowsManualNeedling`,
  `lymphedema_allowsContralateralLimb` — guardam contra motor que bloqueia tudo.
  *Motor que veta tudo parece seguro e é inútil: a médica desliga no 3º dia.*
- `aFailingRuleCannotCrashTheEngine` — falha parcial > falha total silenciosa.
- `unknownTopicYieldsNoEvidence_soTheModelIsNeverCalled` — o portão do R2.
- `unpinnedModelFailsClosed_neverOpen` — o portão do R3.
- `initIsNotRequired_apisResolveWithoutThrowing` — regressão do crash de launch.
- `roundTrip_preservesEverything` — regressão do plugin de serialização ausente.

---

## Estado honesto

- **Executado e verde:** motor de segurança, catálogo de modelos, integridade, busca,
  RAG, mapper de prontuário (101 testes, `./gradlew testDebugUnitTest`), `compileDebugKotlin`.
- **Compilado, não testado em device:** Compose, Room (migração 8→9), MediaPipe.
- **Nunca testado:** inferência on-device (só roda em Android real).
- **MediaPipe está em modo manutenção** — migrar para LiteRT-LM. O raio de explosão
  está confinado ao `LocalLlmProvider` de propósito.
- **Rasas ainda:** nenhuma feature principal — Educação/Flashcards saiu da lista em
  2026-07-25 (rebuild completo, ver handoff abaixo). Agenda, CRM, Financeiro,
  Relatórios e Analytics já tinham saído em sessões anteriores.
- **As regras clínicas precisam do aval da médica.** `ClinicalSafetyEngine.kt` é
  legível de propósito — ela audita sem saber Kotlin.

### Onde parei (2026-07-25, parte 2) — auditoria de bugs "UI que finge funcionar"

Depois do rebuild da Educação (parte 1, abaixo), rodei uma auditoria em 3 frentes
(Android UI/ViewModel wiring, silent failures, paridade backend/web) atrás do mesmo
padrão de bug já achado antes (triagem clínica inerte em 07-22, "Gerar com IA" falso
do Flashcards). Achou ~19 problemas reais. **12 corrigidos e commitados
(`4c5af4c`)** — mecânicos, sem ambiguidade de produto: CRM Kanban movendo a coluna
inteira em vez de 1 paciente (dado real sendo corrompido), `ExameViewModel`
descartando `Result` em toda escrita (inclusive alergias), card de IA morto no
Simulador, cache/endereço falsos em Ajustes, dashboard/financeiro zerando em
silêncio no erro, `LibraryReviewViewModel` engolindo falha de aprovar/rejeitar,
`BackupManager.restoreBackup` não-atômico (apagava o banco antes de terminar de
escrever o novo — risco real de perda total do prontuário local numa restauração
que falha no meio), tombstone de agendamento vazando no backend web, cookie de
sessão web durando mais que o JWT (derrubava a médica do painel com sessão ainda
válida), `/crm`+`/analytics` fora do guard de refresh do middleware, componente
órfão removido. 151 testes Kotlin + 63 backend (3 novos) + `tsc`/`next build` web,
tudo verde.

**Ficaram de fora de propósito — cada um é decisão de produto ou toca a superfície
clínica, não bug mecânico:**
- **`RelatoriosScreen`**: botão "Gerar" em templates "com IA" cria um relatório
  vazio (`status=READY`, sem `body`), sem chamar IA nenhuma, descartando o nome do
  paciente digitado. Mesmo padrão do Flashcards falso, mas aqui não dá pra só
  remover o botão sem decidir o que "Gerar" deveria fazer de verdade.
- **`AjustesScreen`**: switch "Google Drive" fica "Conectado" sem OAuth nenhum, e
  "Fazer backup agora" desse caminho falha em silêncio — é um segundo caminho
  paralelo e quebrado pro mesmo backup que já funciona direito no `BackupCard` da
  aba Segurança. Precisa decidir: remover o duplicado ou consertar o OAuth.
- **Backend CRM (`/api/v1/crm`)**: estruturalmente morto em produção — a tabela
  `crm_patients` do servidor nunca recebe uma linha porque não existe
  `SyncEntityType.CRM_PATIENT` nem writer no `SyncEngine` do app. A tela web
  (`/crm`) sempre vai mostrar "nenhum contato" mesmo com pacientes reais no app.
  É trabalho de arquitetura (schema de sync + writer + endpoint), não um fix.
- **Semanas de gestação**: `ClinicalSafetyEngine` já tem a regra testada pro
  primeiro trimestre, mas nenhuma tela chama `SupremoViewModel.updateGestationalWeeks`
  — o alerta é logicamente inatingível. Mesmo padrão do bug de `toggleFlag` (2026-07-22),
  mas fica de fora por tocar a superfície clínica direto — precisa de aval antes.
- **`BibliotecaViewModel`**: busca híbrida MKIS inteira (`toggleSearchMode`,
  `performHybridSearch`, `MkisDetailSheet`) sem nenhum ponto de entrada na UI —
  feature construída e nunca ligada a um botão. Não decidido se vale ligar ou é
  código morto pra remover.
- Docstrings contraditórias entre `patient.py` e `crm_patient.py` sobre qual tabela
  é "a" contraparte do app — cosmético, mas é o sintoma da causa raiz do item CRM
  acima.

### Onde parei (2026-07-25) — leia antes de continuar

Branch `fix/clinic-audit-phases` (== `main` local == `origin/main`, todos em
`c119402` — a fase anterior já foi mergeada e empurrada pro GitHub num momento não
documentado entre a sessão de 07-24 e esta). **Mudanças desta sessão ainda não
commitadas** — só no working tree, aguardando revisão antes de virar commit.

- **Rebuild completo da Educação/Flashcards**, implementado exatamente pelo plano
  salvo em `docs/plano-educacao-flashcards.md` (nenhum desvio de design não
  documentado): união dos 12 cards fixos (`educacao/data/BuiltinFlashcards.kt`,
  movidos verbatim da tela antiga) com cards autorais da médica (tabela `flashcards`,
  Room v18→19), progresso persistido em `flashcard_progress` via Leitner-lite
  determinístico (`LeitnerScheduler`, caixas 0-4, intervalos 1/3/7/14 dias — sem IA),
  e criação assistida a partir de artigo aprovado com extração **verbatim** de seção
  + confirmação obrigatória antes de salvar (R4: zero geração por LLM, a médica é
  sempre autora de registro).
- **`MtcRetriever.extractBestSection` refatorado** para delegar em
  `core/util/MarkdownSections.kt` (novo) — a regex de split de heading que antes
  vivia duplicada como `SECTION_HEADING` privada agora tem uma fonte só, reusada
  pela extração de flashcards. `retrieve()` e o gate R2 (`if (!hasEvidence)`)
  **não foram tocados**.
- **Migração `MIGRATION_18_19`** em `DatabaseModule.kt` (aditiva, sem `DEFAULT` no
  SQL, sem FK — `flashcards`/`flashcard_progress` não dependem de nenhuma tabela
  existente), `DB_VERSION` e `AppDatabase` version → 19. Coberta por
  `FlashcardsMigrationTest` (Robolectric + SQLite real): tabelas existem pós-
  migração, insert funciona, índice único `(tenantId, cardKey)` rejeita duplicata
  no mesmo tenant e aceita a mesma key em tenant diferente.
- **Suite: 151 testes (2 skipped de propósito, os `@Ignore` R4 de 07-24), 0
  falhas** — 116 anteriores + 35 novos (`MarkdownSectionsTest` 8,
  `LeitnerSchedulerTest` 6, `BuiltinFlashcardsTest` 4, `FlashcardRepositoryTest` 7,
  `FlashcardsViewModelTest` 6, `FlashcardsMigrationTest` 4). `assembleDebug` verde,
  **nenhum warning novo** (o único warning restante continua sendo o `GoogleSignIn`
  de `GoogleDriveClient.kt`, pré-existente).
- **Ainda não feito:** nada testado em device (a migração real v18→19 num
  `bioacupunt_db` de verdade fica para quando houver device conectado — só o
  Robolectric cobre hoje); revisão da médica sobre o rebuild (ela não viu a tela
  nova ainda); os `@Ignore` de R4 e os 7/18 `ClinicalFlag` sem regra continuam
  parados, sem relação com esta sessão; `GoogleSignIn` deprecado idem.
- **Commitado em `5d8a36a`** (só local — nada empurrado pro GitHub ainda): 5
  arquivos modificados (`MtcRetriever.kt`, `AppDatabase.kt`, `AppContainer.kt`,
  `DatabaseModule.kt`, `FlashcardsScreen.kt`) + `core/util/MarkdownSections.kt`
  novo + o pacote `educacao/` inteiro (domain/data/presentation) + os 6 arquivos
  de teste.

### Onde parei (2026-07-24) — leia antes de continuar

Branch `fix/clinic-audit-phases`, 4 commits novos locais (nada empurrado pro GitHub):

- **`5ed3768` — 4 arquivos de teste órfãos recuperados** de worktrees abandonadas em
  `.claude/worktrees/` (sobra de agentes paralelos): portão R2 testado no use case com
  espião contando chamadas ao modelo (`AskLibraryUseCaseTest`), invariante do perfil de
  risco permanente (`MtcAssessmentRepositoryStandingFlagsTest`), regressão do override
  persistido (`SupremoViewModelTest`), resiliência do motor + **2 testes `@Ignore` R4**
  nomeando a pergunta clínica exata (cardiopatia grave / hipertensão não controlada ×
  eletroacupuntura) — un-ignore só com aval da médica. Worktrees e branches
  `worktree-agent-*` removidas após a extração; `.claude-flow/` e `.claude/worktrees/`
  agora no .gitignore (as "métricas de auditoria" do claude-flow eram stub, não achado real).
- **`8028091`** — `MasterKeys`→`MasterKey.Builder` em `SecurePreferences` (arquivo guarda
  token+hash do PIN — fazer smoke test de login em device antes de confiar 100%), 9
  ícones→`Icons.AutoMirrored`, e **remoção do "Gerar com IA" falso do Flashcards** (não
  chamava IA nenhuma; fabricava texto placeholder fingindo exigir o modelo local).
- **`1b0214b`** — 15× `Locale("pt","BR")`→`Locale.Builder()`, checagem `ADB_ENABLED`
  inerte removida do `AppHardening` (API bloqueada para apps normais desde a API 17 —
  sempre caía no catch), `menuAnchor(MenuAnchorType.PrimaryNotEditable)`, `arrayOf<Any>`
  no `VecKnowledgeNodeDao`. **Único warning de depreciação restante no app:**
  `GoogleSignIn` em `GoogleDriveClient.kt` — migrar para Credential Manager muda o fluxo
  de auth de verdade; só com device conectado.
- Suite: **116 testes (2 skipped de propósito), 0 falhas**. Correção de percepção:
  **Agenda NÃO é mais rasa** (tela completa, picker de paciente vindo do CRM real);
  **Educação/Flashcards É rasa** — 12 cards fixos no código, progresso zera ao sair.
- **Plano completo do rebuild da Educação salvo em `docs/plano-educacao-flashcards.md`**
  (não aprovado, não implementado — sessão encerrada a pedido antes do ExitPlanMode):
  união builtins+cards da médica (sem seed), Leitner-lite determinístico, criação
  assistida a partir de artigo aprovado com confirmação obrigatória (R4), migração
  Room v18→19 com DDL pronto, testes por camada. Próxima sessão: retomar por esse doc.

### Onde parei (2026-07-23)

Branch `fix/clinic-audit-phases`. Tudo compila (`compileDebugKotlin`), Kotlin verde
(`testDebugUnitTest`), backend verde (`50 passed`), web verde (`tsc` + `next build`).

- **Fase 3 commitada (`83de460`):** nuvem OPCIONAL (`CloudAiProvider`,
  off-by-default, R1/R2 intactos, scorer local-first, toggle LGPD fiado em Ajustes),
  auto-lock por inatividade (`AppLock` na MainActivity + slider), prompt de fallback
  reescrito (tarefas administrativas liberadas, parede clínica intacta), remoção da
  infra órfã de agente/tool/plugin, remoção da `PatientsScreen` órfã.
- **Endpoints financeiro/relatórios + web (`5cb7fa0`):** `GET /api/v1/transactions`,
  `/transactions/summary`, `/reports/overview` no FastAPI (escopo por clínica,
  exclui tombstones, matemática espelhando o `TransacaoDao`); telas web Financeiro e
  Relatórios saíram de "TODO backend" para reais.
- **Pendências (a) e (b) do handoff anterior: RESOLVIDAS.** (a) `TransacaoEntity` tem
  `tenantId` + índices, repo escopa por tenant. (b) `SupremoViewModel.overrideVeto`
  persiste razão(≥10)+usuário+horário e salva na hora, fiado no `ProntuarioScreen`.
- **Ainda aberto:** (c) 7/18 `ClinicalFlag` sem regra — **R4, só a médica**;
  Biblioteca no web (não há modelo dela no backend — vive no Android); **nada testado
  em device**; warnings de depreciação (Icons.AutoMirrored, GoogleSignIn, MasterKeys).
- **Commits ainda são só locais — nada empurrado pro GitHub.**

### Onde parei (2026-07-22)

Sessão de expansão da biblioteca + fusão de IA, feita com subagentes em paralelo.
Tudo compila e `./gradlew testDebugUnitTest` passa (101 testes, 0 falhas), incluindo
os 8 testes sagrados. **Nada testado em device ainda.**

- **Biblioteca — conteúdo real ingerido:** `app/src/main/assets/packs/open_access/`
  tem **1.069 itens reais** de fontes abertas verificadas (PubMed, WHO IRIS, PAHO
  IRIS, DOAJ, NCBI Bookshelf, Europe PMC, + afiliação das 20 universidades pedidas +
  USP), 49 arquivos JSON, zero duplicata, todos com citação+URL reais. Carregados por
  `OpenAccessPacks.load(context)` na tela Curadoria. **Cada arquivo é um OBJETO
  `{source,items}`, NUNCA um array** — o loader Kotlin exige isso (um agente errou e
  foi corrigido). Total disponível pra curadoria: ~3.574 itens (509 packs Kotlin +
  1.996 PCDT + 1.069 abertos). **NADA disso está no acervo/RAG ainda:** a médica
  precisa Importar + Aprovar item a item na Curadoria (R4). Só os 16 fixos +
  aprovados entram no `MtcRetriever`.
- **Curadoria:** ganhou filtro (categoria/proveniência/busca) + link "Abrir fonte"
  clicável. Página de detalhe do artigo (`ArticleDetailSheet.kt`) agora mostra
  citação/fonte/proveniência/relacionados — antes era só um AlertDialog cru.
- **IA unificada:** os 2 chats viraram 1 (`UnifiedAiChatViewModel` +
  `InteligenciaScreen` reescrita). Roteamento por mensagem: **sempre tenta
  `AskLibraryUseCase` primeiro (gate R2 intacto); só cai no chat geral quando
  `NoEvidence`.** O gate `if (!grounding.hasEvidence)` NÃO foi tocado. Deletados
  `AiAssistantViewModel/Screen/Route` e `GeneralChatViewModel`. "MCP" foi
  interpretado como a infra de agente/tools já existente (não protocolo de rede —
  inviável offline); ver relatório: `AgentRegistry`/`ToolRegistry`/orquestrador
  precisam de investigação antes de rotear o fallback por eles.
- **BUG CRÍTICO corrigido — a triagem clínica estava INERTE:** a UI pra marcar
  contraindicações (`ClinicalFlag`: gestação, marca-passo, etc.) nunca tinha sido
  fiada — `SupremoViewModel.toggleFlag` não tinha chamador. Resultado: o
  `ClinicalSafetyEngine` (correto e testado) sempre via `flags` vazio e mostrava
  "Sem contraindicações" pra TODA paciente. Fiado agora em `AnamneseTab`
  (`ProntuarioScreen.kt`). **Precisa do aval da médica + teste em device.**
- **Pendências reportadas, NÃO corrigidas** (precisam de decisão de produto/médica):
  (a) `Transacao` não tem `tenantId` — mistura financeiro entre tenants; (b) o
  callback de override do veto clínico é no-op (não persiste usuário+hora como o
  CLAUDE.md exige); (c) 7 de 18 `ClinicalFlag` não têm regra no
  `ClinicalSafetyEngine` (conteúdo clínico — R4, só a médica). 
- **Fontes que não deram (anti-bot/SPA):** SciELO, BVS/LILACS, Karolinska Open
  Archive, USP teses, WashU DigitalCommons. Karolinska/USP capturados via afiliação
  no Europe PMC mesmo assim.
- **Próximos passos:** testar em device (triagem clínica fiada + IA unificada);
  resolver as 3 pendências acima com a médica; a meta de "11.000 assuntos" é de longo
  prazo (hoje 1.069 reais abertos) e exige mais rodadas de fontes. **Nada foi
  empurrado pro GitHub** — commits só locais.
