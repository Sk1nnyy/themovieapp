package dev.themobiledev.movie.moviedetails

import app.cash.turbine.test
import dev.themobiledev.movie.data.toMovie
import dev.themobiledev.movie.domain.FavoritesRepository
import dev.themobiledev.movie.domain.MovieDetails
import dev.themobiledev.movie.domain.MoviesRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MovieDetailsViewModelTest {

    private val mainDispatcher = UnconfinedTestDispatcher()
    private val repository = mockk<MoviesRepository>()
    private val favoritesRepository = mockk<FavoritesRepository>()

    @Before
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
        every { favoritesRepository.observeIsFavorite(any()) } returns flowOf(false)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun details(id: Long) = MovieDetails(
        id = id,
        title = "Movie $id",
        overview = "overview",
        tagline = null,
        runtime = null,
        budget = 0,
        revenue = 0,
        homepage = null,
        genres = emptyList(),
        posterPath = null,
        backdropPath = null,
        releaseDate = null,
        voteAverage = 5.0,
        voteCount = 0,
    )

    @Test
    fun onBackClicked_emitsNavigateBackEffect() = runTest(mainDispatcher) {
        every { repository.getMovieDetails(movieId = any(), forceRefresh = any()) } returns
            flowOf(Result.success(details(1)))

        val viewModel = MovieDetailsViewModel(movieId = 1, moviesRepository = repository, favoritesRepository = favoritesRepository)

        viewModel.effect.test {
            viewModel.handleIntent(MovieDetailsIntent.OnBackClicked)

            assertEquals(MovieDetailsEffect.NavigateBack, awaitItem())
        }
    }

    @Test
    fun initialLoad_emitsLoadingThenDetails() = runTest(mainDispatcher) {
        val gate = CompletableDeferred<Unit>()
        every { repository.getMovieDetails(movieId = any(), forceRefresh = any()) } returns flow {
            gate.await()
            emit(Result.success(details(1)))
        }

        val viewModel = MovieDetailsViewModel(movieId = 1, moviesRepository = repository, favoritesRepository = favoritesRepository)

        viewModel.state.test {
            val loading = awaitItem()
            assertTrue(loading.isLoading)
            assertEquals(null, loading.movieDetails)

            gate.complete(Unit)

            val loaded = awaitItem()
            assertTrue(!loaded.isLoading)
            assertEquals(details(1), loaded.movieDetails)
            assertNull(loaded.error)
        }
    }

    @Test
    fun onFailure_setsError() = runTest(mainDispatcher) {
        val gate = CompletableDeferred<Unit>()
        every { repository.getMovieDetails(movieId = any(), forceRefresh = any()) } returns flow {
            gate.await()
            emit(Result.failure(IllegalStateException("boom")))
        }

        val viewModel = MovieDetailsViewModel(movieId = 1, moviesRepository = repository, favoritesRepository = favoritesRepository)

        viewModel.state.test {
            assertTrue(awaitItem().isLoading)

            gate.complete(Unit)

            val failed = awaitItem()
            assertTrue(!failed.isLoading)
            assertEquals("boom", failed.error)
            assertNull(failed.movieDetails)
        }
    }

    @Test
    fun retry_reloadsSuccessfullyAfterFailure() = runTest(mainDispatcher) {
        every { repository.getMovieDetails(movieId = any(), forceRefresh = any()) } returns
            flowOf(Result.failure(IllegalStateException("boom")))

        val viewModel = MovieDetailsViewModel(movieId = 1, moviesRepository = repository, favoritesRepository = favoritesRepository)
        assertEquals("boom", viewModel.state.value.error)

        val gate = CompletableDeferred<Unit>()
        every { repository.getMovieDetails(movieId = any(), forceRefresh = any()) } returns flow {
            gate.await()
            emit(Result.success(details(1)))
        }

        viewModel.state.test {
            assertEquals("boom", awaitItem().error)

            viewModel.handleIntent(MovieDetailsIntent.Retry)

            assertTrue(awaitItem().isLoading)

            gate.complete(Unit)

            val loaded = awaitItem()
            assertTrue(!loaded.isLoading)
            assertEquals(details(1), loaded.movieDetails)
            assertNull(loaded.error)
        }
    }

    @Test
    fun toggleFavorite_delegatesCurrentFavoriteStateToRepository() = runTest(mainDispatcher) {
        val movieDetails = details(1)
        every { repository.getMovieDetails(movieId = any(), forceRefresh = any()) } returns
            flowOf(Result.success(movieDetails))
        every { favoritesRepository.observeIsFavorite(any()) } returns flowOf(true)
        coEvery { favoritesRepository.toggleFavorite(any(), any()) } returns Unit

        val viewModel = MovieDetailsViewModel(movieId = 1, moviesRepository = repository, favoritesRepository = favoritesRepository)

        viewModel.handleIntent(MovieDetailsIntent.ToggleFavorite)

        coVerify(exactly = 1) { favoritesRepository.toggleFavorite(movieDetails.toMovie(), true) }
    }

    @Test
    fun initialLoad_cachedDetailsShownImmediately_thenUpdatedByNetwork() = runTest(mainDispatcher) {
        val cachedDetails = details(1).copy(title = "Cached Title")
        val freshDetails = details(1).copy(title = "Fresh Title")
        val networkGate = CompletableDeferred<Unit>()
        every { repository.getMovieDetails(movieId = 1, forceRefresh = any()) } returns flow {
            emit(Result.success(cachedDetails))
            networkGate.await()
            emit(Result.success(freshDetails))
        }

        val viewModel = MovieDetailsViewModel(movieId = 1, moviesRepository = repository, favoritesRepository = favoritesRepository)

        viewModel.state.test {
            val cached = awaitItem()
            assertTrue(!cached.isLoading)
            assertEquals(cachedDetails, cached.movieDetails)

            networkGate.complete(Unit)

            val fresh = awaitItem()
            assertEquals(freshDetails, fresh.movieDetails)
        }
    }

    @Test
    fun staleCachedDetails_setIsOffline_clearedOnceFreshDetailsArrive() = runTest(mainDispatcher) {
        val staleDetails = details(1).copy(isStale = true)
        val freshDetails = details(1).copy(isStale = false)
        val networkGate = CompletableDeferred<Unit>()
        every { repository.getMovieDetails(movieId = 1, forceRefresh = any()) } returns flow {
            emit(Result.success(staleDetails))
            networkGate.await()
            emit(Result.success(freshDetails))
        }

        val viewModel = MovieDetailsViewModel(movieId = 1, moviesRepository = repository, favoritesRepository = favoritesRepository)

        viewModel.state.test {
            val stale = awaitItem()
            assertTrue(stale.isOffline)

            networkGate.complete(Unit)

            val fresh = awaitItem()
            assertTrue(!fresh.isOffline)
        }
    }
}
