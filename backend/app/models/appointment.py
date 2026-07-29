from datetime import datetime
from decimal import Decimal

from sqlalchemy import DateTime, ForeignKey, Index, Numeric, String, Text, func
from sqlalchemy.orm import Mapped, mapped_column

from app.db.base import Base
from app.models.syncable import SyncableMixin


class AppointmentStatus:
    SCHEDULED = "SCHEDULED"
    CONFIRMED = "CONFIRMED"
    CANCELLED = "CANCELLED"
    COMPLETED = "COMPLETED"
    NO_SHOW = "NO_SHOW"


class Appointment(Base, SyncableMixin):
    __tablename__ = "appointments"
    __table_args__ = (
        # Toda leitura filtra os dois juntos (ver appointment_repository.py) —
        # um índice em clinic_id sozinho não serve bem essa combinação.
        Index("ix_appointments_clinic_deleted", "clinic_id", "deleted_at"),
    )

    id: Mapped[int] = mapped_column(primary_key=True)
    clinic_id: Mapped[int] = mapped_column(ForeignKey("clinics.id"), index=True)
    patient_id: Mapped[int] = mapped_column(ForeignKey("patients.id"), index=True)
    # Nullable: a booking can reach the server from a device that does not yet
    # know which professional will take it. Rejecting the sync over a missing
    # professional would lose a real appointment to a detail that can be filled
    # in later.
    professional_id: Mapped[int | None] = mapped_column(
        ForeignKey("users.id"), index=True, nullable=True
    )
    scheduled_at: Mapped[datetime] = mapped_column(DateTime(timezone=True))
    # Indexed: reports_repository.count_appointments_by_status() GROUP BYs this.
    status: Mapped[str] = mapped_column(String(20), default=AppointmentStatus.SCHEDULED, index=True)
    notes: Mapped[str] = mapped_column(Text, default="")
    # Numeric, not float: binary floating point accumulates error over sums, and
    # this column feeds the revenue figures the practice is run on.
    value_brl: Mapped[Decimal] = mapped_column(Numeric(10, 2), default=Decimal("0"))
    paid: Mapped[bool] = mapped_column(default=False)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
