import hashlib
import json
import logging
from datetime import timezone
from fastapi import Depends, FastAPI, HTTPException
from cryptography.fernet import Fernet
from .config import config
from .db import connect
from .models import BatchIn, BatchOut, ItemResult, TransactionIn
from .security import Identity, authenticate

app = FastAPI(title="SMS Read API", version="0.1.0")
logger = logging.getLogger(__name__)

@app.get("/health")
def health():
    return {"status": "ok"}

def canonical_hash(item: TransactionIn) -> bytes:
    canonical = item.model_dump(mode="json", exclude_none=False)
    # Event ID is included: the same ID with different payload must be rejected.
    encoded = json.dumps(canonical, sort_keys=True, ensure_ascii=False, separators=(",", ":")).encode()
    return hashlib.sha256(encoded).digest()

def utc_naive(value):
    if value is None:
        return None
    return value.astimezone(timezone.utc).replace(tzinfo=None)

def insert_one(cursor, identity: Identity, item: TransactionIn, cipher: Fernet) -> ItemResult:
    event_id = str(item.event_id)
    digest = canonical_hash(item)
    cursor.execute(
        "SELECT PayloadHash FROM finance.SmsTransaction WITH (UPDLOCK, HOLDLOCK) WHERE UserId=? AND EventId=?",
        str(identity.user_id), event_id
    )
    previous = cursor.fetchone()
    if previous:
        return ItemResult(event_id=item.event_id, status="duplicate" if bytes(previous[0]) == digest else "rejected",
                          code=None if bytes(previous[0]) == digest else "EVENT_ID_COLLISION")
    # Device registration and ownership must be checked on every request.
    cursor.execute(
        "SELECT 1 FROM finance.Device WHERE DeviceId=? AND UserId=? AND RevokedAtUtc IS NULL",
        str(identity.device_id), str(identity.user_id)
    )
    if cursor.fetchone() is None:
        return ItemResult(event_id=item.event_id, status="rejected", code="DEVICE_NOT_REGISTERED")
    if item.bank_code:
        cursor.execute("SELECT 1 FROM finance.Bank WHERE BankCode=?", item.bank_code)
        if cursor.fetchone() is None:
            return ItemResult(event_id=item.event_id, status="rejected", code="UNKNOWN_BANK")
    parsed = item.bank_code is not None and item.direction is not None and item.amount_irr is not None
    cursor.execute("""
        INSERT INTO finance.SmsTransaction
        (UserId,DeviceId,EventId,AccountId,BankCode,Direction,AmountIrr,BalanceIrr,
         OccurredAtUtc,OccurredAtJalali,TimestampSource,ReceivedAtUtc,SenderCipher,
         RawSmsCipher,PayloadHash,ParserVersion,ParseStatus,Confidence)
        VALUES (?,?,?,NULL,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
    """, str(identity.user_id), str(identity.device_id), event_id, item.bank_code,
         item.direction.value if item.direction else None, item.amount_irr, item.balance_irr,
         utc_naive(item.occurred_at_utc), item.occurred_at_jalali, item.timestamp_source,
         utc_naive(item.received_at_utc), cipher.encrypt(item.sender.encode("utf-8")),
         cipher.encrypt(item.raw_sms.encode("utf-8")), pyodbc_binary(digest),
         item.parser_version, "PARSED" if parsed else "REVIEW", item.confidence)
    return ItemResult(event_id=item.event_id, status="accepted")

def pyodbc_binary(value: bytes):
    import pyodbc
    return pyodbc.Binary(value)

@app.post("/v1/transactions/batch", response_model=BatchOut)
def batch(payload: BatchIn, identity: Identity = Depends(authenticate)):
    _, _, encryption_key = config()
    cipher = Fernet(encryption_key.encode("ascii"))
    results = []
    try:
        with connect() as connection:
            cursor = connection.cursor()
            # SERIALIZABLE ensures uniqueness checks are serialized even under concurrent requests.
            cursor.execute("SET TRANSACTION ISOLATION LEVEL SERIALIZABLE")
            for item in payload.transactions:
                results.append(insert_one(cursor, identity, item, cipher))
            connection.commit()
        return BatchOut(results=results)
    except Exception:
        logger.exception("Batch persistence failed; transaction rolled back")
        raise HTTPException(503, "Temporary database failure")
