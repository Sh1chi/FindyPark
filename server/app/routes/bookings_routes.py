"""
Маршруты для бронирований с авторизацией через Firebase ID-token.
    - POST /bookings — создать бронь
    - GET /bookings — список броней пользователя
    - DELETE /bookings/{id} — отменить бронь
"""

from uuid import UUID
from fastapi import (
    APIRouter,
    Depends,
    HTTPException,
    Request,
    status,
)
from firebase_admin import auth
from sqlalchemy import text
from typing import Annotated

from app.db import async_session
from app.schemas.booking_schema import BookingIn, BookingOut
from app.services.booking_service import create_booking, list_bookings, cancel_booking

router = APIRouter(prefix="/bookings", tags=["bookings"])


async def get_current_user(request: Request) -> dict:
    """
    Аутентификация пользователя через Firebase ID-token.

    Возвращает:
        dict: {"user_uid": <uid>} при успехе

    Вызывает:
        HTTPException: 401 при невалидном токене, 404 если пользователя нет в БД
    """
    auth_header = request.headers.get("Authorization")
    if not auth_header or not auth_header.startswith("Bearer "):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Требуется авторизация: Bearer <ID-token>",
            headers={"WWW-Authenticate": "Bearer"},
        )

    id_token = auth_header.split(" ", 1)[1].strip()

    try:
        decoded = auth.verify_id_token(id_token)
        uid: str = decoded["uid"]
    except auth.ExpiredIdTokenError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Срок действия токена истек",
            headers={"WWW-Authenticate": "Bearer"},
        )
    except auth.InvalidIdTokenError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Неверный токен авторизации",
            headers={"WWW-Authenticate": "Bearer"},
        )
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail=f"Ошибка аутентификации: {str(e)}",
            headers={"WWW-Authenticate": "Bearer"},
        )

    # Проверка существования пользователя в БД
    async with async_session() as ses:
        exists = await ses.execute(
            text("SELECT 1 FROM users WHERE user_uid = :uid"),
            {"uid": uid},
        )
        if not exists.scalar():
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Пользователь не найден в системе",
            )

    return {"user_uid": uid}


@router.post(
    "",
    response_model=BookingOut,
    status_code=status.HTTP_201_CREATED,
    summary="Создать новое бронирование",
    responses={
        400: {"description": "Нет свободных мест/некорректное время"},
        401: {"description": "Неавторизованный запрос"},
        403: {"description": "Доступ запрещен"},
        404: {"description": "Пользователь или парковка не найдены"}
    }
)
async def api_create_booking(
    data: BookingIn,
    user: Annotated[dict, Depends(get_current_user)]
) -> BookingOut:
    """
    Создаёт новое бронирование парковки.

    Требует:
    - Валидный Firebase ID-token в заголовке Authorization
    - Свободные места на выбранное время
    - Корректные временные интервалы (ts_to > ts_from)
    """
    try:
        return await create_booking(data, user["user_uid"])
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Ошибка при создании бронирования: {str(e)}",
        )


@router.get(
    "",
    response_model=list[BookingOut],
    summary="Список бронирований пользователя",
    responses={
        401: {"description": "Неавторизованный запрос"},
        404: {"description": "Пользователь не найден"}
    }
)
async def api_list_bookings(
    user: Annotated[dict, Depends(get_current_user)],
    limit: int = 20
) -> list[BookingOut]:
    """
    Возвращает последние бронирования текущего пользователя.

    Параметры:
    - limit: максимальное количество возвращаемых записей (по умолчанию 20, максимум 100)
    """
    if limit > 100:
        limit = 100
    return await list_bookings(user["user_uid"], limit)


@router.delete(
    "/{booking_id}",
    status_code=status.HTTP_204_NO_CONTENT,
    summary="Отменить бронирование",
    responses={
        401: {"description": "Неавторизованный запрос"},
        403: {"description": "Нет прав на отмену"},
        404: {"description": "Бронирование не найдено"},
        409: {"description": "Невозможно отменить начавшееся бронирование"}
    }
)
async def api_cancel_booking(
    booking_id: UUID,
    user: Annotated[dict, Depends(get_current_user)]
):
    """
    Отменяет существующее бронирование.

    Требования:
    - Бронирование должно принадлежать текущему пользователю
    - Бронирование должно существовать
    - Бронирование еще не началось
    """
    try:
        await cancel_booking(booking_id, user["user_uid"])
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Ошибка при отмене бронирования: {str(e)}",
        )