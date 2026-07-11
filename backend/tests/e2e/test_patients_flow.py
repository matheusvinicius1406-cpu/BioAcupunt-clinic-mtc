import pytest
from sqlalchemy import select

pytestmark = pytest.mark.e2e


async def _login(app_client, seeded_clinic_and_admin) -> str:
    user = seeded_clinic_and_admin["user"]
    response = await app_client.post(
        "/api/v1/auth/login", json={"email": user.email, "password": seeded_clinic_and_admin["password"]}
    )
    return response.json()["access_token"]


async def test_create_patient_requires_auth(app_client):
    response = await app_client.post("/api/v1/patients", json={"name": "Ana"})
    assert response.status_code == 401


async def test_create_and_read_patient(app_client, seeded_clinic_and_admin):
    token = await _login(app_client, seeded_clinic_and_admin)
    headers = {"Authorization": f"Bearer {token}"}

    created = await app_client.post(
        "/api/v1/patients", json={"name": "Ana Lima", "document": "123.456.789-00"}, headers=headers
    )
    assert created.status_code == 201
    patient_id = created.json()["id"]

    fetched = await app_client.get(f"/api/v1/patients/{patient_id}", headers=headers)
    assert fetched.status_code == 200
    assert fetched.json()["name"] == "Ana Lima"


async def test_patient_create_and_read_are_audited(app_client, seeded_clinic_and_admin, db_session):
    from app.models.audit import DataAccessLog

    token = await _login(app_client, seeded_clinic_and_admin)
    headers = {"Authorization": f"Bearer {token}"}

    created = await app_client.post("/api/v1/patients", json={"name": "Carlos"}, headers=headers)
    patient_id = created.json()["id"]
    await app_client.get(f"/api/v1/patients/{patient_id}", headers=headers)

    result = await db_session.execute(
        select(DataAccessLog).where(DataAccessLog.resource_type == "Patient", DataAccessLog.resource_id == patient_id)
    )
    logs = result.scalars().all()
    actions = sorted(log.action for log in logs)
    assert actions == ["CREATE", "READ"]
