"""IdentityMap API — the bridge between BioAcupunt Patient and Twenty Person.

Every cross-system reference goes through this bridge.
No direct foreign keys between BioAcupunt and Twenty tables.
"""

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.deps import get_current_user
from app.db.session import get_db
from app.models.user import User
from app.repositories import identity_map_repository
from app.schemas.identity_map import IdentityMapCreateRequest, IdentityMapResponse

router = APIRouter(prefix="/api/v1/identity-map", tags=["identity-map"])


@router.get("", response_model=list[IdentityMapResponse])
async def list_mappings(
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> list[IdentityMapResponse]:
    """List all identity mappings for the current clinic."""
    mappings = await identity_map_repository.list_all(
        db, clinic_id=current_user.clinic_id
    )
    return mappings


@router.get("/patient/{patient_id}", response_model=IdentityMapResponse | None)
async def get_by_patient(
    patient_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> IdentityMapResponse | None:
    """Find the Twenty Person for a BioAcupunt Patient."""
    mapping = await identity_map_repository.get_by_patient(
        db, clinic_id=current_user.clinic_id, bioacupunt_patient_id=patient_id
    )
    return mapping


@router.get("/person/{person_id}", response_model=IdentityMapResponse | None)
async def get_by_person(
    person_id: str,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> IdentityMapResponse | None:
    """Find the BioAcupunt Patient for a Twenty Person."""
    mapping = await identity_map_repository.get_by_person(
        db, clinic_id=current_user.clinic_id, twenty_person_id=person_id
    )
    return mapping


@router.post("", response_model=IdentityMapResponse, status_code=201)
async def create_mapping(
    request: IdentityMapCreateRequest,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> IdentityMapResponse:
    """Create a Patient ↔ Person mapping.

    Fails if either side is already mapped (unique constraints).
    """
    try:
        mapping = await identity_map_repository.create_mapping(
            db,
            clinic_id=current_user.clinic_id,
            bioacupunt_patient_id=request.bioacupunt_patient_id,
            twenty_person_id=request.twenty_person_id,
            source=request.source,
        )
        return mapping
    except ValueError as e:
        raise HTTPException(status_code=409, detail=str(e))


@router.delete("/patient/{patient_id}")
async def deactivate_mapping(
    patient_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> dict:
    """Deactivate a Patient ↔ Person mapping (soft delete)."""
    deactivated = await identity_map_repository.deactivate_mapping(
        db, clinic_id=current_user.clinic_id, bioacupunt_patient_id=patient_id
    )
    if not deactivated:
        raise HTTPException(status_code=404, detail="Mapping not found")
    return {"status": "deactivated", "patient_id": patient_id}
