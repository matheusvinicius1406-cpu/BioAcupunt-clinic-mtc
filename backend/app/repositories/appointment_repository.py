from datetime import datetime

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.appointment import Appointment


async def create_appointment(
    db: AsyncSession,
    *,
    clinic_id: int,
    patient_id: int,
    professional_id: int,
    scheduled_at: datetime,
    notes: str,
) -> Appointment:
    appointment = Appointment(
        clinic_id=clinic_id,
        patient_id=patient_id,
        professional_id=professional_id,
        scheduled_at=scheduled_at,
        notes=notes,
    )
    db.add(appointment)
    await db.commit()
    await db.refresh(appointment)
    return appointment


async def list_appointments(db: AsyncSession, *, clinic_id: int) -> list[Appointment]:
    result = await db.execute(select(Appointment).where(Appointment.clinic_id == clinic_id))
    return list(result.scalars().all())


async def get_appointment(db: AsyncSession, *, clinic_id: int, appointment_id: int) -> Appointment | None:
    result = await db.execute(
        select(Appointment).where(Appointment.id == appointment_id, Appointment.clinic_id == clinic_id)
    )
    return result.scalar_one_or_none()


async def update_status(db: AsyncSession, *, appointment: Appointment, status: str) -> Appointment:
    appointment.status = status
    await db.commit()
    await db.refresh(appointment)
    return appointment
