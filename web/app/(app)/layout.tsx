import { redirect } from "next/navigation";

import Sidebar from "@/components/Sidebar";
import { apiGet, BackendError } from "@/lib/backend";
import type { User } from "@/lib/types";

// Shell das rotas autenticadas. Busca o usuario atual (/me) no backend; se nao
// der (sessao invalida), o apiGet ja redireciona pro login.
export default async function AppLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  let user: User;
  try {
    user = await apiGet<User>("/api/v1/auth/me");
  } catch (e) {
    if (e instanceof BackendError) {
      redirect("/login");
    }
    throw e;
  }

  return (
    <div className="shell">
      <Sidebar name={user.full_name} role={user.role} />
      <main className="main">{children}</main>
    </div>
  );
}
