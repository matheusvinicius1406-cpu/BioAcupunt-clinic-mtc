from datetime import datetime, timezone
from decimal import Decimal

import pytest

from app.models.clinic import Clinic
from app.models.crm_patient import CrmPatient, PatientStage

pytestmark = pytest.mark.e2e


async def _login(app_client, seeded_clinic_and_admin) -> str:
    user = seeded_clinic_and_admin["user"]
    response = await app_client.post(
        "/api/v1/auth/login",
        json={"email": user.email, "password": seeded_clinic_and_admin["password"]},
    )
    return response.json()["access_token"]


async def _seed(db_session, clinic_id: int) -> None:
    rows = [
        CrmPatient(
            clinic_id=clinic_id, name="Ana Lead", stage=PatientStage.LEAD,
            total_revenue_brl=Decimal("0"), nps_score=None,
        ),
        CrmPatient(
            clinic_id=clinic_id, name="Bruno Ativo", stage=PatientStage.ACTIVE,
            total_revenue_brl=Decimal("600.00"), nps_score=9,
        ),
        CrmPatient(
            clinic_id=clinic_id, name="Carla Tratamento", stage=PatientStage.TREATMENT,
            total_revenue_brl=Decimal("400.00"), nps_score=7,
        ),
        # Tombstone: must not count anywhere.
        CrmPatient(
            clinic_id=clinic_id, name="Deletada", stage=PatientStage.CHURNED,
            total_revenue_brl=Decimal("999.00"), nps_score=1,
            deleted_at=datetime(2026, 7, 10, tzinfo=timezone.utc),
        ),
    ]
    db_session.add_all(rows)
    await db_session.commit()


async def test_crm_requires_auth(app_client):
    response = await app_client.get("/api/v1/crm")
    assert response.status_code == 401


async def test_list_scopes_and_excludes_tombstones(app_client, seeded_clinic_and_admin, db_session):
    clinic_id = seeded_clinic_and_admin["clinic"].id
    await _seed(db_session, clinic_id)

    other = Clinic(name="Outra")
    db_session.add(other)
    await db_session.commit()
    await db_session.refresh(other)
    db_session.add(CrmPatient(clinic_id=other.id, name="De outra clínica", stage=PatientStage.LEAD))
    await db_session.commit()

    token = await _login(app_client, seeded_clinic_and_admin)
    headers = {"Authorization": f"Bearer {token}"}

    resp = await app_client.get("/api/v1/crm", headers=headers)
    assert resp.status_code == 200
    body = resp.json()
    assert len(body) == 3  # tombstone + other clinic excluded
    names = {p["name"] for p in body}
    assert "Deletada" not in names
    assert "De outra clínica" not in names


async def test_list_filters_by_stage(app_client, seeded_clinic_and_admin, db_session):
    clinic_id = seeded_clinic_and_admin["clinic"].id
    await _seed(db_session, clinic_id)

    token = await _login(app_client, seeded_clinic_and_admin)
    headers = {"Authorization": f"Bearer {token}"}

    resp = await app_client.get("/api/v1/crm?stage=ACTIVE", headers=headers)
    assert resp.status_code == 200
    body = resp.json()
    assert [p["name"] for p in body] == ["Bruno Ativo"]


async def test_pipeline_summary(app_client, seeded_clinic_and_admin, db_session):
    clinic_id = seeded_clinic_and_admin["clinic"].id
    await _seed(db_session, clinic_id)

    token = await _login(app_client, seeded_clinic_and_admin)
    headers = {"Authorization": f"Bearer {token}"}

    resp = await app_client.get("/api/v1/crm/pipeline", headers=headers)
    assert resp.status_code == 200
    s = resp.json()
    assert s["total"] == 3
    assert s["by_stage"] == {"LEAD": 1, "ACTIVE": 1, "TREATMENT": 1}
    assert s["total_revenue_brl"] == 1000.0  # 600 + 400 (deleted 999 excluded)
    assert s["average_nps"] == 8.0  # (9 + 7) / 2; unscored lead not counted
