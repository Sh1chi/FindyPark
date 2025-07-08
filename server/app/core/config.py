from functools import lru_cache
from pydantic_settings import BaseSettings, SettingsConfigDict

class Settings(BaseSettings):
    """
    Конфигурация приложения, загружаемая из переменных окружения.

    Атрибуты:
        data_mos_token (str | None): API-ключ data.mos.ru (может быть не задан).
        postgres_user (str): Логин PostgreSQL.
        postgres_password (str): Пароль PostgreSQL.
        postgres_host (str): Хост PostgreSQL (по умолчанию localhost).
        postgres_port (int): Порт PostgreSQL (по умолчанию 5432).
        postgres_db (str): Название базы данных.
        firebase_credentials_path (str): Путь к JSON-файлу Firebase Admin SDK.
    """
    data_mos_token: str  | None = None     # имя переменной из .env

    postgres_user: str
    postgres_password: str
    postgres_host: str = "localhost"
    postgres_port: int = 5432
    postgres_db: str

    firebase_credentials_path: str

    model_config = SettingsConfigDict(
        env_file=".env",    # Используем файл .env в корне проекта
        extra="ignore"      # Игнорируем переменные, не описанные в модели
    )

    @property
    def database_url(self) -> str:  # ← строка подключения
        """
        Формирует строку подключения к PostgreSQL (asyncpg-формат).
        """
        return (
            "postgresql+asyncpg://"
            f"{self.postgres_user}:{self.postgres_password}"
            f"@{self.postgres_host}:{self.postgres_port}/{self.postgres_db}"
        )


@lru_cache
def get_settings() -> Settings:
    """
    Возвращает экземпляр настроек приложения с кешированием.

    Используем lru_cache, чтобы конфигурация создавалась один раз
    и переиспользовалась без повторного чтения.
    """
    return Settings()
