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
  RAG, mapper de prontuário (62 testes, `./gradlew testDebugUnitTest`), `assembleDebug`.
- **Compilado, não testado em device:** Compose, Room (migração 8→9), MediaPipe.
- **Nunca testado:** inferência on-device (só roda em Android real).
- **MediaPipe está em modo manutenção** — migrar para LiteRT-LM. O raio de explosão
  está confinado ao `LocalLlmProvider` de propósito.
- **Rasas ainda:** Agenda, CRM, Financeiro, Relatórios, Analytics, Educação.
- **As regras clínicas precisam do aval da médica.** `ClinicalSafetyEngine.kt` é
  legível de propósito — ela audita sem saber Kotlin.
