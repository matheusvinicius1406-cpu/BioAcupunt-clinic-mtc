import "server-only";

import { redirect } from "next/navigation";

import { API_URL } from "./config";
import { getAccessToken } from "./session";

export class BackendError extends Error {
  constructor(
    public status: number,
    public code: string,
    message: string,
  ) {
    super(message);
  }
}

interface BackendErrorBody {
  error?: { code?: string; message?: string };
  detail?: unknown;
}

/**
 * Server-side fetch to the FastAPI backend, authenticated with the httpOnly
 * access-token cookie.
 *
 * Token refresh is NOT done here: server components cannot mutate cookies
 * during render, and the backend rotates refresh tokens (reusing one twice is
 * treated as theft and revokes every session). Refresh happens in exactly one
 * place — middleware.ts — before the request reaches this code, so by here the
 * access cookie is expected to be valid. A 401 therefore means the session is
 * genuinely gone: we send the user back to /login.
 */
async function apiFetch<T>(path: string, init?: RequestInit): Promise<T> {
  const token = await getAccessToken();
  if (!token) {
    redirect("/login");
  }

  const res = await fetch(`${API_URL}${path}`, {
    ...init,
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
      ...(init?.headers ?? {}),
    },
    // Dados clinicos: nunca cachear respostas autenticadas.
    cache: "no-store",
  });

  if (res.status === 401) {
    redirect("/login");
  }

  if (!res.ok) {
    let code = "http_error";
    let message = `Erro ${res.status}`;
    try {
      const body = (await res.json()) as BackendErrorBody;
      if (body.error) {
        code = body.error.code ?? code;
        message = body.error.message ?? message;
      } else if (typeof body.detail === "string") {
        message = body.detail;
      }
    } catch {
      /* corpo nao-JSON: mantem a mensagem generica */
    }
    throw new BackendError(res.status, code, message);
  }

  if (res.status === 204) {
    return undefined as T;
  }
  return (await res.json()) as T;
}

export function apiGet<T>(path: string): Promise<T> {
  return apiFetch<T>(path, { method: "GET" });
}
