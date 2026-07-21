from collections.abc import Iterable

from sqlalchemy.ext.asyncio import AsyncSession

from app.models.audit import DataAccessLog


async def record_many(
    db: AsyncSession,
    *,
    user_id: int,
    clinic_id: int,
    entries: Iterable[tuple[str, int, str]],
) -> None:
    """Records a batch of accesses as (resource_type, resource_id, action).

    One row per resource, one commit for the batch. A sync push touches many
    records at once, and the LGPD trail has to say *which* patient was written —
    a single "a sync happened" entry answers no question anyone would ask of an
    audit log. Committing per row instead would mean 500 commits for one push.
    """
    rows = [
        DataAccessLog(
            user_id=user_id,
            clinic_id=clinic_id,
            resource_type=resource_type,
            resource_id=resource_id,
            action=action,
        )
        for resource_type, resource_id, action in entries
    ]
    if not rows:
        return
    db.add_all(rows)
    await db.commit()


async def record_access(
    db: AsyncSession,
    *,
    user_id: int,
    clinic_id: int,
    resource_type: str,
    resource_id: int,
    action: str,
) -> None:
    db.add(
        DataAccessLog(
            user_id=user_id,
            clinic_id=clinic_id,
            resource_type=resource_type,
            resource_id=resource_id,
            action=action,
        )
    )
    await db.commit()
