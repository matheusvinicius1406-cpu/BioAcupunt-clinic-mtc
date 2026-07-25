"""
Vercel Serverless Function — BioAcupunt FastAPI backend.

Vercel's Python builder auto-detects a top-level `app` FastAPI/ASGI instance in
any file under `api/` and runs it natively via ASGI — see
https://vercel.com/docs/frameworks/backend/fastapi. The variable must be named
`app` at module level for detection to work; a renamed import or a
Mangum-wrapped `handler` fails at build time with "does not define a top-level
app" (the shape this file used before native ASGI support existed on Vercel).
"""

import sys
import os

# `backend/app/main.py` imports its own siblings as `from app.xxx import ...`,
# which only resolves if `backend/` itself is on sys.path (making the `app/`
# folder inside it importable as a top-level package). Root Directory on
# Vercel is the repo root, so this has to be added by hand before the import
# below.
_backend_path = os.path.join(os.path.dirname(__file__), '..', 'backend')
if _backend_path not in sys.path:
    sys.path.insert(0, _backend_path)

# FastAPI's own `lifespan` (declared in backend/app/main.py) runs
# bootstrap_admin_if_empty() on startup, and that function is a no-op once any
# user exists — safe to run on every cold start. No extra bootstrap plumbing
# needed here.
from app.main import app

__all__ = ["app"]
