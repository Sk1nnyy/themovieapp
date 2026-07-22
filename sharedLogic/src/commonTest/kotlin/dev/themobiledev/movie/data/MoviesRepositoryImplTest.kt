package dev.themobiledev.movie.data

import dev.themobiledev.movie.domain.MoviesRepository
import dev.themobiledev.movie.network.ApiException
import dev.themobiledev.movie.network.createHttpClient
import dev.themobiledev.movie.network.service.MoviesApi
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class MoviesRepositoryImplTest {

    private fun repositoryWith(engine: MockEngine): MoviesRepository =
        MoviesRepositoryImpl(MoviesApi(createHttpClient(engine)))

    @Test
    fun getPopularMovies_mapsResponseToDomain() = runTest {
        val engine = MockEngine { _ ->
            respond(
                content = """
                    {
                      "page": 1,
                      "results": [
                        {"id": 1, "title": "Movie One", "overview": "Overview one", "poster_path": "/one.jpg", "release_date": "2024-01-01", "vote_average": 7.5}
                      ],
                      "total_pages": 5,
                      "total_results": 100
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val page = repositoryWith(engine).getPopularMovies(page = 1).getOrThrow()

        assertEquals(1, page.page)
        assertEquals(5, page.totalPages)
        assertEquals(100, page.totalResults)
        assertEquals(1, page.movies.size)
        with(page.movies.first()) {
            assertEquals(1, id)
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

        val result = repositoryWith(engine).getPopularMovies()

        val exception = assertIs<ApiException.Http>(result.exceptionOrNull())
        assertEquals(401, exception.code)
    }

    @Test
    fun getUpcomingMovies_requestsUpcomingEndpointAndMapsResponse() = runTest {
        var capturedUrl = ""
        val engine = MockEngine { request ->
            capturedUrl = request.url.toString()
            respond(
                content = """
                    {
                      "page": 1,
                      "results": [
                        {"id": 2, "title": "Movie Two", "overview": "Overview two", "poster_path": "/two.jpg", "release_date": "2024-02-01", "vote_average": 6.5}
                      ],
                      "total_pages": 3,
                      "total_results": 50
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val page = repositoryWith(engine).getUpcomingMovies(page = 1).getOrThrow()

        assertEquals(true, capturedUrl.contains("movie/upcoming"))
        assertEquals(1, page.movies.size)
        assertEquals("Movie Two", page.movies.first().title)
    }

    @Test
    fun getTopRatedMovies_requestsTopRatedEndpointAndMapsResponse() = runTest {
        var capturedUrl = ""
        val engine = MockEngine { request ->
            capturedUrl = request.url.toString()
            respond(
                content = """
                    {
                      "page": 1,
                      "results": [
                        {"id": 3, "title": "Movie Three", "overview": "Overview three", "poster_path": "/three.jpg", "release_date": "2024-03-01", "vote_average": 9.0}
                      ],
                      "total_pages": 2,
                      "total_results": 30
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val page = repositoryWith(engine).getTopRatedMovies(page = 1).getOrThrow()

        assertEquals(true, capturedUrl.contains("movie/top_rated"))
        assertEquals(1, page.movies.size)
        assertEquals("Movie Three", page.movies.first().title)
    }

    @Test
    fun getNowPlayingMovies_requestsNowPlayingEndpointAndMapsResponse() = runTest {
        var capturedUrl = ""
        val engine = MockEngine { request ->
            capturedUrl = request.url.toString()
            respond(
                content = """
                    {
                      "page": 1,
                      "results": [
                        {"id": 4, "title": "Movie Four", "overview": "Overview four", "poster_path": "/four.jpg", "release_date": "2024-04-01", "vote_average": 4.0}
                      ],
                      "total_pages": 4,
                      "total_results": 60
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val page = repositoryWith(engine).getNowPlayingMovies(page = 1).getOrThrow()

        assertEquals(true, capturedUrl.contains("movie/now_playing"))
        assertEquals(1, page.movies.size)
        assertEquals("Movie Four", page.movies.first().title)
    }

    @Test
    fun getMovieDetails_mapsResponseToDomain() = runTest {
        val engine = MockEngine { _ ->
            respond(
                content = """
                    {
                      "id": 42,
                      "title": "Details Movie",
                      "overview": "Overview",
                      "tagline": "Tagline",
                      "runtime": 120,
                      "budget": 1000,
                      "revenue": 5000,
                      "homepage": "https://example.com",
                      "genres": [{"id": 1, "name": "Action"}],
                      "poster_path": "/p.jpg",
                      "backdrop_path": "/b.jpg",
                      "release_date": "2024-05-01",
                      "vote_average": 8.1,
                      "vote_count": 900
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val details = repositoryWith(engine).getMovieDetails(42).getOrThrow()

        assertEquals(42, details.id)
        assertEquals("Details Movie", details.title)
        assertEquals(1, details.genres.size)
        assertEquals("Action", details.genres.first().name)
    }

    @Test
    fun searchMovies_sendsQueryAndPageAsRequestParameters() = runTest {
        var capturedUrl = ""
        val engine = MockEngine { request ->
            capturedUrl = request.url.toString()
            respond(
                content = """{"page": 1, "results": [], "total_pages": 1, "total_results": 0}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        repositoryWith(engine).searchMovies(query = "matrix", page = 2)

        assertEquals(true, capturedUrl.contains("query=matrix"))
        assertEquals(true, capturedUrl.contains("page=2"))
    }
}
