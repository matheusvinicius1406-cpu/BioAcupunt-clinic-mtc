from collections import defaultdict

from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.appointment import Appointment
from app.models.patient import Patient
from app.models.transaction import Transaction, TransactionStatus, TransactionType


async def count_patients(db: AsyncSession, *, clinic_id: int) -> int:
    stmt = select(func.count()).where(
        Patient.clinic_id == clinic_id,
        Patient.deleted_at.is_(None),
    )
    return int((await db.execute(stmt)).scalar_one())


async def count_appointments_by_status(db: AsyncSession, *, clinic_id: int) -> dict[str, int]:
    """Grouped count, tombstones excluded. Absent statuses simply don't appear."""
    stmt = (
        select(Appointment.status, func.count())
        .where(
            Appointment.clinic_id == clinic_id,
            Appointment.deleted_at.is_(None),
        )
        .group_by(Appointment.status)
    )
    rows = (await db.execute(stmt)).all()
    return {status: int(count) for status, count in rows}


def _sorted_points(buckets: dict[str, float]) -> list[dict]:
    """Month → value dict into a chronologically sorted list of points.

    "YYYY-MM" sorts correctly as a plain string, so no date parsing needed.
    """
    return [{"month": m, "value": buckets[m]} for m in sorted(buckets)]


async def monthly_analytics(db: AsyncSession, *, clinic_id: int) -> dict[str, list[dict]]:
    """Real monthly series, bucketed in Python so the same code works on both
    SQLite (tests) and Postgres (prod) without dialect-specific date functions.

    Tombstones excluded everywhere: a deleted row must not show up in a trend.
    """
    # ── Receita líquida por mês ──
    tx_rows = (
        await db.execute(
            select(
                Transaction.occurred_on,
                Transaction.amount_brl,
                Transaction.status,
                Transaction.type,
            ).where(
                Transaction.clinic_id == clinic_id,
                Transaction.deleted_at.is_(None),
            )
        )
    ).all()
    revenue: dict[str, float] = defaultdict(float)
    for occurred_on, amount, status, ttype in tx_rows:
        month = occurred_on.strftime("%Y-%m")
        if status == TransactionStatus.PAID and ttype == TransactionType.PAYMENT:
            revenue[month] += float(amount)
        elif status == TransactionStatus.REFUNDED and ttype == TransactionType.REFUND:
            revenue[month] -= float(amount)

    # ── Agendamentos por mês ──
    appt_rows = (
        await db.execute(
            select(Appointment.scheduled_at).where(
                Appointment.clinic_id == clinic_id,
                Appointment.deleted_at.is_(None),
            )
        )
    ).all()
    appointments: dict[str, float] = defaultdict(float)
    for (scheduled_at,) in appt_rows:
        appointments[scheduled_at.strftime("%Y-%m")] += 1

    # ── Pacientes novos por mês ──
    pat_rows = (
        await db.execute(
            select(Patient.created_at).where(
                Patient.clinic_id == clinic_id,
                Patient.deleted_at.is_(None),
            )
        )
    ).all()
    new_patients: dict[str, float] = defaultdict(float)
    for (created_at,) in pat_rows:
        new_patients[created_at.strftime("%Y-%m")] += 1

    return {
        "monthly_net_revenue": _sorted_points(revenue),
        "monthly_appointments": _sorted_points(appointments),
        "monthly_new_patients": _sorted_points(new_patients),
    }
