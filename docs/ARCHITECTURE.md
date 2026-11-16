# Architecture and synchronization (Phase 1)

```text
Android SMS_RECEIVED broadcast
  -> validate sender + message (bank-specific rules)
  -> Room transaction: save raw SMS, parsed transaction, outbox PENDING
  -> WorkManager unique work with NetworkType.CONNECTED
  -> HTTPS POST /v1/transactions/batch
  -> FastAPI authentication and validation
  -> SQL Server transaction + unique (user_id, event_id)
  -> per-item ACK
  -> Room mark ACKED
```

## Guarantees and constraints
- Persist locally **before** attempting network transmission. A failed parse is stored with REVIEW status rather than silently discarded.
- Stable UUID event_id is generated once at first receipt and never regenerated for retries.
- Server deduplicates on (user_id,event_id), verifies payload fingerprint on replay, and returns conflict for same ID with different data.
- Acknowledgement is committed locally only after a successful server response. Partial batch failures remain pending.
- WorkManager schedules constrained retries with exponential backoff; enqueue immediate work on new SMS, network return, app start and device boot where permitted. No guarantee of execution after user force-stop or under OEM battery restrictions.
- Receiver must finish quickly: do minimal parsing and local persistence using goAsync() with bounded execution, or hand off to an expedited worker; never depend on a long-lived foreground service.
- Android SMS permission is sensitive and Google Play has restricted policies. Review eligibility and consent before publishing; do not bypass permission restrictions.
- Parse timestamps into UTC with explicit Asia/Tehran context and retain original Jalali text. When SMS omits a timestamp, use received_at and mark timestamp_source=RECEIVED, not BANK.
- Amounts and balances are integer IRR minor denomination as actually represented by the message; explicitly capture unit (IRR or TOMAN) and convert to IRR using an exact factor of 10. Do not assume unknown units.
- Account identity: prefer stable verified account/card identifiers. Masked card tails alone cannot uniquely identify an account; unresolved transactions remain unassigned until user review.
- Balance is a reported snapshot, **not** computed from deposits minus withdrawals when bank-reported balances exist. Update latest account balance only for a newer trusted bank timestamp; equal timestamps or ambiguous ordering require reconciliation. A late-arriving older SMS must never overwrite a newer balance.
- Encryption at rest for raw SMS and account identifiers on mobile and server; use Android Keystore-backed key management and server-side key management. Do not log raw SMS, tokens or card identifiers.
- Keep immutable transaction records; user annotations/categorization are separate mutable data with versioning.

## Next implementation tasks
1. Create Gradle Android project and manifest receiver.
2. Implement parser interfaces and real, redacted bank SMS test fixtures.
3. Implement Room entities and migration tests.
4. Implement FastAPI auth and batch endpoint with pyodbc/SQLAlchemy.
5. Add integration tests for duplicate, out-of-order, ambiguous account, offline retry and token expiry.
