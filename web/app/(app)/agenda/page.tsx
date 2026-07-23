import ErrorState from "@/components/ErrorState";
import { apiGet, BackendError } from "@/lib/backend";
import {
  appointmentBadgeClass,
  formatDate,
  formatTime,
  statusLabel,
} from "@/lib/format";
import type { Appointment, Patient } from "@/lib/types";

export const dynamic = "force-dynamic";

// Agrupa agendamentos por dia (chave YYYY-MM-DD), ordenado.
function groupByDay(items: Appointment[]): [string, Appointment[]][] {
  const map = new Map<string, Appointment[]>();
  for (const a of items) {
    const key = a.scheduled_at.slice(0, 10);
    (map.get(key) ?? map.set(key, []).get(key)!).push(a);
  }
  return [...map.entries()].sort((a, b) => a[0].localeCompare(b[0]));
}

export default async function AgendaPage() {
  let appointments: Appointment[];
  let patients: Patient[];
  try {
    [appointments, patients] = await Promise.all([
      apiGet<Appointment[]>("/api/v1/appointments"),
      apiGet<Patient[]>("/api/v1/patients"),
    ]);
  } catch (e) {
    const msg = e instanceof BackendError ? e.message : "erro inesperado";
    return (
      <>
        <div className="page-head">
          <h1>Agenda</h1>
        </div>
        <ErrorState message={msg} />
      </>
    );
  }

  const patientById = new Map(patients.map((p) => [p.id, p.name]));
  const sorted = [...appointments].sort(
    (a, b) =>
      new Date(a.scheduled_at).getTime() - new Date(b.scheduled_at).getTime(),
  );
  const groups = groupByDay(sorted);

  return (
    <>
      <div className="page-head">
        <h1>Agenda</h1>
        <p>Consultas agendadas na clínica, agrupadas por dia.</p>
      </div>

      {groups.length === 0 ? (
        <div className="card card-pad">
          <div className="empty">
            <span className="emoji">◷</span>
            Nenhum agendamento registrado.
          </div>
        </div>
      ) : (
        groups.map(([day, items]) => (
          <section key={day} style={{ marginBottom: 26 }}>
            <h2
              style={{
                fontSize: 14,
                color: "var(--text-muted)",
                marginBottom: 10,
                textTransform: "capitalize",
              }}
            >
              {formatDate(`${day}T00:00:00`)}
            </h2>
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th style={{ width: 90 }}>Hora</th>
                    <th>Paciente</th>
                    <th>Situação</th>
                    <th>Observações</th>
                  </tr>
                </thead>
                <tbody>
                  {items.map((a) => (
                    <tr key={a.id}>
                      <td className="cell-strong">
                        {formatTime(a.scheduled_at)}
                      </td>
                      <td>
                        {patientById.get(a.patient_id) ??
                          `Paciente #${a.patient_id}`}
                      </td>
                      <td>
                        <span
                          className={`badge ${appointmentBadgeClass(a.status)}`}
                        >
                          {statusLabel(a.status)}
                        </span>
                      </td>
                      <td className="cell-muted">{a.notes || "—"}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>
        ))
      )}
    </>
  );
}
