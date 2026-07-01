# BioAcupunt Supremo — implementação v15
Implementação manual dos módulos Clean MVVM + Room + DI manual.

- Fundação: `core/util`, `core/domain`, `di/AppContainer`, `data/local/database`
- Módulos prontos: `crm`, `agenda`, `financeiro`, `prontuario`, `biblioteca`, `relatorios`
- IA: `GeminiEngine` + `AiAgents` + `AiAssistantScreen`
- Placeholders: infra externa não implementada, apenas portas/interfaces + UI documentada com `// TODO: Configurar produção`
- Build/Gerador final
  - Local bloqueado por ambiente Windows/Gradle lock (2/2 tentativas).
  - Este artefato foi validado com verificação estática ad-hoc e inspeção arquitetural.
  - Use GitHub Actions como verdade de build.
