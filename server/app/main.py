# app/main.py

import asyncio
import logging

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.db import test_connection

from app.services.sync import refresh_data
from app.models.parking import Parking

# Настраиваем базовый логгер
logging.basicConfig(level=logging.INFO)

# Инициализируем FastAPI-приложение
app = FastAPI(
    title="Свободные парковки Москвы",
    version="0.2.0",
)

# Разрешаем все CORS-запросы (можно ограничить в будущем)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.on_event("startup")
async def on_startup():
    await test_connection()  # проверяем и логируем
    """
    Обработчик события запуска приложения.

    Инициализирует контейнер для парковок и запускает фоновую
    задачу по периодической синхронизации данных.
    """
    #app.state.parkings = []   # Хранилище данных о парковках в памяти приложения
    #asyncio.create_task(refresh_data(app.state)) # Запуск фоновой синхронизации


@app.get("/parkings", response_model=list[Parking])
async def get_parkings():
    """
    Получить список всех парковок.

    Returns:
        list[Parking]: Список парковок с актуальными данными.
    """
    return app.state.parkings