from typing import Annotated

from fastapi import Request, HTTPException, status, Depends
from firebase_admin import auth
from sqlalchemy import text

from app.db import async_session
from app.routes.parkings_routes import router

from app.schemas.booking_schema import BookingOut, BookingIn
from app.services.booking_service import create_booking
from app.services.user_service import sync_user_profile

async def get_current_user(request: Request) -> dict:
    """
    Аутентификация пользователя через Firebase ID-token.
    Автоматически синхронизирует пользователя с БД при успешной аутентификации.
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

        # Автоматическая синхронизация пользователя
        await sync_user_profile(uid)

        return {"user_uid": uid}

    except auth.ExpiredIdTokenError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Срок действия токена истек",
        )
    except auth.InvalidIdTokenError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Неверный токен авторизации",
        )
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Ошибка аутентификации: {str(e)}",
        )


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