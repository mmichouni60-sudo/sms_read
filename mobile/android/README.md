# Android prototype — Kotlin

Initial files: SMS BroadcastReceiver, conservative parser, synthetic JUnit fixtures and manifest.

**Not yet a buildable Android Studio project:** Gradle settings, app build configuration, launcher Activity, runtime permission UX, Room database, durable receiver persistence and WorkManager are still required.

## Important
- `RECEIVE_SMS` is a dangerous permission requiring explicit runtime consent.
- The receiver currently parses in memory only and **does not save transactions**. Messages received before durable persistence is added may be lost.
- Real Iranian bank sender IDs and message templates are not yet verified. Synthetic fixtures are examples, not evidence of bank support.
- For multipart messages, production implementation must group by originating address and SMS concatenation metadata rather than blindly joining arbitrary PDUs.
- Never upload raw messages without informed consent; apply retention and encryption controls.
- SMS reception can be restricted by OS/OEM policies and is not guaranteed after force-stop.
- Google Play restricts SMS permissions; evaluate distribution policy before release.
