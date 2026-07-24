package dev.themobiledev.movie.popularmovies

import app.cash.turbine.test
import dev.themobiledev.movie.R
import dev.themobiledev.movie.domain.FavoritesRepository
import dev.themobiledev.movie.domain.Movie
import dev.themobiledev.movie.domain.MoviesPage
import dev.themobiledev.movie.domain.MoviesRepository
import dev.themobiledev.movie.presentation.popularmovies.MovieFilter
import dev.themobiledev.movie.presentation.popularmovies.PopularMoviesEffect
import dev.themobiledev.movie.presentation.popularmovies.PopularMoviesIntent
import dev.themobiledev.movie.presentation.popularmovies.PopularMoviesViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
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
    private val favoritesRepository = mockk<FavoritesRepository>()

    @Before
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
        every { favoritesRepository.observeFavorites() } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun movie(id: Long, title: String = "Movie $id") = Movie(
        id = id,
        title = title,
        overview = "overview",
        posterPath = null,
        releaseDate = null,
        voteAverage = 5.0,
    )

    private fun page(movies: List<Movie>, page: Int = 1, totalPages: Int = 1, isStale: Boolean = false) =
        MoviesPage(movies = movies, page = page, totalPages = totalPages, totalResults = movies.size, isStale = isStale)

    @Test
    fun initialLoad_emitsLoadingThenPopularMovies() = runTest(mainDispatcher) {
        val gate = CompletableDeferred<Unit>()
        every { repository.getPopularMovies(page = any(), forceRefresh = any()) } returns flow {
            gate.await()
            emit(Result.success(page(listOf(movie(1)))))
        }

        val viewModel = PopularMoviesViewModel(repository, favoritesRepository)

        viewModel.state.test {
            val loading = awaitItem()
            assertTrue(loading.isLoading)
            assertTrue(loading.movies.isEmpty())

            gate.complete(Unit)

            val loaded = awaitItem()
            assertEquals(listOf(movie(1)), loaded.movies)
            assertTrue(!loaded.isLoading)
        }

        verify(exactly = 1) { repository.getPopularMovies(page = 1, forceRefresh = any()) }
    }

    @Test
    fun loadNextPage_emitsLoadingMoreThenAppendsDedupedMovies() = runTest(mainDispatcher) {
        val secondPageGate = CompletableDeferred<Unit>()
        every { repository.getPopularMovies(page = 1, forceRefresh = any()) } returns
            flowOf(Result.success(page(listOf(movie(1), movie(2)), page = 1, totalPages = 2)))
        every { repository.getPopularMovies(page = 2, forceRefresh = any()) } returns flow {
            secondPageGate.await()
            emit(Result.success(page(listOf(movie(2), movie(3)), page = 2, totalPages = 2)))
        }

        val viewModel = PopularMoviesViewModel(repository, favoritesRepository)

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
        every { repository.getPopularMovies(page = any(), forceRefresh = any()) } returns flow {
            popularGate.await()
            emit(Result.success(page(listOf(movie(1, "Popular Movie")))))
        }
        every { repository.getUpcomingMovies(page = any(), forceRefresh = any()) } returns
            flowOf(Result.success(page(listOf(movie(2, "Upcoming Movie")))))

        val viewModel = PopularMoviesViewModel(repository, favoritesRepository)

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

        verify(exactly = 1) { repository.getPopularMovies(page = any(), forceRefresh = any()) }
        verify(exactly = 1) { repository.getUpcomingMovies(page = any(), forceRefresh = any()) }
    }

    @Test
    fun onFailure_setsErrorAndEmitsShowErrorEffect() = runTest(mainDispatcher) {
        val gate = CompletableDeferred<Unit>()
        every { repository.getPopularMovies(page = any(), forceRefresh = any()) } returns flow {
            gate.await()
            emit(Result.failure(IllegalStateException("boom")))
        }

        val viewModel = PopularMoviesViewModel(repository, favoritesRepository)

        viewModel.effect.test {
            gate.complete(Unit)

            assertEquals(PopularMoviesEffect.ShowError(R.string.error_generic), awaitItem())
        }

        assertEquals(R.string.error_generic, viewModel.state.value.errorRes)
        assertTrue(viewModel.state.value.movies.isEmpty())
    }

    @Test
    fun onMovieClicked_emitsNavigateToDetailEffect() = runTest(mainDispatcher) {
        every { repository.getPopularMovies(page = any(), forceRefresh = any()) } returns
            flowOf(Result.success(page(listOf(movie(1)))))

        val viewModel = PopularMoviesViewModel(repository, favoritesRepository)
        val clicked = movie(1)

        viewModel.effect.test {
            viewModel.handleIntent(PopularMoviesIntent.OnMovieClicked(clicked))

            assertEquals(PopularMoviesEffect.NavigateToDetail(clicked), awaitItem())
        }
    }

    @Test
    fun onFavoriteClicked_togglesWithNotFavoritedWhenNotAlreadyFavorited() = runTest(mainDispatcher) {
        every { repository.getPopularMovies(page = any(), forceRefresh = any()) } returns
            flowOf(Result.success(page(listOf(movie(1)))))
        coEvery { favoritesRepository.toggleFavorite(any(), any()) } returns Unit

        val viewModel = PopularMoviesViewModel(repository, favoritesRepository)
        val clicked = movie(1)

        viewModel.handleIntent(PopularMoviesIntent.OnFavoriteClicked(clicked))

        coVerify(exactly = 1) { favoritesRepository.toggleFavorite(clicked, false) }
    }

    @Test
    fun onFavoriteClicked_togglesWithFavoritedWhenAlreadyFavorited() = runTest(mainDispatcher) {
        val favorite = movie(1)
        every { favoritesRepository.observeFavorites() } returns flowOf(listOf(favorite))
        every { repository.getPopularMovies(page = any(), forceRefresh = any()) } returns
            flowOf(Result.success(page(listOf(favorite))))
        coEvery { favoritesRepository.toggleFavorite(any(), any()) } returns Unit

        val viewModel = PopularMoviesViewModel(repository, favoritesRepository)

        viewModel.handleIntent(PopularMoviesIntent.OnFavoriteClicked(favorite))

        coVerify(exactly = 1) { favoritesRepository.toggleFavorite(favorite, true) }
    }

    @Test
    fun initialLoad_cachedMoviesShownImmediately_thenUpdatedByNetwork() = runTest(mainDispatcher) {
        val networkGate = CompletableDeferred<Unit>()
        every { repository.getPopularMovies(page = 1, forceRefresh = any()) } returns flow {
            emit(Result.success(page(listOf(movie(1, "Cached Movie")))))
            networkGate.await()
            emit(Result.success(page(listOf(movie(1, "Fresh Movie")))))
        }

        val viewModel = PopularMoviesViewModel(repository, favoritesRepository)

        viewModel.state.test {
            val cached = awaitItem()
            assertTrue(!cached.isLoading)
            assertEquals(listOf(movie(1, "Cached Movie")), cached.movies)

            networkGate.complete(Unit)

            val fresh = awaitItem()
            assertEquals(listOf(movie(1, "Fresh Movie")), fresh.movies)
        }
    }

    @Test
    fun loadNextPage_staleThenFreshEmissionForSamePage_replacesRatherThanAccumulates() = runTest(mainDispatcher) {
        every { repository.getPopularMovies(page = 1, forceRefresh = any()) } returns
            flowOf(Result.success(page(listOf(movie(1), movie(2)), page = 1, totalPages = 2)))

        val staleGate = CompletableDeferred<Unit>()
        val freshGate = CompletableDeferred<Unit>()
        every { repository.getPopularMovies(page = 2, forceRefresh = any()) } returns flow {
            // Stale cache hit for page 2, followed by a fresh network result where movie(3) has
            // dropped out of the listing and movie(5) is now new. Each emission is gated so the
            // isLoadingMore state update isn't coalesced away by the stale emission that follows
            // it (StateFlow only guarantees collectors see the latest value, not every one).
            staleGate.await()
            emit(Result.success(page(listOf(movie(3), movie(4)), page = 2, totalPages = 2, isStale = true)))
            freshGate.await()
            emit(Result.success(page(listOf(movie(4), movie(5)), page = 2, totalPages = 2)))
        }

        val viewModel = PopularMoviesViewModel(repository, favoritesRepository)

        viewModel.state.test {
            assertEquals(listOf(movie(1), movie(2)), awaitItem().movies)

            viewModel.handleIntent(PopularMoviesIntent.LoadNextPage)
            assertTrue(awaitItem().isLoadingMore)

            staleGate.complete(Unit)
            val stale = awaitItem()
            assertEquals(listOf(movie(1), movie(2), movie(3), movie(4)), stale.movies)
            assertTrue(stale.isOffline)

            freshGate.complete(Unit)
            val fresh = awaitItem()
            // movie(3) must not linger from the stale emission once the fresh page-2 result,
            // which no longer contains it, arrives.
            assertEquals(listOf(movie(1), movie(2), movie(4), movie(5)), fresh.movies)
            assertTrue(!fresh.isOffline)
        }
    }

    @Test
    fun initialLoad_noCachedMovies_onlyEmitsNetworkResult() = runTest(mainDispatcher) {
        val gate = CompletableDeferred<Unit>()
        every { repository.getPopularMovies(page = any(), forceRefresh = any()) } returns flow {
            gate.await()
            emit(Result.success(page(listOf(movie(1)))))
        }

        val viewModel = PopularMoviesViewModel(repository, favoritesRepository)

        viewModel.state.test {
            val loading = awaitItem()
            assertTrue(loading.isLoading)
            assertTrue(loading.movies.isEmpty())

            gate.complete(Unit)

            val loaded = awaitItem()
            assertEquals(listOf(movie(1)), loaded.movies)
        }
    }
}
