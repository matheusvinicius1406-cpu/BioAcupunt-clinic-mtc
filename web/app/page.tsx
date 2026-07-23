import { redirect } from "next/navigation";

import { getAccessToken, getRefreshToken } from "@/lib/session";

// A raiz nao renderiza nada: manda para o painel se ha sessao, senao pro login.
export default async function Home() {
  const hasSession = (await getAccessToken()) || (await getRefreshToken());
  redirect(hasSession ? "/dashboard" : "/login");
}
