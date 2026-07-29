from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.security import hash_document
from app.models.patient import Patient


async def create_patient(db: AsyncSession, *, clinic_id: int, name: str, document: str | None) -> Patient:
    patient = Patient(
        clinic_id=clinic_id,
        name=name,
        document_hash=hash_document(document) if document else None,
    )
    db.add(patient)
    await db.commit()
    await db.refresh(patient)
    return patient


async def list_patients(db: AsyncSession, *, clinic_id: int, limit: int = 1000) -> list[Patient]:
    """Scoped by clinic, tombstones excluded, most recently updated first."""
    stmt = (
        select(Patient)
        .where(Patient.clinic_id == clinic_id, Patient.deleted_at.is_(None))
        .order_by(Patient.updated_at.desc(), Patient.id.desc())
        .limit(limit)
    )
    result = await db.execute(stmt)
    return list(result.scalars().all())


async def get_patient(db: AsyncSession, *, clinic_id: int, patient_id: int) -> Patient | None:
    """Scoped by clinic_id: a patient from another clinic must never resolve here."""
    result = await db.execute(
        select(Patient).where(
            Patient.id == patient_id,
            Patient.clinic_id == clinic_id,
            Patient.deleted_at.is_(None),
        )
    )
    return result.scalar_one_or_none()
