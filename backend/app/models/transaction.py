from datetime import date, datetime
from decimal import Decimal

from sqlalchemy import Date, DateTime, ForeignKey, Numeric, String, Text, func
from sqlalchemy.orm import Mapped, mapped_column

from app.db.base import Base
from app.models.syncable import SyncableMixin


class TransactionType:
    PAYMENT = "PAGAMENTO"
    REFUND = "REEMBOLSO"
    EXPENSE = "DESPESA"


class TransactionStatus:
    PAID = "PAGO"
    PENDING = "PENDENTE"
    REFUNDED = "REEMBOLSADO"


class Transaction(Base, SyncableMixin):
    """A financial movement — the server side of the app's `transacoes` table."""

    __tablename__ = "transactions"

    id: Mapped[int] = mapped_column(primary_key=True)
    clinic_id: Mapped[int] = mapped_column(ForeignKey("clinics.id"), index=True)
    # Nullable: not every movement belongs to a patient (rent, supplies), and a
    # payment whose patient was deleted must survive as a financial record.
    patient_id: Mapped[int | None] = mapped_column(
        ForeignKey("patients.id"), index=True, nullable=True
    )
    appointment_id: Mapped[int | None] = mapped_column(
        ForeignKey("appointments.id"), index=True, nullable=True
    )
    amount_brl: Mapped[Decimal] = mapped_column(Numeric(10, 2), default=Decimal("0"))
    occurred_on: Mapped[date] = mapped_column(Date, index=True)
    type: Mapped[str] = mapped_column(String(20), default=TransactionType.PAYMENT)
    method: Mapped[str] = mapped_column(String(20), default="PIX")
    category: Mapped[str] = mapped_column(String(40), default="SESSÃO")
    status: Mapped[str] = mapped_column(String(20), default=TransactionStatus.PAID)
    notes: Mapped[str] = mapped_column(Text, default="")
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
