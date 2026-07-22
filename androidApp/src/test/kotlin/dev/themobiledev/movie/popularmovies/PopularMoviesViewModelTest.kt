package dev.themobiledev.movie.popularmovies

import app.cash.turbine.test
import dev.themobiledev.movie.domain.Movie
import dev.themobiledev.movie.domain.MoviesPage
import dev.themobiledev.movie.domain.MoviesRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PopularMoviesViewModelTest {

    private val mainDispatcher = UnconfinedTestDispatcher()
    private val repository = mockk<MoviesRepository>()

    @Before
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun movie(id: Int, title: String = "Movie $id") = Movie(
        id = id,
        title = title,
        overview = "overview",
        posterPath = null,
        releaseDate = null,
        voteAverage = 5.0,
    )

    private fun page(movies: List<Movie>, page: Int = 1, totalPages: Int = 1) =
        MoviesPage(movies = movies, page = page, totalPages = totalPages, totalResults = movies.size)

    @Test
    fun initialLoad_emitsLoadingThenPopularMovies() = runTest(mainDispatcher) {
        val gate = CompletableDeferred<Unit>()
        coEvery { repository.getPopularMovies(any()) } coAnswers {
            gate.await()
            Result.success(page(listOf(movie(1))))
        }

        val viewModel = PopularMoviesViewModel(repository)

        viewModel.state.test {
            val loading = awaitItem()
            assertTrue(loading.isLoading)
            assertTrue(loading.movies.isEmpty())

            gate.complete(Unit)

            val loaded = awaitItem()
            assertEquals(listOf(movie(1)), loaded.movies)
            assertTrue(!loaded.isLoading)
        }

        coVerify(exactly = 1) { repository.getPopularMovies(page = 1) }
    }

    @Test
    fun loadNextPage_emitsLoadingMoreThenAppendsDedupedMovies() = runTest(mainDispatcher) {
        val secondPageGate = CompletableDeferred<Unit>()
        coEvery { repository.getPopularMovies(page = 1) } returns
            Result.success(page(listOf(movie(1), movie(2)), page = 1, totalPages = 2))
        coEvery { repository.getPopularMovies(page = 2) } coAnswers {
            secondPageGate.await()
            Result.success(page(listOf(movie(2), movie(3)), page = 2, totalPages = 2))
        }

        val viewModel = PopularMoviesViewModel(repository)

        viewModel.state.test {
            assertEquals(listOf(movie(1), movie(2)), awaitItem().movies)

            viewModel.handleIntent(PopularMoviesIntent.LoadNextPage)
            assertTrue(awaitItem().isLoadingMore)

            secondPageGate.complete(Unit)

            val loaded = awaitItem()
            assertTrue(!loaded.isLoadingMore)
            assertEquals(listOf(movie(1), movie(2), movie(3)), loaded.movies)
        }
    }

    @Test
    fun switchingFilterWhileRequestInFlight_ignoresStaleResult() = runTest(mainDispatcher) {
        val popularGate = CompletableDeferred<Unit>()
        coEvery { repository.getPopularMovies(any()) } coAnswers {
            popularGate.await()
            Result.success(page(listOf(movie(1, "Popular Movie"))))
        }
        coEvery { repository.getUpcomingMovies(any()) } returns
            Result.success(page(listOf(movie(2, "Upcoming Movie"))))

        val viewModel = PopularMoviesViewModel(repository)

        viewModel.state.test {
            assertTrue(awaitItem().isLoading) // initial Popular load stuck behind popularGate

            viewModel.handleIntent(PopularMoviesIntent.OnFilterSelected(MovieFilter.Upcoming))

            val switching = awaitItem()
            assertEquals(MovieFilter.Upcoming, switching.selectedFilter)
            assertTrue(switching.movies.isEmpty())

            val loaded = awaitItem()
            assertEquals(listOf(movie(2, "Upcoming Movie")), loaded.movies)

            // The stale Popular request resolves last; it was cancelled, so it must not emit.
            popularGate.complete(Unit)
            expectNoEvents()

            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 1) { repository.getPopularMovies(any()) }
        coVerify(exactly = 1) { repository.getUpcomingMovies(any()) }
    }

    @Test
    fun onFailure_setsErrorAndEmitsShowErrorEffect() = runTest(mainDispatcher) {
        val gate = CompletableDeferred<Unit>()
        coEvery { repository.getPopularMovies(any()) } coAnswers {
            gate.await()
            Result.failure(IllegalStateException("boom"))
        }

        val viewModel = PopularMoviesViewModel(repository)

        viewModel.effect.test {
            gate.complete(Unit)

            assertEquals(PopularMoviesEffect.ShowError("boom"), awaitItem())
        }

        assertEquals("boom", viewModel.state.value.error)
        assertTrue(viewModel.state.value.movies.isEmpty())
    }

    @Test
    fun onMovieClicked_emitsNavigateToDetailEffect() = runTest(mainDispatcher) {
        coEvery { repository.getPopularMovies(any()) } returns Result.success(page(listOf(movie(1))))

        val viewModel = PopularMoviesViewModel(repository)
        val clicked = movie(1)

        viewModel.effect.test {
            viewModel.handleIntent(PopularMoviesIntent.OnMovieClicked(clicked))

            assertEquals(PopularMoviesEffect.NavigateToDetail(clicked), awaitItem())
        }
    }
}
