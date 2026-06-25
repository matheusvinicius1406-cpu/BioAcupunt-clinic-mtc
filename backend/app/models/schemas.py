from pydantic import BaseModel
from typing import Optional, List, Dict, Any
from datetime import datetime

class User(BaseModel):
    id: str
    email: str

class KnowledgeNode(BaseModel):
    id: str
    type: str
    title: str
    content: str
    summary: str
    tags: List[str]
    version: int
    metadata: Dict[str, Any]

class SyncRequest(BaseModel):
    last_sync: datetime
    local_changes: List[Dict[str, Any]]
    device_id: str
