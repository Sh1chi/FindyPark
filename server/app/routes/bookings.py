# app/routes/bookings.py
from fastapi import APIRouter, Depends, HTTPException, status
from app.schemas.booking import BookingIn, BookingOut
from app.services.booking import create_booking, list_bookings

router = APIRouter(prefix="/bookings", tags=["bookings"])

# временная «заглушка» авторизации
def get_current_user():
    # вернёт объект/словарь с user_uid; потом поменяешь на Firebase
    return {"user_uid": "Iyl5Z2iAcag1bNFg9jQgMRxa2e63"}

@router.post("", response_model=BookingOut, status_code=status.HTTP_201_CREATED)
async def api_create_booking(
    data: BookingIn,
    user = Depends(get_current_user),
):
    return await create_booking(data, user["user_uid"])

@router.get("", response_model=list[BookingOut])
async def api_list_bookings(
    user = Depends(get_current_user),
    limit: int = 20,
):
    return await list_bookings(user["user_uid"], limit)
