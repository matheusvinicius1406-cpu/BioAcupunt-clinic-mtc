from datetime import datetime
from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session
from app.schemas.patient import PatientCreate, PatientUpdate, PatientOut
from app.models.patient import PatientModel, Base
from app.core.database import engine, get_db
from app.core.events import _emit

router = APIRouter()


@router.on_event("startup")
def on_startup():
    Base.metadata.create_all(bind=engine)


@router.post("/", response_model=PatientOut)
def create_patient(payload: PatientCreate, db: Session = Depends(get_db)):
    patient = PatientModel(
        tenant_id=payload.tenant_id,
        full_name=payload.full_name,
        birth_date=payload.birth_date,
        gender=payload.gender,
    )
    db.add(patient)
    db.commit()
    db.refresh(patient)

    _emit(
        "PatientCreated",
        {
            "patientId": patient.id,
            "tenantId": patient.tenant_id,
            "fullName": patient.full_name,
            "createdAt": datetime.utcnow().isoformat() + "Z",
        },
    )

    return PatientOut(
        id=patient.id,
        tenant_id=patient.tenant_id,
        full_name=patient.full_name,
        birth_date=patient.birth_date,
        gender=patient.gender,
        status=patient.status,
        created_at=patient.created_at,
        updated_at=patient.updated_at,
    )
