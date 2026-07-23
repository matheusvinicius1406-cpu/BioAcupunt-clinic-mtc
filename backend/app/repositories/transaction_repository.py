from datetime import date

from sqlalchemy import and_, case, func, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.transaction import Transaction, TransactionStatus, TransactionType


async def list_transactions(
    db: AsyncSession,
    *,
    clinic_id: int,
    start: date | None = None,
    end: date | None = None,
    limit: int = 500,
) -> list[Transaction]:
    """Newest first, scoped by clinic and excluding tombstones.

    `clinic_id` scoping is not optional: a transaction from another clinic must
    never resolve here, exactly as with patients.
    """
    stmt = select(Transaction).where(
        Transaction.clinic_id == clinic_id,
        Transaction.deleted_at.is_(None),
    )
    if start is not None:
        stmt = stmt.where(Transaction.occurred_on >= start)
    if end is not None:
        stmt = stmt.where(Transaction.occurred_on <= end)
    stmt = stmt.order_by(Transaction.occurred_on.desc(), Transaction.id.desc()).limit(limit)
    result = await db.execute(stmt)
    return list(result.scalars().all())


async def summary(
    db: AsyncSession,
    *,
    clinic_id: int,
    start: date | None = None,
    end: date | None = None,
) -> dict[str, float | int]:
    """One-query conditional aggregation, mirroring the app's DAO sums.

    Excludes soft-deleted rows so a refunded/deleted movement stops counting.
    """
    amount = Transaction.amount_brl
    payments_expr = func.coalesce(
        func.sum(
            case(
                (
                    and_(
                        Transaction.status == TransactionStatus.PAID,
                        Transaction.type == TransactionType.PAYMENT,
                    ),
                    amount,
                ),
                else_=0,
            )
        ),
        0,
    )
    refunds_expr = func.coalesce(
        func.sum(
            case(
                (
                    and_(
                        Transaction.status == TransactionStatus.REFUNDED,
                        Transaction.type == TransactionType.REFUND,
                    ),
                    amount,
                ),
                else_=0,
            )
        ),
        0,
    )
    pending_expr = func.coalesce(
        func.sum(case((Transaction.status == TransactionStatus.PENDING, amount), else_=0)),
        0,
    )
    expenses_expr = func.coalesce(
        func.sum(case((Transaction.type == TransactionType.EXPENSE, amount), else_=0)),
        0,
    )

    stmt = select(
        payments_expr,
        refunds_expr,
        pending_expr,
        expenses_expr,
        func.count(),
    ).where(
        Transaction.clinic_id == clinic_id,
        Transaction.deleted_at.is_(None),
    )
    if start is not None:
        stmt = stmt.where(Transaction.occurred_on >= start)
    if end is not None:
        stmt = stmt.where(Transaction.occurred_on <= end)

    payments, refunds, pending, expenses, count = (await db.execute(stmt)).one()
    payments = float(payments)
    refunds = float(refunds)
    return {
        "payments_brl": payments,
        "refunds_brl": refunds,
        "net_revenue_brl": payments - refunds,
        "pending_brl": float(pending),
        "expenses_brl": float(expenses),
        "transaction_count": int(count),
    }
