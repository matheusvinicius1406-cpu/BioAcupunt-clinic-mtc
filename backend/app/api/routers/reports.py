from datetime import date

from fastapi import APIRouter, Depends, Query
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.deps import get_current_user
from app.db.session import get_db
from app.models.user import User
from app.repositories import reports_repository, transaction_repository
from app.schemas.report import AnalyticsOverview, ReportsOverview
from app.schemas.transaction import FinancialSummary

router = APIRouter(prefix="/api/v1/reports", tags=["reports"])


@router.get("/overview", response_model=ReportsOverview)
async def reports_overview(
    start: date | None = Query(default=None, description="Início do período financeiro (YYYY-MM-DD)."),
    end: date | None = Query(default=None, description="Fim do período financeiro (YYYY-MM-DD)."),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> ReportsOverview:
    clinic_id = current_user.clinic_id
    patients_total = await reports_repository.count_patients(db, clinic_id=clinic_id)
    by_status = await reports_repository.count_appointments_by_status(db, clinic_id=clinic_id)
    finance_totals = await transaction_repository.summary(
        db, clinic_id=clinic_id, start=start, end=end
    )
    return ReportsOverview(
        patients_total=patients_total,
        appointments_total=sum(by_status.values()),
        appointments_by_status=by_status,
        finance=FinancialSummary(start=start, end=end, **finance_totals),
    )


@router.get("/analytics", response_model=AnalyticsOverview)
async def reports_analytics(
    start: date | None = Query(default=None, description="Início do período (YYYY-MM-DD)."),
    end: date | None = Query(default=None, description="Fim do período (YYYY-MM-DD)."),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> AnalyticsOverview:
    series = await reports_repository.monthly_analytics(
        db, clinic_id=current_user.clinic_id, start=start, end=end
    )
    return AnalyticsOverview(**series)
