package dev.themobiledev.movie.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.turbine.test
import dev.themobiledev.movie.db.MovieAppDatabase
import dev.themobiledev.movie.domain.MoviesRepository
import dev.themobiledev.movie.network.ApiException
import dev.themobiledev.movie.network.createHttpClient
import dev.themobiledev.movie.network.service.MoviesApi
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

class MoviesRepositoryImplTest {

    private class FakeClock(private var instant: Instant) : Clock {
        override fun now(): Instant = instant
        fun advanceBy(duration: Duration) {
            instant += duration
        }
    }

    /** The final (authoritative) value of a cache-then-network flow, unwrapped. */
    private suspend fun <T> Flow<Result<T>>.finalValue(): T = last().getOrThrow()

    private fun newRepository(engine: MockEngine, clock: Clock = Clock.System): MoviesRepository {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        MovieAppDatabase.Schema.create(driver)
        return MoviesRepositoryImpl(MoviesApi(createHttpClient(engine)), MovieAppDatabase(driver), clock)
    }

    private fun MockRequestHandleScope.jsonResponse(content: String, status: HttpStatusCode = HttpStatusCode.OK) =
        respond(
            content = content,
            status = status,
            headers = headersOf(HttpHeaders.ContentType, "application/json"),
        )

    private fun popularMoviesResponse(id: Long = 1, title: String = "Movie One") = """
        {
          "page": 1,
          "results": [
            {"id": $id, "title": "$title", "overview": "Overview one", "poster_path": "/one.jpg", "release_date": "2024-01-01", "vote_average": 7.5}
          ],
          "total_pages": 5,
          "total_results": 100
        }
    """.trimIndent()

    private fun movieDetailsResponse(genresJson: String = """[{"id": 1, "name": "Action"}]""") = """
        {
          "id": 42,
          "title": "Details Movie",
          "overview": "Overview",
          "tagline": "Tagline",
          "runtime": 120,
          "budget": 1000,
          "revenue": 5000,
          "homepage": "https://example.com",
          "genres": $genresJson,
          "poster_path": "/p.jpg",
          "backdrop_path": "/b.jpg",
          "release_date": "2024-05-01",
          "vote_average": 8.1,
          "vote_count": 900
        }
    """.trimIndent()

    @Test
    fun getPopularMovies_mapsResponseToDomain() = runTest {
        val engine = MockEngine { _ -> jsonResponse(popularMoviesResponse()) }

        val page = newRepository(engine).getPopularMovies(page = 1).finalValue()

        assertEquals(1, page.page)
        assertEquals(5, page.totalPages)
        assertEquals(100, page.totalResults)
        assertFalse(page.isStale)
        assertEquals(1, page.movies.size)
        with(page.movies.first()) {
            assertEquals(1L, id)
            assertEquals("Movie One", title)
            assertEquals("Overview one", overview)
            assertEquals("/one.jpg", posterPath)
            assertEquals("2024-01-01", releaseDate)
            assertEquals(7.5, voteAverage)
        }
    }

    @Test
    fun getPopularMovies_httpErrorSurfacesAsApiExceptionHttp() = runTest {
        val engine = MockEngine { _ ->
            respond(
                content = """{"status_message": "Invalid API key", "status_code": 7}""",
                status = HttpStatusCode.Unauthorized,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val result = newRepository(engine).getPopularMovies().last()

        val exception = assertIs<ApiException.Http>(result.exceptionOrNull())
        assertEquals(401, exception.code)
    }

    @Test
    fun getUpcomingMovies_requestsUpcomingEndpointAndMapsResponse() = runTest {
        var capturedUrl = ""
        val engine = MockEngine { request ->
            capturedUrl = request.url.toString()
            jsonResponse(popularMoviesResponse(id = 2, title = "Movie Two"))
        }

        val page = newRepository(engine).getUpcomingMovies(page = 1).finalValue()

        assertEquals(true, capturedUrl.contains("movie/upcoming"))
        assertEquals(1, page.movies.size)
        assertEquals("Movie Two", page.movies.first().title)
    }

    @Test
    fun getTopRatedMovies_requestsTopRatedEndpointAndMapsResponse() = runTest {
        var capturedUrl = ""
        val engine = MockEngine { request ->
            capturedUrl = request.url.toString()
            jsonResponse(popularMoviesResponse(id = 3, title = "Movie Three"))
        }

        val page = newRepository(engine).getTopRatedMovies(page = 1).finalValue()

        assertEquals(true, capturedUrl.contains("movie/top_rated"))
        assertEquals(1, page.movies.size)
        assertEquals("Movie Three", page.movies.first().title)
    }

    @Test
    fun getNowPlayingMovies_requestsNowPlayingEndpointAndMapsResponse() = runTest {
        var capturedUrl = ""
        val engine = MockEngine { request ->
            capturedUrl = request.url.toString()
            jsonResponse(popularMoviesResponse(id = 4, title = "Movie Four"))
        }

        val page = newRepository(engine).getNowPlayingMovies(page = 1).finalValue()

        assertEquals(true, capturedUrl.contains("movie/now_playing"))
        assertEquals(1, page.movies.size)
        assertEquals("Movie Four", page.movies.first().title)
    }

    @Test
    fun getPopularMovies_noCacheYet_emitsExactlyOneValue() = runTest {
        val engine = MockEngine { _ -> jsonResponse(popularMoviesResponse()) }

        newRepository(engine).getPopularMovies(page = 1).test {
            assertTrue(awaitItem().isSuccess)
            awaitComplete()
        }
    }

    @Test
    fun getPopularMovies_freshCache_emitsOnlyOneValueWithoutHittingNetworkAgain() = runTest {
        var callCount = 0
        val engine = MockEngine { _ ->
            callCount++
            jsonResponse(popularMoviesResponse())
        }
        val repository = newRepository(engine)
        repository.getPopularMovies(page = 1).finalValue()

        repository.getPopularMovies(page = 1).test {
            val cached = awaitItem().getOrThrow()
            assertFalse(cached.isStale)
            assertEquals("Movie One", cached.movies.first().title)
            awaitComplete()
        }

        assertEquals(1, callCount)
    }

    @Test
    fun getPopularMovies_noCacheNetworkFails_returnsFailure() = runTest {
        val engine = MockEngine { _ -> respond(content = "error", status = HttpStatusCode.InternalServerError) }

        val result = newRepository(engine).getPopularMovies(page = 1).last()

        assertTrue(result.isFailure)
    }

    @Test
    fun getPopularMovies_expiredCache_emitsStaleCacheThenFreshData() = runTest {
        val clock = FakeClock(Instant.fromEpochMilliseconds(0))
        var responseTitle = "Old Title"
        val engine = MockEngine { _ -> jsonResponse(popularMoviesResponse(title = responseTitle)) }
        val repository = newRepository(engine, clock)
        repository.getPopularMovies(page = 1).finalValue()

        clock.advanceBy(8.days)
        responseTitle = "New Title"

        repository.getPopularMovies(page = 1).test {
            val cached = awaitItem().getOrThrow()
            assertTrue(cached.isStale)
            assertEquals("Old Title", cached.movies.first().title)

            val fresh = awaitItem().getOrThrow()
            assertFalse(fresh.isStale)
            assertEquals("New Title", fresh.movies.first().title)

            awaitComplete()
        }
    }

    @Test
    fun getPopularMovies_expiredCacheNetworkFails_emitsStaleCachedDataExactlyOnce() = runTest {
        val clock = FakeClock(Instant.fromEpochMilliseconds(0))
        var shouldFail = false
        val engine = MockEngine { _ ->
            if (shouldFail) {
                respond(content = "error", status = HttpStatusCode.InternalServerError)
            } else {
                jsonResponse(popularMoviesResponse(title = "Cached Title"))
            }
        }
        val repository = newRepository(engine, clock)
        repository.getPopularMovies(page = 1).finalValue()

        clock.advanceBy(8.days)
        shouldFail = true

        repository.getPopularMovies(page = 1).test {
            val page = awaitItem().getOrThrow()
            assertTrue(page.isStale)
            assertEquals("Cached Title", page.movies.first().title)
            awaitComplete()
        }
    }

    @Test
    fun getPopularMovies_forceRefresh_bypassesFreshCacheAndHitsNetworkAgain() = runTest {
        var callCount = 0
        var responseTitle = "First Title"
        val engine = MockEngine { _ ->
            callCount++
            jsonResponse(popularMoviesResponse(title = responseTitle))
        }
        val repository = newRepository(engine)
        repository.getPopularMovies(page = 1).finalValue()

        responseTitle = "Refreshed Title"
        val result = repository.getPopularMovies(page = 1, forceRefresh = true).finalValue()

        assertEquals(2, callCount)
        assertEquals("Refreshed Title", result.movies.first().title)
    }

    @Test
    fun getPopularMovies_emptyResultsPage_isCacheHitWithoutNetworkCall() = runTest {
        var callCount = 0
        val engine = MockEngine { _ ->
            callCount++
            jsonResponse("""{"page": 1, "results": [], "total_pages": 1, "total_results": 0}""")
        }
        val repository = newRepository(engine)

        repository.getPopularMovies(page = 1).finalValue()
        val second = repository.getPopularMovies(page = 1).finalValue()

        assertEquals(1, callCount)
        assertEquals(0, second.totalResults)
        assertTrue(second.movies.isEmpty())
    }

    @Test
    fun getPopularMovies_andUpcomingMovies_doNotShareCache() = runTest {
        val engine = MockEngine { request ->
            if (request.url.toString().contains("popular")) {
                jsonResponse(popularMoviesResponse(id = 1, title = "Popular Movie"))
            } else {
                jsonResponse(popularMoviesResponse(id = 2, title = "Upcoming Movie"))
            }
        }
        val repository = newRepository(engine)

        val popular = repository.getPopularMovies(page = 1).finalValue()
        val upcoming = repository.getUpcomingMovies(page = 1).finalValue()

        assertEquals("Popular Movie", popular.movies.first().title)
        assertEquals("Upcoming Movie", upcoming.movies.first().title)
    }

    @Test
    fun getMovieDetails_mapsResponseToDomain() = runTest {
        val engine = MockEngine { _ -> jsonResponse(movieDetailsResponse()) }

        val details = newRepository(engine).getMovieDetails(42).finalValue()

        assertEquals(42L, details.id)
        assertEquals("Details Movie", details.title)
        assertFalse(details.isStale)
        assertEquals(1, details.genres.size)
        assertEquals("Action", details.genres.first().name)
    }

    @Test
    fun getMovieDetails_noCacheYet_emitsExactlyOneValue() = runTest {
        val engine = MockEngine { _ -> jsonResponse(movieDetailsResponse()) }

        newRepository(engine).getMovieDetails(42).test {
            assertTrue(awaitItem().isSuccess)
            awaitComplete()
        }
    }

    @Test
    fun getMovieDetails_freshCache_emitsOnlyOneValueWithoutHittingNetworkAgain() = runTest {
        var callCount = 0
        val engine = MockEngine { _ ->
            callCount++
            jsonResponse(movieDetailsResponse())
        }
        val repository = newRepository(engine)
        repository.getMovieDetails(42).finalValue()

        repository.getMovieDetails(42).test {
            val cached = awaitItem().getOrThrow()
            assertFalse(cached.isStale)
            assertEquals(1, cached.genres.size)
            awaitComplete()
        }

        assertEquals(1, callCount)
    }

    @Test
    fun getMovieDetails_expiredCache_emitsStaleCacheThenFreshData() = runTest {
        val clock = FakeClock(Instant.fromEpochMilliseconds(0))
        val engine = MockEngine { _ -> jsonResponse(movieDetailsResponse()) }
        val repository = newRepository(engine, clock)
        repository.getMovieDetails(42).finalValue()

        clock.advanceBy(8.days)

        repository.getMovieDetails(42).test {
            assertTrue(awaitItem().getOrThrow().isStale)
            assertFalse(awaitItem().getOrThrow().isStale)
            awaitComplete()
        }
    }

    @Test
    fun getMovieDetails_expiredCacheNetworkFails_emitsStaleDataExactlyOnce() = runTest {
        val clock = FakeClock(Instant.fromEpochMilliseconds(0))
        var shouldFail = false
        val engine = MockEngine { _ ->
            if (shouldFail) {
                respond(content = "error", status = HttpStatusCode.InternalServerError)
            } else {
                jsonResponse(movieDetailsResponse())
            }
        }
        val repository = newRepository(engine, clock)
        repository.getMovieDetails(42).finalValue()

        clock.advanceBy(8.days)
        shouldFail = true

        repository.getMovieDetails(42).test {
            val details = awaitItem().getOrThrow()
            assertTrue(details.isStale)
            assertEquals("Details Movie", details.title)
            awaitComplete()
        }
    }

    @Test
    fun getMovieDetails_forceRefresh_bypassesFreshCacheAndHitsNetworkAgain() = runTest {
        var callCount = 0
        val engine = MockEngine { _ ->
            callCount++
            jsonResponse(movieDetailsResponse())
        }
        val repository = newRepository(engine)
        repository.getMovieDetails(42).finalValue()

        repository.getMovieDetails(42, forceRefresh = true).finalValue()

        assertEquals(2, callCount)
    }

    @Test
    fun getMovieDetails_noGenres_roundTripsAsEmptyList() = runTest {
        val engine = MockEngine { _ -> jsonResponse(movieDetailsResponse(genresJson = "[]")) }
        val repository = newRepository(engine)
        repository.getMovieDetails(42).finalValue()

        val second = repository.getMovieDetails(42).finalValue()

        assertTrue(second.genres.isEmpty())
    }
}
