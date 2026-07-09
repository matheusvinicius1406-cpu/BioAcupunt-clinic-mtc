# MKIS — Executive Summary

Matriz de maturidade estimada: 90% (53/59 checks)

---

## Pontos fortes

- Especificação abrangente em 22 documentos com escopo e fronteiras claras (Android desacoplado).
- Domínio científico bem modelado: KnowledgeNode/KnowledgeArtifact com invariantes, políticas e versionamento imutável.
- Decisões arquiteturais explícitas (hexagonal, filas, eventos, pgvector) com trade-offs documentados.
- Segurança e conformidade contempladas em profundidade (LGPD, ABAC, SSRF, XXE, prompt injection, retenção).
- Busca híbrida, grafo, IA e observabilidade projetadas com separação de contextos.

## Fragilidades

- Nomenclatura canônica não 100% propagada: termos como SearchContext x Search Engine precisam ser uniformizados.
- Falta enum fechado para campos-chave (knowledge_type, status, evidence_level, bias_risk).
- Ausência de vendor/tecnologia escolhida para antivírus, OCR e LLM/embeddings.
- Falta exemplos request/response por endpoint nos contratos públicos.
- Sem política concreta de migração/rollback do índice vetorial e reindexação online.

## Riscos críticos

1. Prompt injection em documentos importados sem sandbox/rules executáveis. Ação: definir scanner/allowlists/review humana.
2. Custo incontrolado de embeddings/LLM por ausência de budget/limits por tenant. Ação: política de custo por canal e fallback.
3. LGPD/deep delete não detalhado em cascata; risco de remoção parcial. Ação: matriz de retenção + job de purge assinado.
4. APIs científicas externas indisponíveis sem fallback completo para busca lexical. Ação: cache expandido e modo degraded documentado.

## Decisões arquiteturais mais importantes

- Backend dedicado desacoplado do Android, comunicação só por contratos públicos.
- PostgreSQL + pgvector como gate de verdade único.
- Event sourcing/outbox como contrato primário entre contextos.
- Versionamento imutável de conhecimento (nunca overwrite).
- Busca híbrida obrigatória; lexical como fallback.
- Observabilidade e segurança from day one.

## Recomendação para início da implementação

Iniciar MVP pela base técnica (Fase 2) em backend próprio separado:
1) scaffolding, CI, migrations, RLS, auth;
2) domínio core com eventos e contratos;
3) índice relacional+vetorial e busca híbrida simples;
4) ingestão manual com parser genérico e embeddings;
5) piloto fechado 1 tenant com métricas e feature flags.
Bloquear início até definir vendor/stacks faltantes (LLM, antivírus, OCR, Redis/broker).
