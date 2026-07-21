"""End-to-end tests for delta sync.

The rule under test throughout: **the server never silently overwrites an edit
it did not expect.** Two devices editing the same record is not an error to be
resolved by whoever pushed last — it is a question only the doctor can answer,
and these tests exist to make sure the server keeps asking it.
"""

import pytest

pytestmark = pytest.mark.anyio


async def _login(app_client, seeded) -> dict:
    response = await app_client.post(
        "/api/v1/auth/login",
        json={"email": seeded["user"].email, "password": seeded["password"]},
    )
    assert response.status_code == 200, response.text
    return {"Authorization": f"Bearer {response.json()['access_token']}"}


async def _push(app_client, headers, changes: list[dict]) -> dict:
    response = await app_client.post(
        "/api/v1/sync/push", json={"changes": changes}, headers=headers
    )
    assert response.status_code == 200, response.text
    return response.json()


async def test_new_record_is_applied_and_gets_a_revision(app_client, seeded_clinic_and_admin):
    headers = await _login(app_client, seeded_clinic_and_admin)

    body = await _push(app_client, headers, [
        {
            "entity_type": "patient",
            "op": "upsert",
            "client_id": "dev-a-1",
            "payload": {"name": "Ana Lima", "phone": "11999990000"},
        }
    ])

    result = body["results"][0]
    assert result["status"] == "applied"
    assert result["server_id"] is not None
    assert result["rev"] > 0


async def test_pull_returns_what_another_device_pushed(app_client, seeded_clinic_and_admin):
    headers = await _login(app_client, seeded_clinic_and_admin)
    await _push(app_client, headers, [
        {"entity_type": "patient", "op": "upsert", "client_id": "dev-a-1",
         "payload": {"name": "Ana Lima"}}
    ])

    # A second device, starting from nothing.
    response = await app_client.get("/api/v1/sync/pull?since=0", headers=headers)
    assert response.status_code == 200
    body = response.json()

    names = [r["payload"]["name"] for r in body["records"] if r["entity_type"] == "patient"]
    assert "Ana Lima" in names


async def test_concurrent_edit_is_reported_as_conflict_and_not_overwritten(
    app_client, seeded_clinic_and_admin
):
    """The case this whole design exists for.

    Two devices edit the same patient offline. The second one to arrive must be
    refused — not merged, not applied on top. Whichever note the doctor wrote is
    still on her other device, and the server has no way to know which one she
    would keep.
    """
    headers = await _login(app_client, seeded_clinic_and_admin)

    created = await _push(app_client, headers, [
        {"entity_type": "patient", "op": "upsert", "client_id": "shared-1",
         "payload": {"name": "Ana Lima", "notes": "original"}}
    ])
    server_id = created["results"][0]["server_id"]
    rev_1 = created["results"][0]["rev"]

    # Device A edits, based on rev_1 — accepted.
    edit_a = await _push(app_client, headers, [
        {"entity_type": "patient", "op": "upsert", "client_id": "shared-1",
         "server_id": server_id, "base_rev": rev_1,
         "payload": {"name": "Ana Lima", "notes": "anotação do celular"}}
    ])
    assert edit_a["results"][0]["status"] == "applied"

    # Device B was offline and still believes the record is at rev_1.
    edit_b = await _push(app_client, headers, [
        {"entity_type": "patient", "op": "upsert", "client_id": "shared-1",
         "server_id": server_id, "base_rev": rev_1,
         "payload": {"name": "Ana Lima", "notes": "anotação do tablet"}}
    ])
    conflict = edit_b["results"][0]
    assert conflict["status"] == "conflict"
    # The server hands back its version so the app can show both.
    assert conflict["server_payload"]["notes"] == "anotação do celular"

    # And device A's note is untouched on the server.
    pull = await app_client.get("/api/v1/sync/pull?since=0", headers=headers)
    patient = next(
        r for r in pull.json()["records"]
        if r["entity_type"] == "patient" and r["server_id"] == server_id
    )
    assert patient["payload"]["notes"] == "anotação do celular"


async def test_retried_push_does_not_duplicate_the_record(app_client, seeded_clinic_and_admin):
    """A lost response must not create a second chart.

    The client pushes, the network drops the reply, the client retries the same
    change. `client_id` is what makes the second attempt find the first one.
    """
    headers = await _login(app_client, seeded_clinic_and_admin)
    change = {
        "entity_type": "patient", "op": "upsert", "client_id": "retry-1",
        "payload": {"name": "Carlos Souza"},
    }

    first = await _push(app_client, headers, [change])
    second = await _push(app_client, headers, [change])

    # The retry is refused as a conflict rather than silently duplicating —
    # the client resolves it by adopting the server's row.
    assert second["results"][0]["status"] == "conflict"
    assert second["results"][0]["server_id"] == first["results"][0]["server_id"]

    pull = await app_client.get("/api/v1/sync/pull?since=0", headers=headers)
    carlos = [
        r for r in pull.json()["records"]
        if r["entity_type"] == "patient" and r["payload"]["name"] == "Carlos Souza"
    ]
    assert len(carlos) == 1, "a retried push must not create a second patient"


async def test_one_conflict_does_not_block_the_rest_of_the_batch(
    app_client, seeded_clinic_and_admin
):
    """Offline work arrives in batches; one contentious row must not strand the others."""
    headers = await _login(app_client, seeded_clinic_and_admin)
    created = await _push(app_client, headers, [
        {"entity_type": "patient", "op": "upsert", "client_id": "batch-1",
         "payload": {"name": "Ana"}}
    ])
    server_id = created["results"][0]["server_id"]

    body = await _push(app_client, headers, [
        # Stale base_rev — will conflict.
        {"entity_type": "patient", "op": "upsert", "client_id": "batch-1",
         "server_id": server_id, "base_rev": 999, "payload": {"name": "Ana editada"}},
        # Perfectly good new record travelling in the same batch.
        {"entity_type": "patient", "op": "upsert", "client_id": "batch-2",
         "payload": {"name": "Maria Santos"}},
    ])

    statuses = {r["client_id"]: r["status"] for r in body["results"]}
    assert statuses["batch-1"] == "conflict"
    assert statuses["batch-2"] == "applied"


async def test_deleted_record_is_sent_as_a_tombstone(app_client, seeded_clinic_and_admin):
    """A device offline during a delete must still hear about it.

    Otherwise it re-uploads the copy it still holds and the record comes back
    from the dead.
    """
    headers = await _login(app_client, seeded_clinic_and_admin)
    created = await _push(app_client, headers, [
        {"entity_type": "patient", "op": "upsert", "client_id": "del-1",
         "payload": {"name": "Paciente Removida"}}
    ])
    result = created["results"][0]

    await _push(app_client, headers, [
        {"entity_type": "patient", "op": "delete", "client_id": "del-1",
         "server_id": result["server_id"], "base_rev": result["rev"]}
    ])

    pull = await app_client.get("/api/v1/sync/pull?since=0", headers=headers)
    record = next(
        r for r in pull.json()["records"]
        if r["entity_type"] == "patient" and r["server_id"] == result["server_id"]
    )
    assert record["deleted"] is True


async def test_payload_cannot_reassign_a_record_to_another_clinic(
    app_client, seeded_clinic_and_admin
):
    """clinic_id is not a writable field. A client must not be able to move its
    own records into someone else's clinic by including the key in a payload."""
    headers = await _login(app_client, seeded_clinic_and_admin)
    real_clinic_id = seeded_clinic_and_admin["clinic"].id

    body = await _push(app_client, headers, [
        {"entity_type": "patient", "op": "upsert", "client_id": "evil-1",
         "payload": {"name": "Teste", "clinic_id": real_clinic_id + 999, "rev": 10_000}}
    ])
    assert body["results"][0]["status"] == "applied"

    # It landed in the caller's own clinic, and rev came from the server.
    pull = await app_client.get("/api/v1/sync/pull?since=0", headers=headers)
    record = next(r for r in pull.json()["records"] if r["client_id"] == "evil-1")
    assert record["rev"] < 10_000


async def test_money_survives_the_round_trip_exactly(app_client, seeded_clinic_and_admin):
    """150.10 must come back as 150.10, not 150.09999999999999."""
    headers = await _login(app_client, seeded_clinic_and_admin)
    patient = await _push(app_client, headers, [
        {"entity_type": "patient", "op": "upsert", "client_id": "pay-p1",
         "payload": {"name": "Ana"}}
    ])
    patient_id = patient["results"][0]["server_id"]

    await _push(app_client, headers, [
        {"entity_type": "transaction", "op": "upsert", "client_id": "tx-1",
         "payload": {
             "patient_id": patient_id, "amount_brl": 150.10,
             "occurred_on": "2026-07-18", "type": "PAGAMENTO", "status": "PAGO",
         }}
    ])

    pull = await app_client.get("/api/v1/sync/pull?since=0", headers=headers)
    tx = next(r for r in pull.json()["records"] if r["entity_type"] == "transaction")
    assert tx["payload"]["amount_brl"] == 150.10


async def test_pull_is_scoped_to_the_callers_clinic(app_client, seeded_clinic_and_admin, db_session):
    """A second clinic's records must never appear in this clinic's pull."""
    from app.core.security import hash_password
    from app.models.clinic import Clinic
    from app.models.patient import Patient
    from app.models.user import User, UserRole

    other = Clinic(name="Outra Clínica")
    db_session.add(other)
    await db_session.commit()
    await db_session.refresh(other)
    db_session.add(
        Patient(clinic_id=other.id, name="Paciente de Outra Clínica", rev=1, client_id="other-1")
    )
    db_session.add(
        User(
            clinic_id=other.id, email="outra@example.com",
            password_hash=hash_password("Sup3rSecret!"), full_name="Outra",
            role=UserRole.ADMIN,
        )
    )
    await db_session.commit()

    headers = await _login(app_client, seeded_clinic_and_admin)
    pull = await app_client.get("/api/v1/sync/pull?since=0", headers=headers)

    names = [r["payload"].get("name") for r in pull.json()["records"]]
    assert "Paciente de Outra Clínica" not in names


async def test_sync_requires_authentication(app_client):
    response = await app_client.get("/api/v1/sync/pull?since=0")
    assert response.status_code == 401
