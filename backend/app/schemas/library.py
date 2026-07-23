from datetime import datetime

from pydantic import BaseModel


class LibraryNodeResponse(BaseModel):
    id: str
    type: str
    title: str
    content: str
    summary: str
    tags: str
    version: int
    metadata_json: str
    created_at: datetime

    model_config = {"from_attributes": True}
