"""IdentityMap repository — CRUD for the Patient ↔ Person bridge.

All operations are scoped by clinic_id. No cross-tenant mapping is possible.
"""

from sqlalchemy import select, update
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.identity_map import CrmIdentityMap


async def get_by_patient(
    db: AsyncSession,
    *,
    clinic_id: int,
    bioacupunt_patient_id: int,
) -> CrmIdentityMap | None:
    """Find the Twenty Person mapping for a BioAcupunt Patient."""
    result = await db.execute(
        select(CrmIdentityMap).where(
            CrmIdentityMap.clinic_id == clinic_id,
            CrmIdentityMap.bioacupunt_patient_id == bioacupunt_patient_id,
            CrmIdentityMap.status == "ACTIVE",
        )
    )
    return result.scalar_one_or_none()


async def get_by_person(
    db: AsyncSession,
    *,
    clinic_id: int,
    twenty_person_id: str,
) -> CrmIdentityMap | None:
    """Find the BioAcupunt Patient mapping for a Twenty Person."""
    result = await db.execute(
        select(CrmIdentityMap).where(
            CrmIdentityMap.clinic_id == clinic_id,
            CrmIdentityMap.twenty_person_id == twenty_person_id,
            CrmIdentityMap.status == "ACTIVE",
        )
    )
    return result.scalar_one_or_none()


async def create_mapping(
    db: AsyncSession,
    *,
    clinic_id: int,
    bioacupunt_patient_id: int,
    twenty_person_id: str,
    source: str = "MANUAL",
) -> CrmIdentityMap:
    """Create a new Patient ↔ Person mapping.

    Raises ValueError if mapping already exists (unique constraint).
    """
    # Check for existing mapping
    existing = await get_by_patient(
        db, clinic_id=clinic_id, bioacupunt_patient_id=bioacupunt_patient_id
    )
    if existing:
        raise ValueError(
            f"Patient {bioacupunt_patient_id} already mapped to "
            f"Twenty Person {existing.twenty_person_id}"
        )

    existing_person = await get_by_person(
        db, clinic_id=clinic_id, twenty_person_id=twenty_person_id
    )
    if existing_person:
        raise ValueError(
            f"Twenty Person {twenty_person_id} already mapped to "
            f"Patient {existing_person.bioacupunt_patient_id}"
        )

    mapping = CrmIdentityMap(
        clinic_id=clinic_id,
        bioacupunt_patient_id=bioacupunt_patient_id,
        twenty_person_id=twenty_person_id,
        source=source,
        status="ACTIVE",
    )
    db.add(mapping)
    await db.commit()
    await db.refresh(mapping)
    return mapping


async def deactivate_mapping(
    db: AsyncSession,
    *,
    clinic_id: int,
    bioacupunt_patient_id: int,
) -> bool:
    """Deactivate a mapping (soft delete)."""
    result = await db.execute(
        update(CrmIdentityMap)
        .where(
            CrmIdentityMap.clinic_id == clinic_id,
            CrmIdentityMap.bioacupunt_patient_id == bioacupunt_patient_id,
            CrmIdentityMap.status == "ACTIVE",
        )
        .values(status="INACTIVE")
    )
    await db.commit()
    return result.rowcount > 0


async def list_all(
    db: AsyncSession,
    *,
    clinic_id: int,
    limit: int = 100,
) -> list[CrmIdentityMap]:
    """List all active identity mappings for a clinic."""
    result = await db.execute(
        select(CrmIdentityMap).where(
            CrmIdentityMap.clinic_id == clinic_id,
            CrmIdentityMap.status == "ACTIVE",
        ).limit(limit)
    )
    return list(result.scalars().all())
