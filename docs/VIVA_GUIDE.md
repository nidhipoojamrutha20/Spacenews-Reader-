# SpaceNews viva guide

## What the app does

SpaceNews reads current spaceflight article metadata from the Spaceflight News API. It does not copy the complete publisher article: the API summary is labeled as a summary and the original publisher link is available separately. Users can save article text and metadata in a local Room database.

## Important classes

- `SpaceflightNewsApi`: Retrofit declarations for the article list, detail, and API-provided next-page URL.
- `SpaceflightNewsDto` / `ApiPage`: Gson models for nullable article fields and the paginated response envelope.
- `NewsRepository`: maps API DTOs to the app model, applies safe fallbacks, and exposes search/page requests.
- `Article`: shared app data model for a news story and optional bookmark timestamp.
- `NewsViewModel`: owns Home, Explore, and For You state; debounces/cancels searches; refreshes and paginates; observes saved article IDs and coordinates bookmark actions.
- `BookmarkEntity`, `BookmarkDao`, `BookmarkRepository`: Room row, SQL access/Flow observations, and save/remove behavior. Article ID is the primary key; insert uses replace to avoid duplicates. The repository preserves existing `savedAt` values.
- `SpaceNewsDatabase`: one application-scoped database and a 1→2 migration that adds nullable `savedAt`; older saved rows keep a null value rather than receiving a made-up date.
- `ThemePreferences` / `SettingsViewModel`: SharedPreferences-backed theme, reading size, speech speed, and interest selection exposed as StateFlows.
- `ArticleSpeechController`: one TextToSpeech engine per article-details lifetime, readiness/language checks, utterance status, audio focus, stop on background, and shutdown on disposal.
- `SpaceNewsApp` / `NewsScreens`: Compose navigation, lifecycle-aware state collection, feed screens, and presentation.

## Useful concepts and syllabus mappings

- **REST and JSON:** Retrofit calls, Gson serialization, API DTOs, HTTP/error handling.
- **Coroutines:** suspending network/Room calls, debounce delay, cancellation, concurrent interest requests.
- **MVVM:** ViewModels expose state; Composables render state and send user actions.
- **Reactive state:** `StateFlow`, Room `Flow`, `collectAsStateWithLifecycle`.
- **Persistence:** Room entity/DAO/database, a data-preserving schema migration, SharedPreferences for simple settings.
- **Compose:** reusable Composables, state hoisting, lifecycle-aware collection, lazy lists, Material 3.
- **Data processing:** deduplicate by API article ID, sort recent publication/save time, local filtering.
- **Android platform APIs:** TextToSpeech, audio focus, lifecycle callbacks, intents to view/share publisher links.
- **Testing:** JVM tests for model mapping, deduplication, bookmark filtering/sort; an Android instrumentation test for nullable timestamp migration.

## Likely questions

**Why is the API summary not shown as the complete story?** The endpoint supplies a discovery summary and source URL, not the publisher’s full article text. The UI labels this distinction and links to the original.

**How does pagination work?** The response’s `next` URL is passed to Retrofit as-is. The UI requests another page near the end of the list and merges records by ID.

**How are Explore topics implemented?** The API offers general text search, not a topic category property. Topic chips are named searches and keyword text uses the same supported search parameter.

**How does search avoid stale results?** Input is debounced, active requests are cancelled, and monotonically increasing request tokens prevent older responses from replacing the current query.

**Why is `savedAt` nullable?** Existing schema-v1 bookmarks have no save timestamp. The migration adds a nullable column without fabricating history.

**How are duplicate bookmarks prevented?** Room uses article `id` as its primary key; repository operations are serialized and inserts replace the same ID.

**Does saved content work offline?** Stored title, summary, publisher, publication date and URL metadata do. Remote images are not guaranteed cached, and opening the source URL requires network access.

**Is For You AI?** No. It is a transparent, interest-based feed assembled from separate API search results.

**Does read aloud need microphone permission?** No. Android TextToSpeech synthesizes available summary text; the app checks engine/language readiness and shuts the engine down with the details screen.
