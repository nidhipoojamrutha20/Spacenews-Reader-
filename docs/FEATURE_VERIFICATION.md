# SpaceNews Reader — Feature Verification

## Implemented application features

| Feature | Implementation | Verification |
|---|---|---|
| Home news feed | Spaceflight News API | Open Home and load articles |
| Explore | Keyword and topic search | Search for Mars or NASA |
| Article details | Title, image, publisher and summary | Open a news article |
| For You | Interest-based news matching | Select interests in Settings |
| Bookmarks | Room database | Save and reopen articles |
| Saved search | Local bookmark filtering | Search saved article titles |
| Bookmark sorting | Date and title sorting | Change Saved sort order |
| Themes | System, light and dark | Change theme and restart |
| Reading preferences | Text-size settings | Change reading size |
| Read aloud | Android TextToSpeech | Play and stop a summary |
| Pagination | API next-page loading | Scroll through Home |
| Error handling | Retry states | Test with network disabled |

## Architecture

- Kotlin and Jetpack Compose for the interface
- Retrofit and OkHttp for API communication
- Room for persistent bookmarks
- ViewModels and coroutines for application state
- SharedPreferences for user preferences
- Android TextToSpeech for summary playback

## Verification status

The application has previously passed a local debug
build and JVM unit-test run.

All functionality must also be demonstrated on an
emulator or physical device before final submission.
