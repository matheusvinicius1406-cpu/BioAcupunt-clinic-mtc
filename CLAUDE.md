# CLAUDE.md — BioAcupunt

Sistema Operacional Clínico para Medicina Tradicional Chinesa.
Kotlin · Jetpack Compose · Room · offline-first · backend FastAPI · multi-tenant.

**A usuária final é uma médica. Um bug aqui pode machucar uma paciente.**

Este documento tem duas categorias de regra, e a diferença importa:

- **R1-R4 logo abaixo são a exceção.** Travadas por teste automatizado,
  não-negociáveis, escritas pra sobreviver a um pedido futuro de "afrouxa isso" —
  inclusive vindo do próprio usuário, numa conversa, sob pressão de prazo ou
  empolgação com uma feature nova. Se alguém pedir pra remover ou contornar uma,
  a resposta certa é explicar o porquê (ele está escrito abaixo) e propor uma
  alternativa segura. Nunca obedecer calado. Já aconteceu — ver "Escopo da IA".
- **Todo o resto do documento é direção, não cela.** Convenção, padrão de código,
  "é assim que fizemos até agora" — pode e deve mudar conforme o projeto cresce.
  Discorde, proponha diferente, refatore, expanda esta doc. A clareza existe pra
  você decidir rápido, não pra te travar.

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

## Escopo da IA — grounded onde importa, livre onde não importa

Decisão tomada em 2026-07-25, depois de um pedido explícito pra "soltar" a IA (sem
RAG, com acesso à internet, podendo agendar/abrir apps) ser recusado e resolvido
assim — registrado aqui pra nenhuma sessão futura precisar relitigar do zero:

- **Conteúdo clínico/MTC continua 100% sob R1/R2/R4, sempre.** Pergunta sobre ponto,
  protocolo, contraindicação, fórmula, diagnóstico diferencial — sempre passa por
  `AskLibraryUseCase`/`ClinicalSafetyEngine`. Sem evidência da biblioteca curada, sem
  resposta. Isso não é negociável por pedido de usuário numa conversa — é o motivo
  do projeto ter essas quatro regras em primeiro lugar. Quem carrega o risco de uma
  resposta errada aqui não é quem pediu a mudança; é a paciente do outro lado.
- **Tudo que NÃO é conteúdo clínico pode — e deve — ser um assistente livre de
  verdade:** busca na internet, agenda da clínica, lembretes, tarefas
  administrativas, redação de mensagem pra paciente. Ferramentas reais, sem mock,
  sem limite artificial. Isso não é meio-termo tímido: é a IA ficando genuinamente
  mais capaz exatamente onde não há paciente em risco.
- **Ainda não construído** (documentado aqui como direção, não como feito): o
  roteador que hoje só distingue "achou evidência" / "não achou evidência"
  (`UnifiedAiChatViewModel`) precisa de uma terceira categoria — "isto é
  administrativo, nunca deveria passar pelo gate clínico" — pra essas tarefas não
  ficarem reféns de uma pergunta de MTC que não achou contexto. Precisa também das
  ferramentas de verdade (busca web, agenda) implementadas e conectadas. Próxima
  sessão de IA: isso — mantendo R1/R2/R4 intocados no caminho clínico.

**2026-07-27 — pedido de a IA inferir o diagnóstico direto foi recusado na ocasião.**
Vide decisão abaixo.

**2026-07-28 — DECISÃO DE PRODUTO: IA de Síntese Clínica foi implementada.**
A Dra. Camila decidiu retomar o pedido com uma arquitetura mais segura: a IA agora
é um **terceiro caminho** — separado do R1 (ClinicalSafetyEngine), do R2 (AskLibraryUseCase)
e do R4 (geração de conteúdo para a biblioteca).

O que foi construído:
- **`ClinicalSynthesisUseCase`** — recebe TODOS os dados do prontuário (MtcAssessment,
  histórico, língua, pulso, Ba Gang, Zang Fu, fatores de piora/melhora, exames,
  medicações), busca evidência na biblioteca curada (RAG), e quando não acha, permite
  busca na web (Gemini + Google Search). Gera uma sugestão estruturada em JSON:
  diagnóstico MTC + CID biomédico + diagnósticos diferenciais + plano terapêutico.
- **`ClinicalSynthesis`** (modelo de domínio) — `ClinicalDiagnosis.kt` — contém
  `TcmDiagnosisSuggestion`, `BiomedicalDiagnosisSuggestion`, `DifferentialSuggestion`,
  `TherapeuticSuggestion`, `EvidenceSource`, `ConfidenceLevel`.
- **UI no ProntuárioScreen** — card "Síntese Diagnóstica IA" na aba Resumo que exibe
  cada componente com botões individuais de aceitar. NUNCA salva automaticamente.

Guardarails mantidos:
- R1 intacto: `ClinicalSafetyEngine` continua Kotlin puro, sem IA. A sugestão de plano
  NÃO passa pelo motor de segurança — a médica leva para a aba Plano, onde o motor roda.
- R2 intacto: `AskLibraryUseCase` continua com gate `if (!grounding.hasEvidence)`.
- R4 intacto: a IA não gera conteúdo para o acervo da biblioteca.
- A sugestão NUNCA é salva automaticamente — cada componente tem botão "Aceitar"
  individual que grava no campo correspondente (`clinicalImpression`, `orientations`).
- Nível de confiança reportado: HIGH / MODERATE / LOW / INSUFFICIENT_EVIDENCE.
- Se o provider de IA estiver indisponível, degrada silenciosamente para vazio.

Arquitetura:
```
ClinicalSynthesisUseCase
  ├── 1. buildClinicalProfile() — monta perfil completo do MtcAssessment
  ├── 2. mtcRetriever.retrieve() — busca RAG na biblioteca (FTS4)
  ├── 3. buildPrompt() — constrói o prompt com evidência (se houver)
  ├── 4. AiRequest(preferLocal = grounding.hasEvidence) — prefere local se achou evidência;
  │     allowWebSearch = !grounding.hasEvidence — busca web se não achou na biblioteca
  └── 5. parseResult() — extrai JSON e retorna ClinicalSynthesis
```

Decisões de design desta implementação:
- O `ClinicalSynthesisUseCase` é um caminho **separado** — não substitui nem compete com
  `AskLibraryUseCase` (RAG do chat) nem com `ClinicalSafetyEngine` (segurança).
- `preferLocal` é `true` apenas quando a biblioteca achou evidência; caso contrário,
  `false` para permitir que o cloud provider (Gemini com Google Search) seja selecionado.
- A médica aceita cada componente individualmente: TCM, biomédico, plano.
- Os ícones usados são do core Material Icons (`Star`, `Description`, `Search`, `Medication`,
  `Check`, `Close`, `Warning`, `Refresh`) para evitar dependência do pacote extended.

---

## Comandos

```bash
# Android
./gradlew testDebugUnitTest    # testes unitários — devem passar todos
./gradlew assembleDebug
./scripts/pin_models.sh        # fixa SHA-256 dos modelos (precisa de HF_TOKEN)

# Backend
cd backend && python -m pytest -q               # testes (unit + e2e)
cd backend && alembic upgrade head               # migrations, contra o host DIRETO (não o pooler)

# Web
cd web && npx tsc --noEmit && npm run build      # typecheck + build de produção
```

Deploy: `DEPLOY.md` na raiz do repositório (Vercel + Supabase/Neon Postgres, dois
projetos Vercel — backend na raiz, web em `web/` — GitHub conectado via "Import Git
Repository" faz deploy automático a cada push na branch configurada).

---

## Como o código deste projeto é escrito

Padrões observados e ativamente seguidos — não é aspiracional, é o que já existe em
dezenas de arquivos. Siga o vizinho mais parecido antes de inventar um padrão novo.

**Camadas (Android):** `domain/model` (dado puro) → `domain/repository` (interface)
→ `data/repository/XRepositoryImpl` (implementação; sempre devolve `Result<T>`,
nunca deixa exceção vazar pro ViewModel) → `data/local` (Entity + Dao + Mapper) →
`presentation/XViewModel` (+ `XUiState` + `XViewModelFactory`).

**ViewModel:** `MutableStateFlow` privado, `StateFlow` público via `.asStateFlow()`.
Toda escrita passa por `viewModelScope.launch`, chama o repository, **checa o
`Result`** — `is Result.Error` vira `_state.update { it.copy(error = ...) }`, nunca
é descartado. `XUiState` é uma `data class` com todo campo tendo default — nenhuma
tela lê um campo que pode não existir ainda.

**DI:** manual, em `AppContainer.kt`, tudo `by lazy`. Sem Hilt/Koin — decisão
antiga, não relitigar sem motivo novo.

**Testes:** FakeDao (implementa a interface do Dao real, estado em memória) pra
testar Repository/ViewModel sem Robolectric — mais rápido, é o padrão de ~90% dos
testes do projeto. Robolectric só entra pra migração de Room (SQLite real) ou
quando o teste precisa de `Context` Android de verdade.

**Multi-tenant:** toda entity com dado de clínica tem `tenantId: Long` sem default
(o chamador é obrigado a informar) + `Index("tenantId")`. Todo repository resolve o
tenant por `TenantManager`/lambda — nunca hardcoded, nunca "0L" como sentinela.

**Soft delete:** coluna `deleted`/`deleted_at`. Toda query de listagem/detalhe
filtra — copie um repositório vizinho já correto (`AppointmentRepositoryImpl`,
`CrmPatientRepositoryImpl`) antes de escrever um novo.

**Migração Room:** aditiva, nunca remove coluna/tabela. **Sem `DEFAULT` no SQL** de
`CREATE TABLE` — Room valida contra o `@ColumnInfo` da entity, que usa default
Kotlin, não SQL; `DEFAULT` no SQL sem correspondente na entity crasha a app inteira
ao abrir o banco. `ALTER TABLE ADD COLUMN` tolera `DEFAULT` (é a exceção). Copie o
estilo da migração mais recente, não uma antiga.

**Backend:** FastAPI + SQLAlchemy async, repository por recurso em
`app/repositories/`, sempre filtrando `clinic_id` + `deleted_at.is_(None)`. Alembic
pra schema. Todo endpoint fora de `auth`/`health` exige `Depends(get_current_user)`.

**Web:** Next.js App Router, Server Components fazem a chamada ao backend (o token
httpOnly nunca chega no JS do navegador). `middleware.ts` é o único lugar que gira
refresh token — não duplique essa lógica em outro lugar.

---

## Anti-padrões — o mesmo bug, em famílias diferentes

Cada item abaixo já apareceu pelo menos uma vez neste projeto, foi achado numa
auditoria e corrigido. Reaparecer é regressão, não coincidência — antes de escrever
um `onClick`, uma escrita, ou um fallback, pergunte se o que está saindo da mão é
um destes seis.

1. **UI que promete e não cumpre.** Um `onClick`, switch ou campo que existe na
   tela mas não está ligado a lógica real — decorativo em vez de funcional. Já foi:
   o toggle de contraindicação clínica (2026-07-22, o pior de todos — a triagem via
   `flags` sempre vazio), o "Gerar com IA" do Flashcards, o card "Caso Gerado por
   IA" do Simulador, o switch "Google Drive conectado" sem OAuth, o Kanban do CRM
   movendo a coluna errada. **Regra:** se a UI sugere uma ação, ou ela executa de
   verdade, ou não existe na tela.

2. **`Result`/exceção descartada.** `viewModelScope.launch { repository.save(x) }`
   sem olhar o retorno. A médica acha que salvou; não salvou; não há como ela
   saber. Já foi: `ExameViewModel` inteiro (vitais, exames, medicações,
   **alergias**), `advanceStage` do Dashboard, `approve`/`reject` da Curadoria.
   **Regra:** todo `Result<T>` de escrita termina em algum `is Result.Error`
   checado, e esse erro chega numa `Text`/`Toast`/`Snackbar` — não só no `Log`.

3. **Falha vira zero/vazio em silêncio.** `.catch { emit(emptyList()) }` ou
   `?: 0.0` sem marcar que foi degradação, não ausência real de dado. Já foi:
   dashboard e financeiro mostrando "R$ 0,00" indistinguível de "mês sem
   faturamento" quando a consulta na verdade tinha falhado. **Regra:** se o
   fallback pode mascarar uma falha real (dado financeiro, clínico, de sync),
   carregue um flag (`unavailable`, `financeUnavailable`) que a UI usa pra mostrar
   "—" em vez do número.

4. **Apagar antes de confirmar que o novo está seguro.**
   `BackupManager.restoreBackup` apagava `.db`/`-wal`/`-shm` antes de terminar de
   escrever os novos — uma falha no meio destruía o prontuário local sem chance de
   recuperação. **Regra:** em qualquer substituição de arquivo/registro que não
   pode ser perdido, escreva o novo primeiro (arquivo temporário, ou linha nova),
   confirme que terminou, só então apague o velho.

5. **Filtro que existe em todo lugar menos aqui.** `appointment_repository` sem
   `deleted_at.is_(None)` enquanto `patient_repository`/`transaction_repository` já
   filtravam. **Regra:** ao escrever um repository/query novo, ache o mais
   parecido já existente e compare campo a campo antes de considerar pronto — não
   confie em lembrar a regra de cabeça.

6. **Estado que finge ser real.** `var cacheSize by remember { mutableStateOf("2.4 MB") }`
   nunca lido de lugar nenhum; campo de formulário sem propriedade correspondente
   em `SecurePreferences` pra persistir. **Regra:** todo `remember { mutableStateOf(...) }`
   que representa dado de negócio (não só estado de UI tipo "diálogo aberto")
   precisa nascer de uma fonte real e morrer gravando numa fonte real. Se não tem
   as duas pontas ainda, isso é um TODO explícito — nunca um valor hardcoded que
   parece dado.

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

## Visão — como fica o projeto quando cada peça amadurece

Não existe uma data de "pronto" — é um sistema clínico vivo. Mas cada peça tem uma
direção clara de onde deveria chegar:

- **Motor de segurança clínica:** cobre 18/18 `ClinicalFlag` (confirmado em auditoria
  de 2026-07-27 — a nota antiga de "11/18" estava desatualizada). Toda regra nova
  continua exigindo revisão e aprovação da médica antes do merge — nunca inferida
  por um agente. `ClinicalSafetyEngine.kt` continua Kotlin puro, legível sem saber
  programar, para sempre.
- **Biblioteca:** hoje 16 artigos fixos + o que a médica aprovar na Curadoria
  (pipeline já traz ~3.500 candidatos de fontes abertas verificadas). Meta de longo
  prazo: 250+ artigos aprovados. Cresce um artigo de cada vez, por aprovação
  humana — nunca por geração. R4 não tem data de expiração.
- **Farmacologia:** mesmo modelo de duas camadas que a Biblioteca. Catálogo
  ANVISA (identificação — nome, princípio ativo, classe, fabricante, registro;
  ~10.260 medicamentos ativos bulk-importados de `dados.anvisa.gov.br`, real e
  público) é global e somente leitura. Posologia/interação/contraindicação/MTC só
  existem depois que a médica cura um `FormularioMedicamento` (bula em mãos) e
  aprova — o `PharmaSafetyEngine` (R1: Kotlin puro, sem IA) trata qualquer item
  sem aprovação como "não verificado", nunca como seguro. Não existe fonte aberta
  em bulk pra bula (Bulário Eletrônico da ANVISA é per-item, atrás de Cloudflare,
  sem API oficial) — a curadoria manual é o único caminho legítimo, R4 aplicado ao
  risco de dose/interação real, não só a um artigo de MTC.
- **IA:** dois caminhos permanentes e deliberadamente diferentes — grounded (MTC,
  sempre com evidência da biblioteca, sempre determinístico no gate) e livre
  (administrativo: agenda, busca, lembretes, sem limite artificial). Ver "Escopo da
  IA" acima.
- **App Android:** fonte de verdade offline-first. Toda feature nova nasce aqui
  antes de existir espelho no backend/web.
- **Backend + web:** painel de gestão/leitura, multi-tenant, complementar ao app —
  nunca o lugar onde decisão clínica é tomada. Isso é regra de arquitetura, não só
  estado atual: o motor de triagem não migra pro backend a menos que a médica
  decida que o painel web também trata paciente, o que hoje não é o caso.
- **CRM no backend:** hoje estruturalmente morto (sem writer no `SyncEngine` do
  app) — precisa de decisão de arquitetura antes de virar real, não é um fix
  simples.
- **Deploy:** Vercel (backend serverless + web, dois projetos, GitHub conectado via
  "Import Git Repository" — cada push builda e publica sozinho) + Postgres gerenciado
  (Supabase ou Neon). Ver `DEPLOY.md`.
- **Device:** nada neste projeto é considerado "pronto" só por compilar e passar
  teste JVM. Compose, migrações Room, inferência on-device — tudo precisa de smoke
  test num Android real antes de virar produção de verdade.

---

## Estado honesto

- **Executado e verde:** motor de segurança, catálogo de modelos, integridade, busca,
  RAG, mapper de prontuário, motor de segurança farmacológica (171 testes,
  `./gradlew testDebugUnitTest`), `compileDebugKotlin`, `assembleDebug`.
- **Compilado, não testado em device:** Compose, Room (migração 8→9, 19→20), MediaPipe.
- **Nunca testado:** inferência on-device (só roda em Android real).
- **MediaPipe está em modo manutenção** — migrar para LiteRT-LM. O raio de explosão
  está confinado ao `LocalLlmProvider` de propósito.
- **Rasas ainda:** nenhuma feature principal — Educação/Flashcards saiu da lista em
  2026-07-25 (rebuild completo). Farmacologia é nova em 2026-07-27 (ver handoff
  abaixo): motor de segurança e curadoria testados por unidade, mas nenhuma tela
  vista pela médica e nenhum repositório/ViewModel da camada de apresentação testado
  ainda. Agenda, CRM, Financeiro, Relatórios e Analytics já tinham saído em sessões
  anteriores.
- **As regras clínicas precisam do aval da médica.** `ClinicalSafetyEngine.kt` é
  legível de propósito — ela audita sem saber Kotlin.

### Onde parei (2026-07-29) — troca de motor local (Qwen → Phi-4 Mini Instruct), Llama 3.2 investigado e rejeitado, bug de crash no Relatórios

Também nesta sessão, sem relação com a troca de modelo: corrigido um crash real
reportado por stack trace (`IllegalArgumentException: Key ... was already used`) na
aba "Gerados" de `RelatoriosScreen.kt` — a lista usava `key = { it.generatedAt +
it.type }` em vez do `id` real (Room `@PrimaryKey(autoGenerate = true)`, já existente
em `ReportEntity`/`Report`), e um duplo toque rápido em "Gerar" no mesmo template
podia produzir dois relatórios com o mesmo timestamp de milissegundo, colidindo a
chave. Trocado para `key = { it.id }`, igual a todo outro `LazyColumn` do app.

**Pedido original**: usar `Llama-3.2-3B-Instruct` como motor por trás de todas as
funções de IA do app (prontuário, biblioteca, assistente livre), "sem restrição de
tokens". Esclarecido em duas rodadas de `AskUserQuestion` antes de tocar em código:
(1) escopo é trocar só o motor — R1/R2/R4 continuam intocados, garantido de graça
porque `AppContainer.kt` liga um único `AiRepository` compartilhado por todos os
use cases; (2) execução continua no dispositivo, offline — não um backend novo com
GPU (o usuário colou snippets `transformers`/`AutoModelForCausalLM`/
`InferenceClient`/Docker Model Runner da própria página do Hugging Face, que são
exemplos genéricos de uso em Python/nuvem/desktop, não o caminho deste app).

**Llama 3.2 3B investigado a fundo e REJEITADO — não é só a licença.** A sessão
gastou muitas rodadas tentando destravar `litert-community/Llama-3.2-3B-Instruct`
(gated, Llama Community License) com o usuário — ele teve dificuldade real
completando o fluxo de licença/token do Hugging Face. No fim, usando
`huggingface_hub` autenticado (não `curl`/`WebFetch` cru, que davam 401/404 pouco
informativos) pra listar os ~282 repos reais do org `litert-community`, descobri
que **`litert-community/Llama-3.2-3B-Instruct` NUNCA EXISTIU** — a URL que
resultados de busca (e eu, repassando pro usuário) insistiam ser real. O repo
correto é `litert-community/Llama-3.2-3B` (sem "-Instruct"), e ele tem três
problemas técnicos reais, independentes de qualquer licença:
1. Só existe em `.litertlm` (`llama3_2_3b_mixed_int4_gpu.litertlm`, ~2.06GB) — sem
   `.task`. Este app só tem runtime implementado pra `.task` (MediaPipe);
   `LocalRuntime.LITERT_LM` não tem código nenhum por trás.
2. É uma build GPU-específica — compatibilidade incerta entre aparelhos Android.
3. A tag `base_model` aponta pra `meta-llama/Llama-3.2-3B` (sem "-Instruct") —
   sinal forte de ser o modelo base/pré-treinado, não afinado pra seguir instrução.

Fica registrado no catálogo como `llama-3.2-3b-rejected` (nunca pinado, nunca será)
só pra nenhuma sessão futura repetir a mesma investigação.

**O que ficou ativo: Phi-4 Mini Instruct.** Depois de rejeitar o Llama, usei a
mesma API autenticada pra auditar os outros candidatos já presentes no catálogo
antes de escolher. `litert-community/Phi-4-mini-instruct`: `gated: False` (MIT,
zero fricção de licença — confirmado também com `curl -I` sem nenhum header de
autenticação, HTTP 200 direto), tem `.task` real
(`Phi-4-mini-instruct_multi-prefill-seq_q8_ekv4096.task`), "instruct" no próprio
nome, contexto real de 4096 tokens (3x o do Qwen — atende de fato o "sem restrição
de token" do pedido original, coisa que o Llama nunca teria garantido). Baixado o
arquivo real (3.910.050.199 bytes) e calculado `sha256sum` de verdade — nunca
inventado (R3): `88665a75f6a0b5083ce65255139212ff6da705d5f682edbbd109eae784b2173c`.
Colado em `LocalModelCatalog.kt` (`runtime` trocado de `LITERT_LM` pra `MEDIAPIPE`,
já que o arquivo real é `.task`; `minDeviceRamMb` subido de 6144 pra 8192 —
estimativa por proporção de tamanho, ainda não validada em device real).
`LocalModelManager.MODEL_ID`/`MODEL_FILE_NAME`/`DEFAULT_MODEL_URL` cortados pro Phi-4
— `DEFAULT_MODEL_URL` continua sendo um link direto ao Hugging Face (confirmado
funcionando sem token, igual o Qwen tinha) porque este repo também não é gated.

Corrigido de passagem, pra próxima troca de modelo não repetir o mesmo
esquecimento: `LocalLlmProvider.displayName` e o descritor em `models` eram string
fixa (`"Qwen 2.5 (no dispositivo)"`); agora leem `LocalModelCatalog.byId(MODEL_ID)`.
Mesma correção em `AjustesScreen.kt` (card "Sobre o App") e `LocalModelCard.kt`.

**Suite: 184 testes, 0 falhas, 2 skipped de propósito.** `compileDebugKotlin` e
`assembleDebug` verdes. Nenhum diff em `ClinicalSafetyEngine.kt`,
`AskLibraryUseCase.kt` ou `ClinicalSynthesisUseCase.kt` — confirmado via `git
status`.

**Lição pra próxima sessão que for pinar um modelo novo**: não confie em URL de
resultado de busca nem em `curl`/`WebFetch` cru pra confirmar existência/formato de
repo gated — os dois deram sinais enganosos aqui (401/404 indistinguíveis de "não
existe"). `huggingface_hub` autenticado (`HfApi().list_models`/`model_info`) foi o
único jeito de obter fato real, e resolveu em minutos o que consumiu a sessão
inteira tentando pelo navegador do usuário. **Nada disso foi testado em device
real** — mesma limitação de sempre; a estimativa de `minDeviceRamMb = 8192` precisa
de validação num aparelho de verdade antes de confiar cegamente.

### Onde parei (2026-07-27, parte 2) — Prontuário: menos checkbox, motivo unificado, 1ª IA no prontuário

Pedido original era reconstrução total do Prontuário (15 "engines", OCR, DICOM,
assinatura digital, body-map) — recusado como descrito, negociado em duas rodadas de
`AskUserQuestion`. Achado que redefiniu o escopo: o design system "premium"
(`SupremoCard`/`SectionHeader`/`SelectableChip`/`AxisSelector`/`CompletenessBar`,
dark/light via `ThemeController`) **já existe e já é usado no Prontuário** — não
havia "software feio" pra redesenhar. O trabalho real virou três coisas concretas:

- **Pulso deixou de ser a maior grade de checkbox do app.** Eram 6 cartões (2
  punhos × 3 posições) × 3 profundidades × 28 `PulseQuality` sempre abertos — até
  504 alvos de toque simultâneos. Agora cada cartão é colapsável (`AnimatedVisibility`),
  fechado por padrão, com resumo de uma linha ("Não registrado" ou as qualidades já
  marcadas). Dado subjacente intocado, só a exposição visual mudou.
  `TongueFinding.notes`/`PulseFinding.notes`/`ZangFuPattern.notes` já existiam no
  domínio e nunca eram renderizados — agora têm campo de texto livre na tela
  (`SupremoViewModel.updatePatternNotes` é novo; `updateTongueNotes`/`updatePulseNotes`
  já existiam, só faltava a UI).
- **"Motivo da Consulta" consolidado.** Antes eram dois campos desconectados: o
  "Queixa principal" do `ResumoTab` (tabela `Prontuario`, nunca usado por nada) e
  `MtcAssessment.chiefComplaint` (o que de fato conta pro `completeness` e pra
  triagem, só editável em `AtendimentoScreen`, ausente do `ProntuarioScreen`).
  `ResumoTab` agora edita só `chiefComplaint`. Nada foi apagado — o campo antigo
  continua no schema, só paramos de escrever nele por essa tela.
- **Primeira vez que texto do prontuário toca IA** (`StructureChiefComplaintUseCase`,
  novo). Estritamente extrativo — reorganiza em JSON (fatores de piora/melhora,
  sintomas citados) o que a médica JÁ escreveu, nunca infere diagnóstico nem sugere
  ponto/protocolo/CID (system prompt proíbe isso explicitamente, testado). Debounce
  de 1200ms sobre `chiefComplaint`, texto <15 chars nunca chama o modelo. Falha
  (JSON malformado, provider indisponível) degrada em silêncio pra "sem sugestão" —
  nunca propaga erro pra médica, nunca crasha. Aceitar um chip de sugestão chama a
  MESMA função que um toque manual chamaria (`toggleAggravating`/`toggleRelieving`/
  `toggleReviewOfSystems`, já existiam) — sem caminho de escrita paralelo. `R1`
  intocado: `ClinicalSafetyEngine.kt` sem diff. `R2` intocado: `AskLibraryUseCase.kt`
  sem diff — isto não é R2 (não responde pergunta clínica), é uma terceira coisa.
- **Consentimento LGPD atualizado** (`CloudConsentDialog.kt` + card em
  `AjustesScreen.kt`): antes dizia explicitamente "nunca clínicas" sobre o que vai
  pra nuvem — ficou falso com esta feature. Texto agora avisa que o Motivo da
  Consulta pode ser enviado quando a nuvem está ligada, e deixa claro que
  diagnóstico/triagem/veto continuam 100% determinísticos e nunca saem do
  aparelho — essa distinção é a parte que importa juridicamente.
- **Suite: 181 testes (171 + 10 novos), 0 falhas, 2 skipped de propósito.** Novos:
  `StructureChiefComplaintUseCaseTest` (7 — inclui "o prompt nunca pede diagnóstico
  ou sugestão de tratamento", verificado por conteúdo literal do system prompt) +
  `SupremoViewModelTest` (+3 — aceitar sugestão grava no campo certo e poda o chip
  aceito; três digitações em sequência viram uma chamada só, não três; texto curto
  nunca aciona o modelo). Ambos os arquivos de teste rodam sob Robolectric (não
  JUnit puro) porque `org.json.JSONObject` é stub fora dele — mesma razão que já
  vale pra `CloudAiProvider`. `assembleDebug` verde.
- **Não feito nesta fatia** (documentado, não escondido): as outras abas
  (Exames/Prescrição/Evolução/Documentos) não foram tocadas — já estavam alinhadas
  com o design system, sem checkbox excessivo. `region`/`durationText`/`symptoms`
  da extração ficaram de fora do v1 (só fatores de piora/melhora/sintomas citados
  viram sugestão) — os campos de domínio pra isso não existem ainda, YAGNI até
  provar que vale a pena. Nada testado em device. Médica não viu a tela nova ainda.

### Onde parei (2026-07-27) — Farmacologia (Pharma Library + Smart Prescription)

Sessão começou como auditoria completa (achados: gap de consentimento LGPD no cloud AI
default-on, 2 falhas silenciosas na Biblioteca — ambos corrigidos e commitados em
`7f6e03d`, só local) e virou construção do módulo de Farmacologia pedido em seguida.
**Tudo desta parte ainda não commitado** — só no working tree.

- **Pedido original** (Neo4j, ANVISA+FDA+EMA completos, imagens licenciadas, bulário
  inteiro em bulk) foi **recusado como descrito** e renegociado com o usuário via
  `AskUserQuestion` em 3 rodadas: fonte de dados, arquitetura, escopo do MVP. Decisão
  final: catálogo ANVISA real (Room/SQLite, não Neo4j — quebraria offline-first) +
  curadoria manual da médica pra tudo que exige julgamento clínico.
- **Achado técnico que redefiniu o escopo**: baixei e inspecionei
  `dados.anvisa.gov.br/dados/DADOS_ABERTOS_MEDICAMENTOS.csv` de verdade — 43.353
  linhas, 16.999 `Ativo`. Cobre só identificação (nome/princípio ativo/classe/
  categoria/fabricante/registro). Testei o Bulário Eletrônico (`consultas.anvisa.gov.br`)
  — HTTP 403 via Cloudflare, sem API oficial, só PDF individual. **Não construí
  scraper pra contornar isso** — posologia/interação/contraindicação/excipiente não
  têm fonte aberta em bulk, ponto final. Isso é R4 aplicado a um risco mais alto que
  Biblioteca: dose/interação real, não artigo de MTC.
- **Pipeline real rodado**: `scripts/pharma/build_anvisa_packs.py` (csv module de
  verdade, latin-1, delimiter `;` — dado tem vírgula dentro de campo citado) filtrou
  `TIPO_PRODUTO=MEDICAMENTO` + `SITUACAO_REGISTRO=Ativo` → **10.260 itens válidos**
  (6.971 rejeitados por não ter `NUMERO_REGISTRO_PRODUTO` — produtos por notificação,
  ex. homeopáticos/baixo risco, isentos de registro formal; ficam de fora até ter uma
  chave natural pra eles). 11 packs JSON em `assets/packs/pharma_anvisa/`.
- **Domínio novo** (`pharma/domain/model/PharmaModels.kt`): `Medicamento` (catálogo,
  somente leitura) + `FormularioMedicamento` (curadoria clínica da médica — posologia,
  contraindicações via `ClinicalFlag` reaproveitado, alérgenos, interações,
  efeitos adversos, Visão Integrativa MTC opcional, status RASCUNHO/APROVADO) +
  `Prescricao` (liga ao paciente, complementa `Medication` livre já existente).
- **`PharmaSafetyEngine`** (`pharma/domain/safety/`) — R1: Kotlin puro, zero import de
  `ai/`. Roda alergia×princípio-ativo mesmo SEM curadoria (dado ANVISA bulk, fato
  objetivo); todo o resto (excipiente, contraindicação, interação) exige
  `FormularioMedicamento` **APROVADO** — sem isso, veredito é `verified=false` com
  finding "Não verificado clinicamente", nunca "seguro" por omissão.
- **Room v19→20** (`MIGRATION_19_20`): `medicamentos` + `medicamentos_fts` (FTS4,
  mesmo padrão de `article_fts`) + `formulario_medicamento` (chave composta
  medicamentoId+tenantId) + `prescricoes` (soft delete via `active`). Sem `DEFAULT`
  no SQL, mesma regra de sempre.
- **UI**: `FarmacologiaScreen` (Pharma Library standalone, sem paciente) +
  `FarmacologiaCuradoriaScreen` (formulário estruturado pra médica curar, gate de
  aprovação exige posologia adulto + via) + aba nova `PRESCRICAO` no `ProntuarioScreen`
  (Smart Prescription — busca, roda o motor contra `standingFlags`/alergias/
  medicações ativas da paciente, `PharmaSafetyPanel` clonado do `ClinicalSafetyPanel`
  com o mesmo contrato de veto não-dispensável + override ≥10 chars).
- **Suite: 171 testes (151 + 20 novos), 0 falhas, 2 skipped de propósito** (os
  `@Ignore` R4 de antes, sem relação). Novos: `PharmaSafetyEngineTest` (10, inclusive
  "não verificado nunca vira seguro" e "regra quebrada não derruba as outras"),
  `FormularioMedicamentoRepositoryTest` (5, gate de aprovação + isolamento de tenant),
  `AnvisaMigrationTest` (5, Robolectric — chave composta rejeita duplicata). `assembleDebug`
  e `compileDebugKotlin` verdes, warnings de depreciação novos corrigidos na hora
  (`Icons.AutoMirrored`).
- **Corrigido de passagem**: nota desatualizada "11/18 `ClinicalFlag`" no CLAUDE.md →
  18/18 (achado ao explorar o motor clínico pra reaproveitar o enum; já estava
  completo, só a doc estava velha).
- **Ainda não feito**: nada testado em device; sem teste de `PrescricaoRepositoryImpl`
  nem de ViewModel (`FarmacologiaViewModel`/`FarmacologiaCuradoriaViewModel`/
  `PrescricaoViewModel`) — `TenantManager` exige `SecurePreferences` real
  (EncryptedSharedPreferences/Context), não dá pra fake sem Robolectric, e não coube
  no tempo desta sessão; a engine e o gate de aprovação (as partes de risco real)
  estão cobertos, a UI não. Revisão da médica sobre o fluxo completo (nenhuma tela
  nova foi vista por ela ainda). Catálogo de 6.971 itens sem registro (homeopáticos/
  baixo risco) fica de fora até decidir uma chave natural. Imagens de medicamento,
  bulário completo, FDA/EMA — não fazem parte deste MVP, nem estão planejados sem
  uma fonte licenciada real.

### Onde parei (2026-07-25, parte 3) — main no GitHub, escopo da IA, banco em produção

- **`main` local avançou (fast-forward) até `70f7860` e foi empurrada pro GitHub.**
  As partes 1 e 2 abaixo (rebuild da Educação + auditoria de bugs) estão em produção
  no repositório remoto agora.
- **Pedido de soltar a IA (sem RAG, com internet/agendar/abrir apps) foi recusado**
  depois de confirmação explícita do usuário — ver "Escopo da IA" acima pra decisão
  e raciocínio completo. Resumo: R1/R2/R4 continuam intactos pra conteúdo clínico;
  assistente livre de verdade (ferramentas reais, sem mock) fica reservado pra
  tarefas administrativas. **O roteamento e as ferramentas em si ainda não foram
  implementados** — só a decisão de arquitetura está registrada.
- **Banco de produção: trocado de Supabase pra Neon (`sa-east-1`, São Paulo)** —
  o usuário criou o projeto e passou a connection string na sequência. Rodado
  `alembic upgrade head` de verdade (não offline) contra o host **direto**
  (`ep-spring-recipe-acmrem1k.sa-east-1.aws.neon.tech`). 10 tabelas + `alembic_version`
  confirmadas, head em `d1f8a3c46e27`, banco limpo e dedicado só a este projeto
  (bem melhor que o Supabase reaproveitado antes, que tinha schema de outro app
  dentro — esse projeto Supabase ficou órfão, sem uso; pode ser pausado/ignorado).
  Vercel usa o host **pooler** (`ep-spring-recipe-acmrem1k-pooler.sa-east-1.aws.neon.tech`)
  pra `DATABASE_URL`; migrations futuras rodam contra o host direto.
- **Secrets de produção gerados** (`JWT_SECRET_KEY`, `DOCUMENT_HASH_SECRET`, 32
  bytes hex cada) — não ficam neste arquivo por serem segredo real; estão só na
  resposta da sessão em que foram gerados. Gere novos se precisar (`python -c
  "import secrets; print(secrets.token_hex(32))"`).
- **Deploy Vercel: o que dava pra automatizar foi automatizado, o resto é
  estrutural.** As ferramentas de MCP disponíveis fazem upload de arquivo avulso,
  não "importar repositório do GitHub" — e é exatamente esse import que liga o
  projeto ao GitHub pra deploy contínuo (o que foi pedido). Então o fluxo real
  continua sendo o de `DEPLOY.md`: dois cliques de "Add New → Project → Import" no
  painel (raiz = backend, `web/` = frontend), colando as env vars (banco + secrets
  já prontos acima). Depois disso, todo `git push` na `main` publica sozinho.
- **CLAUDE.md expandido** com "Escopo da IA", "Como o código deste projeto é
  escrito", "Anti-padrões" (generalização dos bugs da parte 2, com regra de bolso
  por família) e "Visão". Nada em R1-R4 foi reescrito — só o preâmbulo explicando a
  diferença entre regra travada e direção flexível.

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
