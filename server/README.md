# FastAPI server — development prototype

## Setup
1. Create a dedicated SQL Server development database, review and run `database/sqlserver/001_initial.sql`.
2. Install Microsoft ODBC Driver 18, Python 3.11+ and `pip install -r server/requirements.txt`.
3. Set `SQLSERVER_CONNECTION_STRING`, `JWT_PUBLIC_KEY_PEM` (RS256 verification public key) and `SMS_ENCRYPTION_KEY_BASE64` (generate using `cryptography.fernet.Fernet.generate_key()`).
4. Start from repo root: `uvicorn server.app.main:app --host 127.0.0.1 --port 8000`. Create `server/__init__.py` if needed for your packaging setup.
5. Apache should terminate TLS and reverse proxy only to localhost port 8000.

## Current limits — NOT production ready
- No login, registration or token issuance endpoint; provisioning AppUser and Device and issuing RS256 tokens must be built securely.
- The batch API encrypts raw SMS and sender before storage and checks device registration.
- Events remain unassigned to accounts. **AccountBalance is not updated** until verified account identity and safe snapshot reconciliation are implemented.
- Partial per-item validation is supported for unknown bank, revoked device and event collision, but unexpected SQL errors roll back the whole batch.
- Application-level Fernet key rotation and secrets management are not yet implemented.
- No request rate limiting, audit logs, migration runner or integration tests yet.
- `/health` only confirms process availability, not database health.
- This code has not been executed against SQL Server and requires tests before deployment.
