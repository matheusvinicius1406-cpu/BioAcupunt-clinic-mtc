// Sem isto, nenhuma rota do painel tinha limite de Suspense — toda navegacao
// pela Sidebar travava a UI inteira ate o fetch da pagina (force-dynamic,
// cache: "no-store" em lib/backend.ts) responder por completo. Cobre as 8
// rotas de (app)/ com um unico arquivo, mesmo padrao de streaming do App
// Router do Next.
export default function Loading() {
  return (
    <div className="empty" role="status" aria-live="polite">
      <span className="emoji">⏳</span>
      Carregando…
    </div>
  );
}
