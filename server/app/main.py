import asyncio
import logging
from cgitb import text

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from firebase_admin import credentials, initialize_app

from app.core.config import get_settings
from app.db import test_connection, async_session
from app.routes import parkings_routes, bookings_routes, assistant_routes, reviews_routes, users_routes
from app.services.parking_service import refresh_data
from app.services.user_service import refresh_user_data

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
app.include_router(users_routes.router)
app.include_router(bookings_routes.router)
app.include_router(reviews_routes.router)
app.include_router(assistant_routes.router)

# Разрешаем все CORS-запросы
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.on_event("startup")
async def on_startup():
    """Обработчик события запуска приложения"""
    await test_connection()  # проверяем соединение с БД

    # Инициализируем Firebase Admin SDK
    cred = credentials.Certificate(settings.firebase_credentials_path)
    initialize_app(cred)

    # Запускаем фоновые задачи
    asyncio.create_task(refresh_data())  # обновление данных парковок
    asyncio.create_task(refresh_user_data())  # синхронизация пользователей

