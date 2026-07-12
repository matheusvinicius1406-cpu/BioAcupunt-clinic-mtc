import pytest

from app.core.config import Settings

pytestmark = pytest.mark.unit


def _settings(url: str) -> Settings:
    return Settings(database_url=url, jwt_secret_key="x", document_hash_secret="y")


def test_postgres_scheme_is_rewritten_to_asyncpg():
    s = _settings("postgres://u:p@host:5432/db")
    assert s.database_url == "postgresql+asyncpg://u:p@host:5432/db"


def test_postgresql_scheme_is_rewritten_to_asyncpg():
    s = _settings("postgresql://u:p@host:5432/db")
    assert s.database_url == "postgresql+asyncpg://u:p@host:5432/db"


def test_already_async_url_is_left_untouched():
    url = "postgresql+asyncpg://u:p@host:5432/db"
    assert _settings(url).database_url == url


def test_sqlite_url_is_left_untouched():
    url = "sqlite+aiosqlite:///:memory:"
    assert _settings(url).database_url == url
