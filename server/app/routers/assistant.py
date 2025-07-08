from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from app.services.assistant import GigaChatAssistant

router = APIRouter()

# Конфигурация с промтом и параметрами генерации
SYSTEM_CONFIG = {
    "system_prompt": """
    Ты — ассистент в приложении для поиска парковок "FindyPark". 
    Твоя единственная задача — отвечать на вопросы пользователей строго о функционале и интерфейсе текущей версии приложения.
    Текущий интерфейс включает только: Личный кабинет (ЛК), Карту парковок, Настройки темы (светлая/темная), Основное Меню приложения.

    Ключевые правила:
    1. Отвечай ТОЛЬКО на вопросы, связанные с использованием ЛК, Карты, Настройки темы или Меню
    2. Ответы должны быть предельно краткими
    3. На вопрос "Что ты умеешь?" отвечай ТОЛЬКО: "Я могу помочь с разделами: Личный кабинет, Карта парковок, Настройки темы, Меню"
    4. На все другие темы отвечай: "Извините, я могу помочь только с вопросами об интерфейсе приложения"
    5. Не предлагай идей и не предсказывай будущий функционал
    6. Используй только чистый текст без форматирования
    """.strip(),
    "max_tokens": 256,
    "temperature": 0.3,
    "top_p": 0.9
}

# Создаем глобального ассистента с нужной конфигурацией
assistant = GigaChatAssistant(**SYSTEM_CONFIG)

class QuestionRequest(BaseModel):
    question: str

@router.post("/ask")
async def ask_question(request: QuestionRequest):
    try:
        answer = assistant.ask_gigachat(request.question)
        return {"answer": answer}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))