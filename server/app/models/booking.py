# app/models/bookings.py

from enum import Enum
from datetime import datetime

class VehicleKind(str, Enum):
    car = "car"
    motorcycle = "motorcycle"
    truck = "truck"

class Booking:
    def __init__(self, id, user_uid, parking_id, vehicle_type, plate, ts_from, ts_to):
        self.id = id
        self.user_uid = user_uid
        self.parking_id = parking_id
        self.vehicle_type = vehicle_type
        self.plate = plate
        self.ts_from = ts_from
        self.ts_to = ts_to

    def to_dict(self):
        return {
            "id": self.id,
            "user_uid": self.user_uid,
            "parking_id": self.parking_id,
            "vehicle_type": self.vehicle_type,
            "plate": self.plate,
            "ts_from": self.ts_from.isoformat(),
            "ts_to": self.ts_to.isoformat()
        }
