from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.appointment import Appointment
from app.models.patient import Patient


async def count_patients(db: AsyncSession, *, clinic_id: int) -> int:
    stmt = select(func.count()).where(
        Patient.clinic_id == clinic_id,
        Patient.deleted_at.is_(None),
    )
    return int((await db.execute(stmt)).scalar_one())


async def count_appointments_by_status(db: AsyncSession, *, clinic_id: int) -> dict[str, int]:
    """Grouped count, tombstones excluded. Absent statuses simply don't appear."""
    stmt = (
        select(Appointment.status, func.count())
        .where(
            Appointment.clinic_id == clinic_id,
            Appointment.deleted_at.is_(None),
        )
        .group_by(Appointment.status)
    )
    rows = (await db.execute(stmt)).all()
    return {status: int(count) for status, count in rows}
