# API contract (proposed v1)

All endpoints HTTPS; JSON UTF-8. Authorization: Bearer access token bound to user and registered device. Refresh tokens rotated and securely stored. Requests carry X-Device-Id. Device registration requires user authentication; device ID alone is not authentication.

## POST /v1/auth/device/register
Registers device after interactive user login. Returns device-scoped credentials (exact login method to be decided).

## POST /v1/transactions/batch
Idempotent partial-success batch, maximum 100 items; each item includes:
```json
{
  "event_id": "d0c983a6-3ca2-4f7e-997e-1a31c8149932",
  "bank_code": "MELLAT",
  "direction": "DEPOSIT",
  "amount_irr": 1500000,
  "balance_irr": 4500000,
  "currency": "IRR",
  "account_hint": "****1234",
  "occurred_at_utc": "2026-11-16T07:00:00Z",
  "occurred_at_jalali": "1405/08/25 10:30",
  "timestamp_source": "BANK",
  "received_at_utc": "2026-11-16T07:00:02Z",
  "sender": "BANK",
  "raw_sms": "example redacted text",
  "parser_version": "mellat-1",
  "confidence": 0.98
}
```
Response:
```json
{
  "results": [
    {"event_id": "d0c983a6-3ca2-4f7e-997e-1a31c8149932", "status": "accepted"},
    {"event_id": "another-uuid", "status": "duplicate"},
    {"event_id": "another-uuid", "status": "rejected", "code": "VALIDATION_ERROR"}
  ]
}
```
Server must validate event ownership, bank code, amount, limits, dates, sender length, and payload size. Replay of an existing event_id with different canonical payload is a conflict, not a duplicate. Use one SQL transaction per item or isolated savepoints; never ACK an uncommitted item.

## GET /v1/transactions?cursor=...&limit=...
Paginated transactions scoped to authenticated user.

## GET /v1/accounts
Accounts with latest trusted balance snapshot, timestamp, source and reconciliation status.

## GET /v1/sync/status
Last accepted sync and pending server reconciliation count.

Errors: 401 unauthorized, 403 device revoked, 409 event ID collision, 413 payload too large, 422 validation failure, 429 rate limit, 503 temporary unavailable. Use request IDs; redact sensitive data from logs.
