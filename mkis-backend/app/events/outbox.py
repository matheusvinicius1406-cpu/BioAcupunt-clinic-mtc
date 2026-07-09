"""
MKIS — Outbox e domínio de eventos.
Garante exactly-once, schema versionado e reprocessamento.
"""

from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime, timezone
from enum import Enum
from typing import Any
from uuid import UUID, uuid4


class EventStatus(str, Enum):
    pending = "pending"
    published = "published"
    failed = "failed"


@dataclass
class OutboxEvent:
    id: UUID = field(default_factory=uuid4)
    occurred_at: datetime = field(default_factory=lambda: datetime.now(timezone.utc))
    event_name: str = ""
    stream_id: UUID | None = None
    correlation_id: UUID | None = None
    causation_id: UUID | None = None
    tenant_id: UUID = field(default_factory=uuid4)
    request_id: UUID = field(default_factory=uuid4)
    actor_id: UUID | None = None
    payload_hash: str = ""
    schema_version: str = "v1"
    payload: dict[str, Any] = field(default_factory=dict)
    status: EventStatus = EventStatus.pending
    attempts: int = 0
    last_error: str | None = None


def now_utc() -> datetime:
    return datetime.now(timezone.utc)
