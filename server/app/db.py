import logging

from sqlalchemy.ext.asyncio import create_async_engine, AsyncSession
from sqlalchemy.orm import sessionmaker
from sqlalchemy import text

from app.core.config import get_settings

# Логгер для событий, связанных с базой данных
log = logging.getLogger("db")

# Загрузка настроек приложения
settings = get_settings()

# Инициализация асинхронного SQLAlchemy-движка для PostgreSQL
engine = create_async_engine(
    settings.database_url,
    echo=True,          # Отключаем подробный SQL-лог
    pool_size=20,
    max_overflow=10,
    pool_pre_ping=True   # Автоматическая проверка соединения
)

# Создаём фабрику асинхронных сессий
async_session = sessionmaker(
    engine, class_=AsyncSession, expire_on_commit=False
)


async def test_connection() -> None:
    """
    Проверка соединения с базой данных.Что

    Открываем и закрываем соединение для теста доступности.
    Логируем успешное подключение или ошибку.
    """
    try:
        async with engine.connect() as conn:
            await conn.execute(text("SELECT 1"))  # Минимальный тестовый запрос
        log.info("  Connected to PostgreSQL at %s", settings.database_url)
    except Exception as exc:  # noqa: BLE001
        log.warning("   Failed DB connection: %s", exc)
        raise
