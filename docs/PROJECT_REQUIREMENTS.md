# SMS Read — Personal Finance Management

## Original project brief

Design and implement a personal finance management project with the following specifications.

### Phase 1 (Current Priority)
A mobile application (preferably Android using Flutter or Kotlin) that can automatically read bank SMS messages and manage transaction data.

**Core features**
- Automatically read bank SMS messages from Iranian banks, at least Bank Melli, Mellat, Saderat, Parsian, Saman, Pasargad, Tejarat and Keshavarzi.
- Detect bank from sender or message content.
- Extract and store bank name, transaction type (deposit/withdrawal), amount, remaining balance, transaction date/time (Persian/Shamsi), account/card number if available, and full raw SMS text.

**Offline capability and synchronization**
- If server is unavailable, store transactions locally on device.
- Automatically synchronize stored transactions once server connectivity is restored, without user intervention.
- Keep latest balance per account accurate and prevent conflicts.

**Background execution**
- Process new bank SMS while app UI is closed.
- Persist locally and sync automatically when possible.

**Existing infrastructure**
- SQL Server database; Apache web server.
- Backend can use PHP, Node.js, Python or suitable technology compatible with SQL Server and Apache.

**Technical requirements**
- Store and display Persian/Shamsi dates.
- Clean, scalable, secure architecture and proper mobile/server authentication.

**Expected outputs**
1. Overall architecture (mobile, offline layer, synchronization, server).
2. SQL Server database tables and suggested mobile local database.
3. Background SMS reading implementation.
4. Offline/online sync strategy.
5. Sample SMS parsing code for at least three banks.
6. Required API structure.
7. Backend recommendation considering SQL Server and Apache.
8. Suggested libraries/packages.
9. Roadmap for subsequent phases.

## Initial technical direction (proposal, not yet implemented)

- Android native Kotlin, Jetpack Compose, Room/SQLite, BroadcastReceiver, WorkManager, Retrofit/OkHttp.
- Python FastAPI behind Apache reverse proxy; SQL Server via SQLAlchemy and pyodbc.
- Local-first transactional outbox and idempotent batch upload with stable message IDs.
- Server-side uniqueness constraints; latest balance derived by valid transaction chronology, not arrival order.
- Store UTC timestamps alongside Persian calendar representations.
- Bank-specific versioned parsing rules and test fixtures; uncertain matches enter review queue.
- HTTPS, authenticated devices, encryption of sensitive data, no raw SMS in logs.
- Android background execution has OS and OEM restrictions; Google Play SMS permission policies must be evaluated before distribution.

## Proposed roadmap
- P1: repository structure, architecture ADR, database migrations, parser fixtures.
- P2: Android SMS receiver, parser, encrypted local persistence.
- P3: FastAPI authentication and SQL Server ingestion.
- P4: WorkManager synchronization, idempotency and conflict tests.
- P5: Persian UI, account summaries, audit and security hardening.
- Later: categorization, budgets, reports, notifications, multi-device reconciliation.

> This document captures requirements and a proposed implementation direction. No runtime implementation is implied.
