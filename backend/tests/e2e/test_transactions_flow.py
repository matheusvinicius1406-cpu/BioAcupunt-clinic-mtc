from datetime import date, datetime, timezone
from decimal import Decimal

import pytest

from app.models.clinic import Clinic
from app.models.transaction import Transaction, TransactionStatus, TransactionType

pytestmark = pytest.mark.e2e


async def _login(app_client, seeded_clinic_and_admin) -> str:
    user = seeded_clinic_and_admin["user"]
    response = await app_client.post(
        "/api/v1/auth/login",
        json={"email": user.email, "password": seeded_clinic_and_admin["password"]},
    )
    return response.json()["access_token"]


async def _seed_transactions(db_session, clinic_id: int) -> None:
    """A representative mix: two payments, one refund, one pending, one expense,
    plus a soft-deleted payment that must NOT count."""
    rows = [
        Transaction(
            clinic_id=clinic_id, amount_brl=Decimal("200.00"), occurred_on=date(2026, 7, 1),
            type=TransactionType.PAYMENT, status=TransactionStatus.PAID, category="SESSÃO",
        ),
        Transaction(
            clinic_id=clinic_id, amount_brl=Decimal("150.00"), occurred_on=date(2026, 7, 5),
            type=TransactionType.PAYMENT, status=TransactionStatus.PAID, category="SESSÃO",
        ),
        Transaction(
            clinic_id=clinic_id, amount_brl=Decimal("50.00"), occurred_on=date(2026, 7, 6),
            type=TransactionType.REFUND, status=TransactionStatus.REFUNDED, category="SESSÃO",
        ),
        Transaction(
            clinic_id=clinic_id, amount_brl=Decimal("80.00"), occurred_on=date(2026, 7, 7),
            type=TransactionType.PAYMENT, status=TransactionStatus.PENDING, category="SESSÃO",
        ),
        Transaction(
            clinic_id=clinic_id, amount_brl=Decimal("300.00"), occurred_on=date(2026, 7, 8),
            type=TransactionType.EXPENSE, status=TransactionStatus.PAID, category="ALUGUEL",
        ),
        # Tombstone: a deleted payment. Must be invisible to list and sums.
        Transaction(
            clinic_id=clinic_id, amount_brl=Decimal("999.00"), occurred_on=date(2026, 7, 9),
            type=TransactionType.PAYMENT, status=TransactionStatus.PAID, category="SESSÃO",
            deleted_at=datetime(2026, 7, 10, tzinfo=timezone.utc),
        ),
    ]
    db_session.add_all(rows)
    await db_session.commit()


async def test_transactions_requires_auth(app_client):
    response = await app_client.get("/api/v1/transactions")
    assert response.status_code == 401


async def test_list_excludes_tombstones_and_scopes_by_clinic(app_client, seeded_clinic_and_admin, db_session):
    clinic_id = seeded_clinic_and_admin["clinic"].id
    await _seed_transactions(db_session, clinic_id)

    # A transaction in a *different* clinic must never leak into the response.
    other = Clinic(name="Outra Clínica")
    db_session.add(other)
    await db_session.commit()
    await db_session.refresh(other)
    db_session.add(
        Transaction(
            clinic_id=other.id, amount_brl=Decimal("500.00"), occurred_on=date(2026, 7, 1),
            type=TransactionType.PAYMENT, status=TransactionStatus.PAID,
        )
    )
    await db_session.commit()

    token = await _login(app_client, seeded_clinic_and_admin)
    headers = {"Authorization": f"Bearer {token}"}

    resp = await app_client.get("/api/v1/transactions", headers=headers)
    assert resp.status_code == 200
    body = resp.json()
    # 5 visible rows (tombstone excluded), all from our clinic, newest first.
    assert len(body) == 5
    assert all(t["clinic_id"] == clinic_id for t in body)
    assert 999.0 not in [t["amount_brl"] for t in body]
    dates = [t["occurred_on"] for t in body]
    assert dates == sorted(dates, reverse=True)


async def test_summary_math_matches_app(app_client, seeded_clinic_and_admin, db_session):
    clinic_id = seeded_clinic_and_admin["clinic"].id
    await _seed_transactions(db_session, clinic_id)

    token = await _login(app_client, seeded_clinic_and_admin)
    headers = {"Authorization": f"Bearer {token}"}

    resp = await app_client.get("/api/v1/transactions/summary", headers=headers)
    assert resp.status_code == 200
    s = resp.json()
    assert s["payments_brl"] == 350.0  # 200 + 150 (deleted 999 excluded)
    assert s["refunds_brl"] == 50.0
    assert s["net_revenue_brl"] == 300.0  # 350 - 50
    assert s["pending_brl"] == 80.0
    assert s["expenses_brl"] == 300.0
    assert s["transaction_count"] == 5


async def test_summary_respects_date_range(app_client, seeded_clinic_and_admin, db_session):
    clinic_id = seeded_clinic_and_admin["clinic"].id
    await _seed_transactions(db_session, clinic_id)

    token = await _login(app_client, seeded_clinic_and_admin)
    headers = {"Authorization": f"Bearer {token}"}

    # Only 2026-07-01..2026-07-05 → just the two 200/150 payments.
    resp = await app_client.get(
        "/api/v1/transactions/summary?start=2026-07-01&end=2026-07-05", headers=headers
    )
    assert resp.status_code == 200
    s = resp.json()
    assert s["payments_brl"] == 350.0
    assert s["refunds_brl"] == 0.0
    assert s["net_revenue_brl"] == 350.0
    assert s["transaction_count"] == 2


async def test_reports_overview(app_client, seeded_clinic_and_admin, db_session):
    clinic_id = seeded_clinic_and_admin["clinic"].id
    await _seed_transactions(db_session, clinic_id)

    token = await _login(app_client, seeded_clinic_and_admin)
    headers = {"Authorization": f"Bearer {token}"}

    resp = await app_client.get("/api/v1/reports/overview", headers=headers)
    assert resp.status_code == 200
    body = resp.json()
    assert body["patients_total"] == 0
    assert body["appointments_total"] == 0
    assert body["appointments_by_status"] == {}
    assert body["finance"]["net_revenue_brl"] == 300.0
