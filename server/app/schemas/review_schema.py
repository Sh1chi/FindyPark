from datetime import datetime
from uuid import UUID
from pydantic import BaseModel, Field, PositiveInt, conint


class ReviewIn(BaseModel):
    """
    Модель входных данных для создания отзыва.

    Используется при POST-запросе от клиента.
    """
    parking_id: PositiveInt = Field(..., description="ID парковки")
    rating: conint(ge=1, le=5) = Field(..., description="Оценка 1-5")
    comment: str | None = Field(None, max_length=2000)


class ReviewOut(BaseModel):
    """
    Модель выходных данных для возврата отзыва клиенту.

    Используется в ответах API при чтении отзыва.
    """
    id: UUID
    user_uid: str
    parking_id: int
    rating: int
    comment: str | None
    created_at: datetime
