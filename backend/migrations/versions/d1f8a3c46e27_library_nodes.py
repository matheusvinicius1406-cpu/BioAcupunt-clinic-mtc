"""library_nodes: server counterpart of the app's biblioteca_nodes

The curated acervo (16 fixed articles + curadoria-approved items) lived only in
the app, loaded from reviewed source packs. This adds the server table so the web
painel can show the library read-only. Global (not clinic-scoped) and not synced,
mirroring the app entity which has neither tenant nor sync identity.

Populated by the ingestion pipeline, never by hand (R4) — empty until then.

Revision ID: d1f8a3c46e27
Revises: c7d3e2a15b90
"""
from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op

revision: str = "d1f8a3c46e27"
down_revision: Union[str, None] = "c7d3e2a15b90"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.create_table(
        "library_nodes",
        sa.Column("id", sa.String(length=128), nullable=False),
        sa.Column("type", sa.String(length=40), nullable=False, server_default=""),
        sa.Column("title", sa.String(length=500), nullable=False),
        sa.Column("content", sa.Text(), nullable=False, server_default=""),
        sa.Column("summary", sa.Text(), nullable=False, server_default=""),
        sa.Column("tags", sa.String(length=1000), nullable=False, server_default=""),
        sa.Column("version", sa.Integer(), nullable=False, server_default="1"),
        sa.Column("metadata_json", sa.Text(), nullable=False, server_default="{}"),
        sa.Column(
            "created_at",
            sa.DateTime(timezone=True),
            server_default=sa.text("(CURRENT_TIMESTAMP)"),
            nullable=False,
        ),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index("ix_library_nodes_title", "library_nodes", ["title"])


def downgrade() -> None:
    op.drop_index("ix_library_nodes_title", table_name="library_nodes")
    op.drop_table("library_nodes")
