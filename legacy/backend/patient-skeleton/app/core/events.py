from datetime import datetime
from uuid import uuid4
from app.models.patient import PatientModel
from app.schemas.patient import PatientCreate, PatientUpdate


class EventBus:
    """Event bus mínimo para registro e entrega."""

    def __init__(self) -> None:
        self._handlers: list[callable] = []

    def subscribe(self, handler: callable):
        self._handlers.append(handler)

    def publish(self, event: dict):
        for handler in self._handlers:
            try:
                handler(event)
            except Exception as e:  # pragma: no cover
                print(f"[EVENT_BUS_ERROR] {e}")


event_bus = EventBus()


def _emit(event_name: str, payload: dict):
    event_bus.publish(
        {
            "eventId": str(uuid4()),
            "eventType": event_name,
            "eventVersion": "1.0.0",
            "occurredAt": datetime.utcnow().isoformat() + "Z",
            "payload": payload,
        }
    )


def handle_patient_event(event: dict):
    print(f"[PATIENT_EVENT] {event['eventType']}: {event['payload']}")


event_bus.subscribe(handle_patient_event)

