# PENDENCIAS_BUILD.md

## STATUS DO BUILD (atualizado em 2026-07-11)

- `assembleDebug` e `compileDebugKotlin`: funcionam localmente, sem erros.
- `assembleRelease` (R8 + `lintVitalRelease`): compila limpo (`compileReleaseKotlin`
  sem erros), mas a etapa de R8/lint trava localmente nesta máquina de
  desenvolvimento por falta de RAM (4GB) — não é um bug de código. Validado com
  sucesso no GitHub Actions (runners com 7GB de RAM), que roda `assembleRelease`
  a cada push/PR para `main` (`.github/workflows/android-build.yml`).
- CI (`android-build.yml`) está verde: `assembleDebug` e `assembleRelease` passam.

## HISTÓRICO

O bloqueio original (trava de cache/lock do Gradle no Windows) foi superado.
Causas raiz reais encontradas e corrigidas desde então:

- `local.properties` estava versionado com um `sdk.dir` do Windows, quebrando
  o runner Ubuntu do CI (corrigido: arquivo removido do controle de versão).
- `buildToolsVersion` divergia da versão instalada pelo workflow do CI.
- Aceite de licenças do Android SDK no `android-actions/setup-android@v3`
  travava em prompts interativos (corrigido: aceite explícito via `yes |`).

## PRÓXIMA AÇÃO

- Para validar `assembleRelease` completo (R8/lint), usar o GitHub Actions —
  não insistir localmente nesta máquina.
- Se a máquina de desenvolvimento ganhar mais RAM, revalidar localmente.
