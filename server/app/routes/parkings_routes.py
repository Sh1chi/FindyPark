from fastapi import APIRouter,  Query, HTTPException
from fastapi import APIRouter, HTTPException, Body
from sqlalchemy import text
from sqlalchemy import Float, Integer, bindparam

from app.db import async_session
from app.schemas.parking_schema import Parking
from app.schemas.parking_schema import ParkingSuggest

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


@router.get("/suggest", response_model=list[ParkingSuggest])
async def suggest_parkings(
    q: str = Query(..., min_length=2, max_length=60,
                   description="Начало адреса, минимум 2 символа"),
    lat: float | None = Query(None, ge=-90, le=90,
                              description="Текущая широта (для ранжирования)"),
    lon: float | None = Query(None, ge=-180, le=180,
                              description="Текущая долгота"),
    limit: int = Query(10, ge=1, le=20, description="Сколько подсказок вернуть"),
):
    """
    Type-ahead адресных подсказок **только по нашим парковкам**.

    - Сначала сортируем по текстовой похожести (pg_trgm `similarity` DESC).
    - При наличии `lat`/`lon` добавляем ранжирование по расстоянию (ASC).
    """
    prefix = f"{q}%"

    sql = text("""
    WITH origin AS (
        SELECT CASE
            WHEN :lat IS NULL OR :lon IS NULL THEN NULL::geography
            ELSE ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography
        END AS g
    ),
    ranked AS (
        SELECT
            p.id,
            p.address,
            ST_Y(p.geom::geometry)  AS lat,
            ST_X(p.geom::geometry)  AS lon,
            similarity(p.address, :q) AS text_score,
            CASE
                WHEN (SELECT g FROM origin) IS NOT NULL
                THEN p.geom::geography <-> (SELECT g FROM origin)
                ELSE NULL
            END AS distance_m
        FROM parkings AS p
        WHERE address ILIKE :prefix
           OR address % :q
    )
    SELECT *
    FROM ranked
    ORDER BY text_score DESC, COALESCE(distance_m, 1e9) ASC
    LIMIT :limit;
    """).bindparams(
        bindparam("lat",     type_=Float),
        bindparam("lon",     type_=Float),
        bindparam("q"),                      # строка, тип по умолчанию
        bindparam("prefix"),                 # для ILIKE
        bindparam("limit",   type_=Integer),
    )

    # Открываем сессию и выполняем запрос
    async with async_session() as session:
        result = await session.execute(
            sql,
            {"lat": lat, "lon": lon, "q": q, "prefix": prefix, "limit": limit}
        )
        rows = result.mappings().all()

    # Преобразуем в Pydantic-модель для ответа
    return [ParkingSuggest(**row) for row in rows]


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