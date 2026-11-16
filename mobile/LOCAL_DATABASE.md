# Android local Room database (design)

## SmsEvent
- eventId: UUID primary key
- senderCipher, rawSmsCipher: encrypted bytes
- receivedAtUtc: Instant
- bankCode, direction, amountIrr, balanceIrr: nullable parsed fields
- occurredAtUtc, occurredAtJalali, timestampSource
- accountHintCipher, parserVersion, parseStatus, confidence
- payloadHash: stable SHA-256 of canonical immutable payload
- syncStatus: PENDING | IN_FLIGHT | ACKED | REVIEW
- attemptCount, nextAttemptAtUtc, lastErrorCode

## Outbox
- eventId: FK SmsEvent, unique
- createdAtUtc, lastAttemptAtUtc, leaseUntilUtc
- worker selects PENDING and expired IN_FLIGHT; ACKED rows are retained locally.

## BankRule
- bankCode, senderPattern, contentPattern, parserVersion, enabled
- sender alone is not proof of authenticity; require content validation and manual review on ambiguity.

## Synchronization
- Room transaction saves SmsEvent and Outbox atomically.
- WorkManager unique work + network constraint + exponential retry.
- Only ACK after server confirms accepted/duplicate.
- Do not use SMS timestamp alone for deduplication; repeated identical bank SMS may represent separate real transactions.
- Use Android Keystore-backed encryption strategy and test backup/restore key lifecycle.
