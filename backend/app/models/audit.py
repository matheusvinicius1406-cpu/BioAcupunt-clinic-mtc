from datetime import datetime

from sqlalchemy import DateTime, ForeignKey, Integer, String, func
from sqlalchemy.orm import Mapped, mapped_column

from app.db.base import Base


class DataAccessLog(Base):
    """LGPD audit trail: who accessed/changed what resource, and when."""

    __tablename__ = "data_access_logs"

    id: Mapped[int] = mapped_column(primary_key=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id"), index=True)
    clinic_id: Mapped[int] = mapped_column(ForeignKey("clinics.id"), index=True)
    resource_type: Mapped[str] = mapped_column(String(50))
    resource_id: Mapped[int] = mapped_column(Integer)
    action: Mapped[str] = mapped_column(String(20))  # CREATE, READ, UPDATE, DELETE
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
