import pytest

from app.models.clinic import Clinic
from app.repositories import patient_repository

pytestmark = pytest.mark.integration


async def _make_two_clinics(db_session):
    clinic_a = Clinic(name="Clínica A")
    clinic_b = Clinic(name="Clínica B")
    db_session.add_all([clinic_a, clinic_b])
    await db_session.commit()
    await db_session.refresh(clinic_a)
    await db_session.refresh(clinic_b)
    return clinic_a, clinic_b


async def test_create_and_list_patients(db_session):
    clinic = Clinic(name="Clínica X")
    db_session.add(clinic)
    await db_session.commit()
    await db_session.refresh(clinic)

    await patient_repository.create_patient(db_session, clinic_id=clinic.id, name="Ana Lima", document="12345678900")

    patients = await patient_repository.list_patients(db_session, clinic_id=clinic.id)
    assert len(patients) == 1
    assert patients[0].name == "Ana Lima"


async def test_document_is_stored_hashed_not_plaintext(db_session):
    clinic = Clinic(name="Clínica X")
    db_session.add(clinic)
    await db_session.commit()
    await db_session.refresh(clinic)

    patient = await patient_repository.create_patient(
        db_session, clinic_id=clinic.id, name="Ana Lima", document="123.456.789-00"
    )
    assert patient.document_hash is not None
    assert "123" not in patient.document_hash


async def test_get_by_id_respects_clinic_boundary(db_session):
    """A patient created for clinic A must never resolve through clinic B's
    scoped lookup — the core multi-tenancy guarantee. If this regresses,
    patient data leaks across clinics."""
    clinic_a, clinic_b = await _make_two_clinics(db_session)

    patient = await patient_repository.create_patient(db_session, clinic_id=clinic_a.id, name="Paciente A", document=None)

    same_clinic = await patient_repository.get_patient(db_session, clinic_id=clinic_a.id, patient_id=patient.id)
    other_clinic = await patient_repository.get_patient(db_session, clinic_id=clinic_b.id, patient_id=patient.id)

    assert same_clinic is not None
    assert other_clinic is None


async def test_soft_deleted_patient_not_listed(db_session):
    from datetime import datetime, timezone

    clinic = Clinic(name="Clínica X")
    db_session.add(clinic)
    await db_session.commit()
    await db_session.refresh(clinic)

    patient = await patient_repository.create_patient(db_session, clinic_id=clinic.id, name="Paciente", document=None)
    patient.deleted_at = datetime.now(timezone.utc)
    await db_session.commit()

    patients = await patient_repository.list_patients(db_session, clinic_id=clinic.id)
    assert patients == []
