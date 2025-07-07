# app/schemas/bookings.py
from datetime import datetime
from pydantic import BaseModel, Field, validator
from app.models.enums import VehicleKind
from uuid import UUID

class BookingIn(BaseModel):
    parking_id:  int
    vehicle_type: VehicleKind
    plate:       str = Field(min_length=2, max_length=12)
    ts_from:     datetime
    ts_to:       datetime

    @validator("ts_to")
    def _later_than_from(cls, v, values):
        if "ts_from" in values and v <= values["ts_from"]:
            raise ValueError("ts_to must be after ts_from")
        return v

class BookingOut(BookingIn):
    id:       UUID          # UUID в строке
    user_uid: str
