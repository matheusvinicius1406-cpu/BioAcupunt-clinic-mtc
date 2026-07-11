import os

os.environ.setdefault("JWT_SECRET_KEY", "test-jwt-secret")
os.environ.setdefault("DOCUMENT_HASH_SECRET", "test-document-secret")
os.environ.setdefault("DATABASE_URL", "sqlite+aiosqlite:///:memory:")
os.environ.setdefault("RATE_LIMIT_LOGIN_PER_MINUTE", "1000")

import pytest
from httpx import ASGITransport, AsyncClient
from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker, create_async_engine
from sqlalchemy.pool import StaticPool

from app.core.config import get_settings
from app.core.security import hash_password
from app.db import all_models  # noqa: F401
from app.db.base import Base
from app.db.session import get_db
from app.models.clinic import Clinic
from app.models.user import User, UserRole


@pytest.fixture
async def db_engine():
    """Fresh engine per test — created inside the test's own event loop,
    since an engine built at import time can't be reused across the
    different event loop each test function gets.

    Honors DATABASE_URL: SQLite in-memory locally by default (fast, zero
    setup), or a real Postgres URL when CI provides one via a service
    container — StaticPool/check_same_thread is SQLite-only wiring and
    must not leak into the Postgres path.
    """
    database_url = get_settings().database_url
    is_sqlite = database_url.startswith("sqlite")

    engine = (
        create_async_engine(database_url, connect_args={"check_same_thread": False}, poolclass=StaticPool)
        if is_sqlite
        else create_async_engine(database_url, pool_pre_ping=True)
    )
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
    yield engine
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.drop_all)
    await engine.dispose()


@pytest.fixture
async def db_session(db_engine) -> AsyncSession:
    session_factory = async_sessionmaker(db_engine, expire_on_commit=False)
    async with session_factory() as session:
        yield session


@pytest.fixture
async def app_client(db_engine):
    from app.main import app

    session_factory = async_sessionmaker(db_engine, expire_on_commit=False)

    async def _override_get_db():
        async with session_factory() as session:
            yield session

    app.dependency_overrides[get_db] = _override_get_db
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        yield client
    app.dependency_overrides.clear()


@pytest.fixture
async def seeded_clinic_and_admin(db_session: AsyncSession) -> dict:
    clinic = Clinic(name="Clínica Teste")
    db_session.add(clinic)
    await db_session.commit()
    await db_session.refresh(clinic)

    user = User(
        clinic_id=clinic.id,
        email="admin@example.com",
        password_hash=hash_password("Sup3rSecret!"),
        full_name="Admin Teste",
        role=UserRole.ADMIN,
    )
    db_session.add(user)
    await db_session.commit()
    await db_session.refresh(user)

    return {"clinic": clinic, "user": user, "password": "Sup3rSecret!"}
