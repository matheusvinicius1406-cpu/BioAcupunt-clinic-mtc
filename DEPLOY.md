# Deploy — BioAcupunt (Vercel + Neon)

Checklist do que **só você** (a pessoa dona das contas) precisa fazer. O código
já está pronto: backend serverless em `api/` + `backend/`, e o painel web em
`web/`. São **dois projetos Vercel separados** apontando para o **mesmo repositório**.

> Segurança clínica: o painel web é só de gestão/leitura. Ele **não** edita
> prontuário nem sugere protocolo. O motor determinístico de triagem
> (`ClinicalSafetyEngine`) vive só no app Android. Não coloque decisão clínica no
> web enquanto essa triagem não estiver no backend.

---

## 1. Banco de dados — Neon (Postgres)

1. Crie um projeto em https://neon.tech.
2. Em **Connection Details**, copie **duas** strings:
   - **Pooled** (host com `-pooler`) → para o app rodar em serverless.
   - **Direct** (host sem `-pooler`) → para rodar as migrations.
3. Anote a senha; você vai colar as URLs na Vercel e no comando do Alembic.

Formato pooled (exemplo):
```
postgresql://user:senha@ep-xxxx-pooler.us-east-1.aws.neon.tech/neondb?sslmode=require
```
O backend converte `postgres://`/`postgresql://` para o driver `asyncpg`
automaticamente e já roda com `NullPool` + `statement_cache_size=0`, exigido pelo
pooler do Neon (PgBouncer em modo transaction). **Não precisa mudar código.**

---

## 2. Rodar as migrations (uma vez, e a cada mudança de schema)

Migrations **não** rodam dentro do lambda (serverless não tem um "start" para
isso). Rode do seu computador, apontando para o host **direto** (sem `-pooler`):

```bash
cd backend
python -m venv .venv && source .venv/bin/activate   # Windows: .venv\Scripts\activate
pip install -r requirements.txt
export DATABASE_URL="postgresql+asyncpg://user:senha@ep-xxxx.us-east-1.aws.neon.tech/neondb?sslmode=require"
alembic upgrade head
```
(No PowerShell use `$env:DATABASE_URL="..."` no lugar do `export`.)

Repita `alembic upgrade head` sempre que novas migrations forem adicionadas.

---

## 3. Projeto Vercel — Backend (API)

1. Vercel → **Add New → Project** → importe o repositório.
2. **Root Directory:** deixe a **raiz do repositório** (onde estão `vercel.json`,
   `requirements.txt` e a pasta `api/`). Não aponte para `backend/`.
3. **Environment Variables** (Settings → Environment Variables):

   | Variável | Obrigatória | Valor |
   |---|---|---|
   | `DATABASE_URL` | ✅ | string **pooled** do Neon (host `-pooler`) |
   | `JWT_SECRET_KEY` | ✅ | `openssl rand -hex 32` |
   | `DOCUMENT_HASH_SECRET` | ✅ | `openssl rand -hex 32` (outro valor) |
   | `ENVIRONMENT` | recomendada | `production` |
   | `CORS_ORIGINS` | condicional | veja abaixo |
   | `BOOTSTRAP_ADMIN_EMAIL` | 1º deploy | seu e-mail de login inicial |
   | `BOOTSTRAP_ADMIN_PASSWORD` | 1º deploy | senha forte (guarde-a) |
   | `BOOTSTRAP_CLINIC_NAME` | opcional | nome da clínica |

   > `JWT_SECRET_KEY` e `DOCUMENT_HASH_SECRET` são **required** (pydantic). Sem
   > elas o app nem sobe. No Render eram geradas sozinhas; na Vercel você define.

   **CORS:**
   - Só app mobile: `CORS_ORIGINS=["*"]` (o backend desliga `credentials`
     sozinho — `*` + credentials é proibido pela spec).
   - Com o painel web: liste a URL, ex.
     `CORS_ORIGINS=["https://bioacupunt-web.vercel.app"]`.

4. Deploy. Teste `https://SEU-BACKEND.vercel.app/healthz` → `{"status":"ok"}`.
   `/docs` abre o Swagger.

   > **Bootstrap do admin:** roda no primeiro request (cold start) enquanto o
   > banco não tiver usuários. Se preferir não depender do cold start, crie o
   > admin manualmente via `POST /api/v1/auth/register` (cria clínica + admin)
   > uma vez, e pode deixar `BOOTSTRAP_ADMIN_*` em branco.

---

## 4. Projeto Vercel — Frontend web (`web/`)

1. Vercel → **Add New → Project** → mesmo repositório, **outro** projeto.
2. **Root Directory:** `web`.
3. Framework: Next.js (detecta sozinho).
4. **Environment Variable:**
   - `NEXT_PUBLIC_API_URL` = URL do backend, ex.
     `https://SEU-BACKEND.vercel.app` (sem barra no final).
5. Deploy. Abra a URL, faça login com o admin do passo 3.
6. **Volte no backend** e adicione a URL deste web ao `CORS_ORIGINS` (passo 3.3).

---

## 5. Modelo de IA `.task` / `.litertlm` — NÃO na Vercel

O modelo on-device (centenas de MB) **não** pode ir para o lambda (limite de
tamanho e de resposta). Hospede num **CDN / storage** (Cloudflare R2, S3+CloudFront,
Backblaze, GitHub Release asset) e atualize a URL de download no app Android.
O app verifica o SHA-256 fixado (`ModelIntegrity`) antes de usar — não invente
hash; rode `./scripts/pin_models.sh` com `HF_TOKEN` para preenchê-lo.

---

## Resumo do que é só seu

- [ ] Criar projeto Neon; copiar URLs pooled + direct.
- [ ] `alembic upgrade head` contra o host **direto**.
- [ ] Projeto Vercel do backend (root = raiz) + env vars (`JWT_SECRET_KEY`,
      `DOCUMENT_HASH_SECRET`, `DATABASE_URL` pooled, `CORS_ORIGINS`,
      `BOOTSTRAP_ADMIN_*`).
- [ ] Projeto Vercel do web (root = `web`) + `NEXT_PUBLIC_API_URL`.
- [ ] Acrescentar a URL do web ao `CORS_ORIGINS` do backend.
- [ ] Hospedar o `.task` num CDN e atualizar a URL no app.
