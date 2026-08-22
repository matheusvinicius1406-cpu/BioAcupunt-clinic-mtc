"""CrmIdentityMap — the bridge between BioAcupunt Patient and Twenty Person.

This is the ONLY place where cross-system identity is tracked.
Every Patient ↔ Person mapping goes through this table.

Rules:
- UNIQUE(tenant_id, bioacupunt_patient_id) — one patient maps to one person
- UNIQUE(tenant_id, twenty_person_id) — one person maps to one patient
- No cross-tenant mapping ever
"""

from datetime import datetime

from sqlalchemy import DateTime, ForeignKey, Index, Integer, String, UniqueConstraint, func
from sqlalchemy.orm import Mapped, mapped_column

from app.db.base import Base


class CrmIdentityMap(Base):
    """Maps BioAcupunt Patient ↔ Twenty Person.

    This is the BRIDGE entity — neither clinical nor CRM, but the
    connection between them.
    """

    __tablename__ = "crm_identity_map"
    __table_args__ = (
        UniqueConstraint(
            "clinic_id",
            "bioacupunt_patient_id",
            name="uq_identity_map_patient",
        ),
        UniqueConstraint(
            "clinic_id",
            "twenty_person_id",
            name="uq_identity_map_person",
        ),
        Index("ix_identity_map_clinic", "clinic_id"),
        Index("ix_identity_map_patient", "bioacupunt_patient_id"),
        Index("ix_identity_map_person", "twenty_person_id"),
    )

    id: Mapped[int] = mapped_column(primary_key=True)
    clinic_id: Mapped[int] = mapped_column(ForeignKey("clinics.id"), nullable=False)
    bioacupunt_patient_id: Mapped[int] = mapped_column(Integer, nullable=False)
    twenty_person_id: Mapped[str] = mapped_column(String(100), nullable=False)
    source: Mapped[str] = mapped_column(
        String(20), default="MANUAL", server_default="MANUAL"
    )  # MANUAL, MIGRATED, SYNCED
    status: Mapped[str] = mapped_column(
        String(20), default="ACTIVE", server_default="ACTIVE"
    )  # ACTIVE, INACTIVE, CONFLICT
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now()
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), onupdate=func.now()
    )
