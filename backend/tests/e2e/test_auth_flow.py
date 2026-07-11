import pytest

pytestmark = pytest.mark.e2e


async def test_health_check(app_client):
    response = await app_client.get("/healthz")
    assert response.status_code == 200
    assert response.json() == {"status": "ok"}


async def test_login_success(app_client, seeded_clinic_and_admin):
    user = seeded_clinic_and_admin["user"]
    response = await app_client.post(
        "/api/v1/auth/login", json={"email": user.email, "password": seeded_clinic_and_admin["password"]}
    )
    assert response.status_code == 200
    body = response.json()
    assert "access_token" in body and "refresh_token" in body


async def test_login_wrong_password_returns_401(app_client, seeded_clinic_and_admin):
    user = seeded_clinic_and_admin["user"]
    response = await app_client.post("/api/v1/auth/login", json={"email": user.email, "password": "wrong"})
    assert response.status_code == 401


async def test_protected_route_without_token_returns_401_not_403(app_client):
    """FastAPI's HTTPBearer defaults to 403 for a missing Authorization
    header, which is the wrong HTTP status for "not authenticated"."""
    response = await app_client.get("/api/v1/auth/me")
    assert response.status_code == 401


async def test_protected_route_with_token_returns_200(app_client, seeded_clinic_and_admin):
    user = seeded_clinic_and_admin["user"]
    login = await app_client.post(
        "/api/v1/auth/login", json={"email": user.email, "password": seeded_clinic_and_admin["password"]}
    )
    access_token = login.json()["access_token"]

    response = await app_client.get("/api/v1/auth/me", headers={"Authorization": f"Bearer {access_token}"})
    assert response.status_code == 200
    assert response.json()["email"] == user.email


async def test_refresh_rotates_token(app_client, seeded_clinic_and_admin):
    user = seeded_clinic_and_admin["user"]
    login = await app_client.post(
        "/api/v1/auth/login", json={"email": user.email, "password": seeded_clinic_and_admin["password"]}
    )
    old_refresh = login.json()["refresh_token"]

    refreshed = await app_client.post("/api/v1/auth/refresh", json={"refresh_token": old_refresh})
    assert refreshed.status_code == 200
    new_refresh = refreshed.json()["refresh_token"]
    assert new_refresh != old_refresh


async def test_refresh_token_reuse_revokes_all_sessions(app_client, seeded_clinic_and_admin):
    """Critical security property: presenting an already-rotated refresh
    token (as a stolen token would be, after the legitimate client already
    rotated it) must be treated as theft — revoke everything, including the
    new token that was legitimately issued by the rotation."""
    user = seeded_clinic_and_admin["user"]
    login = await app_client.post(
        "/api/v1/auth/login", json={"email": user.email, "password": seeded_clinic_and_admin["password"]}
    )
    original_refresh = login.json()["refresh_token"]

    first_rotation = await app_client.post("/api/v1/auth/refresh", json={"refresh_token": original_refresh})
    assert first_rotation.status_code == 200
    legit_new_refresh = first_rotation.json()["refresh_token"]

    # Replay the already-used (revoked) token — simulates a stolen token.
    reuse_attempt = await app_client.post("/api/v1/auth/refresh", json={"refresh_token": original_refresh})
    assert reuse_attempt.status_code == 401
    assert reuse_attempt.json()["error"]["code"] == "token_reuse_detected"

    # The legitimate token issued by the rotation must now be dead too.
    legit_now_dead = await app_client.post("/api/v1/auth/refresh", json={"refresh_token": legit_new_refresh})
    assert legit_now_dead.status_code == 401


async def test_logout_revokes_refresh_token(app_client, seeded_clinic_and_admin):
    user = seeded_clinic_and_admin["user"]
    login = await app_client.post(
        "/api/v1/auth/login", json={"email": user.email, "password": seeded_clinic_and_admin["password"]}
    )
    refresh_token = login.json()["refresh_token"]

    logout = await app_client.post("/api/v1/auth/logout", json={"refresh_token": refresh_token})
    assert logout.status_code == 200

    after_logout = await app_client.post("/api/v1/auth/refresh", json={"refresh_token": refresh_token})
    assert after_logout.status_code == 401
