from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    PROJECT_NAME: str = "Incidex AI Engine"
    
    # pydantic-settings 会自动从 .env 文件中读取 GEMINI_API_KEY 和 DATABASE_URL
    GEMINI_API_KEY: str = ""
    DATABASE_URL: str = "postgresql://incidex_user:incidex_password@localhost:5432/incidex_db"

    class Config:
        env_file = ".env"
        extra = "ignore"

settings = Settings()