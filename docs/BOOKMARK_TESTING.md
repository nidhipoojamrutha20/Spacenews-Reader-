# SpaceNews Reader — Bookmark Testing

## Module

Bookmarks, Room database and saved-article management.

## Test scenarios

1. Save an article from Home or Explore.
2. Open Saved and verify that the article is displayed.
3. Search for a saved article by title.
4. Test uppercase and lowercase search terms, and extra spaces.
5. Test bookmark sorting by date and title.
6. Open a saved article after restarting the application.
7. Remove a bookmark and verify that it disappears.
8. Check Room migration behavior using instrumentation tests.

## Implementation notes

- Bookmarks are stored locally with Room (`SpaceNewsDatabase`, version 2).
- `MIGRATION_1_2` adds a nullable `savedAt` column. Bookmarks saved before
  the upgrade keep a null save time and sort after timestamped ones.
- Saved-article search (`filterAndSortBookmarks`) trims the query, ignores
  letter case, and collapses repeated spaces/tabs in both the query and the
  title, so "  MARS   rover " matches "Mars Rover".

## Automated tests added

- `BookmarkSearchTest` (unit): blank/whitespace queries, leading and trailing
  spaces, mixed case, repeated spaces or tabs in query and title, no-match.
- `BookmarkMigrationTest.savedArticlesSurviveUpgradeAndRemainReadableThroughRoom`
  (instrumentation): creates a real version-1 database with two bookmarks,
  opens it through Room with `MIGRATION_1_2`, and verifies every field is
  preserved, legacy `savedAt` stays null on re-save, new saves get a
  timestamp, and removal still works.

## Verification

| Check | Command / method | Result |
|---|---|---|
| Unit tests | `./gradlew :app:testDebugUnitTest` | 14 tests, BUILD SUCCESSFUL |
| Instrumentation tests | `./gradlew :app:connectedDebugAndroidTest` on emulator "minion" (Android 16) | 3/3 passed, BUILD SUCCESSFUL |
| Manual: scenarios 1–7 | Emulator | ___ |

Tested by Pranavi on 16 November 2026.
