import ErrorState from "@/components/ErrorState";
import { apiGet, BackendError } from "@/lib/backend";
import { formatDate, patientBadgeClass } from "@/lib/format";
import type { Patient } from "@/lib/types";

export const dynamic = "force-dynamic";

export default async function PacientesPage() {
  let patients: Patient[];
  try {
    patients = await apiGet<Patient[]>("/api/v1/patients");
  } catch (e) {
    const msg = e instanceof BackendError ? e.message : "erro inesperado";
    return (
      <>
        <Head count={null} />
        <ErrorState message={msg} />
      </>
    );
  }

  const sorted = [...patients].sort((a, b) => a.name.localeCompare(b.name, "pt-BR"));

  return (
    <>
      <Head count={patients.length} />
      {sorted.length === 0 ? (
        <div className="card card-pad">
          <div className="empty">
            <span className="emoji">◐</span>
            Nenhum paciente cadastrado ainda.
          </div>
        </div>
      ) : (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Nome</th>
                <th>Situação</th>
                <th>Cadastrado em</th>
                <th>Atualizado em</th>
              </tr>
            </thead>
            <tbody>
              {sorted.map((p) => (
                <tr key={p.id}>
                  <td className="cell-strong">{p.name}</td>
                  <td>
                    <span className={`badge ${patientBadgeClass(p.status)}`}>
                      {p.status}
                    </span>
                  </td>
                  <td className="cell-muted">{formatDate(p.created_at)}</td>
                  <td className="cell-muted">{formatDate(p.updated_at)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </>
  );
}

function Head({ count }: { count: number | null }) {
  return (
    <div className="page-head">
      <h1>Pacientes</h1>
      <p>
        {count === null
          ? "Cadastro de pacientes da clínica."
          : `${count} paciente${count === 1 ? "" : "s"} — somente leitura. Prontuário e dados clínicos ficam no aplicativo.`}
      </p>
    </div>
  );
}
