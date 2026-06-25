from qdrant_client import QdrantClient
from app.core.config import settings

# Cliente Qdrant
client = QdrantClient(url=settings.QDRANT_HOST)

def index_knowledge_node(node_id: str, text: str, payload: dict):
    # Indexação vetorial no Qdrant
    pass

def semantic_search(query: str, limit: int = 5):
    # Busca semântica no Qdrant
    return []
