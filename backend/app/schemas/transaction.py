from datetime import date, datetime

from pydantic import BaseModel

# amount fields are typed `float` on purpose: the model stores Numeric(10,2)
# (Decimal), but the web client only reads and formats these, and Pydantic v2
# serializes Decimal as a JSON *string* by default — which would force the
# browser to parse it. Exposing float gives the panel plain JSON numbers.


class TransactionResponse(BaseModel):
    id: int
    clinic_id: int
    patient_id: int | None
    appointment_id: int | None
    amount_brl: float
    occurred_on: date
    type: str
    method: str
    category: str
    status: str
    notes: str
    created_at: datetime

    model_config = {"from_attributes": True}


class FinancialSummary(BaseModel):
    """Aggregates over a period, mirroring the Android app's financeiro math
    (TransacaoDao / TransacaoRepositoryImpl) so app and web never disagree.

    `start`/`end` are null when the summary covers the whole history.
    Tombstoned rows (deleted_at set) are excluded — a deleted payment must not
    keep inflating revenue.
    """

    start: date | None
    end: date | None
    #: PAGO + PAGAMENTO menos REEMBOLSADO + REEMBOLSO — o que de fato entrou.
    net_revenue_brl: float
    #: PAGO + PAGAMENTO (bruto recebido, antes de reembolsos).
    payments_brl: float
    #: REEMBOLSADO + REEMBOLSO.
    refunds_brl: float
    #: Status PENDENTE (faturado, ainda não pago).
    pending_brl: float
    #: type DESPESA (saídas da clínica).
    expenses_brl: float
    transaction_count: int
