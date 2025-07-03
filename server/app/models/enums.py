import enum
from sqlalchemy import Enum as SQLAEnum

# Python-enum для роли пользователя
class UserRole(str, enum.Enum):
    user    = "user"
    admin   = "admin"
    support = "support"

# Python-enum для типа транспорта
class VehicleKind(str, enum.Enum):
    car   = "car"
    moto  = "moto"
    truck = "truck"
    bus   = "bus"

# SQLAlchemy-типы, чтобы залинковать их с Postgres-enum’ами
user_role_enum    = SQLAEnum(UserRole, name="user_role", create_type=False)
vehicle_kind_enum = SQLAEnum(VehicleKind, name="vehicle_kind", create_type=False)