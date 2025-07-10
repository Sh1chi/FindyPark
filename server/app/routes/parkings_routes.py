from fastapi import APIRouter, HTTPException, Body
from sqlalchemy import text

from app.db import async_session
from app.schemas.parking_schema import Parking

router = APIRouter(prefix="/parkings", tags=["parkings"])


@router.get("", response_model=list[Parking])
async def get_parkings():
    """
    Чтение актуальных парковок из БД.
    """
    async with async_session() as session:
        result = await session.execute(text("""
                                            SELECT
                                              id,
                                              parking_zone_number,
                                              name,
                                              address,
                                              adm_area,
                                              district,
                                              ST_Y(geom::geometry) AS lat,
                                              ST_X(geom::geometry) AS lon,
                                              capacity,
                                              capacity_disabled,
                                              available_spaces AS free_spaces
                                            FROM parkings
                                        """))
        rows = result.all()

    return [
        Parking(
            id=row.id,
            parking_zone_number=row.parking_zone_number,
            name=row.name,
            address=row.address,
            adm_area=row.adm_area,
            district=row.district,
            lat=row.lat,
            lon=row.lon,
            capacity=row.capacity,
            capacity_disabled=row.capacity_disabled,
            free_spaces=row.free_spaces,
        )
        for row in rows
    ]


@router.patch("/{parking_id}/occupancy", response_model=Parking)
async def set_parking_occupancy(
        parking_id: int,
        occupied_spaces: int = Body(..., gt=0, description="Количество занятых мест"),
):
    """
    Искусственное изменение загруженности парковки (для тестирования)
    """
    async with async_session() as session:
        # Проверяем существование парковки
        parking = await session.execute(
            text("SELECT id, capacity FROM parkings WHERE id = :id"),
            {"id": parking_id}
        )
        parking = parking.first()
        if not parking:
            raise HTTPException(status_code=404, detail="Парковка не найдена")

        capacity = parking.capacity
        if occupied_spaces > capacity:
            raise HTTPException(
                status_code=400,
                detail=f"Не может быть больше {capacity} занятых мест"
            )

        # Обновляем данные
        await session.execute(
            text("""
                UPDATE parkings 
                SET available_spaces = :available 
                WHERE id = :id
            """),
            {"available": capacity - occupied_spaces, "id": parking_id}
        )
        await session.commit()

        # Возвращаем обновленные данные
        updated = await session.execute(
            text("""
                SELECT
                    id,
                    parking_zone_number,
                    name,
                    address,
                    adm_area,
                    district,
                    ST_Y(geom::geometry) AS lat,
                    ST_X(geom::geometry) AS lon,
                    capacity,
                    capacity_disabled,
                    available_spaces AS free_spaces
                FROM parkings
                WHERE id = :id
            """),
            {"id": parking_id}
        )
        row = updated.first()
        return Parking(
            id=row.id,
            parking_zone_number=row.parking_zone_number,
            name=row.name,
            address=row.address,
            adm_area=row.adm_area,
            district=row.district,
            lat=row.lat,
            lon=row.lon,
            capacity=row.capacity,
            capacity_disabled=row.capacity_disabled,
            free_spaces=row.free_spaces,
        )