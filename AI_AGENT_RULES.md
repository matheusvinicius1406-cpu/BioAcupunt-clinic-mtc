AI Agent Rules: Proteção de arquivos críticos de build
====================================================

Resumo
-----
Este repositório contém arquivos e configurações críticos para o processo de build (Android/Gradle). Para evitar que agentes automatizados ou scripts alterem acidentalmente esses arquivos e quebrem o build, siga as regras abaixo.

Regras para agentes de IA
-------------------------
- Não modifique diretamente os arquivos listados na seção "Arquivos protegidos".
- Qualquer alteração proposta a esses arquivos deve ser feita via Pull Request (PR) e revisada por um responsável pelo repositório.
- Alterações de emergência podem ser feitas apenas por mantenedores humanos e documentadas no PR com justificativa técnica clara.

Arquivos protegidos (exemplos)
------------------------------
- app/ (todo o módulo Android)
- build.gradle.kts
- settings.gradle.kts
- gradle.properties
- gradle/ (wrapper e versões)
- local.defaults.properties
- local.properties
- .github/workflows/ (workflows de CI)
- backend/ (serviços e scripts backend que afetam build)

Como validar alterações
-----------------------
- Antes de abrir um PR, execute o build localmente: ./gradlew assembleRelease.
- Use o script scripts/verify_protected_changes.sh para checar se arquivos protegidos foram alterados.
- A CI roda .github/workflows/protect-build-files.yml em PRs e bloqueará merges que alterem arquivos protegidos sem revisão.

Responsabilidade humana
----------------------
Mantenedores: @matheusvinicius1406-cpu

Notas
-----
Estas regras são orientativas e servem para proteger a integridade do processo de build. Se precisar ajustar a lista de arquivos protegidos, abra um PR e solicite revisão explícita.
