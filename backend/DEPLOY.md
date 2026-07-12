# Deploy do backend BioAcupunt

Guia para publicar o backend online e conectar o app Android a ele.

## Opção A — Render (recomendada, tudo num lugar)

O Render hospeda o backend (FastAPI/Docker) **e** provisiona um Postgres
gerenciado, tudo a partir do `render.yaml` deste diretório.

1. Crie uma conta em https://render.com e conecte sua conta do GitHub.
2. No Render: **New > Blueprint**, selecione o repositório `BioAcupunt-clinic-mtc`.
   O Render lê o `backend/render.yaml` automaticamente.
3. Antes de aplicar, o Render pedirá dois valores (marcados como `sync: false`):
   - `BOOTSTRAP_ADMIN_EMAIL` → o e-mail do seu primeiro login (ex.: `voce@exemplo.com`).
   - `BOOTSTRAP_ADMIN_PASSWORD` → uma senha forte. **Guarde-a**, é o login inicial.
4. Clique em **Apply**. O Render vai:
   - criar o banco `bioacupunt-db`;
   - buildar a imagem Docker do backend;
   - rodar as migrations (`alembic upgrade head`) no start;
   - criar a clínica + o usuário admin com as credenciais acima (só na primeira vez).
5. Quando o serviço ficar **Live**, copie a URL pública, algo como
   `https://bioacupunt-api.onrender.com`.
6. Teste no navegador: abra `https://SUA-URL/healthz` → deve responder
   `{"status":"ok"}`. E `https://SUA-URL/docs` abre a documentação da API.

> Nota: no plano grátis o serviço "dorme" após inatividade e o primeiro
> acesso depois disso demora ~30s para acordar. É normal.

## Opção B — Render + Supabase (backend no Render, banco no Supabase)

Se preferir o Postgres no Supabase (ex.: já usa Supabase, ou quer o banco
separado do host do backend):

1. Crie um projeto no Supabase e copie a **connection string** do Postgres
   (Project Settings > Database > Connection string > URI). Ela vem no
   formato `postgres://postgres:SENHA@db.xxxx.supabase.co:5432/postgres`.
2. No Render, ao aplicar o Blueprint, **remova o bloco `databases:`** do
   `render.yaml` (ou ignore o banco criado) e defina `DATABASE_URL`
   manualmente com a string do Supabase.
3. O resto é igual à Opção A. O backend converte `postgres://` para o driver
   async automaticamente — não precisa mudar código.

> Importante: use a connection string **direta** (porta 5432) para as
> migrations do Alembic. Se usar o pooler do Supabase (porta 6543,
> "Transaction" mode), migrations podem falhar; prefira a conexão direta.

## Conectar o app ao backend publicado

1. Abra o app no celular → **Ajustes > Sistema > Servidor**.
2. Cole a URL do backend (ex.: `https://bioacupunt-api.onrender.com`) e toque
   em **Salvar servidor**.
3. Volte ao login e entre com o `BOOTSTRAP_ADMIN_EMAIL` / `BOOTSTRAP_ADMIN_PASSWORD`
   configurados no deploy.

Deixar o campo de servidor em branco faz o app usar o servidor local de
desenvolvimento (`http://10.0.2.2:8000`, que só funciona no emulador).

## Criar mais usuários

O bootstrap cria só o admin inicial. Ainda **não há** um endpoint de gestão de
usuários exposto na API (está na lista de pendências). Por enquanto, usuários
adicionais são criados direto no banco ou via um script — me avise se quiser
que eu implemente o endpoint `POST /api/v1/users` protegido por admin.
