# MKIS — Contratos públicos
## REST, DTOs, versionamento, códigos HTTP, OpenAPI, cache, idempotência

---

## 1. Regras de design da API

- A API é versionada no path: `/api/v1/knowledge/...`.
- Todos endpoints retornam `ApiResponse[T]`.
- Identificadores públicos sempre UUIDv7.
- `GET` é idempotente e cacheável.
- `POST/PATCH` são não-idempotentes salvo header `Idempotency-Key`.
- Erros devolvem corpo estruturado; nunca stacktrace em produção.

---

## 2. Formato canônico de resposta

```json
{
  "success": boolean,
  "data": object,
  "meta": {
    "tenant_id": "uuid",
    "request_id": "uuid",
    "trace_id": "string",
    "requested_at": "iso8601",
    "source": "string",
    "cached": boolean,
    "ttl_seconds": int
  },
  "errors": []
}
```

Regras:
- `meta.cached = true` quando resposta veio de cache controlado.
- `errors` é lista; pode conter múltiplos problemas downstream.

---

## 3. Paginação

- Cursor-based obrigatória em listagens grandes.
- Formato:

```json
{
  "items": [],
  "page_info": {
    "next_cursor": "string",
    "prev_cursor": "string?",
    "has_next": boolean,
    "limit": int,
    "total_estimate": "long"
  }
}
```

- `limit` default: 20; máximo: 50.
- `total_estimate` pode ser estimado por índice.

---

## 4. Filtros e ordenação

- Filtros sempre via query params.
- Ordenação apenas por atributos indexados.
- Operadores compostos por `AND` lógico; `OR` com wrapper explícito `AnyFilter`.

---

## 5. Códigos HTTP utilizados

| Código | Uso canônico |
|--------|--------------|
| 200 | Sucesso |
| 201 | Criado |
| 204 | Removido |
| 400 | Erro de validação |
| 401 | Não autenticado |
| 403 | Não autorizado |
| 404 | Recurso inexistente |
| 409 | Conflito (idempotência, duplicidade, lock) |
| 422 | Regra de negócio violada |
| 429 | Rate limit |
| 500 | Erro genérico |
| 503 | Serviço indisponível |
| 504 | Timeout |

Nunca retornar códigos HTTP genéricos sem corpo estruturado.

---

## 6. Endpoints centrais (v1)

- `POST /api/v1/knowledge/nodes` — criar draft
- `GET /api/v1/knowledge/nodes/{id}` — obter publicação permitida
- `GET /api/v1/knowledge/nodes` — busca pública com paginação
- `GET /api/v1/knowledge/search` — busca híbrida pública
- `POST /api/v1/knowledge/nodes/{id}/reviews/approve` — aprovação autorizada
- `POST /api/v1/knowledge/artifacts/upload` — upload com idempotência
- `POST /api/v1/knowledge/graph/edges` — criar relação
- `GET /api/v1/knowledge/graph/edges` — consultar relações
- `GET /api/v1/knowledge/evidence/{node_id}` — cadeia de evidência
- `POST /api/v1/knowledge/nodes/{id}/quarantine` — colocar em quarentena
- `POST /api/v1/knowledge/purge/request` — solicitar hard delete LGPD
- `POST /api/v1/knowledge/purge/{certificate_id}/approve` — aprovar purge
- `POST /api/v1/knowledge/purge/{certificate_id}/execute` — executar purge

Todos endpoints com RBAC/ABAC avaliado antes de prosseguir.

---

## 7. OpenAPI

- Documentação em `/docs/openapi.json` atualizada por CI.
- Schemas versionados por pasta `schemas/v1/`.
- Exemplos por status_code e erro.
- Auto-valida contra contrato antes do deploy.

---

## 8. Cache

- `Cache-Control`, `ETag`, `Vary` sempre quando aplicável.
- Cache evitado quando `tenant_id` difere ou filtro é personalizado por authorization.
- Invalidação via eventos: `KnowledgeNodeApproved`, `EvidenceScoreUpdated`, `GraphEdgeCreated`.
- TTL default: `300 segundos`.
- Chave de cache inclui `tenant_id`, filtros_hash, `embedding_version`.