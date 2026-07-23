from datetime import datetime

from pydantic import BaseModel


class CrmPatientResponse(BaseModel):
    id: int
    clinic_id: int
    name: str
    phone: str
    email: str
    birth_date: str
    stage: str
    total_sessions: int
    total_revenue_brl: float
    last_visit: str
    next_appointment: str
    tags: str
    notes: str
    referral_source: str
    nps_score: int | None
    health_insurance: str
    main_complaint: str
    created_at: datetime
    updated_at: datetime

    model_config = {"from_attributes": True}


class CrmPipelineSummary(BaseModel):
    """Pipeline health for the web CRM dashboard.

    Tombstones excluded; only stages actually present appear in `by_stage`.
    """

    total: int
    #: Contagem por estágio ("FIRST_CONTACT", "LEAD", ...). Só estágios presentes.
    by_stage: dict[str, int]
    total_revenue_brl: float
    #: Média de NPS só entre quem foi pontuado (null = ninguém pontuado ainda).
    average_nps: float | None
