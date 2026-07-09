"""
MKIS — Application factory e wiring de dependências.
"""

from __future__ import annotations

from contextlib import asynccontextmanager

from fastapi import FastAPI

from mkis_backend.app.api.v1.routes import router as v1_router
from mkis_backend.app.core.config import settings
from mkis_backend.app.infra.database import engine
from mkis_backend.app.models.knowledge.models import Base


@asynccontextmanager
async def lifespan(app: FastAPI):
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
    yield


app = FastAPI(title="MKIS API", version="0.1.0", lifespan=lifespan)
app.include_router(v1_router, prefix="/api/v1/knowledge", tags=["knowledge"])


@app.get("/health")
async def health():
    return {"status": "ok", "env": settings.app_env}
