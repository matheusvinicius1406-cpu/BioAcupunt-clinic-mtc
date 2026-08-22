"""Schemas for IdentityMap API."""

from datetime import datetime

from pydantic import BaseModel


class IdentityMapResponse(BaseModel):
    """An identity mapping between BioAcupunt Patient and Twenty Person."""

    id: int
    clinic_id: int
    bioacupunt_patient_id: int
    twenty_person_id: str
    source: str  # MANUAL, MIGRATED, SYNCED
    status: str  # ACTIVE, INACTIVE, CONFLICT
    created_at: datetime
    updated_at: datetime

    model_config = {"from_attributes": True}


class IdentityMapCreateRequest(BaseModel):
    """Request to create an identity mapping."""

    bioacupunt_patient_id: int
    twenty_person_id: str
    source: str = "MANUAL"
