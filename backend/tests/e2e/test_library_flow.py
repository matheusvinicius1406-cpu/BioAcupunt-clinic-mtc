import pytest

from app.models.library_node import LibraryNode

pytestmark = pytest.mark.e2e


async def _login(app_client, seeded_clinic_and_admin) -> str:
    user = seeded_clinic_and_admin["user"]
    response = await app_client.post(
        "/api/v1/auth/login",
        json={"email": user.email, "password": seeded_clinic_and_admin["password"]},
    )
    return response.json()["access_token"]


async def test_library_requires_auth(app_client):
    response = await app_client.get("/api/v1/library")
    assert response.status_code == 401


async def test_library_empty_by_default(app_client, seeded_clinic_and_admin):
    # No content is seeded — the acervo is fed by ingestion, never invented (R4).
    token = await _login(app_client, seeded_clinic_and_admin)
    headers = {"Authorization": f"Bearer {token}"}
    resp = await app_client.get("/api/v1/library", headers=headers)
    assert resp.status_code == 200
    assert resp.json() == []


async def test_list_search_and_detail(app_client, seeded_clinic_and_admin, db_session):
    db_session.add_all(
        [
            LibraryNode(
                id="a1", type="artigo", title="Meridiano do Pulmão",
                summary="Trajeto e pontos", content="...", tags="pulmao,meridiano",
            ),
            LibraryNode(
                id="b2", type="artigo", title="Ponto LI4 (Hegu)",
                summary="Indicações e cautelas", content="...", tags="ponto",
            ),
        ]
    )
    await db_session.commit()

    token = await _login(app_client, seeded_clinic_and_admin)
    headers = {"Authorization": f"Bearer {token}"}

    # Alphabetical listing.
    resp = await app_client.get("/api/v1/library", headers=headers)
    assert [n["id"] for n in resp.json()] == ["a1", "b2"]

    # Case-insensitive search over title.
    resp = await app_client.get("/api/v1/library?q=hegu", headers=headers)
    assert [n["id"] for n in resp.json()] == ["b2"]

    # Detail by id.
    resp = await app_client.get("/api/v1/library/a1", headers=headers)
    assert resp.status_code == 200
    assert resp.json()["title"] == "Meridiano do Pulmão"

    # Missing id → 404.
    resp = await app_client.get("/api/v1/library/does-not-exist", headers=headers)
    assert resp.status_code == 404
