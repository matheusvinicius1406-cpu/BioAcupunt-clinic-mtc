"""crm_patients: server counterpart of the app's CRM table

The app carries a `crm_patients` entity (pipeline stage, referral source, NPS,
tags) with full sync identity (`client_id`), but there was no server table — so
the relationship record never left the device. This adds it, syncable like the
others (`rev` / `client_id` / `updated_at` / `deleted_at`).

Distinct from the clinical `patients` table on purpose: CRM is the marketing /
relationship view, not the clinical chart.

Revision ID: c7d3e2a15b90
Revises: b2f4c1a70d31
"""
from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op

revision: str = "c7d3e2a15b90"
down_revision: Union[str, None] = "b2f4c1a70d31"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.create_table(
        "crm_patients",
        sa.Column("id", sa.Integer(), nullable=False),
        sa.Column("clinic_id", sa.Integer(), nullable=False),
        sa.Column("name", sa.String(length=200), nullable=False),
        sa.Column("phone", sa.String(length=40), nullable=False, server_default=""),
        sa.Column("email", sa.String(length=200), nullable=False, server_default=""),
        sa.Column("birth_date", sa.String(length=20), nullable=False, server_default=""),
        sa.Column("stage", sa.String(length=30), nullable=False, server_default="FIRST_CONTACT"),
        sa.Column("total_sessions", sa.Integer(), nullable=False, server_default="0"),
        sa.Column("total_revenue_brl", sa.Numeric(10, 2), nullable=False, server_default="0"),
        sa.Column("last_visit", sa.String(length=20), nullable=False, server_default=""),
        sa.Column("next_appointment", sa.String(length=40), nullable=False, server_default=""),
        sa.Column("tags", sa.String(length=500), nullable=False, server_default=""),
        sa.Column("notes", sa.Text(), nullable=False, server_default=""),
        sa.Column("referral_source", sa.String(length=120), nullable=False, server_default=""),
        sa.Column("nps_score", sa.Integer(), nullable=True),
        sa.Column("health_insurance", sa.String(length=120), nullable=False, server_default=""),
        sa.Column("main_complaint", sa.Text(), nullable=False, server_default=""),
        sa.Column("rev", sa.BigInteger(), nullable=False, server_default="0"),
        sa.Column("client_id", sa.String(length=64), nullable=True),
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
        sa.Column("deleted_at", sa.DateTime(timezone=True), nullable=True),
        sa.ForeignKeyConstraint(["clinic_id"], ["clinics.id"]),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index("ix_crm_patients_clinic_id", "crm_patients", ["clinic_id"])
    op.create_index("ix_crm_patients_name", "crm_patients", ["name"])
    op.create_index("ix_crm_patients_stage", "crm_patients", ["stage"])
    op.create_index("ix_crm_patients_rev", "crm_patients", ["rev"])
    op.create_index("ix_crm_patients_client_id", "crm_patients", ["client_id"])


def downgrade() -> None:
    op.drop_index("ix_crm_patients_client_id", table_name="crm_patients")
    op.drop_index("ix_crm_patients_rev", table_name="crm_patients")
    op.drop_index("ix_crm_patients_stage", table_name="crm_patients")
    op.drop_index("ix_crm_patients_name", table_name="crm_patients")
    op.drop_index("ix_crm_patients_clinic_id", table_name="crm_patients")
    op.drop_table("crm_patients")
