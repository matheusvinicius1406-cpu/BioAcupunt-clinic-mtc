"""Single import point for every SQLAlchemy model.

Import this module (not individual model files) anywhere Base.metadata
needs to know about every table — Alembic's env.py and app startup.
Importing a model file directly and forgetting a sibling model is how a
foreign key silently fails to resolve at runtime (NoReferencedTableError)
even though everything type-checks.
"""

from app.models.appointment import Appointment
from app.models.audit import DataAccessLog
from app.models.auth import RefreshSession
from app.models.clinic import Clinic
from app.models.patient import Patient
from app.models.user import User

__all__ = ["Appointment", "DataAccessLog", "RefreshSession", "Clinic", "Patient", "User"]
