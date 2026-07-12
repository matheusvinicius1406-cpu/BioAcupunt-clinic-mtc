import logging

from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.config import get_settings
from app.models.clinic import Clinic
from app.models.user import User, UserRole
from app.services import auth_service

logger = logging.getLogger("bioacupunt.bootstrap")


async def bootstrap_admin_if_empty(db: AsyncSession) -> None:
    """On a fresh deploy the database has no users, and creating a clinic
    requires an admin (RBAC) — a chicken-and-egg lock. If no users exist yet
    and BOOTSTRAP_ADMIN_* are configured, create the first clinic + admin so
    there is a way to log in. No-op once any user exists, so it is safe to run
    on every startup.
    """
    settings = get_settings()
    if not settings.bootstrap_admin_email or not settings.bootstrap_admin_password:
        return

    existing = await db.scalar(select(func.count()).select_from(User))
    if existing:
        return

    clinic = Clinic(name=settings.bootstrap_clinic_name)
    db.add(clinic)
    await db.commit()
    await db.refresh(clinic)

    await auth_service.register_user(
        db,
        clinic_id=clinic.id,
        email=settings.bootstrap_admin_email,
        password=settings.bootstrap_admin_password,
        full_name="Administrador",
        role=UserRole.ADMIN,
    )
    logger.info("Bootstrapped initial admin %s for clinic '%s'", settings.bootstrap_admin_email, clinic.name)
