from pydantic import BaseModel

from app.schemas.transaction import FinancialSummary


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
