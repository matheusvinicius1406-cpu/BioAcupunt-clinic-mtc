import ErrorState from "@/components/ErrorState";
import { apiGet, BackendError } from "@/lib/backend";
import {
  appointmentBadgeClass,
  formatCurrency,
  statusLabel,
} from "@/lib/format";
import type { ReportsOverview } from "@/lib/types";

export const dynamic = "force-dynamic";

export default async function RelatoriosPage() {
  let overview: ReportsOverview;
  try {
    overview = await apiGet<ReportsOverview>("/api/v1/reports/overview");
  } catch (e) {
    const msg = e instanceof BackendError ? e.message : "erro inesperado";
    return (
      <>
        <div className="page-head">
          <h1>Relatórios</h1>
        </div>
        <ErrorState message={msg} />
      </>
    );
  }

  const { finance } = overview;
  const statusRows = Object.entries(overview.appointments_by_status).sort(
    (a, b) => b[1] - a[1],
  );

  return (
    <>
      <div className="page-head">
        <h1>Relatórios</h1>
        <p>Indicadores agregados da clínica, calculados no backend.</p>
      </div>

      <div className="stat-grid">
        <div className="stat">
          <div className="label">Pacientes</div>
          <div className="value">{overview.patients_total}</div>
          <div className="hint">cadastrados na clínica</div>
        </div>
        <div className="stat">
          <div className="label">Agendamentos</div>
          <div className="value">{overview.appointments_total}</div>
          <div className="hint">total no histórico</div>
        </div>
        <div className="stat">
          <div className="label">Receita líquida</div>
          <div className="value">{formatCurrency(finance.net_revenue_brl)}</div>
          <div className="hint">recebido menos reembolsos</div>
        </div>
        <div className="stat">
          <div className="label">A receber</div>
          <div className="value">{formatCurrency(finance.pending_brl)}</div>
          <div className="hint">faturado, ainda pendente</div>
        </div>
      </div>

      <h2 style={{ fontSize: 16, marginBottom: 14 }}>Agendamentos por situação</h2>
      {statusRows.length === 0 ? (
        <div className="card card-pad">
          <div className="empty">
            <span className="emoji">◷</span>
            Nenhum agendamento registrado ainda.
          </div>
        </div>
      ) : (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Situação</th>
                <th style={{ textAlign: "right" }}>Quantidade</th>
              </tr>
            </thead>
            <tbody>
              {statusRows.map(([status, count]) => (
                <tr key={status}>
                  <td>
                    <span className={`badge ${appointmentBadgeClass(status)}`}>
                      {statusLabel(status)}
                    </span>
                  </td>
                  <td style={{ textAlign: "right" }} className="cell-strong">
                    {count}
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
