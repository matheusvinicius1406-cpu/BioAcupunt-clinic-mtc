# Legacy — Android SyncWorker e SyncManager

## Por que estes arquivos estão aqui
O `SyncWorker.kt` e o ecossistema de sincronização fazem parte da **arquitetura futura** (Offline First + WorkManager). A implementação atual, porém, referenciava classes inexistentes (`AppContainer`) e não possuía lógica real de merge/resolução de conflitos.

## Motivo da remoção do ciclo principal
Código morto técnico: stub sem valor executável e sem integração com o repositório de dados atual.

## Quando revisitar
- Quando `SyncManager` funcional estiver implementado em `app/src/main/java/com/bioacupunt/...`
- Quando `AppContainer` for substituído por DI real (Hilt)
- Na **Fase 1 — Estabilização Técnica**

Data: 2026-06-26
