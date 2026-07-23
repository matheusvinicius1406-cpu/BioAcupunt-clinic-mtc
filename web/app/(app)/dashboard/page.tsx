import ErrorState from "@/components/ErrorState";
import { apiGet, BackendError } from "@/lib/backend";
import {
  appointmentBadgeClass,
  formatDateTime,
  statusLabel,
} from "@/lib/format";
import type { Appointment, Patient } from "@/lib/types";

export const dynamic = "force-dynamic";

function isSameDay(iso: string, ref: Date): boolean {
  const d = new Date(iso);
  return (
    d.getFullYear() === ref.getFullYear() &&
    d.getMonth() === ref.getMonth() &&
    d.getDate() === ref.getDate()
  );
}

export default async function DashboardPage() {
  let patients: Patient[];
  let appointments: Appointment[];
  try {
    [patients, appointments] = await Promise.all([
      apiGet<Patient[]>("/api/v1/patients"),
      apiGet<Appointment[]>("/api/v1/appointments"),
    ]);
  } catch (e) {
    const msg = e instanceof BackendError ? e.message : "erro inesperado";
    return (
      <>
        <div className="page-head">
          <h1>Visão geral</h1>
        </div>
        <ErrorState message={msg} />
      </>
    );
  }

  const now = new Date();
  const patientById = new Map(patients.map((p) => [p.id, p.name]));
  const today = appointments.filter((a) => isSameDay(a.scheduled_at, now));
  const upcoming = appointments
    .filter(
      (a) =>
        new Date(a.scheduled_at) >= now &&
        a.status !== "CANCELLED" &&
        a.status !== "NO_SHOW",
    )
    .sort(
      (a, b) =>
        new Date(a.scheduled_at).getTime() -
        new Date(b.scheduled_at).getTime(),
    )
    .slice(0, 8);

  return (
    <>
      <div className="page-head">
        <h1>Visão geral</h1>
        <p>Resumo da clínica em tempo real, direto do backend.</p>
      </div>

      <div className="stat-grid">
        <div className="stat">
          <div className="label">Pacientes</div>
          <div className="value">{patients.length}</div>
          <div className="hint">cadastrados na clínica</div>
        </div>
        <div className="stat">
          <div className="label">Agendamentos</div>
          <div className="value">{appointments.length}</div>
          <div className="hint">total no histórico</div>
        </div>
        <div className="stat">
          <div className="label">Hoje</div>
          <div className="value">{today.length}</div>
          <div className="hint">consultas para hoje</div>
        </div>
        <div className="stat">
          <div className="label">Próximas</div>
          <div className="value">{upcoming.length}</div>
          <div className="hint">agendadas ou confirmadas</div>
        </div>
      </div>

      <h2 style={{ fontSize: 16, marginBottom: 14 }}>Próximos agendamentos</h2>
      {upcoming.length === 0 ? (
        <div className="card card-pad">
          <div className="empty">
            <span className="emoji">◷</span>
            Nenhum agendamento futuro no momento.
          </div>
        </div>
      ) : (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Paciente</th>
                <th>Data e hora</th>
                <th>Situação</th>
              </tr>
            </thead>
            <tbody>
              {upcoming.map((a) => (
                <tr key={a.id}>
                  <td className="cell-strong">
                    {patientById.get(a.patient_id) ??
                      `Paciente #${a.patient_id}`}
                  </td>
                  <td className="cell-muted">
                    {formatDateTime(a.scheduled_at)}
                  </td>
                  <td>
                    <span
                      className={`badge ${appointmentBadgeClass(a.status)}`}
                    >
                      {statusLabel(a.status)}
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
