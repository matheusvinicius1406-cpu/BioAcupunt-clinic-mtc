"""Wire format for delta sync.

The payload is a free-form dict per entity rather than a typed union. That is a
deliberate trade: the app's local schema grows faster than the server's, and a
strict server-side model would reject a whole batch — losing a real appointment —
because one field it had never heard of came along for the ride. Each entity's
own writer picks out the fields it knows and ignores the rest.

What is *not* free-form: `entity_type`, `op`, `client_id` and `base_rev`. Those
drive routing and conflict detection, so they are validated strictly.
"""

from enum import Enum
from typing import Any

from pydantic import BaseModel, Field


class SyncEntityType(str, Enum):
    PATIENT = "patient"
    APPOINTMENT = "appointment"
    TRANSACTION = "transaction"


class SyncOp(str, Enum):
    UPSERT = "upsert"
    DELETE = "delete"


class SyncChange(BaseModel):
    """One local edit, on its way up."""

    entity_type: SyncEntityType
    op: SyncOp = SyncOp.UPSERT

    #: Client-generated stable id. Required, because it is what makes a retried
    #: push idempotent.
    client_id: str = Field(min_length=1, max_length=64)

    #: Server id, when the client has already seen this row come back.
    server_id: int | None = None

    #: The revision this edit was based on. `None` means "this row is new".
    #: If it disagrees with what the server holds, the edit is a conflict.
    base_rev: int | None = None

    payload: dict[str, Any] = Field(default_factory=dict)


class SyncPushRequest(BaseModel):
    changes: list[SyncChange] = Field(default_factory=list, max_length=500)


class SyncResultStatus(str, Enum):
    APPLIED = "applied"
    CONFLICT = "conflict"
    REJECTED = "rejected"


class SyncChangeResult(BaseModel):
    """What became of one pushed change."""

    client_id: str
    entity_type: SyncEntityType
    status: SyncResultStatus

    server_id: int | None = None
    rev: int | None = None

    #: On CONFLICT: the server's current version, so the app can show the
    #: doctor both and let her choose. The server never picks for her.
    server_payload: dict[str, Any] | None = None

    #: On REJECTED: why. Never shown raw to the user, but it has to be
    #: recoverable from logs, or a rejected record becomes an unexplained hole.
    reason: str | None = None


class SyncPushResponse(BaseModel):
    results: list[SyncChangeResult]
    server_rev: int


class SyncRecord(BaseModel):
    """One row on its way down."""

    entity_type: SyncEntityType
    server_id: int
    client_id: str | None = None
    rev: int
    deleted: bool = False
    payload: dict[str, Any] = Field(default_factory=dict)


class SyncPullResponse(BaseModel):
    records: list[SyncRecord]
    server_rev: int
    #: True when the batch hit its limit and more remains above the last rev
    #: returned. The client pulls again rather than assuming it is up to date.
    has_more: bool = False
