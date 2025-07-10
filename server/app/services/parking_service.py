import asyncio
import logging
import traceback
import httpx
from typing import List, Optional
from sqlalchemy import text
from app.core.config import get_settings
from app.schemas.parking_schema import Parking
from app.db import async_session
import json

# Конфигурация
DATASET_ID = 623
ROWS_URL = f"https://apidata.mos.ru/v1/datasets/{DATASET_ID}/rows"
MAX_ROWS = 1000
REQUEST_TIMEOUT = 40
MAX_RETRIES = 3
RETRY_DELAY = 15
IS_TEST = True
MAX_TOTAL = 100

log = logging.getLogger(__name__)
settings = get_settings()


def _map_row(row: dict) -> Optional[Parking]:
    """Оптимизированное преобразование данных из API в объект Parking"""
    try:
        # Получение координат
        coords = None
        if "Geometry" in row and row["Geometry"].get("coordinates"):
            coords = row["Geometry"]["coordinates"]
        elif (cells_geo := row.get("Cells", {}).get("geoData")) and cells_geo.get("coordinates"):
            raw = cells_geo["coordinates"]
            if isinstance(raw, list) and raw:
                first = raw[0]
                coords = first[0] if isinstance(first[0], list) else first
        else:
            lon = row.get("Longitude_WGS84") or row.get("Longitude")
            lat = row.get("Latitude_WGS84") or row.get("Latitude")
            if lon is not None and lat is not None:
                coords = [lon, lat]

        if not coords or len(coords) < 2:
            return None

        lon, lat = float(coords[0]), float(coords[1])
        cells = row.get("Cells", {})
        gid = int(row.get("global_id") or cells.get("ID"))
        capacity = int(cells.get("CarCapacity") or cells.get("TotalPlaces") or 0)

        return Parking(
            id=gid,
            parking_zone_number=cells.get("ParkingZoneNumber", ""),
            name=cells.get("ParkingName", cells.get("Address", "Без названия")),
            address=cells.get("Address", ""),
            adm_area=cells.get("AdmArea"),
            district=cells.get("District"),
            lat=lat,
            lon=lon,
            capacity=capacity,
            capacity_disabled=int(cells.get("CarCapacityDisabled") or 0),
            free_spaces=capacity,  # Изначально все места свободны
        )
    except (TypeError, ValueError, KeyError) as e:
        log.debug(f"Skipping row due to error: {e}")
        return None


async def _safe_get(client: httpx.AsyncClient, params: dict) -> Optional[list]:
    """Улучшенный обработчик запросов с повторными попытками"""
    for attempt in range(1, MAX_RETRIES + 1):
        try:
            resp = await client.get(ROWS_URL, params=params, timeout=REQUEST_TIMEOUT)
            resp.raise_for_status()
            return resp.json()
        except (httpx.ReadTimeout, httpx.ConnectTimeout):
            log.warning(f"Timeout {attempt}/{MAX_RETRIES}, retrying in {RETRY_DELAY}s")
            await asyncio.sleep(RETRY_DELAY)
        except httpx.HTTPStatusError as e:
            if e.response.status_code == 429 or 500 <= e.response.status_code < 600:
                log.warning(f"HTTP {e.response.status_code} {attempt}/{MAX_RETRIES}, retrying")
                await asyncio.sleep(RETRY_DELAY)
            else:
                raise
    return None


async def _fetch_parkings() -> List[Parking]:
    """Унифицированный метод загрузки данных"""
    if IS_TEST:
        log.info("Running in TEST mode (limit: %d parkings)", MAX_TOTAL)
        async with httpx.AsyncClient(timeout=REQUEST_TIMEOUT) as client:
            batch = await _safe_get(client, {
                "$top": MAX_TOTAL,
                "$skip": 0,
                "api_key": settings.data_mos_token,
            })
            return [p for row in (batch or [])[:MAX_TOTAL] if (p := _map_row(row))]

    # Режим production: постраничная загрузка
    results: List[Parking] = []
    skip = 0
    async with httpx.AsyncClient(timeout=REQUEST_TIMEOUT) as client:
        while True:
            params = {
                "$top": MAX_ROWS,
                "$skip": skip,
                "api_key": settings.data_mos_token,
            }
            batch = await _safe_get(client, params)
            if batch is None:
                raise RuntimeError(f"Failed to fetch page at offset {skip}")
            if not batch:
                break

            valid_parkings = [p for row in batch if (p := _map_row(row))]
            results.extend(valid_parkings)
            skip += MAX_ROWS
            log.info("Fetched page: %d valid parkings", len(valid_parkings))

    log.info("Total fetched parkings: %d", len(results))
    return results


async def save_parking_to_db(parkings: List[Parking]) -> None:
    """Безопасное массовое обновление данных в БД"""
    if not parkings:
        log.warning("No parkings to save")
        return

    async with async_session() as session:
        try:
            # Сначала получаем текущие данные о парковках
            existing_ids = await session.execute(
                text("SELECT id FROM parkings")
            )
            existing_ids = {row[0] for row in existing_ids}

            # Подготовка данных
            for p in parkings:
                values = {
                    "id": p.id,
                    "zone": p.parking_zone_number,
                    "name": p.name,
                    "address": p.address,
                    "adm_area": p.adm_area,
                    "district": p.district,
                    "capacity": p.capacity,
                    "capacity_disabled": p.capacity_disabled,
                    "free_spaces": p.free_spaces,
                    "lon": p.lon,
                    "lat": p.lat
                }

                if p.id in existing_ids:
                    # Обновление существующей записи
                    await session.execute(text("""
                        UPDATE parkings SET
                            parking_zone_number = :zone,
                            name = :name,
                            address = :address,
                            adm_area = :adm_area,
                            district = :district,
                            capacity = :capacity,
                            capacity_disabled = :capacity_disabled,
                            geom = ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)
                        WHERE id = :id
                    """), values)
                else:
                    # Вставка новой записи
                    await session.execute(text("""
                        INSERT INTO parkings (
                            id, parking_zone_number, name, address, adm_area, 
                            district, capacity, capacity_disabled, 
                            available_spaces, geom
                        ) VALUES (
                            :id, :zone, :name, :address, :adm_area,
                            :district, :capacity, :capacity_disabled,
                            :free_spaces,
                            ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)
                        )
                    """), values)

            await session.commit()
            log.info("Successfully saved %d parkings to DB", len(parkings))

        except Exception as e:
            await session.rollback()
            log.error("Failed to save parkings: %s\n%s", str(e), traceback.format_exc())
            raise

async def refresh_data() -> None:
    """Фоновая задача обновления данных с улучшенной обработкой ошибок"""
    while True:
        try:
            start_time = asyncio.get_event_loop().time()
            parking = await _fetch_parkings()
            await save_parking_to_db(parking)
            elapsed = asyncio.get_event_loop().time() - start_time
            log.info(f"Data refresh completed in {elapsed:.2f} seconds")
        except Exception as e:
            log.error("Data refresh error: %s\n%s", str(e), traceback.format_exc())
            await asyncio.sleep(300)  # Пауза при ошибке 5 минут
        else:
            await asyncio.sleep(60 * 60)  # Обычный интервал 1 час