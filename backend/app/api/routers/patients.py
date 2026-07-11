from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.deps import get_current_user
from app.core.errors import AppError
from app.db.session import get_db
from app.models.user import User
from app.repositories import patient_repository
from app.schemas.patient import PatientCreateRequest, PatientResponse
from app.services.audit_service import record_access

router = APIRouter(prefix="/api/v1/patients", tags=["patients"])


@router.post("", response_model=PatientResponse, status_code=201)
async def create_patient(
    payload: PatientCreateRequest,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> PatientResponse:
    patient = await patient_repository.create_patient(
        db, clinic_id=current_user.clinic_id, name=payload.name, document=payload.document
    )
    await record_access(
        db,
        user_id=current_user.id,
        clinic_id=current_user.clinic_id,
        resource_type="Patient",
        resource_id=patient.id,
        action="CREATE",
    )
    return patient


@router.get("", response_model=list[PatientResponse])
async def list_patients(
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> list[PatientResponse]:
    return await patient_repository.list_patients(db, clinic_id=current_user.clinic_id)


@router.get("/{patient_id}", response_model=PatientResponse)
async def get_patient(
    patient_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> PatientResponse:
    patient = await patient_repository.get_patient(db, clinic_id=current_user.clinic_id, patient_id=patient_id)
    if patient is None:
        raise AppError("Paciente não encontrado.", status_code=404, code="patient_not_found")

    await record_access(
        db,
        user_id=current_user.id,
        clinic_id=current_user.clinic_id,
        resource_type="Patient",
        resource_id=patient.id,
        action="READ",
    )
    return patient
