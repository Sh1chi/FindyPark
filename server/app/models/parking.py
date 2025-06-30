from pydantic import BaseModel, Field


class Parking(BaseModel):
    """
    Модель парковки для валидации и передачи данных.

    Attributes:
        id (int): Уникальный идентификатор парковки.
        name (str): Адрес или название парковки.
        lat (float): Широта (валидируется в диапазоне от -90 до 90).
        lon (float): Долгота (валидируется в диапазоне от -180 до 180).
        capacity (int): Общее количество мест на парковке.
        free_spaces (int): Количество свободных мест.
    """
    id: int
    name: str
    lat: float = Field(..., ge=-90, le=90)
    lon: float = Field(..., ge=-180, le=180)
    capacity: int
    free_spaces: int