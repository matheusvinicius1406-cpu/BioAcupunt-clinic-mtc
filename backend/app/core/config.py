from functools import lru_cache

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
            return "postgresql+asyncpg://" + v[len("postgres://"):]
        if v.startswith("postgresql://"):
            return "postgresql+asyncpg://" + v[len("postgresql://"):]
        return v

    @property
    def is_production(self) -> bool:
        return self.environment == "production"


@lru_cache
def get_settings() -> Settings:
    return Settings()
