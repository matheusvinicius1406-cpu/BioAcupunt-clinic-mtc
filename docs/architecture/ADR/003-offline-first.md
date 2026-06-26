# ADR 3 — offline-first obrigatório com Room + ChangeSet

## Status
Aceito

## Contexto
Profissionais de MTC atendem em locais com conectividade instável.

## Decisão
Room local com fila de ChangeSet; sync eventual idempotente.

## Alternativas
- Apenas online: risco de perda de dados e parada de operação.
- Cache simples sem ChangeSet: resolução de conflitos frágil.

## Riscos
- Complexidade alta de merge/sync.
- Custo de storage local.

## Mitigações
- Estratégias por tipo de entidade.
- Testes de conflito e replay.
