import asyncio
import random
import logging
from app.db import async_session
from sqlalchemy import text

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("occupancy_mocker")


class ParkingOccupancyMocker:
    def __init__(self):
        self.first_run = True

    async def initialize_random_occupancy(self):
        """Инициализация случайными значениями при первом запуске"""
        async with async_session() as session:
            result = await session.execute(text("""
                SELECT id, capacity 
                FROM parkings
            """))
            parkings = result.all()

            for parking_id, capacity in parkings:
                new_free_spaces = random.randint(0, capacity)
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
            result = await session.execute(text("""
                SELECT id, capacity, available_spaces
                FROM parkings
                ORDER BY random()
                LIMIT 12001
                FOR UPDATE
            """))
            parkings = result.all()

            updates = []
            for parking_id, capacity, current_spaces in parkings:
                # С вероятностью 50% увеличиваем загруженность (уменьшаем свободные места)
                if random.random() < 0.5:
                    # Уменьшаем свободные места
                    decrease_amount = random.randint(1, 1)
                    new_free = max(0, current_spaces - decrease_amount)
                else:
                    # С вероятностью 25% уменьшаем загруженность (увеличиваем свободные места)
                    new_free = min(capacity, current_spaces + 1)

                # Сохраняем только если значение изменилось
                if new_free != current_spaces:
                    updates.append({
                        "id": parking_id,
                        "free_spaces": new_free,
                        "old_spaces": current_spaces
                    })

            # Пакетное обновление
            for update in updates:
                await session.execute(text("""
                    UPDATE parkings
                    SET available_spaces = :free_spaces
                    WHERE id = :id
                    AND available_spaces = :old_spaces
                """), {
                    "free_spaces": update["free_spaces"],
                    "id": update["id"],
                    "old_spaces": update["old_spaces"]
                })

            await session.commit()

    async def run(self):
        """Основной цикл работы"""
        if self.first_run:
            logger.info("Delaying first run for 2 minutes to allow data loading...")
            await asyncio.sleep(120)  # Ждем 2 минуты перед первой инициализацией
            logger.info("Starting initial occupancy initialization")
            await self.initialize_random_occupancy()
            self.first_run = False
            logger.info("Initial occupancy initialization completed")

        while True:
            logger.info("Starting incremental occupancy update...")
            await self.update_occupancy_incrementally()
            await asyncio.sleep(30)  # Ждем 30 секунд


async def main():
    mocker = ParkingOccupancyMocker()
    await mocker.run()


if __name__ == "__main__":
    asyncio.run(main())