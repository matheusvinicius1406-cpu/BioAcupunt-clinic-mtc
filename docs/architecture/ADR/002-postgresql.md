# ADR 2 — PostgreSQL como fonte primária de dados

## Status
Aceito

## Contexto
Dados clínicos exigem ACID, relacionamentos e auditoria.

## Decisão
PostgreSQL como fonte oficial; Qdrant apenas para vetores.

## Alternativas
- MongoDB: menos maturidade transacional, dificulta auditoria forte.
- SQLite/outro embedded: inviável multi-tenant/SaaS.

## Riscos
- Escala multi-tenant exige particionamento e monitoramento.

## Mitigações
- Índices e particionamento planejados.
- Read replica + failover.
