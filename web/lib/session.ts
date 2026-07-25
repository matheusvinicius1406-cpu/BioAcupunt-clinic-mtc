import "server-only";

import { cookies } from "next/headers";

// Os tokens JWT vivem em cookies httpOnly — inacessiveis ao JS do navegador
// (defesa contra XSS roubar credencial de dados clinicos). Somente o servidor
// Next (route handlers / server components) le e escreve.
export const ACCESS_COOKIE = "ba_access";
export const REFRESH_COOKIE = "ba_refresh";

// O access token do backend expira em 15 min; o refresh em 30 dias.
//
// O cookie tem que expirar JUNTO com o JWT (não depois): o middleware.ts só
// tenta renovar quando o cookie de access está AUSENTE — ele não decodifica o
// JWT para checar se já venceu. Um MaxAge de cookie maior que o exp do JWT
// (era 60 min aqui) cria uma janela onde o cookie ainda existe, o middleware
// deixa passar, mas o JWT lá dentro já expirou — o backend responde 401 e o
// usuário é jogado pro login mesmo tendo um refresh token válido por 30 dias.
const ACCESS_MAX_AGE = 15 * 60; // casa com o exp do JWT de acesso (15 min) — ver middleware.ts
const REFRESH_MAX_AGE = 60 * 60 * 24 * 30; // 30 dias

function cookieOptions(maxAge: number) {
  return {
    httpOnly: true as const,
    secure: process.env.NODE_ENV === "production",
    sameSite: "lax" as const,
    path: "/",
    maxAge,
  };
}

export async function setAuthCookies(accessToken: string, refreshToken: string) {
  const store = await cookies();
  store.set(ACCESS_COOKIE, accessToken, cookieOptions(ACCESS_MAX_AGE));
  store.set(REFRESH_COOKIE, refreshToken, cookieOptions(REFRESH_MAX_AGE));
}

export async function clearAuthCookies() {
  const store = await cookies();
  store.delete(ACCESS_COOKIE);
  store.delete(REFRESH_COOKIE);
}

export async function getAccessToken(): Promise<string | undefined> {
  return (await cookies()).get(ACCESS_COOKIE)?.value;
}

export async function getRefreshToken(): Promise<string | undefined> {
  return (await cookies()).get(REFRESH_COOKIE)?.value;
}
