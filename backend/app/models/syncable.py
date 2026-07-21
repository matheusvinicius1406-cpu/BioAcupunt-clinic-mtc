"""Columns every syncable table needs.

Kept as a mixin so the three syncable tables cannot drift apart. A table that
silently lacks `rev` would be invisible to delta pull; one lacking `client_id`
would duplicate itself on every retry.
"""

from datetime import datetime

from sqlalchemy import BigInteger, DateTime, String, func
from sqlalchemy.orm import Mapped, mapped_column


class SyncableMixin:
    """Revision, client identity and soft-delete columns for a synced table."""

    #: Server-assigned revision. See app/models/sync.py.
    rev: Mapped[int] = mapped_column(BigInteger, default=0, nullable=False, index=True)

    #: The id the *client* generated for this row before it ever reached the
    #: server. Sync is retried on flaky mobile networks, and a retry whose
    #: response was lost must not create the patient a second time — the server
    #: recognises the client_id and updates instead. Without it, one lost
    #: response produces a duplicate chart, and duplicate charts are how a
    #: contraindication ends up recorded on the copy nobody opens.
    client_id: Mapped[str | None] = mapped_column(String(64), nullable=True, index=True)

    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), onupdate=func.now()
    )

    #: Soft delete. A hard DELETE cannot be communicated to a device that is
    #: offline at the time — it would simply re-upload the row it still has and
    #: resurrect it. A tombstone syncs like any other change.
    deleted_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
