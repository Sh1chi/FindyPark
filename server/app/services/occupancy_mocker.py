import asyncio
import random
from app.db import async_session
from sqlalchemy import text



class ParkingOccupancyMocker:
    def __init__(self):
        self.first_run = True

    async def initialize_random_occupancy(self):
        """Инициализация с учетом текущих значений в БД"""
        async with async_session() as session:
            # Получаем текущее состояние из БД
            result = await session.execute(text("""
                SELECT id, capacity, available_spaces 
                FROM parkings
            """))
            parkings = result.all()

            for parking_id, capacity, current_spaces in parkings:
                # Устанавливаем случайное значение  не превышающее capacity
                new_free_spaces = random.randint(0, capacity)

                # Обновляем только если значение изменилось
                if new_free_spaces != current_spaces:
                    await session.execute(text("""
                        UPDATE parkings
                        SET available_spaces = :free_spaces
                        WHERE id = :id
                    """), {
                        "free_spaces": new_free_spaces,
                        "id": parking_id
                    })

            await session.commit()

    async def update_occupancy_incrementally(self):
        """Обновление с учетом текущих значений в БД"""
        async with async_session() as session:
            # 1. Получаем текущее состояние из БД
            result = await session.execute(text("""
                SELECT id, capacity, available_spaces
                FROM parkings
                ORDER BY random()
                LIMIT 100
            """))
            parkings = result.all()

            updates = []
            for parking_id, capacity, current_spaces in parkings:
                #  +1 или -1
                change = random.choice([-1, 1])
                new_free = max(0, min(capacity, current_spaces + change))

                # Сохраняем для пакетного обновления
                updates.append({
                    "id": parking_id,
                    "free_spaces": new_free,
                    "old_spaces": current_spaces,
                    "capacity": capacity
                })

            # Пакетное обновление
            for update in updates:
                await session.execute(text("""
                    UPDATE parkings
                    SET available_spaces = :free_spaces
                    WHERE id = :id
                """), {
                    "free_spaces": update["free_spaces"],
                    "id": update["id"]
                })

            await session.commit()

    async def run(self):
        """Основной цикл работы"""
        if self.first_run:
            await self.initialize_random_occupancy()
            self.first_run = False

        while True:
            await self.update_occupancy_incrementally()
            await asyncio.sleep(60)  # Ждем 60 секунд


async def main():
    mocker = ParkingOccupancyMocker()
    await mocker.run()


if __name__ == "__main__":
    asyncio.run(main())