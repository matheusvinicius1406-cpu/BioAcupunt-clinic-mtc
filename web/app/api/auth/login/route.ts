import { NextRequest, NextResponse } from "next/server";

import { API_URL } from "@/lib/config";
import { setAuthCookies } from "@/lib/session";
import type { TokenPair } from "@/lib/types";

// Proxy de login: recebe {email, password} do formulario, autentica no FastAPI
// e grava os tokens em cookies httpOnly. O token JWT nunca chega ao JS do
// navegador.
export async function POST(request: NextRequest) {
  let body: { email?: string; password?: string };
  try {
    body = await request.json();
  } catch {
    return NextResponse.json(
      { message: "Requisição inválida." },
      { status: 400 },
    );
  }

  if (!body.email || !body.password) {
    return NextResponse.json(
      { message: "Informe e-mail e senha." },
      { status: 400 },
    );
  }

  let res: Response;
  try {
    res = await fetch(`${API_URL}/api/v1/auth/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email: body.email, password: body.password }),
    });
  } catch {
    return NextResponse.json(
      { message: "Não foi possível contatar o servidor. Verifique a conexão." },
      { status: 502 },
    );
  }

  if (!res.ok) {
    let message = "E-mail ou senha inválidos.";
    try {
      const err = (await res.json()) as { error?: { message?: string } };
      if (err.error?.message) message = err.error.message;
    } catch {
      /* usa mensagem padrao */
    }
    return NextResponse.json({ message }, { status: res.status });
  }

  const pair = (await res.json()) as TokenPair;
  await setAuthCookies(pair.access_token, pair.refresh_token);
  return NextResponse.json({ ok: true });
}
