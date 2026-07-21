"""Server-assigned revisions — the backbone of conflict detection.

Every syncable row carries a `rev`: a number handed out by the server, unique
and strictly increasing *within a clinic*. It does two jobs:

1. **Delta pull.** A client asks for everything above the last `rev` it saw.
   No pagination cursors, no date maths, no "did I already get this?".

2. **Conflict detection.** A client pushing an edit sends the `rev` its edit was
   based on. If the row has moved on since, the edit was written against a
   version that no longer exists, and the server refuses it.

Why a counter and not `updated_at`: timestamps come from device clocks. A tablet
whose clock is ten minutes slow would make a genuinely newer note look older, and
the app would discard the wrong version — silently, and in a patient chart. The
counter is issued in one place, so there is nothing to disagree about.
"""

from datetime import datetime

from sqlalchemy import BigInteger, DateTime, ForeignKey, func, select
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import Mapped, mapped_column

from app.db.base import Base


class ClinicRevision(Base):
    """Holds the last revision number issued for one clinic."""

    __tablename__ = "clinic_revisions"

    clinic_id: Mapped[int] = mapped_column(ForeignKey("clinics.id"), primary_key=True)
    last_rev: Mapped[int] = mapped_column(BigInteger, default=0, nullable=False)
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), onupdate=func.now()
    )


async def next_revision(db: AsyncSession, *, clinic_id: int) -> int:
    """Issues the next revision for a clinic.

    Takes a row lock (`with_for_update`) so two devices pushing at the same
    moment cannot be handed the same number. Duplicate revisions would make a
    delta pull skip a record — the client would ask for everything above N,
    and the second row numbered N would never be sent. A patient created on the
    tablet would simply never appear on the phone, with nothing anywhere
    reporting an error.

    SQLite ignores `FOR UPDATE`, but its writes are serialised anyway, so the
    guarantee holds in tests as well as in Postgres.
    """
    result = await db.execute(
        select(ClinicRevision).where(ClinicRevision.clinic_id == clinic_id).with_for_update()
    )
    row = result.scalar_one_or_none()

    if row is None:
        row = ClinicRevision(clinic_id=clinic_id, last_rev=1)
        db.add(row)
        await db.flush()
        return row.last_rev

    row.last_rev += 1
    await db.flush()
    return row.last_rev


async def current_revision(db: AsyncSession, *, clinic_id: int) -> int:
    """The clinic's newest revision, or 0 when nothing has ever synced."""
    result = await db.execute(
        select(ClinicRevision.last_rev).where(ClinicRevision.clinic_id == clinic_id)
    )
    return result.scalar_one_or_none() or 0
