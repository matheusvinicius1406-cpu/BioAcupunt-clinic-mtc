from app.celery_worker.celery_app import celery_app

@celery_app.task
def download_and_index_pdf(url: str, node_id: str):
    # Baixa PDF, extrai texto, gera embedding e indexa no Qdrant
    pass

@celery_app.task
def generate_embeddings_for_new_nodes():
    # Job periódico para processar nós recém-criados sem embedding
    pass
