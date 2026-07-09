# MKIS — Erros
## Taxonomia de erros, exceções, códigos canônicos e contratos

---

## 1. Princípios

- Nunca retornar erro cru.
- Todo erro expõe apenas: `code`, `message`, `details`, `request_id`, `documentation_url`.
- Nunca expor internais, caminhos, tokens ou segredos.
- Valor de referência canônico: `29-canonical-references.md`.

---

## 2. Formato canônico

```json
{
  "code": "string",
  "message": "string",
  "details": [],
  "request_id": "uuid",
  "documentation_url": "string"
}
```

Regras:
- `code` é identificador estável; não usamos mensagem dinâmica como código.
- `details` é array pequeno; sem stacktrace em produção.
- `request_id` é obrigatório para correlação.

---

## 3. Erros canônicos

- `KnowledgeNotFound` — 404
- `InvalidDocument` — 400
- `DuplicateKnowledge` — 409
- `ImportFailed` — 500
- `OCRFailed` — 500
- `EmbeddingFailed` — 500
- `ValidationFailed` — 422
- `ScientificSourceUnavailable` — 503
- `VectorIndexError` — 500
- `GraphConflict` — 409
- `QuarantineRequired` — 400
- `ApprovalRequired` — 400
- `HardDeleteFailed` — 500
- `RateLimited` — 429
- `Unauthorized` — 401
- `Forbidden` — 403
- `Conflict` — 409
- `Timeout` — 504
- `ServiceUnavailable` — 503

Cada erro define:
- código
- status HTTP
- mensagem padrão
- se retry é recomendado