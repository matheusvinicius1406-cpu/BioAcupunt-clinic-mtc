"""add missing indexes for the read paths already used by every repository

Audit finding (2026-07-29): every read query in appointment/transaction/
crm/patient repositories filters `clinic_id` + `deleted_at IS NULL` together,
but only `clinic_id` had an index — a single-column index on a low-selectivity
column (`deleted_at`, mostly NULL) would not have helped much on its own
either, so this adds the composite instead. `updated_at` (SyncableMixin) backs
every "most recent first" listing plus sync's delta pull, and `status` backs
the GROUP BY in reports_repository.count_appointments_by_status — neither had
an index before this.

Revision ID: 6f699eb30426
Revises: d1f8a3c46e27
"""
from typing import Sequence, Union

from alembic import op

revision: str = "6f699eb30426"
down_revision: Union[str, None] = "d1f8a3c46e27"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.create_index("ix_appointments_clinic_deleted", "appointments", ["clinic_id", "deleted_at"])
    op.create_index("ix_appointments_status", "appointments", ["status"])
    op.create_index("ix_appointments_updated_at", "appointments", ["updated_at"])

    op.create_index("ix_transactions_clinic_deleted", "transactions", ["clinic_id", "deleted_at"])
    op.create_index("ix_transactions_updated_at", "transactions", ["updated_at"])

    op.create_index("ix_crm_patients_clinic_deleted", "crm_patients", ["clinic_id", "deleted_at"])
    op.create_index("ix_crm_patients_updated_at", "crm_patients", ["updated_at"])

    op.create_index("ix_patients_clinic_deleted", "patients", ["clinic_id", "deleted_at"])
    op.create_index("ix_patients_updated_at", "patients", ["updated_at"])


def downgrade() -> None:
    op.drop_index("ix_patients_updated_at", table_name="patients")
    op.drop_index("ix_patients_clinic_deleted", table_name="patients")

    op.drop_index("ix_crm_patients_updated_at", table_name="crm_patients")
    op.drop_index("ix_crm_patients_clinic_deleted", table_name="crm_patients")

    op.drop_index("ix_transactions_updated_at", table_name="transactions")
    op.drop_index("ix_transactions_clinic_deleted", table_name="transactions")

    op.drop_index("ix_appointments_updated_at", table_name="appointments")
    op.drop_index("ix_appointments_status", table_name="appointments")
    op.drop_index("ix_appointments_clinic_deleted", table_name="appointments")
