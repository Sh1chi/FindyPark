import asyncio
import logging

from firebase_admin import auth
from sqlalchemy import text
from app.db import async_session
from app.models.enums import UserRole, VehicleKind

log = logging.getLogger(__name__)


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


async def save_users_to_db(users: list[dict]) -> None:
    """
    Upsert пользователей в таблицу users.
    """
    async with async_session() as session:
        for u in users:
            await session.execute(text("""
                INSERT INTO users
                  (user_uid, display_name, email, phone, role, vehicle_type, plate)
                VALUES
                  (:user_uid, :display_name, :email, :phone, :role, :vehicle_type, :plate)
                ON CONFLICT (user_uid) DO UPDATE SET
                  display_name = EXCLUDED.display_name,
                  email        = EXCLUDED.email,
                  phone        = EXCLUDED.phone
            """), u)
        await session.commit()
    log.info("Синхронизировано %d пользователей", len(users))


async def refresh_user_data() -> None:
    """
    Фоновая задача: обновление пользователей из Firebase каждые 60 минут.
    """
    while True:
        try:
            # Получаем всех пользователей из Firebase
            records = auth.list_users().iterate_all()
            batch = [_map_user(r) for r in records]
            await save_users_to_db(batch)
        except Exception as e:
            log.warning("Ошибка синхронизации пользователей: %s", e, exc_info=True)
        await asyncio.sleep(60 * 60)
