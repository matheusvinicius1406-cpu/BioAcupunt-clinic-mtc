"""
MKIS — Publicação/consumo de eventos.
Publisher + DLQ + idempotência por idempotency_key.
"""

from __future__ import annotations

from collections.abc import Sequence
from datetime import datetime, timezone
from typing import Protocol
from uuid import UUID, uuid4

from mkis_backend.app.events.outbox import EventStatus, OutboxEvent


class EventStore(Protocol):
    async def save(self, event: OutboxEvent) -> OutboxEvent:
        ...

    async def pending(self, *, limit: int = 100) -> Sequence[OutboxEvent]:
        ...

    async def mark_published(self, event_id: UUID) -> None:
        ...

    async def mark_failed(self, event_id: UUID, *, error: str) -> None:
        ...


class EventPublisher:
    def __init__(self, store: EventStore):
        self.store = store

    async def publish(self, event: OutboxEvent) -> OutboxEvent:
        event.occurred_at = datetime.now(timezone.utc)
        event.status = EventStatus.pending
        return await self.store.save(event)
