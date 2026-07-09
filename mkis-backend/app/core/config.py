"""
MKIS — Core settings and application bootstrap.
Config, logging, DI container and dependencies wiring.
"""

from __future__ import annotations

import os
from dataclasses import dataclass
from typing import Optional


HERE = os.path.dirname(__file__)


@dataclass(frozen=True)
class Settings:
    database_url: str = os.getenv("DATABASE_URL", "")
    redis_url: str = os.getenv("REDIS_URL", "")
    llm_provider: str = os.getenv("LLM_PROVIDER", "openai")
    llm_api_key: str = os.getenv("LLM_API_KEY", "")
    llm_fallback_api_key: str = os.getenv("LLM_FALLBACK_API_KEY", "")
    ocr_engine: str = os.getenv("OCR_ENGINE", "tesseract")
    antivirus_enabled: bool = os.getenv("ANTIVIRUS_ENABLED", "false").lower() == "true"
    jwt_secret_key: str = os.getenv("JWT_SECRET_KEY", "change-me")
    jwt_access_ttl_minutes: int = int(os.getenv("JWT_ACCESS_TTL_MINUTES", "15"))
    jwt_refresh_days: int = int(os.getenv("JWT_REFRESH_DAYS", "30"))
    max_query_length: int = int(os.getenv("MAX_QUERY_LENGTH", "2048"))
    default_page_limit: int = int(os.getenv("DEFAULT_PAGE_LIMIT", "20"))
    max_page_limit: int = int(os.getenv("MAX_PAGE_LIMIT", "50"))
    audit_retention_years: int = int(os.getenv("AUDIT_RETENTION_YEARS", "7"))
    otel_exporter_otlp_endpoint: Optional[str] = os.getenv("OTEL_EXPORTER_OTLP_ENDPOINT")
    app_env: str = os.getenv("APP_ENV", "development")


settings = Settings()
