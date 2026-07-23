from pydantic import BaseModel

from app.schemas.transaction import FinancialSummary


class MonthlyPoint(BaseModel):
    """Um ponto de série temporal mensal. `month` é "YYYY-MM"."""

    month: str
    value: float


class AnalyticsOverview(BaseModel):
    """Séries mensais REAIS, calculadas dos dados que já chegam ao backend
    (transações, agendamentos, pacientes) — ao contrário da tela Analytics
    ilustrativa do app. Cada série traz só os meses que têm dado, em ordem.

    Síndromes/retenção clínica ficam de fora de propósito: dado clínico não
    sobe pro backend (decisão de fronteira clínica).
    """

    #: Receita líquida por mês (PAGO/PAGAMENTO − REEMBOLSADO/REEMBOLSO).
    monthly_net_revenue: list[MonthlyPoint]
    #: Agendamentos por mês (contagem).
    monthly_appointments: list[MonthlyPoint]
    #: Pacientes novos por mês (por created_at).
    monthly_new_patients: list[MonthlyPoint]


class ReportsOverview(BaseModel):
    """Aggregated indicators for the web painel's Relatórios screen.

    Everything is scoped to the caller's clinic and computed from the same
    tables the app syncs — no separate reporting store, no simulated numbers.
    """

    patients_total: int
    appointments_total: int
    #: Contagem por status ("SCHEDULED", "CONFIRMED", ...). Só status presentes.
    appointments_by_status: dict[str, int]
    #: Financeiro do mesmo período pedido (all-time quando sem start/end).
    finance: FinancialSummary
