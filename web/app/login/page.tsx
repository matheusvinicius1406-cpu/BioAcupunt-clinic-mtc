"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";

export default function LoginPage() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const res = await fetch("/api/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, password }),
      });
      if (!res.ok) {
        const body = (await res.json().catch(() => ({}))) as {
          message?: string;
        };
        setError(body.message ?? "Não foi possível entrar.");
        setLoading(false);
        return;
      }
      // Sessao gravada em cookie httpOnly pelo route handler. Navega pro painel.
      router.replace("/dashboard");
      router.refresh();
    } catch {
      setError("Falha de conexão. Tente novamente.");
      setLoading(false);
    }
  }

  return (
    <main className="auth-wrap">
      <form className="auth-card" onSubmit={onSubmit}>
        <div className="auth-brand">
          <div className="brand-mark">B</div>
          <div>
            <div className="brand-name">BioAcupunt</div>
            <div className="brand-sub">Painel clínico</div>
          </div>
        </div>

        <h1>Entrar</h1>
        <p className="sub">Acesse o painel de gestão da sua clínica.</p>

        {error && (
          <div className="error-box" style={{ marginBottom: 16 }} role="alert">
            {error}
          </div>
        )}

        <div className="field">
          <label htmlFor="email">E-mail</label>
          <input
            id="email"
            type="email"
            autoComplete="username"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
            placeholder="voce@clinica.com"
          />
        </div>

        <div className="field">
          <label htmlFor="password">Senha</label>
          <input
            id="password"
            type="password"
            autoComplete="current-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            placeholder="••••••••"
          />
        </div>

        <button className="btn-primary" type="submit" disabled={loading}>
          {loading ? "Entrando…" : "Entrar"}
        </button>

        <p className="auth-footnote">
          Painel complementar — somente gestão e consulta.
          <br />
          Prontuário e decisões clínicas ficam no aplicativo.
        </p>
      </form>
    </main>
  );
}
