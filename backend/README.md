# BioAcupunt Backend

Backend oficial do app Android BioAcupunt: autenticação, clínicas, pacientes,
agendamentos e auditoria LGPD. FastAPI + PostgreSQL + SQLAlchemy (async) +
Alembic.

Não confundir com `mkis-backend/` na raiz do repositório — aquele é um
sistema separado de busca/conhecimento clínico, não consumido pelo app.

## Rodando localmente com Docker (recomendado)

```bash
cp .env.example .env   # edite JWT_SECRET_KEY e DOCUMENT_HASH_SECRET
docker compose up --build
```

API em `http://localhost:8000`, docs em `http://localhost:8000/docs`.

## Rodando sem Docker

```bash
python -m venv .venv
.venv/Scripts/activate       # Windows
pip install -r requirements-dev.txt
cp .env.example .env         # ajuste DATABASE_URL para um Postgres local, ou
                              # use sqlite+aiosqlite:///./dev.db para testar sem Postgres
alembic upgrade head
uvicorn app.main:app --reload
```

## Testes

```bash
pip install -r requirements-dev.txt
python -m pytest -v
```

Sem `DATABASE_URL` configurado, os testes usam SQLite em memória
automaticamente (rápido, sem setup). O CI (`.github/workflows/backend-ci.yml`)
roda a mesma suíte contra um Postgres real via service container — é lá que
a compatibilidade real com Postgres é validada.

- `tests/unit/` — regras de negócio puras (hashing, tokens), sem banco.
- `tests/integration/` — repositórios contra um banco real (isolamento
  multi-tenant, soft delete).
- `tests/e2e/` — fluxo HTTP completo via FastAPI (login → refresh → reuso
  malicioso → revogação em cascata, RBAC, auditoria).

## Segurança implementada

- Senhas com Argon2 (`argon2-cffi`).
- Access token (JWT, 15 min) + refresh token (JWT, 30 dias) com rotação:
  cada `/auth/refresh` invalida o token anterior e emite um novo. Reuso de
  um token já rotacionado é tratado como roubo — revoga todas as sessões do
  usuário, inclusive o token novo legítimo emitido na mesma rotação.
- RBAC via `require_role(...)` (ex.: só `admin` pode criar uma clínica).
- Documentos sensíveis (CPF) nunca são armazenados em texto claro — apenas
  o HMAC-SHA256 (`DOCUMENT_HASH_SECRET`).
- Multi-tenancy: toda consulta de paciente/agendamento é filtrada por
  `clinic_id` no repositório, não só na camada de API.
- Auditoria LGPD: toda leitura/escrita de paciente grava uma linha em
  `data_access_logs` (quem, o quê, quando).
- Rate limiting no login (`RATE_LIMIT_LOGIN_PER_MINUTE`).
- Security headers (`X-Content-Type-Options`, `X-Frame-Options`,
  `Referrer-Policy`, `Permissions-Policy`) e log estruturado por
  requisição (`X-Request-ID`, `X-Response-Time-Ms`).
- `401` (não autenticado) vs `403` (sem permissão) tratados corretamente —
  o `HTTPBearer` padrão do FastAPI retorna 403 para ausência de token, o
  que é semântica HTTP incorreta.

## O que ainda falta (deliberadamente fora desta fase)

- Endpoints de gestão de usuários (criar profissional/staff) — hoje só há
  `register_user` como função de serviço, sem router exposto.
- Upload seguro de arquivos.
- Endpoint de agenda com regras de conflito de horário.
- Deploy/CD (só CI de testes está configurado).
