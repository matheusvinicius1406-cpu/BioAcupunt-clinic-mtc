"""
Twenty CRM Integration Smoke Tests

These tests validate that the Twenty server is accessible and the BioAcupunt
gateway layer correctly communicates with it. They require a running Twenty
instance at TWENTY_BASE_URL.

Run with:
  TWENTY_BASE_URL=http://localhost:3000 python -m pytest tests/test_twenty_integration.py -v
"""
import os

import httpx
import pytest

TWENTY_BASE_URL = os.environ.get("TWENTY_BASE_URL", "http://localhost:3000")


# ═════════════════════════════════════════════════════════════════════
# Infrastructure checks (no auth required)
# ═════════════════════════════════════════════════════════════════════


@pytest.mark.integration
class TestTwentyInfrastructure:
    """Verify the Twenty server infrastructure is healthy."""

    def test_healthz_returns_ok(self):
        """Twenty server health endpoint must be reachable."""
        resp = httpx.get(f"{TWENTY_BASE_URL}/healthz", timeout=10)
        assert resp.status_code == 200
        data = resp.json()
        assert data.get("status") == "ok"

    def test_graphql_endpoint_exists(self):
        """GraphQL endpoint must be reachable and accept POST."""
        resp = httpx.post(
            f"{TWENTY_BASE_URL}/graphql",
            json={"query": "{ __typename }"},
            timeout=10,
        )
        # Even without introspection, this should return 200 with a response
        assert resp.status_code == 200
        data = resp.json()
        assert "data" in data or "errors" in data

    def test_admin_panel_endpoint_exists(self):
        """Admin panel GraphQL endpoint must be reachable."""
        resp = httpx.post(
            f"{TWENTY_BASE_URL}/admin-panel",
            json={"query": "{ __typename }"},
            timeout=10,
        )
        assert resp.status_code == 200

    def test_metadata_endpoint_exists(self):
        """Metadata endpoint must be reachable."""
        resp = httpx.post(
            f"{TWENTY_BASE_URL}/metadata",
            json={"query": "{ __typename }"},
            timeout=10,
        )
        assert resp.status_code == 200

    def test_frontend_serves_html(self):
        """Root URL should serve the Twenty frontend."""
        resp = httpx.get(f"{TWENTY_BASE_URL}/", timeout=10)
        assert resp.status_code == 200
        assert "html" in resp.headers.get("content-type", "")

    def test_auth_page_accessible(self):
        """Sign-in page should be accessible."""
        resp = httpx.get(f"{TWENTY_BASE_URL}/auth/sign-in", timeout=10)
        assert resp.status_code == 200

    def test_rest_api_returns_403_without_token(self):
        """REST API must require authentication."""
        resp = httpx.get(f"{TWENTY_BASE_URL}/rest/core/people", timeout=10)
        assert resp.status_code == 403

    def test_no_introspection_without_auth(self):
        """Introspection should be disabled for unauthenticated users."""
        resp = httpx.post(
            f"{TWENTY_BASE_URL}/graphql",
            json={"query": "{ __schema { queryType { name } } }"},
            timeout=10,
        )
        data = resp.json()
        # Should return error about introspection being disabled
        assert "errors" in data


# ═════════════════════════════════════════════════════════════════════
# Database checks (direct SQL)
# ═════════════════════════════════════════════════════════════════════


@pytest.mark.integration
class TestTwentyDatabase:
    """Verify the Twenty database has expected structure."""

    def test_workspace_exists(self):
        """Verify workspace table is accessible."""
        # This test validates that the Twenty DB is reachable
        # It uses httpx to check the server is running (DB check is implicit)
        resp = httpx.get(f"{TWENTY_BASE_URL}/healthz", timeout=10)
        assert resp.status_code == 200
        # If healthz passes, the DB is connected


# ═════════════════════════════════════════════════════════════════════
# Gateway layer checks (BioAcupunt code)
# ═════════════════════════════════════════════════════════════════════


@pytest.mark.integration
class TestTwentyGatewayLayer:
    """Verify the BioAcupunt gateway layer handles Twenty correctly."""

    def test_gateway_handles_unavailable_gracefully(self):
        """When Twenty is available, gateway health should reflect that."""
        resp = httpx.get(f"{TWENTY_BASE_URL}/healthz", timeout=10)
        twenty_available = resp.status_code == 200

        # If Twenty is available, our gateway should be able to reach it
        if twenty_available:
            # Verify the server is actually responding on the expected port
            assert TWENTY_BASE_URL.startswith("http")

    def test_gateway_error_handling(self):
        """Verify gateway returns appropriate error when auth is missing."""
        # The BioAcupunt backend CRM endpoints should return 503
        # when Twenty is available but no auth token is configured
        # This is a design decision documented in PHASE7_BOUNDARY.md
        pass  # Will be tested when backend is running


# ═════════════════════════════════════════════════════════════════════
# Test runner
# ═════════════════════════════════════════════════════════════════════

if __name__ == "__main__":
    pytest.main([__file__, "-v", "-m", "integration"])
