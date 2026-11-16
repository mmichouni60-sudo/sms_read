from dataclasses import dataclass
from uuid import UUID
import jwt
from fastapi import Header, HTTPException
from .config import config

@dataclass(frozen=True)
class Identity:
    user_id: UUID
    device_id: UUID

def authenticate(authorization: str = Header(...), x_device_id: str = Header(...)) -> Identity:
    if not authorization.startswith("Bearer "):
        raise HTTPException(401, "Bearer token required")
    try:
        _, public_key, _ = config()
        claims = jwt.decode(
            authorization[7:], public_key, algorithms=["RS256"],
            audience="sms-read-api", issuer="sms-read", options={"require": ["sub", "exp", "iat"]}
        )
        user_id = UUID(claims["sub"])
        device_id = UUID(x_device_id)
        if claims.get("device_id") != str(device_id):
            raise ValueError("device binding mismatch")
        return Identity(user_id, device_id)
    except (jwt.PyJWTError, ValueError, KeyError) as exc:
        raise HTTPException(401, "Invalid authentication") from exc
