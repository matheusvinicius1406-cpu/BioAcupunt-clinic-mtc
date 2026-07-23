from fastapi import APIRouter, Depends, Query
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.deps import get_current_user
from app.db.session import get_db
from app.models.user import User
from app.repositories import crm_repository
from app.schemas.crm import CrmPatientResponse, CrmPipelineSummary

router = APIRouter(prefix="/api/v1/crm", tags=["crm"])


@router.get("", response_model=list[CrmPatientResponse])
async def list_crm_patients(
    stage: str | None = Query(default=None, description="Filtra por estágio do funil."),
    limit: int = Query(default=1000, ge=1, le=5000),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> list[CrmPatientResponse]:
    return await crm_repository.list_crm_patients(
        db, clinic_id=current_user.clinic_id, stage=stage, limit=limit
    )


@router.get("/pipeline", response_model=CrmPipelineSummary)
async def crm_pipeline(
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> CrmPipelineSummary:
    summary = await crm_repository.pipeline_summary(db, clinic_id=current_user.clinic_id)
    return CrmPipelineSummary(**summary)
