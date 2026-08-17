# Plano de criptografia do banco clínico local

## Estado atual

O Room usa `bioacupunt_db` com backup de pré-migração, mas a auditoria anterior confirmou que o arquivo local ainda não é criptografado em repouso. Tokens e preferências usam armazenamento cifrado/Keystore; isso não protege o arquivo Room.

## Plano seguro

1. Inventariar todas as entidades clínicas e arquivos sidecar (`-wal`, `-shm`, backups).
2. Introduzir uma chave por instalação gerada e protegida pelo Android Keystore.
3. Usar uma biblioteca de SQLite cifrado compatível com Room ou uma migração controlada para arquivo cifrado; validar suporte a FTS/vec antes de escolher.
4. Migrar em cópia transacional, validar contagens/checksums e manter backup recuperável.
5. Cifrar também exportações e backups; nunca registrar chave ou dados clínicos em logs.
6. Testar reinstalação, rotação/invalidade de chave, restore e deep delete.

Nenhuma migração destrutiva foi executada nesta fase. A escolha da biblioteca e a migração ficam bloqueadas até validar compatibilidade com as extensões FTS/vec existentes.
