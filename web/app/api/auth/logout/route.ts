import { NextResponse } from "next/server";

import { API_URL } from "@/lib/config";
import { clearAuthCookies, getRefreshToken } from "@/lib/session";

// Revoga a sessao no backend (best-effort) e limpa os cookies httpOnly.
export async function POST() {
  const refresh = await getRefreshToken();
  if (refresh) {
    try {
      await fetch(`${API_URL}/api/v1/auth/logout`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ refresh_token: refresh }),
      });
    } catch {
      /* mesmo se o backend nao responder, limpamos os cookies localmente */
    }
  }
  await clearAuthCookies();
  return NextResponse.json({ ok: true });
}
