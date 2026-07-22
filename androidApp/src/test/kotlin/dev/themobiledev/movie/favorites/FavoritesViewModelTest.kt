package dev.themobiledev.movie.favorites

import app.cash.turbine.test
import dev.themobiledev.movie.domain.FavoritesRepository
import dev.themobiledev.movie.domain.Movie
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class FavoritesViewModelTest {

    private val mainDispatcher = UnconfinedTestDispatcher()
    private val favoritesRepository = mockk<FavoritesRepository>()

    @Before
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
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

    @Test
    fun state_reflectsFavoritesFromRepository() = runTest(mainDispatcher) {
        val favorites = listOf(movie(2), movie(1))
        every { favoritesRepository.observeFavorites() } returns flowOf(favorites)

        val viewModel = FavoritesViewModel(favoritesRepository)

        viewModel.state.test {
            assertEquals(favorites, awaitItem().favorites)
        }
    }

    @Test
    fun onMovieClicked_emitsNavigateToDetailEffect() = runTest(mainDispatcher) {
        every { favoritesRepository.observeFavorites() } returns flowOf(emptyList())
        val viewModel = FavoritesViewModel(favoritesRepository)
        val clicked = movie(1)

        viewModel.effect.test {
            viewModel.handleIntent(FavoritesIntent.OnMovieClicked(clicked))

            assertEquals(FavoritesEffect.NavigateToDetail(clicked), awaitItem())
        }
    }

    @Test
    fun onRemoveClicked_removesFavoriteFromRepository() = runTest(mainDispatcher) {
        every { favoritesRepository.observeFavorites() } returns MutableSharedFlow()
        coEvery { favoritesRepository.removeFavorite(any()) } returns Unit
        val viewModel = FavoritesViewModel(favoritesRepository)
        val removed = movie(1)

        viewModel.handleIntent(FavoritesIntent.OnRemoveClicked(removed))

        coVerify(exactly = 1) { favoritesRepository.removeFavorite(removed.id) }
    }
}
