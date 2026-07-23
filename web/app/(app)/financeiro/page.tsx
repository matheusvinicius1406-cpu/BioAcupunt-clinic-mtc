import TodoBackend from "@/components/TodoBackend";

export default function FinanceiroPage() {
  return (
    <>
      <div className="page-head">
        <h1>Financeiro</h1>
        <p>Faturamento e pagamentos da clínica.</p>
      </div>
      <TodoBackend
        what="O módulo financeiro"
        endpoint="GET /api/v1/transactions (ou expor value_brl/paid dos agendamentos)"
      />
      <p style={{ color: "var(--text-faint)", fontSize: 13, marginTop: 16 }}>
        Nota: o modelo <code>Appointment</code> já guarda <code>value_brl</code> e{" "}
        <code>paid</code>, e existe um modelo <code>Transaction</code> no backend —
        mas nenhum router os expõe ainda. Sem endpoint, o painel não mostra número
        financeiro para não inventar dados.
      </p>
    </>
  );
}
