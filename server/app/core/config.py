# app/core/config.py
from functools import lru_cache
from pydantic_settings import BaseSettings, SettingsConfigDict

class Settings(BaseSettings):
    """
    Конфигурация приложения, загружаемая из переменных окружения.

    Attributes:
        data_mos_token (str): API-ключ для доступа к data.mos.ru.
    """
    data_mos_token: str  | None = None     # имя переменной из .env

    postgres_user: str
    postgres_password: str
    postgres_host: str = "localhost"
    postgres_port: int = 5432
    postgres_db: str

    firebase_credentials_path: str

    model_config = SettingsConfigDict(
        env_file=".env",
        extra="ignore"
    )

    sber_auth: str  # Ключ для GigaChat API

    model_config = SettingsConfigDict(
        env_file=".env",
        extra="ignore"
    )

    @property
    def database_url(self) -> str:
        return (
                "postgresql+asyncpg://"
                f"{self.postgres_user}:{self.postgres_password}"
                f"@{self.postgres_host}:{self.postgres_port}/{self.postgres_db}"

        )
@lru_cache
def get_settings() -> Settings:
    return Settings()