"use client";

import { usePathname, useRouter } from "next/navigation";
import Link from "next/link";
import { useState } from "react";

import { ROLE_LABELS } from "@/lib/types";

interface NavItem {
  href: string;
  label: string;
  icon: string;
}

const NAV: NavItem[] = [
  { href: "/dashboard", label: "Visão geral", icon: "◧" },
  { href: "/agenda", label: "Agenda", icon: "◷" },
  { href: "/pacientes", label: "Pacientes", icon: "◐" },
  { href: "/crm", label: "CRM", icon: "◈" },
  { href: "/financeiro", label: "Financeiro", icon: "▤" },
  { href: "/relatorios", label: "Relatórios", icon: "◭" },
  { href: "/analytics", label: "Analytics", icon: "◫" },
  { href: "/biblioteca", label: "Biblioteca", icon: "▧" },
];

export default function Sidebar({
  name,
  role,
}: {
  name: string;
  role: string;
}) {
  const pathname = usePathname();
  const router = useRouter();
  const [loggingOut, setLoggingOut] = useState(false);

  async function logout() {
    setLoggingOut(true);
    try {
      await fetch("/api/auth/logout", { method: "POST" });
    } catch {
      /* limpa a sessao mesmo se o backend nao responder */
    }
    router.replace("/login");
    router.refresh();
  }

  return (
    <aside className="sidebar">
      <div className="brand">
        <div className="brand-mark">B</div>
        <div>
          <div className="brand-name">BioAcupunt</div>
          <div className="brand-sub">Painel</div>
        </div>
      </div>

      <nav>
        {NAV.map((item) => {
          const active =
            pathname === item.href || pathname.startsWith(`${item.href}/`);
          return (
            <Link
              key={item.href}
              href={item.href}
              className={`nav-link${active ? " active" : ""}`}
            >
              <span className="nav-icon" aria-hidden>
                {item.icon}
              </span>
              {item.label}
            </Link>
          );
        })}
      </nav>

      <div className="nav-spacer" />

      <div className="sidebar-user">
        <div className="name" title={name}>
          {name}
        </div>
        <div className="role">{ROLE_LABELS[role] ?? role}</div>
        <button
          className="logout-btn"
          onClick={logout}
          disabled={loggingOut}
          type="button"
        >
          {loggingOut ? "Saindo…" : "Sair"}
        </button>
      </div>
    </aside>
  );
}
