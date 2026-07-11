import pytest

from app.core.security import hash_password
from app.models.clinic import Clinic
from app.models.user import User, UserRole

pytestmark = pytest.mark.e2e


async def _create_user(db_session, clinic_id: int, role: str) -> User:
    user = User(
        clinic_id=clinic_id,
        email=f"{role}@example.com",
        password_hash=hash_password("Sup3rSecret!"),
        full_name=f"Usuário {role}",
        role=role,
    )
    db_session.add(user)
    await db_session.commit()
    await db_session.refresh(user)
    return user


async def _login(app_client, email: str) -> str:
    response = await app_client.post("/api/v1/auth/login", json={"email": email, "password": "Sup3rSecret!"})
    return response.json()["access_token"]


async def test_non_admin_cannot_create_clinic(app_client, db_session, seeded_clinic_and_admin):
    clinic = seeded_clinic_and_admin["clinic"]
    staff = await _create_user(db_session, clinic.id, UserRole.STAFF)
    token = await _login(app_client, staff.email)

    response = await app_client.post(
        "/api/v1/clinics", json={"name": "Nova Clínica"}, headers={"Authorization": f"Bearer {token}"}
    )
    assert response.status_code == 403


async def test_admin_can_create_clinic(app_client, seeded_clinic_and_admin):
    token = await _login(app_client, seeded_clinic_and_admin["user"].email)

    response = await app_client.post(
        "/api/v1/clinics", json={"name": "Nova Clínica"}, headers={"Authorization": f"Bearer {token}"}
    )
    assert response.status_code == 201
