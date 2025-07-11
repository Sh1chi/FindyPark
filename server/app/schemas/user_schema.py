from pydantic import BaseModel, EmailStr, Field, constr
from typing import Optional

from app.models.enums import VehicleKind


class UserOut(BaseModel):
    """
    Модель ответа с полным профилем пользователя,
    возвращается в GET /users/me и PATCH /users/me.
    """
    user_uid: str
    display_name: Optional[str] = None
    email: EmailStr
    phone: Optional[str] = None
    vehicle_type: Optional[VehicleKind] = None
    plate: Optional[str] = None


class UserUpdateIn(BaseModel):
    """
    Модель входных данных для обновления профиля пользователя.
    Все поля необязательны. Пустая строка интерпретируется как очистка.
    """
    display_name: Optional[constr(min_length=1, max_length=100)] = None
    phone: Optional[constr(pattern=r'^\+?\d{10,15}$')] = None
    vehicle_type: Optional[VehicleKind] = None
    plate: Optional[constr(strip_whitespace=True, max_length=10)] = None
