# ApiContracts.md — Contratos de API Oficiais

## 1. Objetivo
Definir o contrato público do BioAcupunt Supremo para clientes (Android, Web, iOS, terceiros).
Este documento é a **fonte da verdade** para endpoints, versões, códigos de erro e fluxos.

---

## 2. Regras Gerais

| Regra | Descrição |
|-------|-----------|
| Versionamento | Todas as rotas usam `/api/v1/...` no path. Mudanças breaking geram `/v2`. |
| Segurança | Todas as rotas (exceto `/auth/*` públicas) exigem `Authorization: Bearer <JWT>`. |
| Formato | Request/response em JSON UTF-8. Datas em ISO-8601. IDs em UUID v4. |
| Erros | Sempre `application/problem+json` com `type`, `title`, `status`, `detail`, `instance`, `correlationId`. |
| Paginação | `limit` + `offset` ou `cursor` quando o volume for grande. |
| Tenant | Identificado via header `X-Tenant-Id` para endpoints globais. |
| Idempotência | Chave `Idempotency-Key` em escritas; backend guarda resultado por chave. |

---

## 3. Grupos de Rotas Oficiais

| Grupo | Path Base | Exemplos |
|-------|-----------|---------|
| Auth | `/api/v1/auth` | `register`, `login`, `refresh`, `logout` |
| Patients | `/api/v1/patients` | `list`, `create`, `get`, `update`, `delete` |
| Clinical | `/api/v1/clinical` | `consultations`, `anamnesis`, `syndromes`, `treatment-plans`, `sessions`, `records` |
| Appointments | `/api/v1/appointments` | `list`, `create`, `update`, `cancel`, `reschedule` |
| Protocols | `/api/v1/protocols` | `list`, `create`, `versions`, `publish` |
| Knowledge | `/api/v1/knowledge` | `search`, `import`, `nodes`, `citations` |
| Education | `/api/v1/education` | `courses`, `enrollments`, `lessons`, `quizzes`, `flashcards`, `certifications` |
| Finance | `/api/v1/finance` | `invoices`, `payments`, `transactions`, `charges` |
| CRM | `/api/v1/crm` | `leads`, `opportunities`, `campaigns`, `interactions` |
| AI | `/api/v1/ai` | `query`, `answer`, `reasoning`, `references`, `gaps` |
| Analytics | `/api/v1/analytics` | `metrics`, `dashboards`, `reports` |
| Notifications | `/api/v1/notifications` | `send`, `templates`, `preferences` |
| Administration | `/api/v1/admin` | `tenants`, `plans`, `feature-flags` |
| Sync | `/api/v1/sync` | `changesets`, `conflicts`, `sessions`, `resolve` |

---

## 4. Contratos Principais (MVP + Fase 1)

### 4.1 Auth

#### POST `/api/v1/auth/register`
Request:
```json
{
  "email": "professional@ clinic.example",
  "password": "string",
  "name": "string",
  "tenantId": "uuid"
}
```

Response 201:
```json
{
  "userId": "uuid",
  "email": "string",
  "accessToken": "string",
  "refreshToken": "string",
  "expiresIn": 3600
}
```

#### POST `/api/v1/auth/login`
Request:
```json
{
  "email": "string",
  "password": "string"
}
```

Response 200:
```json
{
  "accessToken": "string",
  "refreshToken": "string",
  "expiresIn": 3600
}
```

#### POST `/api/v1/auth/refresh`
Request:
```json
{
  "refreshToken": "string"
}
```

Response 200:
```json
{
  "accessToken": "string",
  "refreshToken": "string",
  "expiresIn": 3600
}
```

---

### 4.2 Clinical (Core)

#### GET `/api/v1/clinical/patients`
Query:
- `limit`, `offset`
- `query` (filtro textual)
- `status`

Response 200:
```json
{
  "items": [
    {
      "id": "uuid",
      "tenantId": "uuid",
      "profile": { "fullName": "string", "birthDate": "date", "gender": "string" },
      "status": "string",
      "createdAt": "datetime"
    }
  ],
  "total": 120,
  "limit": 20,
  "offset": 0
}
```

#### POST `/api/v1/clinical/appointments`
Request:
```json
{
  "tenantId": "uuid",
  "patientId": "uuid",
  "professionalId": "uuid",
  "startTime": "datetime",
  "endTime": "datetime",
  "type": "first_session|follow_up|return|evaluation",
  "room": "string?",
  "notes": "string?"
}
```

Response 201:
```json
{
  "id": "uuid",
  "status": "scheduled",
  "createdAt": "datetime"
}
```

#### POST `/api/v1/clinical/consultations`
Request:
```json
{
  "tenantId": "uuid",
  "patientId": "uuid",
  "professionalId": "uuid",
  "appointmentId": "uuid?",
  "startedAt": "datetime"
}
```

Response 201 -> gera eventos:
- `ConsultationStarted`

#### POST `/api/v1/clinical/treatment-plans`
Request:
```json
{
  "consultationId": "uuid",
  "diagnosisId": "uuid?",
  "objective": "string",
  "frequency": "string",
  "duration": "string",
  "protocolId": "uuid?",
  "protocolSnapshot": { "version": "string", "steps": [] }
}
```

---

### 4.3 Knowledge

#### GET `/api/v1/knowledge/search`
Query:
- `q` (texto)
- `type` (book|article|pdf|image|video|clinical_case)
- `limit`, `offset`

Response 200:
```json
{
  "items": [
    {
      "id": "uuid",
      "type": "string",
      "title": "string",
      "summary": "string",
      "tags": ["string"],
      "score": 0.87
    }
  ],
  "total": 84
}
```

---

### 4.4 Sync

#### POST `/api/v1/sync/changesets`
Request:
```json
{
  "deviceId": "uuid",
  "changes": [
    {
      "id": "uuid",
      "entityType": "ClinicalRecord",
      "entityId": "uuid",
      "operation": "create|update|delete",
      "payload": {},
      "version": 1,
      "timestamp": "datetime"
    }
  ]
}
```

Response 200:
```json
{
  "status": "accepted",
  "processed": 3,
  "conflicts": [
    {
      "changeSetId": "uuid",
      "strategy": "MERGE|MANUAL|LOCAL|REMOTE",
      "resolutionData": {}
    }
  ]
}
```

---

### 4.5 AI

#### POST `/api/v1/ai/query`
Request:
```json
{
  "tenantId": "uuid",
  "userId": "uuid",
  "patientContextId": "uuid?",
  "query": "string",
  "mode": "diagnosis_support|explanation|protocol_search|general",
  "filters": {}
}
```

Response 200:
```json
{
  "queryId": "uuid",
  "answer": "string",
  "summary": "string",
  "confidence": 0.91,
  "references": [
    {
      "knowledgeNodeId": "uuid",
      "title": "string",
      "source": "string",
      "relevanceScore": 0.83
    }
  ],
  "reasoningTraceId": "uuid",
  "modelVersion": "string",
  "latencyMs": 1204
}
```

---

## 5. Códigos de Erro Oficiais

| Código | Significado | Ação recomendada |
|--------|-------------|------------------|
| 400 | Requisição inválida | Corrigir payload/validação |
| 401 | Autenticação necessária | Renovar token |
| 403 | Permissão negada | Revisar RBAC/role |
| 404 | Recurso não encontrado | Checar id/tenant |
| 409 | Conflito (concorrência ou regra) | Tratar merge/retry |
| 422 | Regra de negócio violada | Exibir mensagem amigável |
| 429 | Rate limit | Backoff exponencial |
| 500/503 | Erro interno | Retry com idempotência |

---

## 6. Versionamento e Compatibilidade

- `eventVersion` em eventos e `apiVersion` em contratos seguem semântica `MAJOR.MINOR.PATCH`.
- breaking changes geram nova versão de API e novo `apiVersion`.
- Campos obsoletos são marcados com `deprecated: true` antes da remoção.

---

## 7. Riscos e Mitigações

| Risco | Mitigação |
|-------|-----------|
| Contratos mudarem sem consumidores saberem | Consumer-driven contracts + testes de integração por cliente |
| Vazamento de dados entre tenants | Middleware obrigatório de tenant isolation em todas as rotas |
| Latência alta por payloads grandes | Limites por tipo de recurso + cache por contrato |
| Quebra por versão | Estratégia de depreciação gradual com 2 versões simultâneas |

---

## 8. Plano de Validação

1. Aprovar contratos principais com time mobile e web.
2. Gerar exemplos de payload para fluxos happy-path e erro.
3. Validar políticas de autenticação e autorização por role.
4. Revisar contratos de sync/offline com time backend.
5. Aprovar limites de paginação e regras de rate limit.
