from decimal import Decimal
from sqlalchemy import text

from app.db import async_session
from app.models.enums import VehicleKind


async def get_weekday_car_tariff(parking_id: int) -> Decimal | None:
    """
    Возвращает тариф (цена в час) для легкового автомобиля
    на указанной парковке в будний день.
    Возвращает None, если тариф не найден.
    """
    sql = text(
        """
        SELECT hour_price
          FROM tariffs
         WHERE parking_id   = :parking_id
           AND period        = 'weekday'          -- строка, не Enum
           AND vehicle_type  = :vehicle_type      -- Enum ➜ строка
         LIMIT 1
        """
    )
    async with async_session() as ses:
        res = await ses.execute(sql, {
            "parking_id":  parking_id,
            "vehicle_type": VehicleKind.car.value
        })
        return res.scalar_one_or_none()

# Сопоставление русских названий периодов с английскими значениями в БД
RU_PERIOD = {
    "будни":          "weekday",
    "выходные дни":   "weekend",
    "праздничные дни": "holiday",
}

# Сопоставление русских названий ТС с Enum VehicleKind
RU_VEHICLE = {
    "Легковой автомобиль": VehicleKind.car,
    "Мотоцикл":            VehicleKind.moto,
    "Грузовой автомобиль":  VehicleKind.truck,
    "Автобус":             VehicleKind.bus,
}


async def upsert_tariffs(parking_id: int, tariffs: list[dict], ses) -> None:
    """
    Перезаписывает тарифы для заданной парковки:
    удаляет все старые тарифы и вставляет новые из выгрузки data.mos.ru.
    """
    await ses.execute(text("DELETE FROM tariffs WHERE parking_id = :pid"), {"pid": parking_id})

    for t in tariffs:
        if t.get("is_deleted"):
            continue  # пропускаем удалённые тарифы

        period   = RU_PERIOD.get(t["TariffPeriod"].lower())
        vehicle  = RU_VEHICLE.get(t["VehicleTypeForThisTariff"])
        if not period or not vehicle:
            continue  # пропускаем неизвестные типы/периоды

        await ses.execute(
            text("""
              INSERT INTO tariffs
                    (parking_id, period, time_range,
                     vehicle_type, tariff_type, hour_price)
              VALUES (:parking_id, :period, :time_range,
                      :vehicle_type, 'fixed', :hour_price)
            """),
            {
                "parking_id":  parking_id,
                "period":      period,
                "time_range":  t.get("TimeRange") or "00:00-24:00",
                "vehicle_type": vehicle.value,
                "hour_price":   t.get("HourPrice") or 0,
            },
        )
