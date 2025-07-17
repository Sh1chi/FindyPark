import asyncio
import logging
from sqlalchemy import text

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from firebase_admin import credentials, initialize_app

from app.core.config import get_settings
from app.db import test_connection, async_session
from app.routes import parkings_routes, bookings_routes, assistant_routes, reviews_routes, users_routes, tariffs_routes
from app.services.parking_service import refresh_data       # периодический импорт open-data
from app.services.user_service import refresh_user_data     # синхронизация профилей
from app.services.occupancy_mocker import main as run_occupancy_mocker

# Настраиваем базовый логгер
logging.basicConfig(level=logging.INFO)

settings = get_settings()

# Инициализируем FastAPI-приложение
app = FastAPI(
    title="Свободные парковки Москвы",
    version="0.2.0",
)

# Подключаем все роутеры
app.include_router(parkings_routes.router)
app.include_router(tariffs_routes.router)
app.include_router(users_routes.router)
app.include_router(bookings_routes.router)
app.include_router(reviews_routes.router)
app.include_router(assistant_routes.router)

# Разрешаем все CORS-запросы (можно ограничить в будущем)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.on_event("startup")
async def on_startup():
    """
    Обработчик события запуска приложения.

    Создает тестовой соединение с сервером.
    Синхронизирует данные парковок и пользователей.
    """
    await test_connection()  # проверяем и логируем

    # Инициализируем Firebase Admin SDK
    cred = credentials.Certificate(settings.firebase_credentials_path)
    initialize_app(cred)

    # Запускаем фоновые задачи
    asyncio.create_task(refresh_data())  # обновление данных парковок
    asyncio.create_task(refresh_user_data())  # синхронизация пользователей

    # Мок-данные
    asyncio.create_task(run_occupancy_mocker())
