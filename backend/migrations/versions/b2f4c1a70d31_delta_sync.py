"""delta sync: revisions, client ids, transactions

Adds the machinery for two-way sync between the app and the server:

  * `clinic_revisions` — the per-clinic revision counter that makes conflict
    detection independent of device clocks.
  * `rev` / `client_id` on every syncable table, plus `updated_at` / `deleted_at`
    where they were missing.
  * `transactions` — the financial table, which had no server counterpart.
  * `appointments.professional_id` becomes nullable, and gains value/paid.

Existing rows are backfilled with `rev = 0`, which puts them *below* every
revision the counter will issue. A client pulling from `since=0` therefore
receives them on its first sync rather than never seeing them at all.

Revision ID: b2f4c1a70d31
Revises: 66a9bfd6d790
"""
from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op

revision: str = "b2f4c1a70d31"
down_revision: Union[str, None] = "66a9bfd6d790"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None

_SYNCABLE_TABLES = ("patients", "appointments", "transactions")


def upgrade() -> None:
    op.create_table(
        "clinic_revisions",
        sa.Column("clinic_id", sa.Integer(), nullable=False),
        sa.Column("last_rev", sa.BigInteger(), nullable=False, server_default="0"),
        sa.Column(
            "updated_at",
            sa.DateTime(timezone=True),
            server_default=sa.text("(CURRENT_TIMESTAMP)"),
            nullable=False,
        ),
        sa.ForeignKeyConstraint(["clinic_id"], ["clinics.id"]),
        sa.PrimaryKeyConstraint("clinic_id"),
    )

    op.create_table(
        "transactions",
        sa.Column("id", sa.Integer(), nullable=False),
        sa.Column("clinic_id", sa.Integer(), nullable=False),
        sa.Column("patient_id", sa.Integer(), nullable=True),
        sa.Column("appointment_id", sa.Integer(), nullable=True),
        sa.Column("amount_brl", sa.Numeric(10, 2), nullable=False, server_default="0"),
        sa.Column("occurred_on", sa.Date(), nullable=False),
        sa.Column("type", sa.String(length=20), nullable=False, server_default="PAGAMENTO"),
        sa.Column("method", sa.String(length=20), nullable=False, server_default="PIX"),
        sa.Column("category", sa.String(length=40), nullable=False, server_default="SESSÃO"),
        sa.Column("status", sa.String(length=20), nullable=False, server_default="PAGO"),
        sa.Column("notes", sa.Text(), nullable=False, server_default=""),
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
        sa.ForeignKeyConstraint(["patient_id"], ["patients.id"]),
        sa.ForeignKeyConstraint(["appointment_id"], ["appointments.id"]),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index("ix_transactions_clinic_id", "transactions", ["clinic_id"])
    op.create_index("ix_transactions_patient_id", "transactions", ["patient_id"])
    op.create_index("ix_transactions_appointment_id", "transactions", ["appointment_id"])
    op.create_index("ix_transactions_occurred_on", "transactions", ["occurred_on"])
    op.create_index("ix_transactions_rev", "transactions", ["rev"])
    op.create_index("ix_transactions_client_id", "transactions", ["client_id"])

    # ── patients: sync columns + the CRM fields the app already holds ──
    op.add_column("patients", sa.Column("rev", sa.BigInteger(), nullable=False, server_default="0"))
    op.add_column("patients", sa.Column("client_id", sa.String(length=64), nullable=True))
    op.add_column("patients", sa.Column("phone", sa.String(length=40), nullable=False, server_default=""))
    op.add_column("patients", sa.Column("email", sa.String(length=200), nullable=False, server_default=""))
    op.add_column("patients", sa.Column("stage", sa.String(length=30), nullable=False, server_default="FIRST_CONTACT"))
    op.add_column("patients", sa.Column("notes", sa.String(length=2000), nullable=False, server_default=""))
    op.create_index("ix_patients_rev", "patients", ["rev"])
    op.create_index("ix_patients_client_id", "patients", ["client_id"])

    # ── appointments ──
    op.add_column("appointments", sa.Column("rev", sa.BigInteger(), nullable=False, server_default="0"))
    op.add_column("appointments", sa.Column("client_id", sa.String(length=64), nullable=True))
    op.add_column(
        "appointments",
        sa.Column(
            "updated_at",
            sa.DateTime(timezone=True),
            server_default=sa.text("(CURRENT_TIMESTAMP)"),
            nullable=False,
        ),
    )
    op.add_column("appointments", sa.Column("deleted_at", sa.DateTime(timezone=True), nullable=True))
    op.add_column("appointments", sa.Column("value_brl", sa.Numeric(10, 2), nullable=False, server_default="0"))
    op.add_column("appointments", sa.Column("paid", sa.Boolean(), nullable=False, server_default=sa.false()))
    op.create_index("ix_appointments_rev", "appointments", ["rev"])
    op.create_index("ix_appointments_client_id", "appointments", ["client_id"])

    # A booking can reach the server before it is known who will take it;
    # rejecting it over that would lose a real appointment.
    with op.batch_alter_table("appointments") as batch:
        batch.alter_column("professional_id", existing_type=sa.Integer(), nullable=True)


def downgrade() -> None:
    with op.batch_alter_table("appointments") as batch:
        batch.alter_column("professional_id", existing_type=sa.Integer(), nullable=False)

    op.drop_index("ix_appointments_client_id", table_name="appointments")
    op.drop_index("ix_appointments_rev", table_name="appointments")
    for column in ("paid", "value_brl", "deleted_at", "updated_at", "client_id", "rev"):
        op.drop_column("appointments", column)

    op.drop_index("ix_patients_client_id", table_name="patients")
    op.drop_index("ix_patients_rev", table_name="patients")
    for column in ("notes", "stage", "email", "phone", "client_id", "rev"):
        op.drop_column("patients", column)

    op.drop_table("transactions")
    op.drop_table("clinic_revisions")
