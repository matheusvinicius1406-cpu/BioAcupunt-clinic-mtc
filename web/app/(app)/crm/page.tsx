import ErrorState from "@/components/ErrorState";
import { apiGet, BackendError } from "@/lib/backend";
import { formatCurrency, formatDate } from "@/lib/format";
import {
  CRM_STAGE_LABELS,
  CRM_STAGES,
  type CrmPatient,
  type CrmPipelineSummary,
  crmStageBadgeClass,
} from "@/lib/types";

export const dynamic = "force-dynamic";

export default async function CrmPage() {
  let patients: CrmPatient[];
  let pipeline: CrmPipelineSummary;
  try {
    [patients, pipeline] = await Promise.all([
      apiGet<CrmPatient[]>("/api/v1/crm"),
      apiGet<CrmPipelineSummary>("/api/v1/crm/pipeline"),
    ]);
  } catch (e) {
    const msg = e instanceof BackendError ? e.message : "erro inesperado";
    return (
      <>
        <div className="page-head">
          <h1>CRM</h1>
        </div>
        <ErrorState message={msg} />
      </>
    );
  }

  // Estágios na ordem do funil, só os que têm alguém.
  const stageRows = CRM_STAGES.map(
    (s) => [s, pipeline.by_stage[s] ?? 0] as const,
  ).filter(([, count]) => count > 0);

  return (
    <>
      <div className="page-head">
        <h1>CRM</h1>
        <p>Funil de relacionamento da clínica, direto do backend.</p>
      </div>

      <div className="stat-grid">
        <div className="stat">
          <div className="label">No funil</div>
          <div className="value">{pipeline.total}</div>
          <div className="hint">contatos ativos</div>
        </div>
        <div className="stat">
          <div className="label">Receita acumulada</div>
          <div className="value">{formatCurrency(pipeline.total_revenue_brl)}</div>
          <div className="hint">soma do histórico</div>
        </div>
        <div className="stat">
          <div className="label">NPS médio</div>
          <div className="value">
            {pipeline.average_nps == null
              ? "—"
              : pipeline.average_nps.toFixed(1)}
          </div>
          <div className="hint">entre quem foi pontuado</div>
        </div>
      </div>

      <h2 style={{ fontSize: 16, marginBottom: 14 }}>Por estágio</h2>
      {stageRows.length === 0 ? (
        <div className="card card-pad">
          <div className="empty">
            <span className="emoji">◈</span>
            Nenhum contato no funil ainda.
          </div>
        </div>
      ) : (
        <div className="table-wrap" style={{ marginBottom: 28 }}>
          <table>
            <thead>
              <tr>
                <th>Estágio</th>
                <th style={{ textAlign: "right" }}>Contatos</th>
              </tr>
            </thead>
            <tbody>
              {stageRows.map(([stage, count]) => (
                <tr key={stage}>
                  <td>
                    <span className={`badge ${crmStageBadgeClass(stage)}`}>
                      {CRM_STAGE_LABELS[stage] ?? stage}
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

      <h2 style={{ fontSize: 16, marginBottom: 14 }}>Contatos</h2>
      {patients.length === 0 ? (
        <div className="card card-pad">
          <div className="empty">
            <span className="emoji">◈</span>
            Nenhum contato registrado.
          </div>
        </div>
      ) : (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Nome</th>
                <th>Estágio</th>
                <th>Telefone</th>
                <th style={{ textAlign: "right" }}>Sessões</th>
                <th style={{ textAlign: "right" }}>Receita</th>
                <th style={{ textAlign: "right" }}>NPS</th>
                <th>Próxima consulta</th>
              </tr>
            </thead>
            <tbody>
              {patients.map((p) => (
                <tr key={p.id}>
                  <td className="cell-strong">{p.name}</td>
                  <td>
                    <span className={`badge ${crmStageBadgeClass(p.stage)}`}>
                      {CRM_STAGE_LABELS[p.stage] ?? p.stage}
                    </span>
                  </td>
                  <td className="cell-muted">{p.phone || "—"}</td>
                  <td style={{ textAlign: "right" }} className="cell-muted">
                    {p.total_sessions}
                  </td>
                  <td style={{ textAlign: "right" }}>
                    {formatCurrency(p.total_revenue_brl)}
                  </td>
                  <td style={{ textAlign: "right" }} className="cell-muted">
                    {p.nps_score ?? "—"}
                  </td>
                  <td className="cell-muted">
                    {p.next_appointment ? formatDate(p.next_appointment) : "—"}
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
