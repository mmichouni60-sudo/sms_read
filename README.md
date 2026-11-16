# SMS Read

Offline-first Android personal finance tracker for Iranian bank SMS.

## Stack
- Android: Kotlin, Room, WorkManager, SMS BroadcastReceiver
- API: Python FastAPI, SQL Server, Apache reverse proxy
- Dates: UTC instants for ordering; Jalali/Shamsi for presentation

## Documents
- [Requirements](docs/PROJECT_REQUIREMENTS.md)
- [Architecture and sync](docs/ARCHITECTURE.md)
- [API contract](docs/API.md)
- [SQL Server schema](database/sqlserver/001_initial.sql)

## Development order
1. Run and review SQL schema on a **development database**.
2. Build authenticated API and idempotent batch ingestion.
3. Build Android receiver, parser fixtures and local Room outbox.
4. Connect WorkManager sync and test offline/duplicate/out-of-order scenarios.

> Status: architecture, schema and API contract only. Android and API runtime are not implemented yet.
