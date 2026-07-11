from datetime import datetime

from pydantic import BaseModel


class PatientCreateRequest(BaseModel):
    name: str
    document: str | None = None


class PatientResponse(BaseModel):
    id: int
    clinic_id: int
    name: str
    status: str
    created_at: datetime
    updated_at: datetime

    model_config = {"from_attributes": True}
