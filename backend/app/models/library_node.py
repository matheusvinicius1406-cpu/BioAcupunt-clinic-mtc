from datetime import datetime

from sqlalchemy import DateTime, Integer, String, Text, func
from sqlalchemy.orm import Mapped, mapped_column

from app.db.base import Base


class LibraryNode(Base):
    """Server counterpart of the app's `biblioteca_nodes` — the curated acervo.

    Global reference content, not clinic-scoped and not synced: the app entity
    has no tenant and no sync identity because it is loaded from reviewed source
    packs on-device, not written per patient. This table is fed by the ingestion
    pipeline (future), never by hand — per R4, content comes from a human-reviewed
    source, never generated. Until that pipeline populates it, the endpoint is
    honestly empty rather than showing invented articles.

    The column is `metadata_json` (not `metadata`) because `metadata` is a
    reserved attribute on SQLAlchemy's Declarative Base.
    """

    __tablename__ = "library_nodes"

    id: Mapped[str] = mapped_column(String(128), primary_key=True)
    type: Mapped[str] = mapped_column(String(40), default="", server_default="")
    title: Mapped[str] = mapped_column(String(500), index=True)
    content: Mapped[str] = mapped_column(Text, default="", server_default="")
    summary: Mapped[str] = mapped_column(Text, default="", server_default="")
    tags: Mapped[str] = mapped_column(String(1000), default="", server_default="")
    version: Mapped[int] = mapped_column(Integer, default=1, server_default="1")
    #: JSON string: citação, fonte, url, proveniência (mesmo shape do app).
    metadata_json: Mapped[str] = mapped_column(Text, default="{}", server_default="{}")
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
