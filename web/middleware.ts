import { NextRequest, NextResponse } from "next/server";

import { API_URL } from "./lib/config";

const ACCESS_COOKIE = "ba_access";
const REFRESH_COOKIE = "ba_refresh";
const ACCESS_MAX_AGE = 15 * 60; // casa com o exp do JWT de acesso (15 min)
const REFRESH_MAX_AGE = 60 * 60 * 24 * 30;

const LOGIN_PATH = "/login";

// Rotas que exigem sessao. Tudo o mais (login, estaticos) passa livre.
const PROTECTED_PREFIXES = [
  "/dashboard",
  "/pacientes",
  "/agenda",
  "/financeiro",
  "/relatorios",
  "/biblioteca",
  "/crm",
  "/analytics",
];

function isProtected(pathname: string): boolean {
  return PROTECTED_PREFIXES.some(
    (p) => pathname === p || pathname.startsWith(`${p}/`),
  );
}

function cookieOpts(maxAge: number) {
  return {
    httpOnly: true,
    secure: process.env.NODE_ENV === "production",
    sameSite: "lax" as const,
    path: "/",
    maxAge,
  };
}

/**
 * Guarda de rotas + ponto UNICO de refresh do token.
 *
 * Se o cookie de access sumiu (expirou) mas ainda existe refresh, renovamos
 * aqui — o unico lugar onde da para (a) girar o refresh token com seguranca e
 * (b) persistir os novos cookies. Fazer isso em server component quebraria: nao
 * se muta cookie durante render, e reusar um refresh token ja girado dispara a
 * deteccao de roubo do backend (revoga todas as sessoes).
 */
export async function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;
  if (!isProtected(pathname)) {
    return NextResponse.next();
  }

  const access = request.cookies.get(ACCESS_COOKIE)?.value;
  if (access) {
    return NextResponse.next();
  }

  const refresh = request.cookies.get(REFRESH_COOKIE)?.value;
  if (!refresh) {
    return redirectToLogin(request);
  }

  // Access expirou; tenta renovar com o refresh (rotacionado pelo backend).
  try {
    const res = await fetch(`${API_URL}/api/v1/auth/refresh`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refresh_token: refresh }),
    });

    if (!res.ok) {
      return redirectToLogin(request, true);
    }

    const pair = (await res.json()) as {
      access_token: string;
      refresh_token: string;
    };

    // Injeta os novos cookies no proprio request para que o server component
    // desta mesma resposta ja enxergue o access valido...
    request.cookies.set(ACCESS_COOKIE, pair.access_token);
    request.cookies.set(REFRESH_COOKIE, pair.refresh_token);
    const response = NextResponse.next({ request });
    // ...e no response para persistir no navegador.
    response.cookies.set(
      ACCESS_COOKIE,
      pair.access_token,
      cookieOpts(ACCESS_MAX_AGE),
    );
    response.cookies.set(
      REFRESH_COOKIE,
      pair.refresh_token,
      cookieOpts(REFRESH_MAX_AGE),
    );
    return response;
  } catch (err) {
    // Isto captura QUALQUER falha em fetch()/res.json() — inclusive um blip
    // transitorio de rede/DNS/timeout com o backend, nao so um refresh token
    // realmente invalido (esse caso ja e tratado acima, em `!res.ok`, com
    // `clear = true` porque o backend respondeu e disse explicitamente "nao").
    // Apagar uma sessao de 30 dias por causa de uma falha de rede momentanea
    // forcaria login de novo sem necessidade — entao aqui NAO limpa os cookies,
    // so redireciona; se a proxima navegacao pegar o backend de volta no ar, o
    // refresh tenta de novo com o mesmo par de cookies ainda intacto.
    console.error("middleware: falha ao chamar /auth/refresh (rede/parse), sessao preservada:", err);
    return redirectToLogin(request);
  }
}

function redirectToLogin(request: NextRequest, clear = false) {
  const url = request.nextUrl.clone();
  url.pathname = LOGIN_PATH;
  url.search = "";
  const response = NextResponse.redirect(url);
  if (clear) {
    response.cookies.delete(ACCESS_COOKIE);
    response.cookies.delete(REFRESH_COOKIE);
  }
  return response;
}

export const config = {
  // Nao intercepta assets do Next, arquivos estaticos nem os route handlers.
  matcher: ["/((?!_next/static|_next/image|favicon.ico|api/).*)"],
};
