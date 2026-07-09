"""
MKIS — API pública v1 e utilitários de resposta/paginação.
"""

from __future__ import annotations

from collections.abc import Sequence
from typing import Any

from fastapi import Depends, HTTPException, status
from pydantic import BaseModel, ConfigDict


class ApiResponse(BaseModel):
    success: bool = True
    data: Any | None = None
    meta: dict[str, Any] | None = None
    errors: list[str] | None = None
    model_config = ConfigDict(extra="allow")


class PageInfo(BaseModel):
    next_cursor: str | None = None
    prev_cursor: str | None = None
    has_next: bool = False
    limit: int = 20
    total_estimate: int | None = None


class PaginatedResponse(BaseModel):
    items: Sequence[Any]
    page_info: PageInfo
