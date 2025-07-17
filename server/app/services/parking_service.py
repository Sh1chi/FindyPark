import asyncio
import logging
import traceback
import httpx

from typing import List, Optional, Tuple
from sqlalchemy import text

from app.core.config import get_settings
from app.schemas.parking_schema import Parking
from app.db import async_session
from app.services.tariff_service import upsert_tariffs

DATASET_ID = 623
ROWS_URL = f"https://apidata.mos.ru/v1/datasets/{DATASET_ID}/rows"

MAX_ROWS = 1000          # Лимит размера страницы data.mos.ru
REQUEST_TIMEOUT = 40     # Таймаут запроса (секунды)
MAX_RETRIES = 3          # Количество попыток при ошибках
RETRY_DELAY = 15         # Задержка между попытками (секунды)

# Просто переключатель: в тестах True, в проде — False
IS_TEST = False
MAX_TOTAL = 100      # Лимит получаемых записей для теста

log      = logging.getLogger(__name__)
settings = get_settings()

def _map_row(row: dict) -> Optional[Tuple[Parking, List[dict]]]:
    """
    Преобразует запись data.mos.ru в объект Parking.

    Если не удаётся получить координаты или ID, возвращает None.
    """
    coords = None

    # Пытаемся получить координаты из разных возможных полей
    if "Geometry" in row and row["Geometry"].get("coordinates"):
        coords = row["Geometry"]["coordinates"]

    elif (cells_geo := row.get("Cells", {}).get("geoData")) and cells_geo.get("coordinates"):
        # Вложенный формат [[[lon, lat], ...]]
        raw = cells_geo["coordinates"]
        if isinstance(raw, list) and raw:
            first = raw[0]
            coords = first[0] if isinstance(first[0], list) else first

    else:
        # Альтернативный формат координат
        lon = row.get("Longitude_WGS84") or row.get("Longitude")
        lat = row.get("Latitude_WGS84")  or row.get("Latitude")
        if lon is not None and lat is not None:
            coords = [lon, lat]

    if not coords or len(coords) < 2:
        return None
    lon, lat = float(coords[0]), float(coords[1])

    cells = row.get("Cells", {})
    try:
        gid = int(row.get("global_id") or cells.get("ID"))
    except (TypeError, ValueError):
        return None

    capacity = int(cells.get("CarCapacity") or cells.get("TotalPlaces") or 0)

    parking = Parking(
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
        free_spaces=capacity,
    )
    # Теперь забираем список тарифов из row["Cells"]
    tariffs = row.get("Cells", {}).get("Tariffs", [])  # list of dict

    return parking, tariffs


async def _safe_get(client: httpx.AsyncClient, params: dict) -> Optional[list]:
    """
    GET-запрос с повторными попытками при таймаутах, ошибках 5xx и 429.

    Args:
        client (httpx.AsyncClient): HTTP-клиент.
        params (dict): Параметры запроса.

    Returns:
        Optional[list]: Список записей или None, если все попытки не удались.
    """
    for attempt in range(1, MAX_RETRIES + 1):
        try:
            resp = await client.get(ROWS_URL, params=params)
            resp.raise_for_status()
            return resp.json()
        except (httpx.ReadTimeout, httpx.ConnectTimeout):
            log.warning("Timeout %s/%s → wait %ss", attempt, MAX_RETRIES, RETRY_DELAY)
        except httpx.HTTPStatusError as e:
            code = e.response.status_code
            if code == 429 or 500 <= code < 600:
                log.warning("HTTP %s %s/%s → wait %ss", code, attempt, MAX_RETRIES, RETRY_DELAY)
            else:
                raise   # Прерываем попытки на других ошибках
        await asyncio.sleep(RETRY_DELAY)
    return None


async def _fetch_all() -> List[Tuple[Parking, list]]:
    """
    Загружает все данные о парковках с data.mos.ru постранично.

    Returns:
        List[Parking]: Список успешно обработанных парковок.

    Raises:
        RuntimeError: Если страница не была загружена после повторных попыток.
    """
    results: List[Tuple[Parking, list]] = []
    skip = 0

    async with httpx.AsyncClient(timeout=REQUEST_TIMEOUT) as client:
        page = 0
        while True:
            params = {
                "$top":   MAX_ROWS,
                "$skip":  skip,
                "api_key": settings.data_mos_token,
            }

            batch = await _safe_get(client, params)
            if batch is None:
                raise RuntimeError(f"Не удалось загрузить страницу offset={skip}")
            if not batch:
                # Нет больше данных — завершаем
                break

            good = bad = 0
            for row in batch:
                p = _map_row(row)
                if p:
                    results.append(p)
                    good += 1
                else:
                    bad += 1
            log.info("page %-4s ok=%-3s dropped=%-3s", page, good, bad)

            skip += MAX_ROWS
            page += 1

    log.info("Fetched %s parking rows total", len(results))
    return results


async def _fetch_limited() -> List[Tuple[Parking, list]]:
    """
    Единственный запрос, возвращает первые MAX_TOTAL записей — только для тестов.
    """
    results: List[Tuple[Parking, list]] = []
    async with httpx.AsyncClient(timeout=REQUEST_TIMEOUT) as client:
        batch = await _safe_get(client, {
            "$top":    MAX_TOTAL,
            "$skip":   0,
            "api_key": settings.data_mos_token,
        })
        if not batch:
            return results
        for row in batch[:MAX_TOTAL]:
            if p := _map_row(row):
                results.append(p)
    log.info("Fetched %s parking rows (test)", len(results))
    return results


async def save_records_to_db(records: List[Tuple[Parking, list]]) -> None:
    """
    Простая вставка/обновление парковок в БД.
    """
    async with async_session() as session:
        for parking, tariffs in records:
            await session.execute(
                text("""
                    INSERT INTO parkings
                        (id, parking_zone_number, name, address, adm_area, district, capacity, capacity_disabled, available_spaces, geom)
                    VALUES
                        (:id, :zone, :name, :address, :adm_area, :district, :capacity, :capacity_disabled, :free_spaces,
                            ST_SetSRID(ST_MakePoint(:lon, :lat), 4326))
                    ON CONFLICT (id) DO UPDATE SET
                        parking_zone_number = EXCLUDED.parking_zone_number,
                        name                = EXCLUDED.name,
                        address             = EXCLUDED.address,
                        adm_area            = EXCLUDED.adm_area,
                        district            = EXCLUDED.district,
                        capacity            = EXCLUDED.capacity,
                        capacity_disabled   = EXCLUDED.capacity_disabled,
                        available_spaces    = EXCLUDED.available_spaces,
                        geom                = EXCLUDED.geom
                """),
                {
                    "id": parking.id,
                    "zone": parking.parking_zone_number,
                    "name": parking.name,
                    "address": parking.address,
                    "adm_area": parking.adm_area,
                    "district": parking.district,
                    "capacity": parking.capacity,
                    "capacity_disabled": parking.capacity_disabled,
                    "free_spaces": parking.free_spaces,
                    "lon": parking.lon,
                    "lat": parking.lat,
                }
            )

            await upsert_tariffs(parking.id, tariffs, session)

        await session.commit()
    logging.getLogger(__name__).info("Saved %d parking rows to DB", len(records))


async def refresh_data() -> None:
    """
    Фоновая задача: обновление данных о парковках каждые 60 минут.
    """
    while True:
        try:
            if IS_TEST:
                records  = await _fetch_limited()
                #log.info("Test-mode fetch: %s records", len(app_state.parkings))
            else:
                records  = await _fetch_all()
                #]log.info("Full fetch: %s records", len(app_state.parkings))
            await save_records_to_db(records)
        except Exception:
            log.warning("Dataset refresh failed:\n%s", traceback.format_exc())
        await asyncio.sleep(60 * 60)
