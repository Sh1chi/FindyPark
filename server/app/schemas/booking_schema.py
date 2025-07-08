from datetime import datetime
from pydantic import BaseModel, Field, validator
from uuid import UUID

from app.models.enums import VehicleKind

class BookingIn(BaseModel):
    """
       Модель входящих данных для создания бронирования.

       Атрибуты:
           parking_id (int): ID выбранной парковки.
           vehicle_type (VehicleKind): Тип транспортного средства.
           plate (str): Госномер (от 2 до 12 символов).
           ts_from (datetime): Дата/время начала брони.
           ts_to (datetime): Дата/время окончания брони.
    """
    parking_id:  int
    vehicle_type: VehicleKind
    plate:       str = Field(min_length=2, max_length=12)
    ts_from:     datetime
    ts_to:       datetime

    @validator("ts_to")
    def _later_than_from(cls, v, values):
        """Проверяет, что ts_to позже ts_from."""
        if "ts_from" in values and v <= values["ts_from"]:
            raise ValueError("ts_to must be after ts_from")
        return v


class BookingOut(BookingIn):
    """
    Модель ответа для API: возвращает данные о брони.

    Атрибуты:
        id (UUID): Уникальный идентификатор брони.
        user_uid (str): UID пользователя, сделавшего бронь.
    """
    id: UUID
    user_uid: str
