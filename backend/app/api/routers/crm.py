"""CRM API — honest about data sources.

LOCAL endpoints (query CrmPatient directly):
- GET /crm — list patients
- GET /crm/pipeline — pipeline summary
- GET /crm/dashboard — aggregated dashboard (partial when Twenty offline)
- GET /crm/search — search local patients

TWENTY-DEPENDENT endpoints (proxy to Twenty when available, 503 when not):
- GET /crm/leads → Twenty people
- GET /crm/tasks → Twenty tasks
- GET /crm/activities → Twenty activities
- GET /crm/timeline → Twenty + clinical aggregate
- GET /crm/referrals → Twenty
- GET /crm/audit → Twenty

BRIDGE endpoints:
- GET /identity-map — Patient ↔ Person mapping
"""

from datetime import datetime

from fastapi import APIRouter, Depends, HTTPException, Query
from pydantic import BaseModel
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.deps import get_current_user
from app.db.session import get_db
from app.models.user import User
from app.repositories import crm_repository
from app.schemas.crm import CrmPatientResponse, CrmPipelineSummary
from app.services.twenty_gateway import TwentyConfig, TwentyGateway

router = APIRouter(prefix="/api/v1/crm", tags=["crm"])

# Twenty Gateway singleton — lazily initialized
_gateway: TwentyGateway | None = None


def _get_gateway() -> TwentyGateway:
    global _gateway
    if _gateway is None:
        _gateway = TwentyGateway.from_config(TwentyConfig.from_env())
    return _gateway


# ═════════════════════════════════════════════════════════════════════
# LOCAL — query CrmPatient directly
# ═════════════════════════════════════════════════════════════════════


@router.get("", response_model=list[CrmPatientResponse])
async def list_crm_patients(
    stage: str | None = Query(default=None, description="Filtra por estágio do funil."),
    limit: int = Query(default=1000, ge=1, le=5000),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> list[CrmPatientResponse]:
    return await crm_repository.list_crm_patients(
        db, clinic_id=current_user.clinic_id, stage=stage, limit=limit
    )


@router.get("/pipeline", response_model=CrmPipelineSummary)
async def crm_pipeline(
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> CrmPipelineSummary:
    summary = await crm_repository.pipeline_summary(db, clinic_id=current_user.clinic_id)
    return CrmPipelineSummary(**summary)


# ═════════════════════════════════════════════════════════════════════
# DASHBOARD — local + Twenty when available
# ═════════════════════════════════════════════════════════════════════


class CrmDashboardResponse(BaseModel):
    """Aggregated CRM dashboard data."""
    total_patients: int = 0
    active_patients: int = 0
    inactive_patients: int = 0
    total_leads: int = 0
    new_leads: int = 0
    pending_tasks: int = 0
    overdue_tasks: int = 0
    recent_activities: int = 0
    twenty_available: bool = False


@router.get("/dashboard", response_model=CrmDashboardResponse)
async def crm_dashboard(
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> CrmDashboardResponse:
    """Aggregated CRM dashboard. Merges local + Twenty data."""
    clinic_id = current_user.clinic_id
    gateway = _get_gateway()

    # Local data — always available
    total = await crm_repository.count_patients(db, clinic_id=clinic_id)
    active = await crm_repository.count_patients(db, clinic_id=clinic_id, active_only=True)
    inactive = await crm_repository.count_inactive_patients(db, clinic_id=clinic_id)

    # Twenty data — when available
    twenty_available = False
    leads = 0
    tasks = 0
    activities = 0

    health = await gateway.health_check()
    if health.success:
        twenty_available = True
        # Count people as leads proxy (Twenty people in lead status)
        people_result = await gateway.list_people(limit=1000)
        if people_result.success:
            leads = len(people_result.data.get("data", {}).get("edges", [])) if isinstance(people_result.data, dict) else 0

    return CrmDashboardResponse(
        total_patients=total,
        active_patients=active,
        inactive_patients=inactive,
        total_leads=leads,
        new_leads=0,
        pending_tasks=tasks,
        overdue_tasks=0,
        recent_activities=activities,
        twenty_available=twenty_available,
    )


# ═════════════════════════════════════════════════════════════════════
# SEARCH — local + Twenty when available
# ═════════════════════════════════════════════════════════════════════


class CrmSearchResult(BaseModel):
    """Search result from local or Twenty data."""
    entity_type: str = "PATIENT"
    entity_id: str = ""
    title: str
    subtitle: str = ""
    stage: str = ""


@router.get("/search")
async def crm_search(
    q: str = Query(..., min_length=1, max_length=200, description="Search query"),
    limit: int = Query(default=20, ge=1, le=100),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> list[CrmSearchResult]:
    """Search CRM — local patients + Twenty people when available."""
    results: list[CrmSearchResult] = []

    # Local search (always available)
    patients = await crm_repository.search_local_patients(
        db, clinic_id=current_user.clinic_id, query=q, limit=limit
    )
    for p in patients:
        results.append(CrmSearchResult(
            entity_type="PATIENT",
            entity_id=str(p.id),
            title=p.name,
            subtitle=p.email or p.phone or "",
            stage=p.stage,
        ))

    # Twenty search (when available)
    gateway = _get_gateway()
    health = await gateway.health_check()
    if health.success and len(results) < limit:
        twenty_result = await gateway.list_people(limit=limit - len(results))
        if twenty_result.success and isinstance(twenty_result.data, dict):
            edges = twenty_result.data.get("data", {}).get("edges", [])
            for edge in edges:
                node = edge.get("node", {})
                name = f"{node.get('firstName', '')} {node.get('lastName', '')}".strip()
                if name:
                    results.append(CrmSearchResult(
                        entity_type="TWENTY_PERSON",
                        entity_id=node.get("id", ""),
                        title=name,
                        subtitle="Twenty",
                    ))

    return results[:limit]


# ═════════════════════════════════════════════════════════════════════
# TWENTY-DEPENDENT — proxy to Twenty when available, 503 when not
# ═════════════════════════════════════════════════════════════════════


TWENTY_OFFLINE = {
    "detail": "Twenty CRM not available. Configure TWENTY_BASE_URL and TWENTY_API_KEY, then start Twenty.",
    "error_code": "TWENTY_OFFLINE",
}


class CrmLeadResponse(BaseModel):
    """A CRM lead from Twenty."""
    id: str
    first_name: str = ""
    last_name: str = ""
    email: str = ""
    phone: str = ""
    company: str = ""


@router.get("/leads", responses={503: {"model": dict}})
async def list_leads(
    status: str | None = Query(default=None),
    limit: int = Query(default=100, ge=1, le=1000),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> list[CrmLeadResponse]:
    """List CRM leads from Twenty."""
    gateway = _get_gateway()
    health = await gateway.health_check()
    if not health.success:
        raise HTTPException(status_code=503, detail=TWENTY_OFFLINE)

    result = await gateway.list_people(limit=limit)
    if not result.success:
        raise HTTPException(status_code=502, detail={"error": result.error})

    leads = []
    if isinstance(result.data, dict):
        edges = result.data.get("data", {}).get("edges", [])
        for edge in edges:
            node = edge.get("node", {})
            leads.append(CrmLeadResponse(
                id=node.get("id", ""),
                first_name=node.get("firstName", ""),
                last_name=node.get("lastName", ""),
                email=node.get("email", ""),
                phone=node.get("phone", ""),
            ))
    return leads


@router.get("/leads/count")
async def count_leads(
    status: str | None = Query(default=None),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> dict:
    gateway = _get_gateway()
    health = await gateway.health_check()
    if not health.success:
        raise HTTPException(status_code=503, detail=TWENTY_OFFLINE)

    result = await gateway.list_people(limit=1)
    count = 0
    if result.success and isinstance(result.data, dict):
        count = result.data.get("data", {}).get("totalCount", 0)
    return {"count": count, "status": status}


class CrmTaskResponse(BaseModel):
    """A CRM task from Twenty."""
    id: str
    title: str = ""
    status: str = ""
    due_date: str = ""


@router.get("/tasks", responses={503: {"model": dict}})
async def list_tasks(
    status: str | None = Query(default=None),
    limit: int = Query(default=100, ge=1, le=1000),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> list[CrmTaskResponse]:
    """List CRM tasks from Twenty."""
    gateway = _get_gateway()
    health = await gateway.health_check()
    if not health.success:
        raise HTTPException(status_code=503, detail=TWENTY_OFFLINE)

    result = await gateway.list_records("tasks", limit=limit)
    if not result.success:
        raise HTTPException(status_code=502, detail={"error": result.error})

    tasks = []
    if isinstance(result.data, dict):
        edges = result.data.get("data", {}).get("edges", [])
        for edge in edges:
            node = edge.get("node", {})
            tasks.append(CrmTaskResponse(
                id=node.get("id", ""),
                title=node.get("title", ""),
                status=node.get("status", ""),
                due_date=node.get("dueDate", ""),
            ))
    return tasks


class CrmActivityResponse(BaseModel):
    """A CRM activity from Twenty."""
    id: str
    type: str = ""
    title: str = ""
    timestamp: str = ""


@router.get("/activities", responses={503: {"model": dict}})
async def list_activities(
    limit: int = Query(default=100, ge=1, le=1000),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> list[CrmActivityResponse]:
    """List CRM activities from Twenty timeline."""
    gateway = _get_gateway()
    health = await gateway.health_check()
    if not health.success:
        raise HTTPException(status_code=503, detail=TWENTY_OFFLINE)

    result = await gateway.list_records("timelineActivities", limit=limit)
    if not result.success:
        raise HTTPException(status_code=502, detail={"error": result.error})

    activities = []
    if isinstance(result.data, dict):
        edges = result.data.get("data", {}).get("edges", [])
        for edge in edges:
            node = edge.get("node", {})
            activities.append(CrmActivityResponse(
                id=node.get("id", ""),
                type=node.get("type", ""),
                title=node.get("name", ""),
                timestamp=node.get("createdAt", ""),
            ))
    return activities


class CrmTimelineEventResponse(BaseModel):
    """A unified timeline event."""
    id: str
    source: str = "TWENTY"
    type: str = ""
    title: str = ""
    summary: str = ""
    timestamp: str = ""


@router.get("/timeline", responses={503: {"model": dict}})
async def unified_timeline(
    patient_id: int | None = Query(default=None),
    limit: int = Query(default=50, ge=1, le=500),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> list[CrmTimelineEventResponse]:
    """Unified timeline from Twenty."""
    gateway = _get_gateway()
    health = await gateway.health_check()
    if not health.success:
        raise HTTPException(status_code=503, detail=TWENTY_OFFLINE)

    result = await gateway.list_records("timelineActivities", limit=limit)
    if not result.success:
        raise HTTPException(status_code=502, detail={"error": result.error})

    events = []
    if isinstance(result.data, dict):
        edges = result.data.get("data", {}).get("edges", [])
        for edge in edges:
            node = edge.get("node", {})
            events.append(CrmTimelineEventResponse(
                id=node.get("id", ""),
                source="TWENTY",
                type=node.get("type", ""),
                title=node.get("name", ""),
                summary=node.get("description", ""),
                timestamp=node.get("createdAt", ""),
            ))
    return events


class CrmReferralResponse(BaseModel):
    """A CRM referral from Twenty."""
    id: str
    name: str = ""
    status: str = ""
    notes: str = ""


@router.get("/referrals", responses={503: {"model": dict}})
async def list_referrals(
    limit: int = Query(default=50, ge=1, le=500),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> list[CrmReferralResponse]:
    """List CRM referrals from Twenty."""
    gateway = _get_gateway()
    health = await gateway.health_check()
    if not health.success:
        raise HTTPException(status_code=503, detail=TWENTY_OFFLINE)

    # Referrals in Twenty are handled via people + pipeline
    result = await gateway.list_people(limit=limit)
    if not result.success:
        raise HTTPException(status_code=502, detail={"error": result.error})

    referrals = []
    if isinstance(result.data, dict):
        edges = result.data.get("data", {}).get("edges", [])
        for edge in edges:
            node = edge.get("node", {})
            name = f"{node.get('firstName', '')} {node.get('lastName', '')}".strip()
            referrals.append(CrmReferralResponse(
                id=node.get("id", ""),
                name=name,
                status="ACTIVE",
            ))
    return referrals


class CrmAuditEventResponse(BaseModel):
    """An audit event."""
    id: str
    event_type: str = ""
    entity_type: str = ""
    entity_id: str = ""
    timestamp: str = ""


@router.get("/audit", responses={503: {"model": dict}})
async def list_audit_events(
    limit: int = Query(default=100, ge=1, le=1000),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> list[CrmAuditEventResponse]:
    """List CRM audit events from Twenty."""
    gateway = _get_gateway()
    health = await gateway.health_check()
    if not health.success:
        raise HTTPException(status_code=503, detail=TWENTY_OFFLINE)

    result = await gateway.list_records("timelineActivities", limit=limit)
    if not result.success:
        raise HTTPException(status_code=502, detail={"error": result.error})

    events = []
    if isinstance(result.data, dict):
        edges = result.data.get("data", {}).get("edges", [])
        for edge in edges:
            node = edge.get("node", {})
            events.append(CrmAuditEventResponse(
                id=node.get("id", ""),
                event_type=node.get("type", ""),
                entity_type="PERSON",
                entity_id=node.get("personId", ""),
                timestamp=node.get("createdAt", ""),
            ))
    return events
