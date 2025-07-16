from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.exc import IntegrityError

from app.schemas.user_schema import UserOut, UserUpdateIn
from app.services.user_service import get_user_profile, update_user_profile, sync_user_profile
from app.dependencies.auth import get_current_user

router = APIRouter(prefix="/users", tags=["users"])

@router.post("/sync", response_model=UserOut)
async def api_sync_user(user = Depends(get_current_user)) -> UserOut:
    """
    Явная синхронизация профиля пользователя с Firebase.
    Возвращает обновленные данные пользователя.
    """
    return await sync_user_profile(user["user_uid"])

@router.get("/me", response_model=UserOut)
async def api_get_profile(user = Depends(get_current_user)) -> UserOut:
    """
    Получить текущий профиль пользователя по ID-токену Firebase.
    """
    return await get_user_profile(user["user_uid"])

@router.patch("/me", response_model=UserOut)
async def api_update_profile(
    data: UserUpdateIn,
    user = Depends(get_current_user)
) -> UserOut:
    """
    Обновить поля профиля текущего пользователя.
    """
    try:
        return await update_user_profile(user["user_uid"], data)
    except IntegrityError:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Phone or plate already in use",
        )

