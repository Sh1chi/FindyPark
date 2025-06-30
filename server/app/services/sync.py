import asyncio
import logging
import traceback
from typing import List, Optional

import httpx

from app.core.config import get_settings
from app.models.parking import Parking

DATASET_ID = 623
ROWS_URL = f"https://apidata.mos.ru/v1/datasets/{DATASET_ID}/rows"

MAX_ROWS = 1000          # Лимит размера страницы data.mos.ru
REQUEST_TIMEOUT = 40     # Таймаут запроса (секунды)
MAX_RETRIES = 3          # Количество попыток при ошибках
RETRY_DELAY = 15         # Задержка между попытками (секунды)

log      = logging.getLogger(__name__)
settings = get_settings()



def _map_row(row: dict) -> Optional[Parking]:
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

    return Parking(
        id=gid,
        name=cells.get("Address", "Без адреса"),
        lat=lat,
        lon=lon,
        capacity=capacity,
        free_spaces=capacity,
    )


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


async def _fetch_all() -> List[Parking]:
    """
    Загружает все данные о парковках с data.mos.ru постранично.

    Returns:
        List[Parking]: Список успешно обработанных парковок.

    Raises:
        RuntimeError: Если страница не была загружена после повторных попыток.
    """
    results: List[Parking] = []
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


async def refresh_data(app_state):
    """
    Фоновая задача: обновление данных о парковках каждые 15 минут.

    Args:
        app_state: Состояние приложения для хранения парковок.
    """
    while True:
        try:
            app_state.parkings = await _fetch_all()
            log.info("Parking dataset refreshed: %s records",
                     len(app_state.parkings))
        except Exception:
            log.warning("Dataset refresh failed:\n%s", traceback.format_exc())
        await asyncio.sleep(60 * 60)
