from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    SUPABASE_URL: str
    SUPABASE_KEY: str
    SUPABASE_SERVICE_ROLE_KEY: str
    REDIS_URL: str
    QDRANT_HOST: str
    SECRET_KEY: str

    class Config:
        env_file = ".env"

settings = Settings()
