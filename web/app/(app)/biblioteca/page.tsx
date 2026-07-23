import TodoBackend from "@/components/TodoBackend";

export default function BibliotecaPage() {
  return (
    <>
      <div className="page-head">
        <h1>Biblioteca</h1>
        <p>Acervo de referência clínica.</p>
      </div>
      <TodoBackend
        what="A biblioteca / acervo"
        endpoint="GET /api/v1/library/* (não existe no backend)"
      />
      <p style={{ color: "var(--text-faint)", fontSize: 13, marginTop: 16 }}>
        Hoje a biblioteca e o RAG vivem dentro do aplicativo Android (assets
        locais + curadoria da médica), não no backend. Conteúdo clínico entra por
        curadoria humana — não é gerado nem servido automaticamente pelo web.
      </p>
    </>
  );
}
