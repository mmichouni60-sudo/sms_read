# SMS Read Android — early scaffold

Kotlin Android Gradle project (AGP 8.7.3 / Kotlin 2.0.21 / JDK 17), Room, SMS receiver, WorkManager and synthetic parser tests.

## Build
Open `mobile/android` in Android Studio with Android SDK 35 and JDK 17. Sync Gradle and run the `app` configuration. A Gradle wrapper is not yet committed; use Android Studio's installed Gradle or generate the wrapper. Build/tests have **not** been executed or verified.

## Current behavior
- User can request RECEIVE_SMS permission from the launcher screen.
- Receiver attempts to persist incoming SMS in Room using a bounded asynchronous broadcast continuation, then queues constrained WorkManager work.
- Unrecognized SMS also enters REVIEW, which currently means non-bank messages may be stored; **do not deploy to real devices before filtering and explicit consent UX are improved**.
- WorkManager currently returns retry when pending data exists. **No server transmission exists yet.**
- Room data is **not encrypted**. Never test with real private bank SMS until encrypted persistence and data deletion controls are added.
- The prototype uses a UUID per broadcast; redelivery can create duplicates. Production needs stable deduplication and multipart handling.
- SMS delivery and execution after force-stop are not guaranteed. Google Play restricts SMS permission use.
- No production claim: this is an untested implementation scaffold, not a release build.

## Next
1. Bank sender allowlist and explicit consent, encrypted persistence, lifecycle tests.
2. Real redacted fixtures for banks; account identity and Jalali timestamp parser.
3. FastAPI auth and idempotent batch endpoint.
4. Replace retry-only worker with authenticated upload, partial ACK and backoff tests.
