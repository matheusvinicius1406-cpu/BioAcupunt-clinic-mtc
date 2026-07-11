import json
import logging
import time
import uuid

from starlette.middleware.base import BaseHTTPMiddleware
from starlette.requests import Request
from starlette.responses import Response

logger = logging.getLogger("bioacupunt.access")


class RequestAuditMiddleware(BaseHTTPMiddleware):
    """Emits one structured JSON log line per request and stamps
    X-Request-ID / X-Response-Time-Ms for traceability."""

    async def dispatch(self, request: Request, call_next) -> Response:
        request_id = str(uuid.uuid4())
        started = time.perf_counter()

        response = await call_next(request)

        duration_ms = round((time.perf_counter() - started) * 1000, 2)
        response.headers["X-Request-ID"] = request_id
        response.headers["X-Response-Time-Ms"] = str(duration_ms)

        logger.info(
            json.dumps(
                {
                    "request_id": request_id,
                    "method": request.method,
                    "endpoint": request.url.path,
                    "status_code": response.status_code,
                    "duration_ms": duration_ms,
                }
            )
        )
        return response
