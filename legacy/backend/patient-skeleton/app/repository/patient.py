from sqlalchemy.orm import Session
from app.models.patient import PatientModel
from app.schemas.patient import PatientCreate, PatientUpdate, PatientOut


class PatientRepository:
    def __init__(self, db: Session):
        self.db = db

    def save(self, patient: PatientModel) -> PatientModel:
        self.db.add(patient)
        self.db.commit()
        self.db.refresh(patient)
        return patient

    def get(self, patient_id: str) -> PatientModel | None:
        return self.db.query(PatientModel).filter(PatientModel.id == patient_id).first()

    def list_by_tenant(self, tenant_id: str) -> list[PatientModel]:
        return (
            self.db.query(PatientModel)
            .filter(PatientModel.tenant_id == tenant_id)
            .filter(PatientModel.deleted_at == None)
            .all()
        )

