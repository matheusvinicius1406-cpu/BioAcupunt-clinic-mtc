"""Delta sync: apply pushed changes, serve pulled ones.

The single most important rule in this file:

    **The server never silently overwrites an edit it did not expect.**

When a client pushes an edit based on revision N and the row is now at revision
M, the two devices changed the same record independently. There is no safe
automatic answer to that — "newest wins" would delete one of them, and the app
has no way to know which one mattered. So the write is refused, both versions
survive, and the choice goes to the doctor.

Everything else here exists to make that rule enforceable.
"""

from datetime import date, datetime, timezone
from decimal import Decimal, InvalidOperation
from typing import Any

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.appointment import Appointment
from app.models.patient import Patient
from app.models.sync import current_revision, next_revision
from app.models.transaction import Transaction
from app.schemas.sync import (
    SyncChange,
    SyncChangeResult,
    SyncEntityType,
    SyncOp,
    SyncRecord,
    SyncResultStatus,
)

MODEL_FOR_ENTITY = {
    SyncEntityType.PATIENT: Patient,
    SyncEntityType.APPOINTMENT: Appointment,
    SyncEntityType.TRANSACTION: Transaction,
}

# Fields the server accepts per entity. An allowlist, not a denylist: an unknown
# key from a newer app version is dropped, never written blindly, and never
# allowed to reach `clinic_id`, `rev` or `id`. A client must not be able to
# reassign its own row to another clinic by including clinic_id in a payload.
WRITABLE_FIELDS = {
    SyncEntityType.PATIENT: {"name", "status", "phone", "email", "stage", "notes"},
    SyncEntityType.APPOINTMENT: {
        "patient_id", "professional_id", "scheduled_at", "status", "notes", "value_brl", "paid",
    },
    SyncEntityType.TRANSACTION: {
        "patient_id", "appointment_id", "amount_brl", "occurred_on", "type", "method",
        "category", "status", "notes",
    },
}

_DECIMAL_FIELDS = {"value_brl", "amount_brl"}
_DATETIME_FIELDS = {"scheduled_at"}
_DATE_FIELDS = {"occurred_on"}


def _coerce(field: str, value: Any) -> Any:
    """Turns JSON scalars into the column types SQLAlchemy expects.

    Money arrives as a JSON number and must become Decimal *via str* — passing a
    float straight to Decimal carries the binary rounding error along with it,
    and 150.0 becomes 149.99999999999997 in a revenue total.
    """
    if value is None:
        return None
    if field in _DECIMAL_FIELDS:
        try:
            return Decimal(str(value))
        except (InvalidOperation, ValueError):
            raise ValueError(f"{field} não é um valor numérico válido: {value!r}") from None
    if field in _DATETIME_FIELDS and isinstance(value, str):
        parsed = datetime.fromisoformat(value.replace("Z", "+00:00"))
        # A naive datetime compared against timezone-aware rows silently
        # misorders the agenda by the UTC offset.
        return parsed if parsed.tzinfo else parsed.replace(tzinfo=timezone.utc)
    if field in _DATE_FIELDS and isinstance(value, str):
        return date.fromisoformat(value[:10])
    return value


def _serialise(entity_type: SyncEntityType, row: Any) -> dict[str, Any]:
    """Row → JSON-safe payload, using the same allowlist as writes."""
    out: dict[str, Any] = {}
    for field in WRITABLE_FIELDS[entity_type]:
        value = getattr(row, field, None)
        if isinstance(value, Decimal):
            out[field] = float(value)
        elif isinstance(value, (datetime, date)):
            out[field] = value.isoformat()
        else:
            out[field] = value
    return out


async def _find_existing(
    db: AsyncSession, model: Any, *, clinic_id: int, change: SyncChange
) -> Any | None:
    """Locates the row a change refers to.

    Tries `server_id` first, then falls back to `client_id`. The fallback is what
    makes a retry safe: if the client pushed successfully but never received the
    response, it retries without a server_id, and this finds the row it already
    created instead of creating a second one.

    Both lookups are scoped by clinic_id — a server_id from another clinic must
    never resolve, or one clinic could overwrite another's records by guessing
    an integer.
    """
    if change.server_id is not None:
        result = await db.execute(
            select(model).where(model.id == change.server_id, model.clinic_id == clinic_id)
        )
        row = result.scalar_one_or_none()
        if row is not None:
            return row

    result = await db.execute(
        select(model).where(model.client_id == change.client_id, model.clinic_id == clinic_id)
    )
    return result.scalar_one_or_none()


async def apply_change(
    db: AsyncSession, *, clinic_id: int, change: SyncChange
) -> SyncChangeResult:
    """Applies one pushed change, or reports why it could not be applied."""
    model = MODEL_FOR_ENTITY[change.entity_type]
    existing = await _find_existing(db, model, clinic_id=clinic_id, change=change)

    # ── Conflict detection ────────────────────────────────────────────────
    # The row moved since the client last saw it. Refuse, and hand back the
    # server's version so the app can present both.
    if existing is not None and change.base_rev is not None and existing.rev != change.base_rev:
        return SyncChangeResult(
            client_id=change.client_id,
            entity_type=change.entity_type,
            status=SyncResultStatus.CONFLICT,
            server_id=existing.id,
            rev=existing.rev,
            server_payload=_serialise(change.entity_type, existing),
        )

    # A client that thinks it is creating a row (base_rev None) but whose
    # client_id already exists is *also* a conflict — two devices created what
    # they each believe is a new record under the same identity. Treated as a
    # conflict rather than an overwrite for the same reason as above.
    if existing is not None and change.base_rev is None and change.op == SyncOp.UPSERT:
        return SyncChangeResult(
            client_id=change.client_id,
            entity_type=change.entity_type,
            status=SyncResultStatus.CONFLICT,
            server_id=existing.id,
            rev=existing.rev,
            server_payload=_serialise(change.entity_type, existing),
        )

    rev = await next_revision(db, clinic_id=clinic_id)

    if change.op == SyncOp.DELETE:
        if existing is None:
            # Deleting something the server never had is not an error: the
            # outcome the client wanted is already true.
            return SyncChangeResult(
                client_id=change.client_id,
                entity_type=change.entity_type,
                status=SyncResultStatus.APPLIED,
                rev=rev,
            )
        existing.deleted_at = datetime.now(timezone.utc)
        existing.rev = rev
        await db.flush()
        return SyncChangeResult(
            client_id=change.client_id,
            entity_type=change.entity_type,
            status=SyncResultStatus.APPLIED,
            server_id=existing.id,
            rev=rev,
        )

    allowed = WRITABLE_FIELDS[change.entity_type]
    try:
        fields = {
            key: _coerce(key, value)
            for key, value in change.payload.items()
            if key in allowed
        }
    except ValueError as exc:
        return SyncChangeResult(
            client_id=change.client_id,
            entity_type=change.entity_type,
            status=SyncResultStatus.REJECTED,
            reason=str(exc),
        )

    if existing is None:
        row = model(clinic_id=clinic_id, client_id=change.client_id, rev=rev, **fields)
        db.add(row)
        await db.flush()
    else:
        for key, value in fields.items():
            setattr(existing, key, value)
        existing.rev = rev
        # An edit un-deletes: the doctor editing a record she can still see is
        # asserting it exists, whatever a stale tombstone says.
        existing.deleted_at = None
        row = existing
        await db.flush()

    return SyncChangeResult(
        client_id=change.client_id,
        entity_type=change.entity_type,
        status=SyncResultStatus.APPLIED,
        server_id=row.id,
        rev=rev,
    )


async def pull_since(
    db: AsyncSession, *, clinic_id: int, since: int, limit: int = 200
) -> tuple[list[SyncRecord], int, bool]:
    """Every change above `since`, oldest revision first.

    Ordered by `rev` so the client can advance its cursor safely: once it has
    processed a batch, everything at or below the last rev is durably handled.
    Ordering by anything else would make a partial batch un-resumable.
    """
    records: list[SyncRecord] = []

    for entity_type, model in MODEL_FOR_ENTITY.items():
        result = await db.execute(
            select(model)
            .where(model.clinic_id == clinic_id, model.rev > since)
            .order_by(model.rev)
            .limit(limit)
        )
        for row in result.scalars().all():
            records.append(
                SyncRecord(
                    entity_type=entity_type,
                    server_id=row.id,
                    client_id=row.client_id,
                    rev=row.rev,
                    # Tombstones are sent, not hidden. A device that was offline
                    # when a row was deleted needs to hear about the deletion,
                    # or it will re-upload the copy it still holds.
                    deleted=row.deleted_at is not None,
                    payload=_serialise(entity_type, row),
                )
            )

    records.sort(key=lambda record: record.rev)
    has_more = len(records) > limit
    if has_more:
        records = records[:limit]

    server_rev = await current_revision(db, clinic_id=clinic_id)
    return records, server_rev, has_more
