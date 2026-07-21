"""Delta sync endpoints.

Scope note: patients, appointments and transactions only. Clinical records —
prontuário, avaliação MTC, língua/pulso, flags de contraindicação — deliberately
do **not** sync. They are sensitive health data under LGPD art. 11 and stay on
the device until that is properly addressed. Do not add a clinical entity to
`SyncEntityType` without revisiting that decision.
"""

from fastapi import APIRouter, Depends, Query
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.deps import get_current_user
from app.db.session import get_db
from app.models.sync import current_revision
from app.models.user import User
from app.repositories import sync_repository
from app.schemas.sync import (
    SyncOp,
    SyncPullResponse,
    SyncPushRequest,
    SyncPushResponse,
    SyncResultStatus,
)
from app.services.audit_service import record_many

router = APIRouter(prefix="/api/v1/sync", tags=["sync"])


@router.post("/push", response_model=SyncPushResponse)
async def push(
    payload: SyncPushRequest,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> SyncPushResponse:
    """Uploads local changes.

    Every change is reported on individually. One conflicting or rejected record
    must not fail the batch: the other twenty edits the doctor made offline are
    just as real, and an all-or-nothing batch would strand them behind whichever
    single row happened to be contentious.

    Committed once, at the end. Each change already flushed inside the shared
    transaction, so either the whole batch lands or none of it does — a partial
    commit would hand out revision numbers for writes that were then rolled
    back, leaving permanent gaps that make delta pull skip records.
    """
    results = []
    for change in payload.changes:
        result = await sync_repository.apply_change(
            db, clinic_id=current_user.clinic_id, change=change
        )
        results.append(result)

    await db.commit()

    # One audit entry per record actually written, naming the resource. A single
    # "a sync happened" row would satisfy nobody auditing who touched a given
    # patient — which is the only question an LGPD trail exists to answer.
    await record_many(
        db,
        user_id=current_user.id,
        clinic_id=current_user.clinic_id,
        entries=[
            (
                result.entity_type.value.capitalize(),
                result.server_id,
                "DELETE" if change.op == SyncOp.DELETE else "UPSERT",
            )
            for change, result in zip(payload.changes, results)
            if result.status == SyncResultStatus.APPLIED and result.server_id is not None
        ],
    )

    server_rev = await current_revision(db, clinic_id=current_user.clinic_id)
    return SyncPushResponse(results=results, server_rev=server_rev)


@router.get("/pull", response_model=SyncPullResponse)
async def pull(
    since: int = Query(0, ge=0, description="Última revisão que este aparelho já processou."),
    limit: int = Query(200, ge=1, le=500),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> SyncPullResponse:
    """Downloads everything that changed above `since`, for this clinic only."""
    records, server_rev, has_more = await sync_repository.pull_since(
        db, clinic_id=current_user.clinic_id, since=since, limit=limit
    )
    return SyncPullResponse(records=records, server_rev=server_rev, has_more=has_more)
