from functools import lru_cache
from urllib.parse import parse_qsl, urlencode

from pydantic import field_validator
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    environment: str = "development"

    database_url: str
    jwt_secret_key: str
    document_hash_secret: str

    jwt_algorithm: str = "HS256"
    access_token_expire_minutes: int = 15
    refresh_token_expire_days: int = 30

    rate_limit_login_per_minute: int = 5

    cors_origins: list[str] = ["*"]

    # First-run bootstrap: if the database has no users yet, a clinic + admin
    # are created from these on startup so there is a way to log in on a fresh
    # deploy. Leave the password blank to disable bootstrapping.
    bootstrap_admin_email: str = ""
    bootstrap_admin_password: str = ""
    bootstrap_clinic_name: str = "Minha Clínica"

    @field_validator("database_url")
    @classmethod
    def _normalize_database_url(cls, v: str) -> str:
        # Managed Postgres providers (Render, Heroku, Railway) hand out a
        # sync "postgres://" / "postgresql://" URL, but our async engine needs
        # the asyncpg driver. Rewrite the scheme so the same env var works
        # unchanged in the cloud.
        if v.startswith("postgres://"):
            v = "postgresql+asyncpg://" + v[len("postgres://"):]
        elif v.startswith("postgresql://"):
            v = "postgresql+asyncpg://" + v[len("postgresql://"):]

        # Neon (and most managed Postgres) append libpq-style query params —
        # sslmode, channel_binding — that asyncpg does not accept as connect()
        # kwargs (TLS negotiates fine without them). A previous version of this
        # validator only ever string-replaced the exact substring
        # "?sslmode=require", which silently mangled the URL the moment Neon's
        # console appended a second param after it: "?sslmode=require&channel_
        # binding=require" left a dangling "&channel_binding=require" glued
        # onto the database name with no "?", so asyncpg tried to connect to a
        # database literally named "neondb&channel_binding=require" and failed
        # with InvalidCatalogNameError.
        #
        # Only touch the query string, never the scheme/host/path: an earlier
        # attempt at this fix round-tripped the whole URL through
        # urlsplit/urlunsplit, which silently drops a "/" from host-less URLs
        # like the test suite's "sqlite+aiosqlite:///:memory:" (urlunsplit only
        # emits "//" before a non-empty netloc). Splitting on the literal "?"
        # instead leaves everything before it untouched.
        if "?" not in v:
            return v
        base, _, query = v.partition("?")
        kept = [
            (key, value)
            for key, value in parse_qsl(query, keep_blank_values=True)
            if key not in {"sslmode", "channel_binding"}
        ]
        return f"{base}?{urlencode(kept)}" if kept else base

    @property
    def is_production(self) -> bool:
        return self.environment == "production"


@lru_cache
def get_settings() -> Settings:
    return Settings()
