from sqlalchemy import text

from app.db import async_session
from app.schemas.review_schema import ReviewIn, ReviewOut


async def create_review(data: ReviewIn, user_uid: str) -> ReviewOut:
    """
        Создаёт новый отзыв от пользователя на парковку.

        Один пользователь может оставить несколько отзывов на одну парковку.
        Отзыв автоматически считается одобренным (approved = TRUE).

        Args:
            data: Данные отзыва (оценка, комментарий, ID парковки).
            user_uid: Идентификатор пользователя (Firebase UID).

        Returns:
            Сформированный объект ReviewOut с ID и временем создания.
    """
    query = text("""
            INSERT INTO reviews (user_uid, parking_id, rating, comment, approved)
            VALUES (:user_uid, :parking_id, :rating, :comment, TRUE)
            RETURNING id, user_uid, parking_id, rating, comment, created_at;
        """)

    params = {
        "user_uid":    user_uid,
        "parking_id":  data.parking_id,
        "rating":      data.rating,
        "comment":     data.comment,
    }

    async with async_session() as ses:
        row = await ses.execute(query, params)
        await ses.commit()
        return ReviewOut(**row.one()._mapping)


async def list_reviews(parking_id: int, limit: int = 50) -> list[ReviewOut]:
    """
    Возвращает список одобренных отзывов для указанной парковки.

    Args:
        parking_id: ID парковки, для которой запрашиваются отзывы.
        limit: Максимальное количество отзывов (по умолчанию 50).

    Returns:
        Список объектов ReviewOut, отсортированный по убыванию даты создания.
    """
    query = text("""
                 SELECT id, user_uid, parking_id, rating, comment, created_at
                 FROM reviews
                 WHERE parking_id = :parking_id AND approved
                 ORDER BY created_at DESC
                 LIMIT :limit
                 """)

    async with async_session() as ses:
        rows = await ses.execute(query, {"parking_id": parking_id, "limit": limit})
        return [ReviewOut(**r._mapping) for r in rows]
