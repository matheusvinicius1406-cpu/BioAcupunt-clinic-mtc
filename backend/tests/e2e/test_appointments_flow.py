from datetime import datetime, timezone

import pytest

from app.models.appointment import Appointment, AppointmentStatus
from app.models.clinic import Clinic
from app.models.patient import Patient

pytestmark = pytest.mark.e2e


async def _login(app_client, seeded_clinic_and_admin) -> str:
    user = seeded_clinic_and_admin["user"]
    response = await app_client.post(
        "/api/v1/auth/login",
        json={"email": user.email, "password": seeded_clinic_and_admin["password"]},
    )
    return response.json()["access_token"]


async def test_appointments_requires_auth(app_client):
    response = await app_client.get("/api/v1/appointments")
    assert response.status_code == 401


async def test_list_excludes_tombstones_and_scopes_by_clinic(app_client, seeded_clinic_and_admin, db_session):
    """Regression: the médica deletes an appointment on the phone (offline), sync
    marks deleted_at; the web panel must never show it back or count it — the
    repository used to have no deleted_at filter at all."""
    clinic = seeded_clinic_and_admin["clinic"]
    user = seeded_clinic_and_admin["user"]

    patient = Patient(clinic_id=clinic.id, name="Paciente Teste")
    db_session.add(patient)
    await db_session.commit()
    await db_session.refresh(patient)

    rows = [
        Appointment(
            clinic_id=clinic.id, patient_id=patient.id, professional_id=user.id,
            scheduled_at=datetime(2026, 7, 1, 10, 0, tzinfo=timezone.utc),
            status=AppointmentStatus.SCHEDULED,
        ),
        Appointment(
            clinic_id=clinic.id, patient_id=patient.id, professional_id=user.id,
            scheduled_at=datetime(2026, 7, 2, 10, 0, tzinfo=timezone.utc),
            status=AppointmentStatus.COMPLETED,
        ),
        # Tombstone: apagado pelo app. Não pode reaparecer no painel web.
        Appointment(
            clinic_id=clinic.id, patient_id=patient.id, professional_id=user.id,
            scheduled_at=datetime(2026, 7, 3, 10, 0, tzinfo=timezone.utc),
            status=AppointmentStatus.CANCELLED,
            deleted_at=datetime(2026, 7, 4, tzinfo=timezone.utc),
        ),
    ]
    db_session.add_all(rows)
    await db_session.commit()

    # An appointment in a *different* clinic must never leak into the response.
    other = Clinic(name="Outra Clínica")
    db_session.add(other)
    await db_session.commit()
    await db_session.refresh(other)
    other_patient = Patient(clinic_id=other.id, name="Paciente de Outra Clínica")
    db_session.add(other_patient)
    await db_session.commit()
    await db_session.refresh(other_patient)
    db_session.add(
        Appointment(
            clinic_id=other.id, patient_id=other_patient.id, professional_id=None,
            scheduled_at=datetime(2026, 7, 1, 10, 0, tzinfo=timezone.utc),
        )
    )
    await db_session.commit()

    token = await _login(app_client, seeded_clinic_and_admin)
    headers = {"Authorization": f"Bearer {token}"}

    resp = await app_client.get("/api/v1/appointments", headers=headers)
    assert resp.status_code == 200
    body = resp.json()
    # 2 visible rows (tombstone excluded), both from our clinic.
    assert len(body) == 2
    assert all(a["clinic_id"] == clinic.id for a in body)
    assert AppointmentStatus.CANCELLED not in [a["status"] for a in body]


async def test_get_appointment_hides_a_deleted_one(app_client, seeded_clinic_and_admin, db_session):
    clinic = seeded_clinic_and_admin["clinic"]
    user = seeded_clinic_and_admin["user"]
    patient = Patient(clinic_id=clinic.id, name="Paciente Teste")
    db_session.add(patient)
    await db_session.commit()
    await db_session.refresh(patient)

    appt = Appointment(
        clinic_id=clinic.id, patient_id=patient.id, professional_id=user.id,
        scheduled_at=datetime(2026, 7, 1, 10, 0, tzinfo=timezone.utc),
        deleted_at=datetime(2026, 7, 2, tzinfo=timezone.utc),
    )
    db_session.add(appt)
    await db_session.commit()
    await db_session.refresh(appt)

    token = await _login(app_client, seeded_clinic_and_admin)
    headers = {"Authorization": f"Bearer {token}"}

    resp = await app_client.patch(
        f"/api/v1/appointments/{appt.id}/status",
        headers=headers,
        json={"status": AppointmentStatus.CONFIRMED},
    )
    assert resp.status_code == 404
