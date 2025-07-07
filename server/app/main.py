# app/main.py
import logging
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.routers import assistant  # Импорт роутера ассистента

# Настраиваем базовый логгер
logging.basicConfig(level=logging.INFO)

# Инициализируем FastAPI-приложение
app = FastAPI(
    title="Парковочный ассистент",
    version="0.1.0",
)

# Разрешаем все CORS-запросы
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

# Подключаем только роутер ассистента
app.include_router(assistant.router, prefix="/assistant", tags=["Assistant"])