from fastapi import FastAPI
from app.routers import patient as patient_router

app = FastAPI(title="BioAcupunt Supremo API", version="0.1.0")
app.include_router(patient_router.router, prefix="/api/v1/patients", tags=["patients"])


@app.get("/healthz")
def healthz():
    return {"status": "ok", "version": app.version}

