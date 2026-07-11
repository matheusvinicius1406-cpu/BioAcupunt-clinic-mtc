from datetime import datetime

from pydantic import BaseModel

from app.models.appointment import AppointmentStatus


class AppointmentCreateRequest(BaseModel):
    patient_id: int
    professional_id: int
    scheduled_at: datetime
    notes: str = ""


class AppointmentUpdateStatusRequest(BaseModel):
    status: str


class AppointmentResponse(BaseModel):
    id: int
    clinic_id: int
    patient_id: int
    professional_id: int
    scheduled_at: datetime
    status: str
    notes: str
    created_at: datetime

    model_config = {"from_attributes": True}


__all__ = ["AppointmentCreateRequest", "AppointmentUpdateStatusRequest", "AppointmentResponse", "AppointmentStatus"]
