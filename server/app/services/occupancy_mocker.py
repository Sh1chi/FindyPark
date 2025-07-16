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
        self.parking_data = {}  # Для хранения текущих значений парковок

    async def initialize_random_occupancy(self):
        """Инициализация случайных значений при первом запуске"""
        async with async_session() as session:
            # Получаем все парковки (или можно ограничить LIMIT 100)
            result = await session.execute(text("""
                SELECT id, capacity FROM parkings
            """))
            parkings = result.all()

            for parking_id, capacity in parkings:
                # Генерируем случайное значение свободных мест
                new_free_spaces = random.randint(0, capacity)
                self.parking_data[parking_id] = {
                    'capacity': capacity,
                    'free_spaces': new_free_spaces
                }

                # Обновляем значение в БД
                await session.execute(text("""
                    UPDATE parkings
                    SET available_spaces = :free_spaces
                    WHERE id = :id
                """), {
                    "free_spaces": new_free_spaces,
                    "id": parking_id
                })
                logger.info(f"Initialized parking {parking_id}: {new_free_spaces}/{capacity}")

            await session.commit()
            self.first_run = False

    async def update_occupancy_incrementally(self):
        """Обновление значений на ±1 от текущего"""
        async with async_session() as session:
            # Берем случайные 100 парковок из уже инициализированных
            parking_ids = list(self.parking_data.keys())
            selected_ids = random.sample(parking_ids, min(100, len(parking_ids)))

            for parking_id in selected_ids:
                data = self.parking_data[parking_id]
                current_free = data['free_spaces']
                capacity = data['capacity']

                # Определяем изменение: +1 или -1 (но не выходим за границы 0..capacity)
                change = random.choice([-1, 1])
                new_free = max(0, min(capacity, current_free + change))

                # Обновляем данные
                self.parking_data[parking_id]['free_spaces'] = new_free

                await session.execute(text("""
                    UPDATE parkings
                    SET available_spaces = :free_spaces
                    WHERE id = :id
                """), {
                    "free_spaces": new_free,
                    "id": parking_id
                })
                logger.info(f"Updated parking {parking_id}: {current_free} → {new_free}/{capacity}")

            await session.commit()

    async def run(self):
        """Основной цикл работы"""
        if self.first_run:
            await self.initialize_random_occupancy()

        while True:
            logger.info("Starting incremental occupancy update...")
            await self.update_occupancy_incrementally()
            await asyncio.sleep(60)  # Ждем 60 секунд


async def main():
    mocker = ParkingOccupancyMocker()
    await mocker.run()


if __name__ == "__main__":
    asyncio.run(main())