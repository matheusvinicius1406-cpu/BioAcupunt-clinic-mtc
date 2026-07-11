from pydantic import BaseModel, Field
from datetime import datetime
from typing import Optional


class PatientCreate(BaseModel):
    tenant_id: str = Field(..., description="Identificador do tenant")
    full_name: str = Field(..., min_length=1)
    birth_date: Optional[str] = None
    gender: Optional[str] = None


class PatientUpdate(BaseModel):
    tenant_id: Optional[str] = None
    full_name: Optional[str] = Field(None, min_length=1)
    birth_date: Optional[str] = None
    gender: Optional[str] = None


class PatientOut(BaseModel):
    id: str
    tenant_id: str
    full_name: str
    birth_date: Optional[str]
    gender: Optional[str]
    status: str
    created_at: Optional[datetime]
    updated_at: Optional[datetime]
