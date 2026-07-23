from sqlalchemy import func, or_, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.library_node import LibraryNode


async def list_nodes(
    db: AsyncSession,
    *,
    query: str | None = None,
    limit: int = 200,
) -> list[LibraryNode]:
    """Global curated library, alphabetical. Optional case-insensitive search
    over title and summary."""
    stmt = select(LibraryNode)
    if query:
        like = f"%{query.lower()}%"
        stmt = stmt.where(
            or_(
                func.lower(LibraryNode.title).like(like),
                func.lower(LibraryNode.summary).like(like),
            )
        )
    stmt = stmt.order_by(LibraryNode.title.asc()).limit(limit)
    result = await db.execute(stmt)
    return list(result.scalars().all())


async def get_node(db: AsyncSession, *, node_id: str) -> LibraryNode | None:
    result = await db.execute(select(LibraryNode).where(LibraryNode.id == node_id))
    return result.scalar_one_or_none()
