from datetime import datetime

from sqlalchemy import DateTime, ForeignKey, String, func
from sqlalchemy.orm import Mapped, mapped_column

from app.db.base import Base
from app.models.syncable import SyncableMixin


class Patient(Base, SyncableMixin):
    """A patient record.

    The server-side counterpart of the app's `crm_patients` table — the single
    patient registry. (The app carried two rival patient tables for a while;
    the CRM one is what survived, and this mirrors it.)

    `updated_at` and `deleted_at` come from SyncableMixin, along with `rev` and
    `client_id`.
    """

    __tablename__ = "patients"

    id: Mapped[int] = mapped_column(primary_key=True)
    clinic_id: Mapped[int] = mapped_column(ForeignKey("clinics.id"), index=True)
    name: Mapped[str] = mapped_column(String(200))
    document_hash: Mapped[str | None] = mapped_column(String(64), nullable=True, index=True)
    status: Mapped[str] = mapped_column(String(20), default="ACTIVE")
    phone: Mapped[str] = mapped_column(String(40), default="")
    email: Mapped[str] = mapped_column(String(200), default="")
    stage: Mapped[str] = mapped_column(String(30), default="FIRST_CONTACT")
    notes: Mapped[str] = mapped_column(String(2000), default="")
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
