from datetime import date, datetime, timezone
from decimal import Decimal

import pytest

from app.models.appointment import Appointment
from app.models.patient import Patient
from app.models.transaction import Transaction, TransactionStatus, TransactionType

pytestmark = pytest.mark.e2e


async def _login(app_client, seeded_clinic_and_admin) -> str:
    user = seeded_clinic_and_admin["user"]
    response = await app_client.post(
        "/api/v1/auth/login",
        json={"email": user.email, "password": seeded_clinic_and_admin["password"]},
    )
    return response.json()["access_token"]


async def test_analytics_requires_auth(app_client):
    response = await app_client.get("/api/v1/reports/analytics")
    assert response.status_code == 401


async def test_monthly_series_are_real(app_client, seeded_clinic_and_admin, db_session):
    clinic_id = seeded_clinic_and_admin["clinic"].id

    db_session.add_all(
        [
            # June: 100 paid; July: 200 paid minus 50 refunded = 150 net.
            Transaction(
                clinic_id=clinic_id, amount_brl=Decimal("100.00"), occurred_on=date(2026, 6, 10),
                type=TransactionType.PAYMENT, status=TransactionStatus.PAID,
            ),
            Transaction(
                clinic_id=clinic_id, amount_brl=Decimal("200.00"), occurred_on=date(2026, 7, 3),
                type=TransactionType.PAYMENT, status=TransactionStatus.PAID,
            ),
            Transaction(
                clinic_id=clinic_id, amount_brl=Decimal("50.00"), occurred_on=date(2026, 7, 20),
                type=TransactionType.REFUND, status=TransactionStatus.REFUNDED,
            ),
            # Tombstoned payment in July — must not count.
            Transaction(
                clinic_id=clinic_id, amount_brl=Decimal("999.00"), occurred_on=date(2026, 7, 25),
                type=TransactionType.PAYMENT, status=TransactionStatus.PAID,
                deleted_at=datetime(2026, 7, 26, tzinfo=timezone.utc),
            ),
            Appointment(
                clinic_id=clinic_id, patient_id=1,
                scheduled_at=datetime(2026, 7, 3, 9, tzinfo=timezone.utc),
            ),
            Appointment(
                clinic_id=clinic_id, patient_id=1,
                scheduled_at=datetime(2026, 7, 5, 10, tzinfo=timezone.utc),
            ),
            Patient(clinic_id=clinic_id, name="Novo Junho", created_at=datetime(2026, 6, 1, tzinfo=timezone.utc)),
            Patient(clinic_id=clinic_id, name="Novo Julho", created_at=datetime(2026, 7, 1, tzinfo=timezone.utc)),
        ]
    )
    await db_session.commit()

    token = await _login(app_client, seeded_clinic_and_admin)
    headers = {"Authorization": f"Bearer {token}"}

    resp = await app_client.get("/api/v1/reports/analytics", headers=headers)
    assert resp.status_code == 200
    body = resp.json()

    assert body["monthly_net_revenue"] == [
        {"month": "2026-06", "value": 100.0},
        {"month": "2026-07", "value": 150.0},
    ]
    assert body["monthly_appointments"] == [{"month": "2026-07", "value": 2.0}]
    assert body["monthly_new_patients"] == [
        {"month": "2026-06", "value": 1.0},
        {"month": "2026-07", "value": 1.0},
    ]


async def test_analytics_empty_when_no_data(app_client, seeded_clinic_and_admin):
    token = await _login(app_client, seeded_clinic_and_admin)
    headers = {"Authorization": f"Bearer {token}"}
    resp = await app_client.get("/api/v1/reports/analytics", headers=headers)
    assert resp.status_code == 200
    body = resp.json()
    assert body["monthly_net_revenue"] == []
    assert body["monthly_appointments"] == []
    assert body["monthly_new_patients"] == []
