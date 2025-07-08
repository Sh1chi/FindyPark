import asyncio
import logging

from typing import List, Set
from firebase_admin import auth
from sqlalchemy import text, bindparam

from app.db import async_session
from app.models.enums import UserRole

log = logging.getLogger(__name__)
POLL_INTERVAL = 60 * 60


def _map_user(record: auth.UserRecord) -> dict:
    """
    Преобразует Firebase UserRecord в словарь для вставки в БД.
    """
    return {
        "user_uid":       record.uid,
        "display_name":   record.display_name or "Без имени",
        "email":          record.email,
        "phone":          record.phone_number,
        "role":           UserRole.user.value,      # по умолчанию 'user'
        "vehicle_type":   None,                     # заполняется позже пользователем
        "plate":          None,
    }


async def _upsert_users(batch: List[dict]) -> None:
    """
    Обновляет или вставляет пользователей из Firebase.

    Использует INSERT ... ON CONFLICT (user_uid) DO UPDATE
    для массового обновления/добавления.
    """
    async with async_session() as ses:
        for u in batch:
            await ses.execute(text("""
                INSERT INTO users
                  (user_uid, display_name, email, phone, role, vehicle_type, plate)
                VALUES
                  (:user_uid, :display_name, :email, :phone, :role, :vehicle_type, :plate)
                ON CONFLICT (user_uid) DO UPDATE SET
                  display_name = EXCLUDED.display_name,
                  email        = EXCLUDED.email,
                  phone        = EXCLUDED.phone
            """), u)
        await ses.commit()
    log.info("Upserted %d users", len(batch))


async def _delete_missing(uids_in_fb: Set[str]) -> None:
    """
    Удаляем из БД тех пользователей, которых уже нет в Firebase,
    вместе со всеми их бронированиями.
    """
    if not uids_in_fb:
        return

    async with async_session() as ses:
        # 1) Сначала очищаем брони пользователей, которых больше нет в Firebase
        await ses.execute(
            text("""
                DELETE FROM bookings
                 WHERE user_uid NOT IN :uids
            """).bindparams(bindparam("uids", expanding=True)),
            {"uids": list(uids_in_fb)},
        )

        # 2) Затем удаляем самих пользователей
        result = await ses.execute(
            text("""
                DELETE FROM users
                 WHERE user_uid NOT IN :uids
            """).bindparams(bindparam("uids", expanding=True)),
            {"uids": list(uids_in_fb)},
        )
        deleted = result.rowcount

        await ses.commit()
        log.info("Deleted %d users (and their bookings) not present in Firebase", deleted)


async def refresh_user_data() -> None:
    """
    Периодически синхронизирует пользователей с Firebase:
    1) upsert новых/изменённых,
    2) Удаляет тех, кого больше нет (и их брони).
    """
    while True:
        try:
            records = list(auth.list_users().iterate_all())
            batch   = [_map_user(r) for r in records]
            uids_fb = {u["user_uid"] for u in batch}

            await _upsert_users(batch)
            await _delete_missing(uids_fb)

        except Exception as exc:
            log.warning("Ошибка синхронизации пользователей: %s", exc, exc_info=True)

        await asyncio.sleep(POLL_INTERVAL)
