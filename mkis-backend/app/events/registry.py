"""
MKIS — Eventos canônicos e contratos.
"""

from __future__ import annotations


CANONICAL_EVENTS = [
    "knowledge.node.created.v1",
    "knowledge.node.versioned.v1",
    "knowledge.node.approved.v1",
    "knowledge.node.deprecated.v1",
    "knowledge.node.rejected.v1",
    "knowledge.artifact.uploaded.v1",
    "knowledge.artifact.quarantined.v1",
    "ingestion.job.status_changed.v1",
    "ingestion.job.failed.v1",
    "ingestion.job.completed.v1",
    "ingestion.job.cancelled.v1",
    "embedding.generated.v1",
    "embedding.migration.completed.v1",
    "graph.edge.created.v1",
    "evidence.score.updated.v1",
    "hard_delete.completed.v1",
]

EVENT_SUBJECTS = [
    "knowledge_node",
    "pathology",
    "syndrome",
    "treatment",
    "acupuncture_point",
    "medication",
    "clinical_evidence",
]
