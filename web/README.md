# BioAcupunt — Painel web

Frontend **complementar** (Next.js App Router + TypeScript) do BioAcupunt.
Somente **gestão e leitura**: agenda, pacientes, financeiro, relatórios.
**Sem prontuário, sem decisão clínica** — a triagem determinística vive só no app
Android e não é reimplementada em JavaScript (seria contornável no navegador).

## Rodar local

```bash
cp .env.example .env.local     # ajuste NEXT_PUBLIC_API_URL
npm install
npm run dev                    # http://localhost:3000
```

Precisa do backend FastAPI rodando (local em `http://127.0.0.1:8000` ou o deploy
na Vercel). Login usa o mesmo admin do backend.

## Como está estruturado

- **Auth:** JWT em **cookies httpOnly** (`ba_access` / `ba_refresh`) — o token
  nunca chega ao JS do navegador (defesa contra XSS em dados clínicos).
  - `app/api/auth/login` e `.../logout` — route handlers que falam com o FastAPI
    (`/api/v1/auth/*`) e gravam/limpam os cookies.
  - `middleware.ts` — guarda as rotas e é o **único** ponto de refresh do token
    (server components não podem mutar cookie, e o backend gira o refresh token).
  - `lib/backend.ts` — fetch server-side autenticado; server components consomem.
- **Telas fiadas ao backend real:** Visão geral (`/dashboard`), Agenda
  (`/agenda`), Pacientes (`/pacientes`) — usam `GET /api/v1/patients` e
  `GET /api/v1/appointments`.
- **Telas marcadas "TODO backend"** (o endpoint não existe): Financeiro,
  Relatórios, Biblioteca. Nada é simulado.

## Endpoints reais consumidos

`POST /api/v1/auth/login` · `POST /api/v1/auth/refresh` · `POST /api/v1/auth/logout`
· `GET /api/v1/auth/me` · `GET /api/v1/patients` · `GET /api/v1/appointments`.

Deploy: veja o `DEPLOY.md` na raiz do repositório.
