from sqlalchemy.ext.asyncio import AsyncSession

from app.models.audit import DataAccessLog


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
