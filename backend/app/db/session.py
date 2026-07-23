from collections.abc import AsyncGenerator

from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker, create_async_engine
from sqlalchemy.pool import NullPool

from app.core.config import get_settings

_settings = get_settings()

# Serverless (Vercel) + Neon's connection pooler:
#
# 1. NullPool — a serverless function is frozen/thawed between invocations, so a
#    SQLAlchemy pool would hold sockets the platform has already killed. Do not
#    pool inside the lambda; let Neon's own pooler (the `-pooler` host) do the
#    pooling. Open a connection per request, close it at the end.
#
# 2. statement_cache_size=0 — Neon's pooler runs PgBouncer in *transaction*
#    mode, which multiplexes many clients over few server connections and so
#    cannot support the server-side prepared statements asyncpg caches by
#    default. Leaving the cache on raises "prepared statement ... does not
#    exist" / "already exists" errors under load. Zero disables it.
#
# Use the `-pooler` Neon host in DATABASE_URL for the app (see DEPLOY.md); run
# Alembic migrations against the DIRECT (non-pooler) host instead.
engine = create_async_engine(
    _settings.database_url,
    poolclass=NullPool,
    connect_args={"statement_cache_size": 0},
)
SessionLocal = async_sessionmaker(engine, expire_on_commit=False)


async def get_db() -> AsyncGenerator[AsyncSession, None]:
    async with SessionLocal() as session:
        yield session
