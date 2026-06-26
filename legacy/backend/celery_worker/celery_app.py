from celery import Celery
from app.core.config import settings

# Celery usa Redis como broker
celery_app = Celery("tasks", broker=settings.REDIS_URL)
celery_app.conf.update(task_serializer='json', accept_content=['json'])
