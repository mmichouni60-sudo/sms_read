from datetime import datetime
from enum import Enum
from uuid import UUID
from pydantic import BaseModel, ConfigDict, Field, field_validator

class Direction(str, Enum):
    DEPOSIT = "DEPOSIT"
    WITHDRAWAL = "WITHDRAWAL"

class TransactionIn(BaseModel):
    model_config = ConfigDict(extra="forbid")
    event_id: UUID
    bank_code: str | None = Field(default=None, max_length=32)
    direction: Direction | None = None
    amount_irr: int | None = Field(default=None, ge=0)
    balance_irr: int | None = Field(default=None, ge=0)
    account_hint: str | None = Field(default=None, max_length=32)
    occurred_at_utc: datetime | None = None
    occurred_at_jalali: str | None = Field(default=None, max_length=32)
    timestamp_source: str = Field(pattern="^(BANK|RECEIVED|UNKNOWN)$")
    received_at_utc: datetime
    sender: str = Field(max_length=128)
    raw_sms: str = Field(min_length=1, max_length=4096)
    parser_version: str | None = Field(default=None, max_length=50)
    confidence: float | None = Field(default=None, ge=0, le=1)

    @field_validator("occurred_at_utc", "received_at_utc")
    @classmethod
    def require_timezone(cls, value):
        if value is not None and (value.tzinfo is None or value.utcoffset() is None):
            raise ValueError("UTC timestamps must be timezone-aware")
        return value

class BatchIn(BaseModel):
    transactions: list[TransactionIn] = Field(min_length=1, max_length=100)

class ItemResult(BaseModel):
    event_id: UUID
    status: str
    code: str | None = None

class BatchOut(BaseModel):
    results: list[ItemResult]
