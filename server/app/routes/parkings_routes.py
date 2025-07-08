from fastapi import APIRouter
from sqlalchemy import text

from app.db import async_session
from app.schemas.parking_schema import Parking

router = APIRouter(prefix="/parkings")

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
