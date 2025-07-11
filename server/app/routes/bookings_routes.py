"""
Маршруты для бронирований с авторизацией через Firebase ID-token.
    - POST /bookings — создать бронь
    - GET /bookings — список броней пользователя
    - DELETE /bookings/{id} — отменить бронь
"""

from fastapi import APIRouter, Depends, status
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

from app.schemas.booking_schema import BookingIn, BookingOut
from app.services.booking_service import create_booking, list_bookings
from app.dependencies.auth import get_current_user
from app.services.booking_service import create_booking, list_bookings, cancel_booking

router = APIRouter(prefix="/bookings", tags=["bookings"])


@router.post("", response_model=BookingOut, status_code=status.HTTP_201_CREATED)
async def api_create_booking(data: BookingIn, user=Depends(get_current_user)) -> BookingOut:
    """Создать бронь для текущего пользователя."""
    return await create_booking(data, user["user_uid"])


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