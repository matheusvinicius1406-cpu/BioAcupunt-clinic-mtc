import ErrorState from "@/components/ErrorState";
import { apiGet, BackendError } from "@/lib/backend";
import { formatCurrency, formatDate } from "@/lib/format";
import {
  type FinancialSummary,
  type Patient,
  type Transaction,
  TRANSACTION_STATUS_LABELS,
  TRANSACTION_TYPE_LABELS,
  transactionBadgeClass,
} from "@/lib/types";

export const dynamic = "force-dynamic";

export default async function FinanceiroPage() {
  let transactions: Transaction[];
  let summary: FinancialSummary;
  let patients: Patient[];
  try {
    [transactions, summary, patients] = await Promise.all([
      apiGet<Transaction[]>("/api/v1/transactions"),
      apiGet<FinancialSummary>("/api/v1/transactions/summary"),
      apiGet<Patient[]>("/api/v1/patients"),
    ]);
  } catch (e) {
    const msg = e instanceof BackendError ? e.message : "erro inesperado";
    return (
      <>
        <div className="page-head">
          <h1>Financeiro</h1>
        </div>
        <ErrorState message={msg} />
      </>
    );
  }

  const patientById = new Map(patients.map((p) => [p.id, p.name]));

  return (
    <>
      <div className="page-head">
        <h1>Financeiro</h1>
        <p>Faturamento e pagamentos da clínica, direto do backend.</p>
      </div>

      <div className="stat-grid">
        <div className="stat">
          <div className="label">Receita líquida</div>
          <div className="value">{formatCurrency(summary.net_revenue_brl)}</div>
          <div className="hint">recebido menos reembolsos</div>
        </div>
        <div className="stat">
          <div className="label">Pagamentos</div>
          <div className="value">{formatCurrency(summary.payments_brl)}</div>
          <div className="hint">total recebido</div>
        </div>
        <div className="stat">
          <div className="label">A receber</div>
          <div className="value">{formatCurrency(summary.pending_brl)}</div>
          <div className="hint">faturado, ainda pendente</div>
        </div>
        <div className="stat">
          <div className="label">Despesas</div>
          <div className="value">{formatCurrency(summary.expenses_brl)}</div>
          <div className="hint">saídas da clínica</div>
        </div>
      </div>

      <h2 style={{ fontSize: 16, marginBottom: 14 }}>Movimentações</h2>
      {transactions.length === 0 ? (
        <div className="card card-pad">
          <div className="empty">
            <span className="emoji">₿</span>
            Nenhuma movimentação registrada.
          </div>
        </div>
      ) : (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Data</th>
                <th>Paciente</th>
                <th>Categoria</th>
                <th>Tipo</th>
                <th style={{ textAlign: "right" }}>Valor</th>
                <th>Situação</th>
              </tr>
            </thead>
            <tbody>
              {transactions.map((t) => (
                <tr key={t.id}>
                  <td className="cell-muted">{formatDate(t.occurred_on)}</td>
                  <td className="cell-strong">
                    {t.patient_id == null
                      ? "—"
                      : (patientById.get(t.patient_id) ??
                        `Paciente #${t.patient_id}`)}
                  </td>
                  <td className="cell-muted">{t.category}</td>
                  <td className="cell-muted">
                    {TRANSACTION_TYPE_LABELS[t.type] ?? t.type}
                  </td>
                  <td style={{ textAlign: "right" }}>
                    {formatCurrency(t.amount_brl)}
                  </td>
                  <td>
                    <span className={`badge ${transactionBadgeClass(t.status)}`}>
                      {TRANSACTION_STATUS_LABELS[t.status] ?? t.status}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </>
  );
}
