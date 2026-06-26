# ⚕ BioAcupunt Supremo — AUDITORIA FASE 1
**Data:** 2026-06-26  
**Escopo:** Estrutura, dependências, build e arquitetura  
**Referência oficial de build:** GitHub Actions (runner ubuntu-latest)  

---

## 1. Estrutura do Projeto

### ✅ Arquivos presentes
- `settings.gradle.kts` — presente, válido, inclui `:app`
- `app/build.gradle.kts` — presente
- `build.gradle.kts` — presente
- `gradle.properties` — presente
- `gradle/libs.versions.toml` — presente com catálogo completo
- `app/src/main/AndroidManifest.xml` — válido, tem `INTERNET`
- Pacote principal: `com.bioacupunt`
- 43 arquivos Kotlin em `app/src/main`

### ❌ Arquivos ausentes
- `gradlew` — **AUSENTE** (bloqueia GitHub Actions)
- `gradlew.bat` — ausente
- `gradle/wrapper/gradle-wrapper.jar` — ausente

### ⚠️ Arquivos órfãos / inconsistências de pacote
- `app/src/main/java/com/bioacupunt/agents/` — pacote não referenciado
- `app/src/main/java/com/bioacupunt/engines/` — não referenciado
- `app/src/main/java/com/bioacupunt/evolution/` — não referenciado
- `app/src/main/java/com/bioacupunt/workers/` — não referenciado
  - Nenhum Worker declarado no `AndroidManifest.xml`

---

## 2. Dependências

### ✅ Dentro do padrão
- Android Gradle Plugin 9.1.1
- Kotlin 2.2.10
- Jetpack Compose BOM 2024.09.00
- Room 2.7.0
- Retrofit 2.12.0 + Moshi
- WorkManager (ok via `androidx.work:work-runtime-ktx`)
- Navigation Compose 2.8.9
- Coroutines 1.10.2

### ❌ Dependências removidas do build, mas ainda no catálogo
- `hilt` 2.51.1 — removida de `app/build.gradle.kts` e `build.gradle.kts`, **mas**:
  - `libs.versions.toml` ainda declara `hilt`, `hilt-android`, `hilt-compiler`, plugin `hilt`
  - `PatientRepositoryImpl.kt:16` ainda tem `@Inject constructor(...)`
  - `PatientsViewModel.kt` injeta via factory manual, **ok**
  - `PatientDataModule.kt` assume injeção manual, **ok**

### ❌ Duplicação de SyncQueueDao (CRÍTICO)
Existem **três** definições de DAO com nomes iguais e duas instâncias de banco separadas:

| Arquivo | Status |
|---|---|
| `com.bioacupunt.sync.data.local.SyncQueueDao` | ✅ Usado pelo código |
| `com.bioacupunt.sync.SyncQueueDao` | ❌ Duplicata morta |
| `com.bioacupunt.sync.data.local.SyncQueueDatabase` | ❌ Instância de BD separada, nunca usada |

> **Risco:** a duplicata `SyncQueueDao` gera erro de tipo com a mesma assinatura. Não será compilável em Kotlin.

### ❌ Referência quebrada em NetworkModule
- `NetworkModule.kt` faz `import com.bioacupunt.data.remote.BioAcupuntApi`
- **Essa interface não existe no projeto** — só existe `PatientApi`
- Causa: `kotlin.Unresolved reference: BioAcupuntApi`

---

## 3. DI / Injeção (pós-remoção Hilt atual)

### ✅ Convertido para manual
- `PatientsViewModel` — usa `ViewModelProvider.Factory` customizada
- `PatientDataModule` — objeto singleton com métodos `provide*`

### ❌ Incompleto: `PatientRepositoryImpl` depende de 3 parâmetros
`PatientRepositoryImpl(api: PatientApi, db: AppDatabase, scheduler: SyncScheduler)`  
`PatientDataModule.providePatientRepository(api, db)` passa apenas 2.
- `SyncScheduler` é construído no `PatientsViewModel.kt` com `localContext.current` — **funcióna**, mas o repositório fica sem `scheduler` injetado.

> **Risco:** se `PatientRepositoryImpl` chamar `scheduler.scheduleSync()` e o scheduler não estiver injetado/recebido, Runtime crash.

### ❌ Worker registration ausente
- `SyncWorker` não registrado em `AndroidManifest.xml`
- WorkManager pode não inicializar corretamente

---

## 4. Pacotes vs estrutura documentada

Estrutura documentada (Master Blueprint) espera pacotes em `feature/`, `di/`, `worker/` etc.  
Estrutura atual:

```
com.bioacupunt
  ├── agents/        (órfão)
  ├── data
  │   ├── local/database
  │   ├── remote
  │   └── repository
  ├── di
  ├── engines/       (órfão)
  ├── evolution/     (órfão)
  ├── patient
  │   ├── data/local
  │   ├── data/repository
  │   ├── di
  │   ├── domain/{model,repository,usecase}
  │   └── presentation
  ├── sync
  │   ├── data/local
  │   └── (duplicatas)
  ├── ui
  │   ├── navigation
  │   ├── screens
  │   └── theme
  └── workers/       (órfão)
```

Pacotes padrão vs esperado pela arquitetura:  
- `feature/` não existe como raiz de módulo  
- `worker/` não existe  
- `sync/` mistura worker code + data layer  
- DI espalhado: `di/` (raiz) + `patient/di/` + `sync/data/local/`  

---

## 5. Compilação / GitHub Actions

### Bloqueio direto
- **`gradlew` inexistente** → passo `chmod +x gradlew` e `./gradlew clean assembleDebug` falham no CI

### Riscos de build (potenciais)
1. `BioAcupuntApi` não resolvido no `NetworkModule.kt`
2. ` SyncQueueDao` duplicado (conflito de nomes/types)
3. `PatientRepositoryImpl` + `PatientsViewModel` podem ter assinatura de factory desalinhada
4. `SyncWorker` sem registration
5. `libs.versions.toml` com entradas Hilt que não são mais usadas (não causa erro direto, mas sinal de dívida)
6. `gradle.properties` com `org.gradle.configuration-cache=true` — incompatível com AGP 9.x em alguns runners; CI pode falhar com warning/error

### Build files que precisam conferência
- `app/build.gradle.kts:90-91` e `:122` — Hilt comentado, mas `libs.versions.toml` mantem o alias
- `app/build.gradle.kts:51-53` — `compileSdk`/`targetSdk = 36`, `JavaVersion.VERSION_11`
  - AGP 9.x requer **Java 17** no compile/target do AGP; `compileOptions` 11 é ok, mas o **runner** deve ter JDK 17 instalado pelo setup-java (já configurado no workflow)

---

## 6. Workflow CI atual

**Arquivo:** `.github/workflows/android-build.yml`  
**Status:** Estrutura correta, mas **não pode rodar** sem `gradlew` no repositório.

Steps existentes:
1. Checkout ✅
2. Setup JDK 17 ✅
3. Setup Android SDK ✅
4. `chmod +x gradlew` ❌ falha se gradlew não existe
5. `./gradlew clean assembleDebug` ❌ falha se gradlew não existe
6. Upload APK ❌ nunca executa

---

## 7. Sumário de Bloqueios e Riscos

| # | Item | Severidade | Fase |
|---|---|---|---|
| 1 | `gradlew` ausente | 🔴 Bloqueante CI | 3/4 |
| 2 | `BioAcupuntApi` não existe em `NetworkModule.kt` | 🔴 Build-error | 3 |
| 3 | `SyncQueueDao` duplicado em dois packages | 🔴 Build-error | 3 |
| 4 | `SyncWorker` não em `AndroidManifest.xml` | 🟡 Runtime | 5 |
| 5 | `PatientRepositoryImpl` + `PatientDataModule` incompletos (SyncScheduler) | 🟡 Runtime | 5 |
| 6 | Pacotes órfãos (`agents/`, `engines/`, `evolution/`, `workers/`) | 🟡 Manutenibilidade | 2 |
| 7 | `libs.versions.toml` com Hilt morto | 🟡 Dívida | 2 |
| 8 | `gradlew.bat` ausente | 🟡 DevOps | 3 |
| 9 | `gradle.properties` com `configuration-cache=true` | 🟡 CI | 4 |
| 10 | Duas BD Room distintas (`AppDatabase` + `SyncQueueDatabase`) | 🟡 Runtime | 5 |

---

## 8. Decisões de Projeto

- **Fonte da verdade priorizada para build:** GitHub Actions, não ambiente local  
- **Segmentation fault:** documentado como ambiente local; não afeta CI (GitHub Actions usa Linux)  
- **Hilt removido:** decisão tomada para evitar dependência de toolchain KSP no CI  
- **Estratégia definitiva:** gerar `gradlew` no repositório (ou usar Gradle instalado no CI diretamente), corrigir referências quebradas, então validar via Actions

---

## 9. Próximas Ações (ordem de execução)

1. Resolver referência quebrada em `NetworkModule.kt` (`BioAcupuntApi` → `PatientApi` ou remover módulo)
2. Eliminar duplicata `com.bioacupunt.sync.SyncQueueDao` e `SyncQueueDatabase`
3. Alinhar `PatientDataModule.providePatientRepository` com `PatientRepositoryImpl` real
4. Registrar `SyncWorker` no `AndroidManifest.xml`
5. Remover entradas Hilt mortas do `libs.versions.toml`
6. Gerar `gradlew` + `gradlew.bat` ou adaptar workflow para instalar Gradle no CI
7. Reavaliar `gradle.properties` para compatibilidade com AGP no CI
8. Rodar verificação estrutural e commitar separadamente
9. Validar GitHub Actions com push do workflow atualizado
10. Verificar artifact `app-debug.apk` gerado

---

*Relatório gerado por HERMES — Fase 1 — Sem alterações de código realizadas.*
