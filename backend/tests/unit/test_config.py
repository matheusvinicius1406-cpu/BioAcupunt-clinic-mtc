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


def test_sslmode_alone_is_stripped():
    s = _settings("postgresql://u:p@host/db?sslmode=require")
    assert s.database_url == "postgresql+asyncpg://u:p@host/db"


def test_sslmode_and_channel_binding_together_are_both_stripped():
    # Regression: the old validator only string-replaced the exact substring
    # "?sslmode=require", which left a dangling "&channel_binding=require"
    # glued onto the database name (no "?" left) whenever Neon's console
    # appended both params — asyncpg then tried to connect to a database
    # literally named "db&channel_binding=require" and failed outright.
    s = _settings("postgresql://u:p@host/db?sslmode=require&channel_binding=require")
    assert s.database_url == "postgresql+asyncpg://u:p@host/db"


def test_channel_binding_before_sslmode_is_also_stripped():
    s = _settings("postgresql://u:p@host/db?channel_binding=require&sslmode=require")
    assert s.database_url == "postgresql+asyncpg://u:p@host/db"


def test_other_query_params_survive_sslmode_removal():
    s = _settings("postgresql://u:p@host/db?sslmode=require&application_name=bioacupunt")
    assert s.database_url == "postgresql+asyncpg://u:p@host/db?application_name=bioacupunt"
