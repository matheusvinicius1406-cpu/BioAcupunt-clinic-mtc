from datetime import datetime
from decimal import Decimal

from sqlalchemy import DateTime, ForeignKey, Integer, Numeric, String, Text, func
from sqlalchemy.orm import Mapped, mapped_column

from app.db.base import Base
from app.models.syncable import SyncableMixin


class PatientStage:
    """Mirror of the app's crm/domain/model/PatientStage enum (name values)."""

    FIRST_CONTACT = "FIRST_CONTACT"
    LEAD = "LEAD"
    ACTIVE = "ACTIVE"
    TREATMENT = "TREATMENT"
    MAINTENANCE = "MAINTENANCE"
    INACTIVE = "INACTIVE"
    CHURNED = "CHURNED"


class CrmPatient(Base, SyncableMixin):
    """The server counterpart of the app's `crm_patients` table.

    Distinct from the clinical `patients` table on purpose: CRM is the
    *relationship* record (pipeline stage, referral source, NPS, tags), not the
    clinical chart. The app already carries this entity with sync identity
    (client_id) but had no server table — so it never left the device.

    Date-ish fields (`birth_date`, `last_visit`, `next_appointment`) are stored
    as strings, exactly as the Android entity stores them: the app writes ISO
    strings or "" and this table must round-trip them without a parse step that
    could reject a real row over a formatting quirk.
    """

    __tablename__ = "crm_patients"

    id: Mapped[int] = mapped_column(primary_key=True)
    clinic_id: Mapped[int] = mapped_column(ForeignKey("clinics.id"), index=True)
    name: Mapped[str] = mapped_column(String(200), index=True)
    phone: Mapped[str] = mapped_column(String(40), default="", server_default="")
    email: Mapped[str] = mapped_column(String(200), default="", server_default="")
    birth_date: Mapped[str] = mapped_column(String(20), default="", server_default="")
    stage: Mapped[str] = mapped_column(
        String(30), default=PatientStage.FIRST_CONTACT, server_default=PatientStage.FIRST_CONTACT, index=True
    )
    total_sessions: Mapped[int] = mapped_column(Integer, default=0, server_default="0")
    total_revenue_brl: Mapped[Decimal] = mapped_column(Numeric(10, 2), default=Decimal("0"), server_default="0")
    last_visit: Mapped[str] = mapped_column(String(20), default="", server_default="")
    next_appointment: Mapped[str] = mapped_column(String(40), default="", server_default="")
    tags: Mapped[str] = mapped_column(String(500), default="", server_default="")
    notes: Mapped[str] = mapped_column(Text, default="", server_default="")
    referral_source: Mapped[str] = mapped_column(String(120), default="", server_default="")
    # Nullable: "not scored" and "scored zero" are different facts.
    nps_score: Mapped[int | None] = mapped_column(Integer, nullable=True)
    health_insurance: Mapped[str] = mapped_column(String(120), default="", server_default="")
    main_complaint: Mapped[str] = mapped_column(Text, default="", server_default="")
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
