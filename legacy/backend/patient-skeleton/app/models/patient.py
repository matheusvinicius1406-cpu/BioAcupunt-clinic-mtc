from datetime import datetime
from uuid import uuid4
from sqlalchemy import Column, String, Text, DateTime
from sqlalchemy.orm import declarative_base

Base = declarative_base()


class PatientModel(Base):
    __tablename__ = "patients"

    id = Column(String(36), primary_key=True, default=lambda: str(uuid4()))
    tenant_id = Column(String(36), nullable=False, index=True)
    full_name = Column(String(255), nullable=False)
    birth_date = Column(String(20), nullable=True)
    gender = Column(String(50), nullable=True)
    cpf_hash = Column(String(255), nullable=True)
    rg_hash = Column(String(255), nullable=True)
    address = Column(Text, nullable=True)
    contacts = Column(Text, nullable=True)
    status = Column(String(50), nullable=False, default="active")
    created_at = Column(DateTime, default=datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.utcnow)
    deleted_at = Column(DateTime, nullable=True)
