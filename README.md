# QuizLock (Android)

An app locker that asks a short Swedish question before a locked app opens. Built with Kotlin and Jetpack Compose (Material 3).

This is the **Android spike**: real locking and a working quiz, with 12 hard-coded Swedish words (`core/SpikeDeck.kt`). The quiz-pack engine and the Swedish SQLite pack replace the spike deck next.

## Run it

1. Open this folder in Android Studio (**File › Open**). Let Gradle sync. If Studio offers to upgrade AGP, Kotlin or the Compose BOM, accepting is fine.
2. Pick a **real phone** (Android 8.0+, API 26) and press Run. An emulator works for the quiz screens, but usage events are unreliable there.
3. In the app, grant **Usage access** and **Display over other apps**. Notifications and Unrestricted battery are recommended.
4. **Choose apps to lock** › Save, then turn **Ask a question before locked apps open** on.
5. Open a locked app and the quiz appears over it:
   - A right answer opens the app.
   - A wrong answer shows the right one, then gives you *Another question* or *Skip & open*.
   - Back takes you to the home screen.
6. If something is off, check **Debug log** in the app, or Logcat with the tag `QuizLock`.

Unit tests for the core logic: `./gradlew test`

## How it works

| Piece | File | Job |
| --- | --- | --- |
| Foreground detection | `lock/MonitorService.kt` + `core/ForegroundTracker.kt` | Foreground service (`specialUse`) polls `UsageStatsManager` every 700 ms |
| When to quiz | `core/LockPolicy.kt` | Quiz only when you **enter** a locked app that isn't in its grace window |
| Quiz | `ui/quiz/QuizActivity.kt`, `QuizScreen.kt` | Own task, hidden from Recents; finishing it reveals the app underneath |
| Settings + state | `data/LockStore.kt` | Locked apps, grace window, unlocks (SharedPreferences) |
| Restart | `lock/BootReceiver.kt` | Restarts monitoring after a reboot or app update |
| App list | `data/InstalledApps.kt` | Launcher apps via `<queries>` (no `QUERY_ALL_PACKAGES`) |

No AccessibilityService is used, because Google Play rejects blockers that use it.

## Spike checks (write results in the plan doc)

- [ ] Delay between opening a locked app and the quiz appearing
- [ ] Quiz appears on Android 14, 15 and 16 (background activity start relies on the overlay permission)
- [ ] Right answer / Skip returns to the locked app, not the home screen
- [ ] Re-entering within the grace window doesn't ask; after it, it asks again
- [ ] Locking survives a reboot and a few hours with the screen off (battery savers!)
- [ ] The service stops cleanly if usage access is revoked

## Versions

AGP 8.10.1 · Kotlin 2.1.21 · Compose BOM 2025.05.01 · compileSdk/targetSdk 36 · minSdk 26 · Gradle 8.14.3 · JDK 17+
# quiz-lock-android
