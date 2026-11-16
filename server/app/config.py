import base64
import os
from functools import lru_cache
from cryptography.fernet import Fernet

@lru_cache
def config():
    connection = os.environ["SQLSERVER_CONNECTION_STRING"]
    public_key = os.environ["JWT_PUBLIC_KEY_PEM"].replace("\\n", "\n")
    encryption_key = os.environ["SMS_ENCRYPTION_KEY_BASE64"]
    if not public_key or not encryption_key:
        raise RuntimeError("JWT_PUBLIC_KEY_PEM and SMS_ENCRYPTION_KEY_BASE64 required")
    Fernet(encryption_key.encode("ascii"))
    return connection, public_key, encryption_key
