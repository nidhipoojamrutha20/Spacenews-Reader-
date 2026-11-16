
# Test and verification results

Verification run on 2026-11-14. Results below reflect commands and emulator behavior observed for this project.

## Build checks

| Command | Result |
|---|---|
| `./gradlew :app:assembleDebug` | Passed; debug APK generated. |
| `./gradlew :app:testDebugUnitTest` | Passed, including mapping, feed de-duplication, bookmark filtering/sorting, and preference tests. |
| `./gradlew :app:lintDebug` | Passed. HTML and SARIF reports are in `app/build/reports/`. |
| `./gradlew :app:connectedDebugAndroidTest` | Passed: 2 instrumentation tests on the existing `Medium_Phone` AVD running Android 17, including the isolated Room 1→2 migration test. |

The three requested Gradle checks were also run together after the implementation changes and completed successfully.

## Live API check

Queried the official `https://api.spaceflightnewsapi.net/v4/schema/` (OpenAPI version 4.37.1) and `GET https://api.spaceflightnewsapi.net/v4/articles/?limit=1`. The returned page included `count`, `next`, `previous`, and `results`; the live article had ID, title, summary, image URL, publisher, publication timestamp, and original URL. The schema documents `search` and the API-provided next-page URL. The app uses these fields and treats optional values as nullable.

## Emulator checks

Run on the existing `Medium_Phone` AVD (Android 17). The final APK was installed with `adb install -r`; app data was not cleared. Captures in `docs/screenshots/` are real emulator screenshots.

- Home displayed current API articles and loaded remote images.
- Explore search for `Mars` returned matching live article titles and summaries after the query was entered.
- Opening a result displayed its image, title, publisher, date, and clearly labeled API summary. Read-aloud and speed controls were visible and interacted with; audio quality/output was not independently assessed.
- Selecting NASA and Mars interests in Settings produced a For You feed with live NASA/Mars search matches.
- A bookmark appeared in Saved. The final APK was force-stopped and reopened without clearing data; the saved story was still present. Removing it from details showed the Saved empty state on return; the same live story was then bookmarked again and restored in Saved.
- With emulator Wi-Fi and data disabled, the saved story opened and its title, publisher/date, and summary remained visible. Wi-Fi and data were re-enabled afterward; airplane mode is off.
- Scrolling Home to the end triggered another API page; older Nov 12 stories appeared after the Nov 13 entries.
- Settings screen showed the selected interest and reading/speech options.

## Not manually verified

The emulator did not confirm the transient offline refresh/search retry message or audible TTS output. Bookmark removal was verified after navigating from details back to Saved; simultaneous presentation across multiple visible screens was not applicable to the single-activity navigation tested. Retry handling is implemented in feed state, and TTS readiness/language/lifecycle behavior is represented in the speech controller. The Room migration and JVM feed/sort logic have focused automated tests. No screenshot or result in this report is fabricated.

## APK

`app/build/outputs/apk/debug/app-debug.apk` is a **debug build**, signed with the local debug key. Do not distribute it as a production release.

---

## Additional verification — 2026-11-16

The following checks were completed after the speech-related changes:

| Command | Result |
|---|---|
| `.\gradlew.bat :app:testDebugUnitTest` | BUILD SUCCESSFUL. |
| `git diff --check` | Passed; no whitespace errors reported. |

### New speech regression tests

Added `PendingSpeechRequestQueueTest.kt` with four test cases covering:

- Storing a pending speech request.
- Replacing an older pending request with a newer request.
- Consuming a request only once.
- Clearing a pending request.

The speech controller and pending request queue changes compiled successfully as part of the Gradle test task.

Audible TTS output and device-level audio-focus behavior have not been newly verified on an emulator or physical device.
