import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from slowapi.errors import RateLimitExceeded
from slowapi.middleware import SlowAPIMiddleware

from app.api.routers import appointments, auth, clinics, health, patients, sync
from app.core.config import get_settings
from app.core.errors import register_exception_handlers
from app.core.limiter import limiter
from app.db import all_models  # noqa: F401  — registers every model on Base.metadata
from app.db.session import SessionLocal
from app.middleware.request_audit import RequestAuditMiddleware
from app.middleware.security_headers import SecurityHeadersMiddleware
from app.services.bootstrap import bootstrap_admin_if_empty

logging.basicConfig(level=logging.INFO)

settings = get_settings()


@asynccontextmanager
async def lifespan(app: FastAPI):
    async with SessionLocal() as session:
        await bootstrap_admin_if_empty(session)
    yield


app = FastAPI(title="BioAcupunt API", version="0.1.0", lifespan=lifespan)

app.state.limiter = limiter
app.add_exception_handler(RateLimitExceeded, lambda request, exc: _rate_limit_handler(request, exc))
app.add_middleware(SlowAPIMiddleware)

# The CORS spec forbids the wildcard origin `*` together with credentials, and
# Starlette silently drops the CORS headers when you ask for that combination —
# so `["*"] + allow_credentials=True` would leave the browser with no CORS
# headers at all. The web client authenticates with a Bearer token in the
# Authorization header (not cookies), so credentials are never required. Only
# enable them when the origins are listed explicitly; keep them off for `*`.
_allow_all_origins = settings.cors_origins == ["*"]
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origins,
    allow_credentials=not _allow_all_origins,
    allow_methods=["*"],
    allow_headers=["*"],
)
app.add_middleware(SecurityHeadersMiddleware)
app.add_middleware(RequestAuditMiddleware)

register_exception_handlers(app)

app.include_router(health.router)
app.include_router(auth.router)
app.include_router(clinics.router)
app.include_router(patients.router)
app.include_router(appointments.router)
app.include_router(sync.router)


def _rate_limit_handler(request, exc):
    from fastapi.responses import JSONResponse

    return JSONResponse(
        status_code=429,
        content={"error": {"code": "rate_limited", "message": "Muitas tentativas. Aguarde e tente novamente."}},
    )
