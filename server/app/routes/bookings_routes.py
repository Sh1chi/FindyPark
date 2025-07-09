"""
Маршруты для бронирований + полноценная авторизация через Firebase ID-token.
    - POST /bookings — создать бронь
    - GET  /bookings — получить список броней текущего пользователя
"""

from fastapi import (
    APIRouter,
    Depends,
    HTTPException,
    Request,
    status,
)
from firebase_admin import auth
from sqlalchemy import text

from app.db import async_session
from app.schemas.booking_schema import BookingIn, BookingOut
from app.services.booking_service import create_booking, list_bookings

router = APIRouter(prefix="/bookings", tags=["bookings"])


async def get_current_user(request: Request) -> dict:
    """Возвращает словарь {"user_uid": <uid>} или 401/404."""

    auth_header = request.headers.get("Authorization")
    if not auth_header or not auth_header.startswith("Bearer "):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Missing ID-token in Authorization header",
        )

    id_token = auth_header.split(" ", 1)[1].strip()

    # Проверяем подпись токена и получаем uid
    try:
        decoded = auth.verify_id_token(id_token)
        uid: str = decoded["uid"]
    except Exception:       # Ошибка верификации или истёк токен
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid or expired ID-token",
        )

    # Проверка наличия пользователя в БД
    async with async_session() as ses:
        row = await ses.execute(
            text("SELECT 1 FROM users WHERE user_uid = :uid"),
            {"uid": uid},
        )
        if row.scalar() is None:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="User not found in DB; run user_sync first",
            )

    return {"user_uid": uid}


@router.post("", response_model=BookingOut, status_code=status.HTTP_201_CREATED)
async def api_create_booking(data: BookingIn, user=Depends(get_current_user)) -> BookingOut:
    """Создать бронь для текущего пользователя."""
    return await create_booking(data, user["user_uid"])


@router.get("", response_model=list[BookingOut])
async def api_list_bookings(user=Depends(get_current_user), limit: int = 20) -> list[BookingOut]:
    """Получить список броней текущего пользователя (макс. `limit`)."""
    return await list_bookings(user["user_uid"], limit)
