# ADR 1 — Modular Monolith como arquitetura inicial

## Status
Aceito

## Contexto
Sistema nasce com poucos domínios, equipe pequena e necessidade de deploy simples.

## Decisão
Adotar monólito modular com modules c boundaries claras e preparação para extração futura.

## Alternativas
- Microsserviços desde o início: custo operacional alto, overengineering.
- Monolito tradicional sem modules: acoplamento alto, difícil evoluir.

## Riscos
- Crescimento desordenado se boundaries não forem respeitados.
- Migração para serviços no futuro exige disciplina.

## Mitigações
- Regras de módulo por bounded context.
- Documentação ADR obrigatória para mudanças estruturais.
