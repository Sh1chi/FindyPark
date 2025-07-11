import asyncio
import logging

from typing import List, Set
from firebase_admin import auth
from sqlalchemy import text, bindparam

from app.db import async_session
from app.models.enums import UserRole
from app.schemas.user_schema import UserOut, UserUpdateIn

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


async def get_user_profile(user_uid: str) -> UserOut:
    """
    Возвращает профиль пользователя по user_uid.
    """
    async with async_session() as ses:
        row = await ses.execute(
            text("""
                SELECT user_uid, display_name, email, phone, vehicle_type, plate
                  FROM users
                 WHERE user_uid = :uid
            """),
            {"uid": user_uid},
        )
        record = row.first()
    return UserOut(**record._mapping)


async def update_user_profile(user_uid: str, data: UserUpdateIn) -> UserOut:
    """
    Обновляет профиль пользователя по его UID.
    Пустая строка = очистка поля (NULL), за исключением display_name,
    которое при очистке становится 'Без имени'.
    """
    # Забираем только те поля, которые были реально переданы клиентом
    raw = data.model_dump(exclude_unset=True)

    # Пустую строку трактуем как NULL, иначе оставляем как есть
    payload: dict[str, object | None] = {}
    for k, v in raw.items():
        if k == "display_name":
            # '' или None  →  'Без имени'
            payload[k] = v if (isinstance(v, str) and v.strip()) else "Без имени"
        else:
            # для остальных полей '' → NULL
            payload[k] = None if (isinstance(v, str) and v == "") else v

    if not payload:
        # Если нечего обновлять — возвращаем текущий профиль без изменений
        return await get_user_profile(user_uid)

    # Генерируем SET-выражение для SQL запроса
    set_clause = ", ".join(f"{k} = :{k}" for k in payload.keys())
    payload["uid"] = user_uid

    async with async_session() as ses:
        await ses.execute(
            text(f"UPDATE users SET {set_clause} WHERE user_uid = :uid"),
            payload,
        )
        await ses.commit()

    # Возвращаем обновлённые данные пользователя
    return await get_user_profile(user_uid)