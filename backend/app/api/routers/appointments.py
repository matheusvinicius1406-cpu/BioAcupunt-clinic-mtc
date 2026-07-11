from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.deps import get_current_user
from app.core.errors import AppError
from app.db.session import get_db
from app.models.user import User
from app.repositories import appointment_repository
from app.schemas.appointment import (
    AppointmentCreateRequest,
    AppointmentResponse,
    AppointmentUpdateStatusRequest,
)

router = APIRouter(prefix="/api/v1/appointments", tags=["appointments"])


@router.post("", response_model=AppointmentResponse, status_code=201)
async def create_appointment(
    payload: AppointmentCreateRequest,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> AppointmentResponse:
    return await appointment_repository.create_appointment(
        db,
        clinic_id=current_user.clinic_id,
        patient_id=payload.patient_id,
        professional_id=payload.professional_id,
        scheduled_at=payload.scheduled_at,
        notes=payload.notes,
    )


@router.get("", response_model=list[AppointmentResponse])
async def list_appointments(
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> list[AppointmentResponse]:
    return await appointment_repository.list_appointments(db, clinic_id=current_user.clinic_id)


@router.patch("/{appointment_id}/status", response_model=AppointmentResponse)
async def update_status(
    appointment_id: int,
    payload: AppointmentUpdateStatusRequest,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> AppointmentResponse:
    appointment = await appointment_repository.get_appointment(
        db, clinic_id=current_user.clinic_id, appointment_id=appointment_id
    )
    if appointment is None:
        raise AppError("Agendamento não encontrado.", status_code=404, code="appointment_not_found")
    return await appointment_repository.update_status(db, appointment=appointment, status=payload.status)
