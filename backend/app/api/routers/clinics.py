from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.deps import require_role
from app.db.session import get_db
from app.models.clinic import Clinic
from app.models.user import User, UserRole
from app.schemas.clinic import ClinicCreateRequest, ClinicResponse

router = APIRouter(prefix="/api/v1/clinics", tags=["clinics"])


@router.post("", response_model=ClinicResponse, status_code=201)
async def create_clinic(
    payload: ClinicCreateRequest,
    db: AsyncSession = Depends(get_db),
    _current_user: User = Depends(require_role(UserRole.ADMIN)),
) -> Clinic:
    clinic = Clinic(name=payload.name)
    db.add(clinic)
    await db.commit()
    await db.refresh(clinic)
    return clinic
