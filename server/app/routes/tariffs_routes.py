from fastapi import APIRouter, HTTPException, Path
from decimal import Decimal

from app.schemas.tariff_schema import WeekdayCarTariffOut
from app.services.tariff_service import get_weekday_car_tariff

router = APIRouter(prefix="/parkings", tags=["tariffs"])

@router.get("/{parking_id}/tariff", response_model=WeekdayCarTariffOut, summary="Тариф (будни, легковой)")
async def read_weekday_car_tariff(parking_id: int = Path(..., gt=0)):
    """
        Возвращает почасовой тариф для легкового автомобиля
        в будний день по указанному ID парковки.

        Возвращает 404, если тариф не найден.
    """
    price: Decimal | None = await get_weekday_car_tariff(parking_id)
    if price is None:
        raise HTTPException(404, "Тариф не найден")
    return WeekdayCarTariffOut(parking_id=parking_id, hour_price=price)
