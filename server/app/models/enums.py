import enum

from sqlalchemy import Enum as SQLAEnum


class UserRole(str, enum.Enum):
    """
    Роль пользователя в системе.

    Используется для разграничения прав (обычный пользователь, админ, модератор).
    """
    user    = "user"
    admin   = "admin"
    support = "support"


class VehicleKind(str, enum.Enum):
    """
    Тип транспортного средства, указываемого при бронировании парковки.
    """
    car   = "car"
    moto  = "moto"
    truck = "truck"
    bus   = "bus"


# SQLAlchemy-типы, чтобы залинковать их с Postgres-enum’ами
user_role_enum    = SQLAEnum(UserRole, name="user_role", create_type=False)
vehicle_kind_enum = SQLAEnum(VehicleKind, name="vehicle_kind", create_type=False)