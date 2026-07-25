# Rebuild "Educação" (Flashcards) — persistência real + conteúdo da biblioteca

## Context

A tela Flashcards é hoje a feature mais rasa do app: 12 cards hardcoded num `private val` dentro de `ui/screens/FlashcardsScreen.kt`, progresso (lembrei/não lembrei) que zera ao sair da tela, e nenhuma ligação com a biblioteca curada que a médica vem aprovando. O botão "Gerar com IA" era falso (fabricava texto placeholder) e já foi removido em commit anterior desta sessão.

O rebuild dá à feature: (1) progresso persistido com repetição espaçada determinística (Leitner, sem IA); (2) cards autorais da médica; (3) criação assistida a partir de artigos aprovados na Curadoria — extração **verbatim** de seção + confirmação/edição obrigatória antes de salvar. Zero geração por LLM (R4): a médica é sempre a autora de registro.

## Decisões de design (fechadas)

- **Padrão união, sem seed no banco**: os 12 cards fixos continuam em código (movidos para `educacao/data/BuiltinFlashcards.kt`, ids estáveis `builtin_*`, read-only) ∪ cards da médica numa tabela nova. Espelha o precedente `MtcKnowledgeBase` (16 artigos fixos + aprovados).
- **Progresso em tabela própria** (`flashcard_progress`), chaveada por `cardKey: String` (`builtin_x` | `user_<id>`), funciona para as duas populações. **Leitner-lite**: box 0..4, intervalos 0/1/3/7/14 dias; lembrou → box+1; esqueceu → box 0, due agora. Clock injetado (`nowMs: () -> Long`) para teste determinístico.
- **`dueAt` como INTEGER epoch-ms** (não ISO TEXT — ordenação lexicográfica de `Instant.toString()` é traiçoeira; tabelas MKIS v18 já usam INTEGER). `createdAt/updatedAt` de conteúdo ficam ISO TEXT como agenda/crm.
- **tenantId em ambas as tabelas** (convenção `AppointmentEntity`: campo obrigatório sem default + índice; queries do DAO recebem tenantId explícito). Sem colunas de sync no v1 (flashcards não têm modelo no backend).
- **Fila de estudo é SNAPSHOT, não reativa**: `observeDeck()` re-emite a cada review; se a fila fosse derivada reativamente, o deck se reordenaria debaixo do dedo da médica a cada resposta. Só `deck`/`dueCount` são reativos; a fila rebuilda em troca de categoria/Reiniciar/CRUD.
- **Desvio deliberado de convenção**: `FlashcardRepositoryImpl` recebe `tenantId: () -> Long` em vez de `TenantManager` (que é classe concreta sobre `SecurePreferences`, impossível de instanciar em teste JVM puro — o padrão de teste do projeto é FakeDao sem Robolectric). O AppContainer passa `{ tenantManager.requireTenantId() }` — comportamento idêntico, resolvido por chamada (não cacheado; troca de tenant não vaza deck).

## Arquivos (ordem de dependência)

1. **`core/util/MarkdownSections.kt`** (novo) — splitter de seções Markdown, única fonte da regex `Regex("(?m)^#{1,3} ")` hoje duplicada como `SECTION_HEADING` privada em `MtcRetriever`. `split()` idêntico byte-a-byte à cadeia histórica (`split → trim → filter`), + `titleOf()`/`bodyOf()`.
2. **`biblioteca/domain/search/MtcRetriever.kt`** (edit mínimo) — `extractBestSection` delega para `MarkdownSections.split`; apaga a `SECTION_HEADING` privada. **Nada mais muda** — `retrieve()` e o gate R2 intocados (cobertos por `AskLibraryUseCaseTest` + teste sagrado).
3. **`educacao/domain/model/FlashcardModels.kt`** (novo) — `Flashcard(key, front, back, category, builtin, sourceArticleId, sourceSection, userRowId)`, `CardProgress(cardKey, box, dueAtEpochMs, lastReviewedAtEpochMs, totalReviews, totalLapses)`, `StudyCard(card, progress?)` com `isDue(nowMs)`.
4. **`educacao/domain/srs/LeitnerScheduler.kt`** (novo) — objeto puro: `onRemembered(prev?, key, nowMs)`, `onForgot(...)`; `INTERVAL_DAYS = [0,1,3,7,14]`.
5. **`educacao/data/BuiltinFlashcards.kt`** (novo) — os 12 cards movidos verbatim da tela, `builtin=true`, keys estáveis; campo `dificuldade` morre (era "médio" em todos; o box do SRS é o sinal honesto). KDoc: conteúdo revisado por humano, R4, novas entradas só por edição manual.
6. **`educacao/data/local/FlashcardEntity.kt`** (novo; 2 entities no mesmo arquivo) + **`FlashcardDao.kt`** (1 DAO p/ 2 tabelas, precedente `ExameDao`) + **`FlashcardMapper.kt`** (`toDomain`/`toEntity`; `key = "user_$id"`).
7. **`data/local/database/AppDatabase.kt`** — +2 entities no array, `version = 18 → 19`, `abstract fun flashcardDao()`, KDoc do histórico.
8. **`di/DatabaseModule.kt`** — `DB_VERSION = 19`, `MIGRATION_18_19` inline (DDL abaixo), registrada em `buildMigrations()`.
9. **`educacao/domain/repository/FlashcardRepository.kt`** (interface) + **`educacao/data/repository/FlashcardRepositoryImpl.kt`** — `observeDeck()` = `combine(cardsFlow, progressFlow)` com builtins prepended no transform (builtins são lista estática — NÃO embrulhar em `flowOf`/combine triplo); `saveCard` recusa builtin, carimba tenant+timestamps; `deleteCard` apaga progresso junto (sem FK de propósito — builtin não tem linha-mãe); `recordReview` = read-modify-write preservando o id da linha (REPLACE no índice único trocaria o autoincrement).
10. **`educacao/presentation/FlashcardsViewModel.kt`** (novo; state+VM+factory num arquivo, precedente Agenda) — estado: `deck` (reativo), `queue` (snapshot due-first: box asc, dueAt asc; depois não-vencidos), `dueCount`, tallies de sessão, `editor: Flashcard?`, `createFromArticle: CreateFromArticleState?`, `message` (snackbar informativo — falha de save de progresso **nunca** bloqueia a sessão). Factory recebe `(repository, sourceArticles: suspend () -> List<MtcArticle>)` — lambda limita a dependência educacao→biblioteca ao modelo.
11. **`di/AppContainer.kt`** — `flashcardDao`/`flashcardRepository`/`flashcardsViewModelFactory` by lazy; `sourceArticles = { MtcKnowledgeBase.articles + libraryStagingRepository.approvedArticles() }` (união baseline+aprovados, mesmo padrão do FtsSearchService; `libraryStagingRepository` já existe em AppContainer.kt:268).
12. **`ui/screens/FlashcardsScreen.kt`** (rewrite in place; rota/NavHost intocados) — mantém FlipCard e chips de categoria; adiciona badge "X para revisar hoje", badge de box por card, botões Lembrei/Não lembrei → `vm.onAnswer`, FAB/menu "Novo card" e "Criar de artigo" (picker 2 passos: artigo → seção → editor pré-preenchido `front=titleOf(seção)`, `back=bodyOf(seção)`, categoria via `runCatching { MtcCategory.valueOf(...).label }` com fallback pro raw — "codec degrada, não quebra"), editar/apagar só em cards da médica (builtin com rótulo "fixo"), estados vazios distintos ("nenhum card" ≠ "nenhum vencido — próxima revisão <data>" com "Estudar mesmo assim"), fim de fila → resumo de sessão (não wrap-around silencioso). Alvos ≥44dp (corrigir o back de 32dp).

## DDL da MIGRATION_18_19 (aditiva; ordem de colunas = ordem dos campos; nomes de índice = derivados do Room; **sem `DEFAULT` no SQL** — regra documentada na v18, entities usam default Kotlin)

```sql
CREATE TABLE IF NOT EXISTS `flashcards` (
  `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  `tenantId` INTEGER NOT NULL, `front` TEXT NOT NULL, `back` TEXT NOT NULL,
  `category` TEXT NOT NULL, `sourceArticleId` TEXT NOT NULL, `sourceSection` TEXT NOT NULL,
  `createdAt` TEXT NOT NULL, `updatedAt` TEXT NOT NULL);
CREATE INDEX IF NOT EXISTS `index_flashcards_tenantId` ON `flashcards` (`tenantId`);
CREATE TABLE IF NOT EXISTS `flashcard_progress` (
  `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  `tenantId` INTEGER NOT NULL, `cardKey` TEXT NOT NULL, `box` INTEGER NOT NULL,
  `dueAtEpochMs` INTEGER NOT NULL, `lastReviewedAtEpochMs` INTEGER NOT NULL,
  `totalReviews` INTEGER NOT NULL, `totalLapses` INTEGER NOT NULL);
CREATE UNIQUE INDEX IF NOT EXISTS `index_flashcard_progress_tenantId_cardKey` ON `flashcard_progress` (`tenantId`, `cardKey`);
CREATE INDEX IF NOT EXISTS `index_flashcard_progress_tenantId_dueAtEpochMs` ON `flashcard_progress` (`tenantId`, `dueAtEpochMs`);
```

## Testes (padrões do projeto)

| Arquivo | Padrão | Garante |
|---|---|---|
| `core/util/MarkdownSectionsTest.kt` | JUnit puro | split em `#/##/###` (não `####`), texto pré-heading vira seção, comportamento idêntico à cadeia histórica do MtcRetriever |
| `educacao/domain/srs/LeitnerSchedulerTest.kt` | JUnit puro, nowMs fixo | box+1 com teto 4, intervalos exatos {1,3,7,14}d; esqueceu → box 0/due agora/lapso+1; determinismo |
| `educacao/data/BuiltinFlashcardsTest.kt` | JUnit puro | 12 cards, keys únicas prefixadas `builtin_`, campos não-vazios (protege namespace de key do LazyColumn) |
| `educacao/data/repository/FlashcardRepositoryTest.kt` | FakeFlashcardDao (estilo SupremoViewModelTest) | união 12+N com progresso por key; recordReview preserva id da linha; review de builtin cria progresso; saveCard recusa builtin; deleteCard apaga progresso; tenantId carimbado; exceção do DAO → `Result.Error`, nunca throw |
| `educacao/presentation/FlashcardsViewModelTest.kt` | FakeDao + repo real + VM real, StandardTestDispatcher | ordenação due-first; resposta persiste e avança; **fila não reordena mid-sessão**; dueCount cai reativo; estados vazio-vs-nada-vencido distinguíveis; prefill do criar-de-artigo correto e **nada salvo antes do confirmar** |
| `data/FlashcardsMigrationTest.kt` | Robolectric + SQLite real (estilo SyncIdentityMigrationTest) | tabelas existem pós-migração; insert ok; índice único (tenantId,cardKey) rejeita duplicata e aceita mesma key em tenant diferente |

## Gotchas confirmados no código

- `DB_VERSION` vive em DOIS lugares (AppDatabase.kt:60 e DatabaseModule.kt:24) — ambos → 19.
- Copiar o estilo da MIGRATION_17_18 (sem DEFAULT no SQL), NÃO o da MIGRATION_8_9.
- R2 na vizinhança: em `MtcRetriever.kt` só a delegação em `extractBestSection`; testes sagrados intocados.
- `MtcCategory.valueOf` pode lançar em dado legado — `runCatching` com fallback.

## Verificação

1. Gate por etapa: `./gradlew testDebugUnitTest` (116 atuais + novos, 0 falhas; 8 sagrados intactos).
2. Final: `./gradlew assembleDebug` + `compileDebugKotlin` sem warning novo.
3. Migração coberta por teste Robolectric (device continua indisponível — v18→v19 real em aparelho fica para a próxima sessão com device, como o resto).
