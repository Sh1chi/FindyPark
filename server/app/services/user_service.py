import asyncio
import logging
from typing import List, Set
from firebase_admin import auth
from sqlalchemy import text, bindparam
from fastapi import HTTPException, status

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
        "user_uid": record.uid,
        "display_name": record.display_name or "Без имени",
        "email": record.email,
        "phone": record.phone_number,
        "role": UserRole.user.value,
        "vehicle_type": None,
        "plate": None,
    }


async def _upsert_users(batch: List[dict]) -> None:
    """
    Обновляет или вставляет пользователей из Firebase.
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
    Удаляем из БД тех пользователей, которых уже нет в Firebase.
    """
    if not uids_in_fb:
        return

    async with async_session() as ses:
        await ses.execute(
            text("""
                DELETE FROM bookings
                 WHERE user_uid NOT IN :uids
            """).bindparams(bindparam("uids", expanding=True)),
            {"uids": list(uids_in_fb)},
        )

        result = await ses.execute(
            text("""
                DELETE FROM users
                 WHERE user_uid NOT IN :uids
            """).bindparams(bindparam("uids", expanding=True)),
            {"uids": list(uids_in_fb)},
        )
        deleted = result.rowcount
        await ses.commit()
        log.info("Deleted %d users not in Firebase", deleted)


async def refresh_user_data() -> None:
    """
    Периодически синхронизирует пользователей с Firebase.
    """
    while True:
        try:
            records = list(auth.list_users().iterate_all())
            batch = [_map_user(r) for r in records]
            uids_fb = {u["user_uid"] for u in batch}

            await _upsert_users(batch)
            await _delete_missing(uids_fb)

        except Exception as exc:
            log.error("User sync error: %s", exc, exc_info=True)

        await asyncio.sleep(POLL_INTERVAL)


async def get_user_profile(user_uid: str) -> UserOut:
    """
    Получение профиля пользователя.
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
        if not record:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="User not found"
            )
        return UserOut(**record._mapping)


async def update_user_profile(user_uid: str, data: UserUpdateIn) -> UserOut:
    """
    Обновление профиля пользователя.
    """
    raw = data.model_dump(exclude_unset=True)
    payload: dict[str, object | None] = {}

    for k, value in raw.items():  # Исправлено: v → value
        if k == "display_name":
            payload[k] = value.strip() if isinstance(value, str) and value.strip() else "Без имени"
        else:
            payload[k] = None if isinstance(value, str) and value == "" else value

    if not payload:
        return await get_user_profile(user_uid)

    set_clause = ", ".join(f"{k} = :{k}" for k in payload.keys())
    payload["uid"] = user_uid

    async with async_session() as ses:
        await ses.execute(
            text(f"UPDATE users SET {set_clause} WHERE user_uid = :uid"),
            payload,
        )
        await ses.commit()  # Исправлен отступ

    return await get_user_profile(user_uid)


async def sync_user_profile(user_uid: str) -> UserOut:
    """
    Синхронизация одного пользователя с Firebase.
    """
    try:
        fb_user = auth.get_user(user_uid)

        async with async_session() as session:
            exists = await session.execute(
                text("SELECT 1 FROM users WHERE user_uid = :uid"),
                {"uid": user_uid}
            )

            if not exists.scalar():
                await session.execute(
                    text("""
                    INSERT INTO users (
                        user_uid, display_name, email, 
                        phone, role, vehicle_type, plate
                    ) VALUES (
                        :uid, :name, :email, 
                        :phone, :role, :vehicle_type, :plate
                    )
                    """),
                    {
                        "uid": fb_user.uid,
                        "name": fb_user.display_name or "Без имени",
                        "email": fb_user.email,
                        "phone": fb_user.phone_number,
                        "role": UserRole.user.value,
                        "vehicle_type": None,
                        "plate": None
                    }
                )
                log.info(f"Created new user: {user_uid}")
            else:
                await session.execute(
                    text("""
                    UPDATE users SET
                        display_name = :name,
                        email = :email,
                        phone = :phone
                    WHERE user_uid = :uid
                    """),
                    {
                        "uid": fb_user.uid,
                        "name": fb_user.display_name or "Без имени",
                        "email": fb_user.email,
                        "phone": fb_user.phone_number
                    }
                )
                log.info(f"Updated user data: {user_uid}")

            await session.commit()
            return await get_user_profile(user_uid)

    except auth.UserNotFoundError:
        log.error(f"Firebase user not found: {user_uid}")
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Firebase user not found"
        )
    except Exception as e:
        log.error(f"User sync failed: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Profile synchronization error"
        )