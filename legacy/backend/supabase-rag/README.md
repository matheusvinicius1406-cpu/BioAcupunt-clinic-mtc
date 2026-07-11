# Legacy — Backend Python

## Por que estes arquivos estão aqui
Arquivos preservados por decisão arquitetural, não utilizados na implementação atual.

### Celery + Workers
Jobs assíncronos (OCR, embeddings, indexação vetorial) fazem parte da arquitetura.
Implementação atual depende de infraestrutura não provisionada.
Será recriada quando Redis + Brokers estiverem operacionais.

### Serviços vetoriais (Qdrant + Embeddings)
Search híbrida/RAG usa vetor database e embeddings.
Hoje são stubs; a arquitetura exige esses conceitos.

### Routers antigos
Contratos preservados como documentação futura:
- `/api/v1/auth`
- `/api/v1/knowledge`
- `/api/v1/library`
- `/api/v1/sync`

## Data
2026-06-26
