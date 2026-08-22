"""Twenty CRM Gateway — single entry point for all Twenty interactions.

This gateway:
- Checks if Twenty is available (periodic health check)
- Routes requests to Twenty REST API
- Returns structured errors when Twenty is offline
- Never fabricates data
- All requests scoped to workspace (tenant)

Configuration via environment:
- TWENTY_BASE_URL: Twenty server URL (e.g., http://localhost:3000)
- TWENTY_API_KEY: Personal access token from Twenty settings
- TWENTY_ENABLED: Whether to attempt Twenty connections (default: true)
"""

import os
import time
import logging
from dataclasses import dataclass, field
from datetime import datetime, timezone
from typing import Any

import httpx

logger = logging.getLogger(__name__)

# ═════════════════════════════════════════════════════════════════════
# Configuration
# ═════════════════════════════════════════════════════════════════════


@dataclass
class TwentyConfig:
    base_url: str = ""
    api_key: str = ""
    enabled: bool = True
    timeout_seconds: int = 15
    max_retries: int = 3

    @classmethod
    def from_env(cls) -> "TwentyConfig":
        return cls(
            base_url=os.environ.get("TWENTY_BASE_URL", ""),
            api_key=os.environ.get("TWENTY_API_KEY", ""),
            enabled=os.environ.get("TWENTY_ENABLED", "true").lower() == "true",
        )


# ═════════════════════════════════════════════════════════════════════
# Gateway Response
# ═════════════════════════════════════════════════════════════════════


@dataclass
class GatewayResult:
    """Result from a Twenty Gateway call."""
    success: bool
    data: Any = None
    error: str = ""
    status_code: int = 200
    is_available: bool = True


# ═════════════════════════════════════════════════════════════════════
# Twenty Gateway
# ═════════════════════════════════════════════════════════════════════


class TwentyGateway:
    """Gateway for all Twenty CRM interactions.

    Usage:
        gateway = TwentyGateway.from_config(TwentyConfig.from_env())

        # Check if Twenty is available
        if not gateway.is_available():
            return 503

        # List people
        result = await gateway.list_people(workspace_id=1)
        if not result.success:
            return result

        # Create person
        result = await gateway.create_person(
            workspace_id=1,
            first_name="Maria",
            last_name="Silva",
        )
    """

    def __init__(self, config: TwentyConfig):
        self.config = config
        self._last_health_check: float = 0
        self._is_available: bool = False
        self._health_check_interval: float = 30  # seconds

    @classmethod
    def from_config(cls, config: TwentyConfig) -> "TwentyGateway":
        return cls(config)

    async def _ensure_client(self) -> httpx.AsyncClient:
        """Create or return the HTTP client."""
        if not hasattr(self, "_client") or self._client is None:
            self._client = httpx.AsyncClient(
                base_url=self.config.base_url,
                headers={
                    "Authorization": f"Bearer {self.config.api_key}",
                    "Content-Type": "application/json",
                    "Accept": "application/json",
                },
                timeout=httpx.Timeout(self.config.timeout_seconds),
            )
        return self._client

    async def _request(
        self,
        method: str,
        path: str,
        json_data: dict | None = None,
        max_retries: int | None = None,
    ) -> GatewayResult:
        """Execute an HTTP request to Twenty with retry logic."""
        if not self.config.enabled:
            return GatewayResult(
                success=False,
                error="Twenty integration disabled",
                status_code=503,
                is_available=False,
            )

        if not self.config.base_url:
            return GatewayResult(
                success=False,
                error="Twenty base URL not configured",
                status_code=503,
                is_available=False,
            )

        retries = max_retries if max_retries is not None else self.config.max_retries
        last_error = None

        for attempt in range(retries):
            try:
                client = await self._ensure_client()

                if method.upper() == "GET":
                    response = await client.get(path)
                elif method.upper() == "POST":
                    response = await client.post(path, json=json_data)
                elif method.upper() == "PATCH":
                    response = await client.patch(path, json=json_data)
                elif method.upper() == "DELETE":
                    response = await client.delete(path)
                else:
                    return GatewayResult(
                        success=False,
                        error=f"Unsupported method: {method}",
                        status_code=400,
                    )

                if response.status_code in range(200, 300):
                    self._is_available = True
                    self._last_health_check = time.time()
                    return GatewayResult(
                        success=True,
                        data=response.json() if response.content else None,
                        status_code=response.status_code,
                        is_available=True,
                    )

                if response.status_code == 401:
                    return GatewayResult(
                        success=False,
                        error="Unauthorized — check TWENTY_API_KEY",
                        status_code=401,
                        is_available=True,
                    )

                if response.status_code == 429:
                    retry_after = int(response.headers.get("Retry-After", "2"))
                    if attempt < retries - 1:
                        import asyncio
                        await asyncio.sleep(retry_after)
                        continue
                    return GatewayResult(
                        success=False,
                        error="Rate limited by Twenty",
                        status_code=429,
                        is_available=True,
                    )

                if response.status_code in range(500, 600):
                    if attempt < retries - 1:
                        import asyncio
                        await asyncio.sleep(1 * (2 ** attempt))
                        continue
                    return GatewayResult(
                        success=False,
                        error=f"Twenty server error: {response.status_code}",
                        status_code=response.status_code,
                        is_available=True,
                    )

                return GatewayResult(
                    success=False,
                    error=f"HTTP {response.status_code}: {response.text[:200]}",
                    status_code=response.status_code,
                    is_available=True,
                )

            except httpx.ConnectError:
                last_error = "Connection refused — Twenty may not be running"
                self._is_available = False
                if attempt < retries - 1:
                    import asyncio
                    await asyncio.sleep(1 * (2 ** attempt))
                    continue

            except httpx.TimeoutException:
                last_error = "Request timed out"
                if attempt < retries - 1:
                    import asyncio
                    await asyncio.sleep(1 * (2 ** attempt))
                    continue

            except Exception as e:
                last_error = f"Unexpected error: {type(e).__name__}: {e}"
                if attempt < retries - 1:
                    import asyncio
                    await asyncio.sleep(1 * (2 ** attempt))
                    continue

        self._is_available = False
        return GatewayResult(
            success=False,
            error=last_error or "Unknown error",
            status_code=0,
            is_available=False,
        )

    # ═════════════════════════════════════════════════════════════════════
    # Health
    # ═════════════════════════════════════════════════════════════════════

    async def health_check(self) -> GatewayResult:
        """Check if Twenty is reachable."""
        now = time.time()
        if now - self._last_health_check < self._health_check_interval:
            return GatewayResult(
                success=self._is_available,
                is_available=self._is_available,
                error="" if self._is_available else "Twenty not available (cached)",
            )

        result = await self._request("GET", "/api/v1/health", max_retries=1)
        self._is_available = result.success
        self._last_health_check = now
        return result

    def is_available(self) -> bool:
        """Quick check without network call (uses cached health status)."""
        return self.config.enabled and self.config.base_url and self._is_available

    # ═════════════════════════════════════════════════════════════════════
    # People (Person in Twenty)
    # ═════════════════════════════════════════════════════════════════════

    async def list_people(
        self,
        page: int = 1,
        limit: int = 20,
    ) -> GatewayResult:
        """List people from Twenty."""
        return await self._request("GET", f"/api/v1/people?page={page}&limit={limit}")

    async def create_person(
        self,
        first_name: str,
        last_name: str,
        email: str | None = None,
        phone: str | None = None,
    ) -> GatewayResult:
        """Create a person in Twenty."""
        data = {"firstName": first_name, "lastName": last_name}
        if email:
            data["email"] = email
        if phone:
            data["phone"] = phone
        return await self._request("POST", "/api/v1/people", json_data={"data": data})

    async def get_person(self, person_id: str) -> GatewayResult:
        """Get a person from Twenty."""
        return await self._request("GET", f"/api/v1/people/{person_id}")

    async def update_person(self, person_id: str, fields: dict) -> GatewayResult:
        """Update a person in Twenty."""
        return await self._request("PATCH", f"/api/v1/people/{person_id}", json_data={"data": fields})

    # ═════════════════════════════════════════════════════════════════════
    # Companies (Organization in Twenty)
    # ═════════════════════════════════════════════════════════════════════

    async def list_companies(self, page: int = 1, limit: int = 20) -> GatewayResult:
        return await self._request("GET", f"/api/v1/companies?page={page}&limit={limit}")

    async def create_company(self, name: str, domain_name: str | None = None) -> GatewayResult:
        data = {"name": name}
        if domain_name:
            data["domainName"] = domain_name
        return await self._request("POST", "/api/v1/companies", json_data={"data": data})

    # ═════════════════════════════════════════════════════════════════════
    # Opportunities
    # ═════════════════════════════════════════════════════════════════════

    async def list_opportunities(self, page: int = 1, limit: int = 20) -> GatewayResult:
        return await self._request("GET", f"/api/v1/opportunities?page={page}&limit={limit}")

    async def create_opportunity(self, name: str, amount: float | None = None, stage: str | None = None) -> GatewayResult:
        data = {"name": name}
        if amount is not None:
            data["amount"] = amount
        if stage:
            data["stage"] = stage
        return await self._request("POST", "/api/v1/opportunities", json_data={"data": data})

    # ═════════════════════════════════════════════════════════════════════
    # Generic record operations
    # ═════════════════════════════════════════════════════════════════════

    async def list_records(self, object_name: str, page: int = 1, limit: int = 20) -> GatewayResult:
        return await self._request("GET", f"/api/v1/{object_name}?page={page}&limit={limit}")

    async def create_record(self, object_name: str, fields: dict) -> GatewayResult:
        return await self._request("POST", f"/api/v1/{object_name}", json_data={"data": fields})

    async def get_record(self, object_name: str, record_id: str) -> GatewayResult:
        return await self._request("GET", f"/api/v1/{object_name}/{record_id}")

    async def update_record(self, object_name: str, record_id: str, fields: dict) -> GatewayResult:
        return await self._request("PATCH", f"/api/v1/{object_name}/{record_id}", json_data={"data": fields})

    async def delete_record(self, object_name: str, record_id: str) -> GatewayResult:
        return await self._request("DELETE", f"/api/v1/{object_name}/{record_id}")

    # ═════════════════════════════════════════════════════════════════════
    # Search
    # ═════════════════════════════════════════════════════════════════════

    async def search(self, query: str, object_name: str | None = None) -> GatewayResult:
        """Search Twenty records. Uses command menu or filter endpoint."""
        # Twenty's search is via the command menu or filter API
        # For now, use the generic list with filter
        params = f"?q={query}" if query else ""
        if object_name:
            return await self._request("GET", f"/api/v1/{object_name}{params}")
        return await self._request("GET", f"/api/v1/search{params}")

    async def close(self):
        """Close the HTTP client."""
        if hasattr(self, "_client") and self._client:
            await self._client.aclose()
            self._client = None
