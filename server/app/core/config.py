# app/core/config.py
from functools import lru_cache
from pydantic_settings import BaseSettings, SettingsConfigDict

class Settings(BaseSettings):
    """
    Конфигурация приложения, загружаемая из переменных окружения.

    Attributes:
        data_mos_token (str): API-ключ для доступа к data.mos.ru.
    """
    data_mos_token: str   # ← имя переменной из .env

    model_config = SettingsConfigDict(
        env_file=".env",    # Используем файл .env в корне проекта
        extra="ignore"      # Игнорируем переменные, не описанные в модели
    )

@lru_cache
def get_settings() -> Settings:
    """
    Возвращает экземпляр настроек приложения с кешированием.

    Используем lru_cache, чтобы конфигурация создавалась один раз
    и переиспользовалась без повторного чтения.
    """
    return Settings()
