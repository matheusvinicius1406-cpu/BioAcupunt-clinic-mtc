import ErrorState from "@/components/ErrorState";
import { apiGet, BackendError } from "@/lib/backend";
import { formatCurrency, formatMonth } from "@/lib/format";
import type { AnalyticsOverview, MonthlyPoint } from "@/lib/types";

export const dynamic = "force-dynamic";

function BarSeries({
  points,
  render,
}: {
  points: MonthlyPoint[];
  render: (value: number) => string;
}) {
  if (points.length === 0) {
    return (
      <div className="empty">
        <span className="emoji">◭</span>
        Sem dados ainda para este período.
      </div>
    );
  }
  const max = Math.max(...points.map((p) => p.value), 1);
  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
      {points.map((p) => (
        <div
          key={p.month}
          style={{ display: "flex", alignItems: "center", gap: 12 }}
        >
          <span
            className="cell-muted"
            style={{ width: 92, fontSize: 13, flexShrink: 0 }}
          >
            {formatMonth(p.month)}
          </span>
          <div
            style={{
              flex: 1,
              background: "var(--bar-track, rgba(127,127,127,0.15))",
              borderRadius: 6,
              overflow: "hidden",
            }}
          >
            <div
              style={{
                width: `${Math.max((p.value / max) * 100, 2)}%`,
                height: 22,
                background: "var(--accent, #4f7cff)",
                borderRadius: 6,
              }}
            />
          </div>
          <span
            className="cell-strong"
            style={{ width: 96, textAlign: "right", fontSize: 13, flexShrink: 0 }}
          >
            {render(p.value)}
          </span>
        </div>
      ))}
    </div>
  );
}

export default async function AnalyticsPage() {
  let data: AnalyticsOverview;
  try {
    data = await apiGet<AnalyticsOverview>("/api/v1/reports/analytics");
  } catch (e) {
    const msg = e instanceof BackendError ? e.message : "erro inesperado";
    return (
      <>
        <div className="page-head">
          <h1>Analytics</h1>
        </div>
        <ErrorState message={msg} />
      </>
    );
  }

  const asInt = (v: number) => String(Math.round(v));

  return (
    <>
      <div className="page-head">
        <h1>Analytics</h1>
        <p>
          Séries mensais reais, calculadas dos dados da própria clínica — sem
          número ilustrativo.
        </p>
      </div>

      <div className="card card-pad" style={{ marginBottom: 20 }}>
        <h2 style={{ fontSize: 15, marginBottom: 16 }}>Receita líquida por mês</h2>
        <BarSeries
          points={data.monthly_net_revenue}
          render={(v) => formatCurrency(v)}
        />
      </div>

      <div className="card card-pad" style={{ marginBottom: 20 }}>
        <h2 style={{ fontSize: 15, marginBottom: 16 }}>Agendamentos por mês</h2>
        <BarSeries points={data.monthly_appointments} render={asInt} />
      </div>

      <div className="card card-pad">
        <h2 style={{ fontSize: 15, marginBottom: 16 }}>Pacientes novos por mês</h2>
        <BarSeries points={data.monthly_new_patients} render={asInt} />
      </div>
    </>
  );
}
