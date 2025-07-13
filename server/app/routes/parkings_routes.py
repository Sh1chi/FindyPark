from fastapi import APIRouter,  Query, HTTPException
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


@router.get("/nearby", response_model=list[Parking])
async def list_parkings(
    lat: float = Query(..., ge=-90, le=90, description="Широта центра поиска"),
    lon: float = Query(..., ge=-180, le=180, description="Долгота центра поиска"),
    radius: int = Query(500, ge=50, le=5000, description="Радиус (м)"),
    free_only: bool = False,
    limit: int = Query(100, le=200),
):
    """
       Возвращает парковки в заданном радиусе от точки (lat, lon).

       - Фильтрует по георадиусу с помощью PostGIS ST_DWithin.
       - Опционально показывает только парковки с доступными местами.
       - Сортирует по расстоянию до заданной точки.
    """
    sql = text("""
        WITH origin AS (
            SELECT ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography AS g
        )
        SELECT  p.id,
                p.parking_zone_number,
                p.name,
                p.address,
                p.adm_area,
                p.district,
                ST_Y(p.geom::geometry)  AS lat,
                ST_X(p.geom::geometry)  AS lon,
                p.capacity,
                p.capacity_disabled,
                p.available_spaces      AS free_spaces,
                (p.geom::geography <-> (SELECT g FROM origin)) AS distance_m
        FROM    parkings AS p
        WHERE   ST_DWithin(p.geom::geography, (SELECT g FROM origin), :radius)
          AND  (:free_only = FALSE OR p.available_spaces > 0)
        ORDER BY distance_m
        LIMIT   :limit;
    """)

    async with async_session() as ses:
        rows = (await ses.execute(sql, {
            "lat": lat, "lon": lon, "radius": radius,
            "free_only": free_only, "limit": limit,
        })).mappings().all()

    return [Parking(**row) for row in rows]
