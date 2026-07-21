import pytest

pytestmark = pytest.mark.e2e


async def test_register_creates_account_and_logs_in(app_client):
    """Self-signup: one call creates the clinic + admin user and returns a
    usable token pair, so the app is logged in immediately after."""
    response = await app_client.post(
        "/api/v1/auth/register",
        json={
            "email": "dra.nova@example.com",
            "password": "senhaforte123",
            "full_name": "Dra. Nova",
            "clinic_name": "Consultório Nova",
        },
    )
    assert response.status_code == 201
    body = response.json()
    assert "access_token" in body and "refresh_token" in body

    # The freshly-created account can immediately reach a protected route.
    me = await app_client.get(
        "/api/v1/auth/me", headers={"Authorization": f"Bearer {body['access_token']}"}
    )
    assert me.status_code == 200
    assert me.json()["email"] == "dra.nova@example.com"
    assert me.json()["role"] == "admin"


async def test_register_persists_so_login_works_afterwards(app_client):
    """The account must be saved in the DB — logging in later with the same
    credentials has to succeed."""
    await app_client.post(
        "/api/v1/auth/register",
        json={"email": "persist@example.com", "password": "senhaforte123", "full_name": "Persist"},
    )
    login = await app_client.post(
        "/api/v1/auth/login", json={"email": "persist@example.com", "password": "senhaforte123"}
    )
    assert login.status_code == 200


async def test_register_duplicate_email_returns_409(app_client):
    payload = {"email": "dup@example.com", "password": "senhaforte123", "full_name": "Dup"}
    first = await app_client.post("/api/v1/auth/register", json=payload)
    assert first.status_code == 201

    second = await app_client.post("/api/v1/auth/register", json=payload)
    assert second.status_code == 409
    assert second.json()["error"]["code"] == "email_taken"


async def test_register_rejects_weak_password(app_client):
    """Password floor is enforced before the hasher ever sees it."""
    response = await app_client.post(
        "/api/v1/auth/register",
        json={"email": "weak@example.com", "password": "123", "full_name": "Weak"},
    )
    assert response.status_code == 422


async def test_register_email_is_normalized(app_client):
    """Mixed-case / padded e-mail on signup must not create a second account
    that login (which normalizes) can't reach."""
    await app_client.post(
        "/api/v1/auth/register",
        json={"email": "  MixedCase@Example.com ", "password": "senhaforte123", "full_name": "Mixed"},
    )
    login = await app_client.post(
        "/api/v1/auth/login", json={"email": "mixedcase@example.com", "password": "senhaforte123"}
    )
    assert login.status_code == 200
