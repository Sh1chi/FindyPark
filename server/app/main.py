# app/main.py

import asyncio
import logging

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.db import test_connection

from app.services.sync import refresh_data
from app.models.parking import Parking
from app.db import async_session
from sqlalchemy import text

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
    asyncio.create_task(refresh_data())
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
        Чтение актуальных парковок из БД.
    """
    async with async_session() as session:
        result = await session.execute(text("""
                                                SELECT
                                                  id,
                                                  parking_zone_number,
                                                  name,
                                                  address,
                                                  adm_area,
                                                  district,
                                                  ST_Y(geom::geometry) AS lat,
                                                  ST_X(geom::geometry) AS lon,
                                                  capacity,
                                                  capacity_disabled,
                                                  available_spaces AS free_spaces
                                                FROM parkings
                                            """))
        rows = result.all()

    return [
        Parking(
            id=row.id,
            parking_zone_number=row.parking_zone_number,
            name=row.name,
            address=row.address,
            adm_area=row.adm_area,
            district=row.district,
            lat=row.lat,
            lon=row.lon,
            capacity=row.capacity,
            capacity_disabled=row.capacity_disabled,
            free_spaces=row.free_spaces,
        )
        for row in rows
    ]