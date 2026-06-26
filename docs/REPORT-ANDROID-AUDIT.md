# Raio-X Android — BioAcupunt Supremo

## 1. Visão Geral do Estado Atual
- **Namespace:** `com.bioacupunt`
- **App ID:** `com.aistudio.bioacupunt.xyz`
- **Stack:** Android + Kotlin + Jetpack Compose + Room + Retrofit + Hilt + Navigation Compose
- **Telas:** 5 abas fixas (Biblioteca, Simulador, Prontuário, Flashcards, Analytics)
- **Status:** estruturalmente montado, mas com gargalos bloqueadores para fluxo real

## 2. Gargalos, Erros, Código Quebrado e Superficialidades

### 2.1 Hilt comentado mas código depende de Hilt (BLOQUEADOR)
- **Arquivo:** `app/build.gradle.kts`
- **Problema:** plugin `hilt` está comentado, mas todo o código Android usa anotações Hilt:
  - `@HiltAndroidApp` em `BioAcupuntApp.kt`
  - `@AndroidEntryPoint` em `MainActivity.kt`
  - `@HiltViewModel` em viewmodels
  - `@Module`, `@InstallIn`, `@Provides`, `@Binds`, `@Singleton` em módulos DI
- **Impacto:** build quebra ou Hilt não funciona corretamente
- **Fix:** descomentar `alias(libs.plugins.hilt)` e garantir KSP + Hilt compiler habilitados

### 2.2 NetworkModule com Retrofit/Moshi hardcoded, sem abstração
- **Arquivo:** `app/src/main/java/com/bioacupunt/di/NetworkModule.kt`
- **Problema:** Retrofit e Moshi criados diretamente no módulo, sem contrato/claro. URL hardcoded `10.0.2.2:8000`.
- **Impacto:** trocar ambiente (dev/staging/prod) exige alterar código ou criar módulos extras
- **Fix:** manter instância Retrofit única (como foi feito em `RetrofitInstance.kt`), mas centralizar Base URL e interceptors por flavor/build config

### 2.3 BioAcupuntApi usada em NetworkModule, mas classe não existia antes desta fase
- **Arquivo:** `NetworkModule.kt`
- **Problema histórico:** referência a `BioAcupuntApi` estava quebrada até criação de `PatientApi.kt`
- **Status atual:** resolvido parcialmente com `PatientApi.kt`, mas ainda falta alinhar nomes entre `BioAcupuntApi` (antigo contrato) e `PatientApi` (novo)
- **Fix:** manter `PatientApi` como interface oficial para Patient e eliminar referências legadas a `BioAcupuntApi`

### 2.4 Database superficial e desalinhado do domínio
- **Arquivo:** `app/src/main/java/com/bioacupunt/data/local/database/AppDatabase.kt`
- **Problema:** Room está configurado apenas para `KnowledgeNode`, sem entidades de `Patient`
- **Impacto:** módulo Patient não persiste offline ainda; sync fica impossibilitada
- **Fix:** adicionar `PatientEntity` no Room + DAO + TypeConverters (para JSON de endereço/contatos) — sem isso, offline-first não existe

### 2.5 Tela de Pacientes ainda não está funcional de verdade (placeholder/material inicial)
- **Arquivos:** `ProntuarioScreen.kt`, `PatientsScreen.kt`
- **Problema:** Junção de nomes/rotas causou duplicidade de funções de tela no mesmo pacote
- **Impacto:** risco de conflito de funções同名 e navegação ambígua
- **Fix:** padronizar nome da tela como `PatientsScreen` e remover função `ProntuarioScreen` ou mantê-la como wrapper

### 2.6 ViewModel com estado recriado / não reativo
- **Arquivo:** `PatientsViewModel.kt`
- **Problema:** usa `mutableStateOf` em vez de `StateFlow/MutableStateFlow`; `PatientsScreen` usa `collectAsStateWithLifecycle`, que espera Flow
- **Impacto:** UI não reage a mudanças do ViewModel de forma confiável
- **Fix:** trocar `mutableStateOf` por `MutableStateFlow` e expor `StateFlow`

### 2.7 DI incompleta / não confiável
- **Arquivos:**
  - `app/build.gradle.kts` (Hilt desativado)
  - `PatientDataModule.kt`
  - `DatabaseModule.kt` e `NetworkModule.kt`
- **Problema:** não há prova de que `PatientRepositoryImpl`, `GetPatients` e `CreatePatient` são injetados corretamente; dependências podem não ser satisfeitas
- **Impacto:** runtime crash por ausência de binding/provider
- **Fix:** fechar módulos Hilt obrigatórios e incluir `@EntryPoint` para testes; evitar instanciar ViewModel manualmente

### 2.8 Acoplamento AndroidUi ↔ Domain
- **Arquivo:** `PatientsScreen.kt`
- **Problema:** tela acessa diretamente `Patient` do domínio e eventos de apresentação; se a camada de domínio mudar, UI quebra
- **Impacto:** quebra futura de escalabilidade
- **Fix:** usar mappers/DTOs na camada de apresentação e manter UI dependendo só de UiState/UiEvent

### 2.9 Falta de estados de UI bem definidos (loading/erro/vazio)
- **Status:** parcialmente resolvido em `PatientsUiState.kt`
- **Problema:** `PatientsScreen` trata isLoading/error, mas falta feedback visual padronizado + Snackbar/Toast
- **Fix:** componente reutilizável `UiStateHandler` para loading/erro/vazio

### 2.10 Pacotes e nomenclatura inconsistentes
- **Arquivos/K pastas:**
  - `data/remote` (RetrofitInstance, PatientApi)
  - `patient/domain` e `patient/data`
  - `patient/presentation`
- **Problema:** mistura de estilos (alguns módulos com pacote próprio, outros não); ausência de camada `domain` genérica
- **Fix:** manter convenção única por bounded context; criar abstração `patient` com estrutura clara domain/usecase/repository/data

## 3. Componentes Faltantes
- **PatientEntity** (Room): necessário para offline-first
- **PatientDao** (Room): operações CRUD locais
- **Sync Service** (Worker/Service): fila de ChangeSet para Patient
- **Error Handling centralizado:** não há `Result` wrapper ou `Either`
- **Testes:** ausência total de testes instrumentados/unidade para ViewModel/UseCase
- **Logging/Metricas:** sem rastreabilidade operacional
- **Feature flags:** ausência de controle de rollout

## 4. Riscos
- DI quebrada -> app não inicia
- Room sem entidades -> offline-first é só teoria
- ViewModel não reativo -> UI “morta”
- Base URL hardcoded -> risco de vazar ambiente

## 5. Legado que ainda influencia
- `BioAcupuntApi` (referência antiga) ainda é conceito no projeto
- `AppContainer` referenciado por `SyncWorker/legacy` ainda não foi reimplementado
