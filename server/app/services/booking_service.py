import uuid
from uuid import UUID
from datetime import datetime

from sqlalchemy import text
from fastapi import HTTPException, status

from app.db import async_session
from app.schemas.booking_schema import BookingIn, BookingOut


async def create_booking(data: BookingIn, user_uid: str) -> BookingOut:
    """
    Создаёт новую бронь с проверкой:
    1. Доступности мест
    2. Отсутствия временных коллизий
    3. Атомарного обновления счётчика мест

    Args:
        data (BookingIn): Данные бронирования
        user_uid (str): UID пользователя

    Returns:
        BookingOut: Подтверждённая бронь

    Raises:
        HTTPException: При ошибках валидации
    """
    # Проверка временного интервала
    if data.ts_from >= data.ts_to:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Некорректный временной интервал"
        )

    async with async_session() as ses:
        # 1. Проверка временных коллизий (ИСПРАВЛЕННЫЙ ЗАПРОС)
        conflict = await ses.execute(text("""
               SELECT COUNT(*) 
               FROM bookings
               WHERE parking_id = :parking_id
               AND ts_from < :ts_to 
               AND ts_to > :ts_from
           """), {
            "parking_id": data.parking_id,
            "ts_from": data.ts_from,
            "ts_to": data.ts_to
        })

        if conflict.scalar() > 0:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Выбранное время уже занято"
            )

        # 2. Блокировка и обновление счётчика мест
        updated = await ses.execute(text("""
            UPDATE parkings
            SET available_spaces = available_spaces - 1
            WHERE id = :parking_id AND available_spaces > 0
            RETURNING available_spaces
        """), {"parking_id": data.parking_id})

        if updated.scalar() is None:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Нет свободных мест"
            )

        # 3. Создание бронирования
        booking_id = uuid.uuid4()
        await ses.execute(text("""
            INSERT INTO bookings
                (id, user_uid, parking_id, vehicle_type, plate, ts_from, ts_to)
            VALUES
                (:id, :user_uid, :parking_id, :vehicle_type, :plate, :ts_from, :ts_to)
        """), {
            "id": booking_id,
            "user_uid": user_uid,
            **data.dict()
        })

        await ses.commit()

    return BookingOut(id=booking_id, user_uid=user_uid, **data.dict())


async def list_bookings(user_uid: str, limit: int = 20) -> list[BookingOut]:
    """
    Возвращает список броней пользователя

    Args:
        user_uid (str): UID пользователя
        limit (int): Лимит записей

    Returns:
        list[BookingOut]: Список броней
    """
    async with async_session() as ses:
        result = await ses.execute(text("""
            SELECT 
                id, user_uid, parking_id, 
                vehicle_type, plate, ts_from, ts_to
            FROM bookings
            WHERE user_uid = :uid
            ORDER BY ts_from DESC
            LIMIT :lim
        """), {"uid": user_uid, "lim": limit})

        return [BookingOut(**row) for row in result.mappings()]


from uuid import UUID
from fastapi import HTTPException, status
from sqlalchemy import text


async def cancel_booking(booking_id: UUID, user_uid: str) -> None:
    async with async_session() as ses:
        try:
            result = await ses.execute(text("""
                WITH deleted_booking AS (
                    DELETE FROM bookings
                    WHERE id = :id AND user_uid = :uid
                    RETURNING parking_id, ts_from, ts_to
                )
                SELECT 
                    db.parking_id,
                    p.capacity,
                    p.available_spaces,
                    db.ts_to < NOW() AS is_expired
                FROM deleted_booking db
                JOIN parkings p ON p.id = db.parking_id
            """), {"id": booking_id, "uid": user_uid})

            booking_data = result.first()
            if not booking_data:
                raise HTTPException(
                    status_code=status.HTTP_404_NOT_FOUND,
                    detail="Бронирование не найдено или не принадлежит пользователю"
                )

            parking_id, capacity, available_spaces, is_expired = booking_data
            if not is_expired and 0 <= available_spaces < capacity:
                await ses.execute(text("""
                    UPDATE parkings
                    SET available_spaces = available_spaces + 1
                    WHERE id = :parking_id
                """), {"parking_id": parking_id})

            await ses.commit()
        except Exception as e:
            await ses.rollback()
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail="Ошибка при отмене бронирования"
            )