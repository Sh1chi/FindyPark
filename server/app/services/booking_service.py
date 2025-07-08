import uuid

from sqlalchemy import text

from app.db import async_session
from app.schemas.booking_schema import BookingIn, BookingOut


async def create_booking(data: BookingIn, user_uid: str) -> BookingOut:
    """
       Создаёт новую бронь в базе данных и возвращает её описание.

       Args:
           data (BookingIn): Данные о бронировании из запроса.
           user_uid (str): UID пользователя из Firebase.

       Returns:
           BookingOut: Подтверждённая бронь с UUID и user_uid.
    """
    booking_id = uuid.uuid4()

    async with async_session() as ses:
        await ses.execute(text("""
            INSERT INTO bookings
              (id, user_uid, parking_id, vehicle_type, plate, ts_from, ts_to)
            VALUES
              (:id, :user_uid, :parking_id, :vehicle_type, :plate, :ts_from, :ts_to)
        """), {"id": booking_id, "user_uid": user_uid, **data.dict()})
        await ses.commit()

    return BookingOut(id=booking_id, user_uid=user_uid, **data.dict())


async def list_bookings(user_uid: str, limit: int = 20) -> list[BookingOut]:
    """
        Возвращает список последних броней пользователя.

        Args:
            user_uid (str): UID пользователя.
            limit (int): Максимальное число записей (по умолчанию 20).

        Returns:
            list[BookingOut]: Список броней, отсортированных по дате начала.
    """
    async with async_session() as ses:
        result  = await ses.execute(text("""
            SELECT id, user_uid, parking_id, vehicle_type,
                   plate, ts_from, ts_to
              FROM bookings
             WHERE user_uid = :uid
             ORDER BY ts_from DESC
             LIMIT :lim
        """), {"uid": user_uid, "lim": limit})
        rows = result.all()
    return [BookingOut(**r._mapping) for r in rows]
