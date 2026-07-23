from datetime import date

from fastapi import APIRouter, Depends, Query
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.deps import get_current_user
from app.db.session import get_db
from app.models.user import User
from app.repositories import transaction_repository
from app.schemas.transaction import FinancialSummary, TransactionResponse

router = APIRouter(prefix="/api/v1/transactions", tags=["transactions"])


@router.get("", response_model=list[TransactionResponse])
async def list_transactions(
    start: date | None = Query(default=None, description="Início do período (YYYY-MM-DD), inclusivo."),
    end: date | None = Query(default=None, description="Fim do período (YYYY-MM-DD), inclusivo."),
    limit: int = Query(default=500, ge=1, le=2000),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> list[TransactionResponse]:
    return await transaction_repository.list_transactions(
        db, clinic_id=current_user.clinic_id, start=start, end=end, limit=limit
    )


@router.get("/summary", response_model=FinancialSummary)
async def transactions_summary(
    start: date | None = Query(default=None, description="Início do período (YYYY-MM-DD), inclusivo."),
    end: date | None = Query(default=None, description="Fim do período (YYYY-MM-DD), inclusivo."),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> FinancialSummary:
    totals = await transaction_repository.summary(
        db, clinic_id=current_user.clinic_id, start=start, end=end
    )
    return FinancialSummary(start=start, end=end, **totals)
