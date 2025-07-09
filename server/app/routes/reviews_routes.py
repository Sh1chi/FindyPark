from fastapi import APIRouter, Depends, status

from app.schemas.review_schema import ReviewIn, ReviewOut
from app.services.review_service import create_review, list_reviews
from app.routes.bookings_routes import get_current_user

router = APIRouter(prefix="/reviews", tags=["reviews"])


@router.post("", response_model=ReviewOut, status_code=status.HTTP_201_CREATED)
async def api_create_review(payload: ReviewIn,user=Depends(get_current_user)) -> ReviewOut:
    """
    Создаёт новый отзыв от пользователя.

    Каждый запрос сохраняет отдельный отзыв, даже если пользователь
    уже оставлял комментарий к этой парковке ранее.

    Args:
        payload: Данные отзыва (ID парковки, рейтинг, комментарий).
        user: Текущий авторизованный пользователь (определяется через Firebase ID-токен).

    Returns:
        Созданный отзыв (ReviewOut).
    """
    return await create_review(payload, user["user_uid"])


@router.get("/parkings/{parking_id}", response_model=list[ReviewOut])
async def api_list_reviews(parking_id: int, limit: int = 50) -> list[ReviewOut]:
    """
    Возвращает список одобренных отзывов по указанной парковке.

    Args:
        parking_id: ID парковки.
        limit: Максимальное количество отзывов (по умолчанию 50).

    Returns:
        Список отзывов, отсортированных по дате создания (от новых к старым).
    """
    return await list_reviews(parking_id, limit)
