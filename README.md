# Movie App

A movie browser backed by [TMDB](https://www.themoviedb.org/documentation/api), built with Kotlin Multiplatform: business logic, networking, persistence, and DI live in one shared module, while Android (Jetpack Compose) and iOS (SwiftUI + The Composable Architecture) each get their own native UI on top.

## Architecture

I split the repo into three Gradle/Xcode modules:

- **`sharedLogic`** — the Kotlin Multiplatform module (`commonMain` + `androidMain` + `iosMain`), with no UI dependencies at all. It owns:
  - Domain models (`Movie`, `MovieDetails`, `MoviesPage`, `Genre`)
  - Repository interfaces (`MoviesRepository`, `FavoritesRepository`) and their implementations
  - Networking: a Ktor `HttpClient` per platform engine (OkHttp on Android, Darwin on iOS) and a small `ApiClient` base class that turns Ktor/serialization exceptions into a typed `ApiException` (`Http` / `Serialization` / `Network`), always returned as `Result<T>` rather than thrown
  - Persistence: SQLDelight, generating typed Kotlin from the `.sq` schema files
  - DI wiring via Koin (`networkModule`, `repositoryModule`, `databaseModule`), with an `expect/actual platformDatabaseModule` supplying the platform-specific `SqlDriver`
- **`androidApp`** — Jetpack Compose UI, per-screen MVI (`*Contract.kt` defines `State` / `Intent` / `Effect`, a `ViewModel` reduces intents into state and emits one-shot effects), `androidx.navigation3` for the nav graph, Coil for images, `androidyoutubeplayer` for trailers.
- **`iosApp`** — SwiftUI UI, one [The Composable Architecture](https://github.com/pointfreeco/swift-composable-architecture) `@Reducer` per screen (`AppFeature` composes `PopularMoviesFeature` and `FavoritesFeature`; each pushes onto its own `StackState<MoviesPath>` for detail navigation). `sharedLogic` is consumed as a compiled `SharedLogic.framework`; I bridge Kotlin `Flow`s to Swift `AsyncThrowingStream` via [KMP-NativeCoroutines](https://github.com/rickclephas/KMP-NativeCoroutines) so TCA effects can `for await` over them directly, wrapped behind `swift-dependencies` clients (`MoviesClient`, `FavoritesClient`) so features never touch the Kotlin framework or Koin directly.

I kept both UIs intentionally thin: screens read a `State`, dispatch an `Intent`/`Action`, and render. All the fetching, caching, and offline fallback logic lives once in `sharedLogic` and is exercised identically on both platforms.

### Data flow / caching strategy

I made `MoviesRepositoryImpl` DB-first with a 7-day TTL, per category (`popular` / `upcoming` / `top_rated` / `now_playing`) and per movie ID for details:

1. Read whatever is cached (if anything) and emit it immediately, flagged `isStale` if past TTL. This lets both UIs paint instantly from disk instead of blocking on network.
2. If the cached value is fresh and no refresh was forced, stop there — no network call.
3. Otherwise call TMDB. On success, overwrite the cache and emit the fresh value. On failure, do nothing further — the (possibly stale) cached emission from step 1 is what the UI is left with, so the app stays usable offline. Only if there was no cache at all does the failure get emitted as an error.

This is exposed as a `Flow<Result<T>>` per call, which both UIs collect and drive their loading/offline banner state from `isStale`.

Favorites live in a separate SQLDelight table, observed reactively (`observeFavorites()`, `observeIsFavorite(id)`) on both platforms. Android's ViewModels subscribe to that `Flow` directly. On iOS, `IOSFavoritesRepository` exposes the same `Flow`s as `@NativeCoroutines`-bridged streams — the same mechanism `MoviesClient` already used for movies/details — so `PopularMoviesFeature`, `FavoritesFeature`, and `MovieDetailFeature` each subscribe once in `.task` and stay live for as long as that screen exists. A favorite toggled from any screen writes to the same table every other open screen is already subscribed to, so it shows up everywhere with no manual resync wiring on either platform.

### Navigation

- **Android**: a single `MovieNavHost` keeps both tab roots (`Favorites`, `PopularMoviesList`) permanently in the back stack and reorders them on tab switches instead of popping/re-pushing, so each tab's `ViewModel` (scroll position, loaded pages, active filter) survives switching away and back. Detail screens push on top with a custom slide transition (including predictive back) and shared-element transitions from the grid/list thumbnail into the detail poster.
- **iOS**: each tab's `StackState<MoviesPath>` plays the same role — `PopularMoviesFeature` and `FavoritesFeature` each own an independent navigation stack, so switching tabs doesn't lose the other tab's pushed detail screen.

## Technical decisions

- **MVI on Android, TCA on iOS, not a shared presentation layer**: I considered sharing view models/reducers across platforms and decided against it. Compose and SwiftUI have different lifecycle and state-observation models, and forcing one presentation abstraction onto both would fight one platform or the other. Both patterns land on the same shape regardless (unidirectional state, explicit intents/actions, one-shot effects for navigation/errors), so the *logic* stays symmetric even though it's written twice.
- **KMP-NativeCoroutines for the Flow → AsyncThrowingStream bridge**: `sharedLogic` exposes idiomatic Kotlin `Flow`s — `Flow<Result<T>>` for movies/details, `Flow<Set<Long>>`/`Flow<List<Movie>>`/`Flow<Boolean>` for favorites. Rather than inventing my own callback- or completion-handler-based Swift API, I used KMP-NativeCoroutines to generate a native `async`-compatible wrapper for all of it, so iOS effects can `for try await` over the exact same streams Android collects, favorites included.
- **No optimistic UI update on favorite toggle**: `PopularMoviesFeature`, `FavoritesFeature`, and `MovieDetailFeature` fire the toggle and let the live `observeFavoriteIds()`/`observeFavorites()`/`observeIsFavorite()` subscription be the only thing that ever updates `favoriteIds`/`isFavorite` — there's no local flip-then-revert-on-failure dance. This matches `PopularMoviesViewModel`/`FavoritesViewModel`/`MovieDetailsViewModel` on Android, which never had an optimistic update either, and it's safe to drop on iOS too since a local SQLite write plus its `Flow` re-emission round-trips fast enough that the delay isn't perceptible.
- **`Result<T>` instead of thrown exceptions at the repository/API boundary**: every TMDB call and every repository method that can fail returns `Result`. Callers (view models, reducers) handle `onSuccess`/`onFailure` explicitly instead of needing try/catch scattered through UI-adjacent code, and I deliberately rethrow Kotlin's `CancellationException` rather than wrap it so structured concurrency cancellation still works.
- **SQLDelight over Room**: Room isn't multiplatform. SQLDelight generates typed Kotlin from `.sq` files for both the Android (via `AndroidSqliteDriver`) and iOS (via `NativeSqliteDriver`) targets from one schema. No `.sqm` migration files are versioned - the app hasn't shipped, so there's no installed base with an on-disk schema to migrate from; a fresh install just applies the current `.sq` schema directly. That'll change to versioned `.sqm` migrations the moment there's a real installed base to preserve.
- **Cache schema stores flattened columns, not JSON blobs, for movies/pages** (genres on `MovieDetails` are the one exception, which I kept as a `genresJson` column) — page items are relational (`category`, `page`, `position` as part of the key) so pagination and per-page replacement fall out of normal SQL rather than JSON diffing.
- **Koin over manual DI or Dagger/Hilt**: Hilt is Android-only; Koin has first-class KMP support and keeps `commonMain` free of code-generation/annotation-processing, at the cost of losing compile-time verification of the graph (I catch that instead with tests around `initKoin`).
- **BuildKonfig for the TMDB token on the Kotlin side**: `TMDB_API_KEY` is read from `local.properties` at build time via BuildKonfig and injected as a bearer token in Ktor's `defaultRequest`, so it's not hardcoded into `sharedLogic` source. **For this submission I've deliberately committed `local.properties` with a working, read-only TMDB token so you can build and run the app right away without needing your own TMDB key.** I wouldn't do this for any secret that actually mattered in a real production repo — that file would be gitignored, the key would live in CI/keychain-managed config, and a token that ended up in git history like this would get treated as compromised and rotated.
- **Distinct-by-ID list merging instead of naive pagination append**: both `PopularMoviesViewModel` (Android) and `PopularMoviesFeature` (iOS) keep pages in a `Map<Int, List<Movie>>` keyed by page number rather than a flat appended list, because a page can be re-emitted twice (stale cache hit, then the fresh network result for that same page) — keying by page lets the second emission *replace* the first instead of duplicating it, and a final `distinctBy`/`distinctByID()` on movie ID guards against TMDB occasionally shifting the same movie across two page boundaries between requests.
- **Disk-backed poster image cache with an explicit 1-day TTL on iOS**: SwiftUI's `AsyncImage` can't be handed a custom `URLSession`/cache, so on its own it just defers to whatever TTL is implied by TMDB's response headers. I wrapped `CachedAsyncImage`/`PosterImageCache` around a dedicated `URLCache` with an app-owned expiration instead, to match the explicit-TTL approach I already used for the SQLDelight cache. Android gets the same behavior for free from Coil's default disk cache.

## Limitations / not implemented

- **No search.** I only wired up the four TMDB list endpoints (popular/upcoming/top rated/now playing) and detail-by-ID — there's no search-by-title screen.
- **No pull-to-refresh** on either platform — refreshing only happens by re-selecting a filter or via the stale-cache-then-refetch flow; there's no explicit "force refresh" gesture, even though both repository methods already accept a `forceRefresh` flag.
- **No pagination beyond `LoadNextPage` on scroll** — no jump-to-page, no total-count display beyond what drives `canLoadMore`.
- **Favorites and cache are unbounded** — the SQLDelight cache has a TTL but nothing evicts old rows, and favorites has no limit. On a long-lived install this grows without bound.
- **No dependency-injected clock/time abstraction on iOS** — TTL freshness for the Kotlin cache is computed in `sharedLogic` (I can test that with an injected `Clock`), but the Swift side never needed its own clock since it only reads through the shared repository.
- **No CI pipeline** — I've only run the test suites locally via Gradle/`xcodebuild`; there's no GitHub Actions/Fastlane config.
- **Trailer playback has no language filter** — `MovieDetails.trailerKey` comes from TMDB's `videos` append-to-response; `bestTrailerKey()` filters to YouTube and prefers an official trailer, then any trailer, then a teaser, but `VideoDto` doesn't capture TMDB's `iso_639_1` field at all, so a movie with only non-English videos will still play one of those instead of showing "no trailer available".
- **No accessibility pass beyond the basics** — I added content descriptions on key elements (e.g. the trailer player), and the Popular Movies grid card exposes its favorite toggle as a named custom accessibility action (since `.accessibilityElement(children: .combine)` would otherwise fold the nested button into the card and make it unreachable to VoiceOver), but I haven't done a systematic VoiceOver/TalkBack audit beyond that.

## Testing

- `sharedLogic`: unit tests for the repositories (`MoviesRepositoryImplTest`, `FavoritesRepositoryImplTest`, cache-first/TTL/stale-fallback behavior) and DTO→domain mapping, run via `commonTest`/`androidHostTest` against an in-memory SQLDelight driver and a mocked Ktor engine.
- `androidApp`: `ViewModel` tests per screen (`PopularMoviesViewModelTest`, `MovieDetailsViewModelTest`, `FavoritesViewModelTest`) using MockK and Turbine against fake repositories.
- `iosApp`: TCA `TestStore` tests per reducer (`PopularMoviesFeatureTests`, `FavoritesFeatureTests`, `MovieDetailFeatureTests`, `AppFeatureTests`) plus a small `EquatableErrorTests` for the error-bridging helper.

Run them with:

```bash
# Shared logic + Android
./gradlew :sharedLogic:testAndroidHostTest :sharedLogic:iosSimulatorArm64Test
./gradlew :androidApp:testDebugUnitTest

# iOS (from iosApp/, or via Xcode)
xcodebuild test -project iosApp.xcodeproj -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 16'
```

## Running the app

1. Both apps read `TMDB_API_KEY` from `local.properties` at the repo root via BuildKonfig (see the note above — I've committed a working key for this submission).
2. Android: open in Android Studio and run the `androidApp` configuration, or `./gradlew :androidApp:assembleDebug`.
3. iOS: open `iosApp/iosApp.xcodeproj` in Xcode and run. Swift Package dependencies (TCA, swift-dependencies, KMP-NativeCoroutines, youtube-ios-player-helper) resolve automatically; `sharedLogic` builds as a Kotlin framework and links in via the Xcode build phase.

## With more time

- Add search and pull-to-refresh — both are the most obviously "missing" features relative to what TMDB's API supports.
- Add cache eviction (LRU by `cachedAt`, or a max-row cap) so the SQLDelight cache doesn't grow unbounded on a long-lived install.
- Add a CI workflow running the Gradle and `xcodebuild` test suites on PRs, plus lint/ktlint and SwiftFormat/SwiftLint checks.
- Extract the "cache-first, TTL, emit-stale-on-failure" logic in `MoviesRepositoryImpl` into a small reusable helper — the `getMoviesPage`/`getMovieDetails` bodies are structurally identical and only differ in the read/write/fetch calls, which I've left as accepted duplication rather than a premature abstraction.
- A real secrets story: move `TMDB_API_KEY` out of a committed file entirely, inject it via CI secrets for release builds and an untracked local override for development.