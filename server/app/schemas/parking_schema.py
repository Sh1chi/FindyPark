from pydantic import BaseModel, Field

class Parking(BaseModel):
    """
    Модель парковки для передачи клиенту через API.

    Используется для сериализации данных о парковке, включая
    координаты, адрес и информацию о доступных местах.

    Атрибуты:
        id (int): Уникальный ID парковки.
        parking_zone_number (str): Номер парковочной зоны.
        name (str): Название или адрес парковки.
        address (str): Адрес парковки.
        adm_area (str | None): Административный округ.
        district (str | None): Район.
        lat (float): Широта (от -90 до 90).
        lon (float): Долгота (от -180 до 180).
        capacity (int): Общее количество мест.
        capacity_disabled (int): Кол-во мест для людей с ОВЗ.
        free_spaces (int): Текущее количество свободных мест.
    """
    id: int
    parking_zone_number: str
    name: str
    address: str
    adm_area: str | None
    district: str | None
    lat: float = Field(..., ge=-90, le=90)
    lon: float = Field(..., ge=-180, le=180)
    capacity: int
    capacity_disabled: int
    free_spaces: int


class ParkingSuggest(BaseModel):
    """
    Lightweight model for type-ahead suggestions.

    Only the data the client needs to render a dropdown.
    """
    id: int
    address: str
    lat: float = Field(..., ge=-90, le=90)
    lon: float = Field(..., ge=-180, le=180)