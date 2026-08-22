"""CRM Repository — honest about data sources.

LOCAL (CrmPatient): patient CRM metadata, pipeline stages, NPS, referral source.
TWENTY (via Gateway): leads, tasks, activities, organizations, opportunities, audit.
BRIDGE (IdentityMap): Patient ↔ Person mapping.

Functions that query CrmPatient directly are LOCAL.
Functions that need Twenty data raise NotImplementedError when Twenty is offline.
"""

from datetime import datetime, timedelta, timezone

from sqlalchemy import func, or_, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.crm_patient import CrmPatient


# ═════════════════════════════════════════════════════════════════════
# LOCAL — queries CrmPatient directly (correct semantics)
# ═════════════════════════════════════════════════════════════════════


async def list_crm_patients(
    db: AsyncSession,
    *,
    clinic_id: int,
    stage: str | None = None,
    limit: int = 1000,
) -> list[CrmPatient]:
    """List CRM patients — scoped by clinic, tombstones excluded."""
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
    """Pipeline summary from local CrmPatient data."""
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


async def count_patients(
    db: AsyncSession,
    *,
    clinic_id: int,
    active_only: bool = False,
) -> int:
    base = [
        CrmPatient.clinic_id == clinic_id,
        CrmPatient.deleted_at.is_(None),
    ]
    if active_only:
        base.append(~CrmPatient.stage.in_(["INACTIVE", "LOST"]))
    result = await db.execute(
        select(func.count()).select_from(CrmPatient).where(*base)
    )
    return result.scalar_one()


async def count_inactive_patients(
    db: AsyncSession,
    *,
    clinic_id: int,
) -> int:
    result = await db.execute(
        select(func.count()).select_from(CrmPatient).where(
            CrmPatient.clinic_id == clinic_id,
            CrmPatient.deleted_at.is_(None),
            CrmPatient.stage == "INACTIVE",
        )
    )
    return result.scalar_one()


async def search_local_patients(
    db: AsyncSession,
    *,
    clinic_id: int,
    query: str,
    limit: int = 20,
) -> list[CrmPatient]:
    """Search local CrmPatient records by name, email, phone, notes."""
    pattern = f"%{query}%"
    stmt = select(CrmPatient).where(
        CrmPatient.clinic_id == clinic_id,
        CrmPatient.deleted_at.is_(None),
        or_(
            CrmPatient.name.ilike(pattern),
            CrmPatient.email.ilike(pattern),
            CrmPatient.phone.ilike(pattern),
            CrmPatient.notes.ilike(pattern),
            CrmPatient.main_complaint.ilike(pattern),
            CrmPatient.tags.ilike(pattern),
        ),
    ).order_by(CrmPatient.updated_at.desc()).limit(limit)
    result = await db.execute(stmt)
    return list(result.scalars().all())


# ═════════════════════════════════════════════════════════════════════
# TWENTY-DEPENDENT — these require Twenty Gateway (Phase 7.3)
# When Twenty is offline, endpoints using these must return 503.
# ═════════════════════════════════════════════════════════════════════


class TwentyOfflineError(Exception):
    """Raised when a Twenty-dependent operation is attempted but Twenty is offline."""
    pass


# These will be wired to TwentyGateway in Phase 7.3+
# For now, they raise TwentyOfflineError so endpoints can return 503

async def require_twenty_leads(*args, **kwargs):
    raise TwentyOfflineError("Leads require Twenty (not yet connected)")

async def require_twenty_tasks(*args, **kwargs):
    raise TwentyOfflineError("Tasks require Twenty (not yet connected)")

async def require_twenty_activities(*args, **kwargs):
    raise TwentyOfflineError("Activities require Twenty (not yet connected)")

async def require_twenty_timeline(*args, **kwargs):
    raise TwentyOfflineError("Unified timeline requires Twenty (not yet connected)")

async def require_twenty_audit(*args, **kwargs):
    raise TwentyOfflineError("Audit trail requires Twenty (not yet connected)")

async def require_twenty_referrals(*args, **kwargs):
    raise TwentyOfflineError("Referrals require Twenty (not yet connected)")
