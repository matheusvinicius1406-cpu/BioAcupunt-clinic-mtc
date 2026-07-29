"use client";

// Nao existia NENHUM error.tsx no projeto ate aqui. Sem ele, qualquer erro nao
// tratado explicitamente (ex.: falha de rede ao chamar o backend em
// (app)/layout.tsx, que so trata BackendError e repassa o resto com `throw e`)
// derrubava a pagina inteira na tela de erro generica do Next, sem nada
// reconhecivel pra quem esta usando o painel.
export default function GlobalError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  return (
    <main className="auth-wrap">
      <div className="auth-card">
        <div className="auth-brand">
          <div className="brand-mark">B</div>
          <div>
            <div className="brand-name">BioAcupunt</div>
            <div className="brand-sub">Painel clínico</div>
          </div>
        </div>

        <h1>Algo deu errado</h1>
        <p className="sub">
          Não conseguimos carregar esta página agora. Pode ser uma falha temporária de conexão com o servidor.
        </p>

        <div className="error-box" role="alert" style={{ marginBottom: 16 }}>
          {error.message || "Erro inesperado."}
        </div>

        <button className="btn-primary" type="button" onClick={reset}>
          Tentar novamente
        </button>
      </div>
    </main>
  );
}
