from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.crm_patient import CrmPatient


async def list_crm_patients(
    db: AsyncSession,
    *,
    clinic_id: int,
    stage: str | None = None,
    limit: int = 1000,
) -> list[CrmPatient]:
    """Scoped by clinic, tombstones excluded, most recently updated first."""
    stmt = select(CrmPatient).where(
        CrmPatient.clinic_id == clinic_id,
        CrmPatient.deleted_at.is_(None),
    )
    if stage is not None:
        stmt = stmt.where(CrmPatient.stage == stage)
    stmt = stmt.order_by(CrmPatient.updated_at.desc(), CrmPatient.id.desc()).limit(limit)
    result = await db.execute(stmt)
    return list(result.scalars().all())


async def pipeline_summary(db: AsyncSession, *, clinic_id: int) -> dict:
    base = (
        CrmPatient.clinic_id == clinic_id,
        CrmPatient.deleted_at.is_(None),
    )

    by_stage_rows = (
        await db.execute(
            select(CrmPatient.stage, func.count())
            .where(*base)
            .group_by(CrmPatient.stage)
        )
    ).all()
    by_stage = {stage: int(count) for stage, count in by_stage_rows}

    revenue = (
        await db.execute(
            select(func.coalesce(func.sum(CrmPatient.total_revenue_brl), 0)).where(*base)
        )
    ).scalar_one()

    # Average only over rows that were actually scored (nps_score not null), so
    # an unscored lead does not drag the average toward zero.
    avg_nps = (
        await db.execute(
            select(func.avg(CrmPatient.nps_score)).where(
                *base, CrmPatient.nps_score.is_not(None)
            )
        )
    ).scalar_one()

    return {
        "total": sum(by_stage.values()),
        "by_stage": by_stage,
        "total_revenue_brl": float(revenue),
        "average_nps": float(avg_nps) if avg_nps is not None else None,
    }
