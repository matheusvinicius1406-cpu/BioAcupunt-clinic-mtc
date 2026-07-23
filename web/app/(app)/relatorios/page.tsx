import TodoBackend from "@/components/TodoBackend";

export default function RelatoriosPage() {
  return (
    <>
      <div className="page-head">
        <h1>Relatórios</h1>
        <p>Indicadores e exportações da clínica.</p>
      </div>
      <TodoBackend
        what="Os relatórios agregados"
        endpoint="GET /api/v1/reports/* (métricas agregadas)"
      />
      <p style={{ color: "var(--text-faint)", fontSize: 13, marginTop: 16 }}>
        Enquanto não houver endpoint de agregação, a Visão geral já traz as
        contagens básicas (pacientes, agendamentos, consultas do dia) calculadas
        a partir dos endpoints existentes.
      </p>
    </>
  );
}
