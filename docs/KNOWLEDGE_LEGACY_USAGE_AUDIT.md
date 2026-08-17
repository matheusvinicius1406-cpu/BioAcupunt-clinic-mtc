# Auditoria de usos legados — Fase 1B

Classificação baseada na busca realizada em Android, backend e web em 2026-08-17.

| Área/ocorrência | Classificação | Decisão |
|---|---|---|
| `BibliotecaNodeEntity`, `BibliotecaDao`, `BibliotecaRepositoryImpl` | `LEGACY_ALLOWED` | Mantidos durante a transição; adapter canônico disponível |
| `KnowledgeNodeEntity`, `KnowledgeNodeDao` | `LEGACY_ALLOWED` | Mantidos para MKIS e LGPD; não apagar |
| `PipelineService` e `IngestionJob` MKIS | `COMPATIBILITY_ONLY` | Pipeline atual continua escrevendo legado até haver importação validada |
| `VecKnowledgeNodeRepository` / sqlite-vec | `MIGRATE_LATER` | Não trocar nesta fase; futura busca canônica poderá reutilizar índice |
| `HybridSearchService` | `MIGRATE_NOW` | Deve ser encapsulado por resultado canônico antes de nova funcionalidade |
| `MtcRetriever`, `MtcSearchEngine`, `FtsSearchService` | `MIGRATE_LATER` | Preservar gate de evidência; adaptar depois da migração piloto |
| `BibliotecaViewModel` e `BibliotecaScreen` | `MIGRATE_NOW` | Selecionar como tela piloto; ainda não redirecionada nesta etapa bloqueada |
| `MkisDetailSheet`, `PipelineMonitor*` | `COMPATIBILITY_ONLY` | Interfaces específicas de curadoria/monitoramento |
| `ClinicalSynthesisUseCase`, `AskLibraryUseCase`, `UnifiedAiChatViewModel` | `MIGRATE_LATER` | Não alterar IA antes da busca canônica validada |
| `AppContainer` e DAOs legados | `LEGACY_ALLOWED` | Registram compatibilidade existente |
| `backend/library` | `MIGRATE_NOW` | Adapter usado pelo endpoint canônico inicial |
| `mkis-backend` removido no worktree | `COMPATIBILITY_ONLY` | Não restaurar nem copiar; tratar histórico separado |
| `web/biblioteca` | `MIGRATE_LATER` | Continua consumindo API antiga até contrato remoto completo |

Nenhuma ocorrência foi substituída automaticamente. A tabela `knowledge_core_*` e `KnowledgeRepository` são a nova fronteira para código novo.
