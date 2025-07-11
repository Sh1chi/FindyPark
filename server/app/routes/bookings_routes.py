"""
Маршруты для бронирований + полноценная авторизация через Firebase ID-token.
    - POST /bookings — создать бронь
    - GET  /bookings — получить список броней текущего пользователя
"""

from fastapi import APIRouter, Depends, status

from app.schemas.booking_schema import BookingIn, BookingOut
from app.services.booking_service import create_booking, list_bookings
from app.dependencies.auth import get_current_user

router = APIRouter(prefix="/bookings", tags=["bookings"])


@router.post("", response_model=BookingOut, status_code=status.HTTP_201_CREATED)
async def api_create_booking(data: BookingIn, user=Depends(get_current_user)) -> BookingOut:
    """Создать бронь для текущего пользователя."""
    return await create_booking(data, user["user_uid"])


@router.get("", response_model=list[BookingOut])
async def api_list_bookings(user=Depends(get_current_user), limit: int = 20) -> list[BookingOut]:
    """Получить список броней текущего пользователя (макс. `limit`)."""
    return await list_bookings(user["user_uid"], limit)
