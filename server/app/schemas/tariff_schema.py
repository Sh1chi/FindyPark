from decimal import Decimal
from pydantic import BaseModel, PositiveInt

class WeekdayCarTariffOut(BaseModel):
    """
    Тариф в будний день для легкового автомобиля.
    hour_price — стоимость часа в рублях.
    """
    parking_id: PositiveInt
    hour_price: Decimal
