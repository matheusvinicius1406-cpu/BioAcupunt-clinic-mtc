"""crm_identity_map: bridge between BioAcupunt Patient and Twenty Person

This table tracks the mapping between the clinical Patient identity
(BioAcupunt) and the CRM Person identity (Twenty). It is the ONLY
place where cross-system identity is tracked.

Rules enforced at the database level:
- UNIQUE(clinic_id, bioacupunt_patient_id) — one patient maps to one person
- UNIQUE(clinic_id, twenty_person_id) — one person maps to one patient
- clinic_id on every row — no cross-tenant mapping

Revision ID: a1b2c3d4e5f6
Revises: d1f8a3c46e27
"""
from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op

revision: str = "a1b2c3d4e5f6"
down_revision: Union[str, None] = "d1f8a3c46e27"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.create_table(
        "crm_identity_map",
        sa.Column("id", sa.Integer(), nullable=False),
        sa.Column("clinic_id", sa.Integer(), nullable=False),
        sa.Column("bioacupunt_patient_id", sa.Integer(), nullable=False),
        sa.Column("twenty_person_id", sa.String(length=100), nullable=False),
        sa.Column(
            "source", sa.String(length=20), nullable=False, server_default="MANUAL"
        ),
        sa.Column(
            "status", sa.String(length=20), nullable=False, server_default="ACTIVE"
        ),
        sa.Column(
            "created_at",
            sa.DateTime(timezone=True),
            server_default=sa.text("(CURRENT_TIMESTAMP)"),
            nullable=False,
        ),
        sa.Column(
            "updated_at",
            sa.DateTime(timezone=True),
            server_default=sa.text("(CURRENT_TIMESTAMP)"),
            nullable=False,
        ),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint(
            "clinic_id",
            "bioacupunt_patient_id",
            name="uq_identity_map_patient",
        ),
        sa.UniqueConstraint(
            "clinic_id",
            "twenty_person_id",
            name="uq_identity_map_person",
        ),
    )
    op.create_index("ix_identity_map_clinic", "crm_identity_map", ["clinic_id"])
    op.create_index(
        "ix_identity_map_patient", "crm_identity_map", ["bioacupunt_patient_id"]
    )
    op.create_index(
        "ix_identity_map_person", "crm_identity_map", ["twenty_person_id"]
    )


def downgrade() -> None:
    op.drop_index("ix_identity_map_person", table_name="crm_identity_map")
    op.drop_index("ix_identity_map_patient", table_name="crm_identity_map")
    op.drop_index("ix_identity_map_clinic", table_name="crm_identity_map")
    op.drop_table("crm_identity_map")
