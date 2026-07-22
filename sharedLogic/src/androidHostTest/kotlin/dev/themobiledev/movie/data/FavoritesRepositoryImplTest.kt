package dev.themobiledev.movie.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.turbine.test
import dev.themobiledev.movie.db.MovieAppDatabase
import dev.themobiledev.movie.domain.Movie
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FavoritesRepositoryImplTest {

    private fun newRepository(): FavoritesRepositoryImpl {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        MovieAppDatabase.Schema.create(driver)
        return FavoritesRepositoryImpl(MovieAppDatabase(driver))
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
    fun addFavorite_appearsInObserveFavorites() = runTest {
        val repository = newRepository()

        repository.observeFavorites().test {
            assertEquals(emptyList(), awaitItem())

            repository.addFavorite(movie(1))

            assertEquals(listOf(movie(1)), awaitItem())
        }
    }

    @Test
    fun removeFavorite_disappearsFromObserveFavorites() = runTest {
        val repository = newRepository()
        repository.addFavorite(movie(1))

        repository.observeFavorites().test {
            assertEquals(listOf(movie(1)), awaitItem())

            repository.removeFavorite(1)

            assertEquals(emptyList(), awaitItem())
        }
    }

    @Test
    fun observeFavorites_ordersMostRecentlyAddedFirst() = runTest {
        val repository = newRepository()

        repository.observeFavorites().test {
            assertEquals(emptyList(), awaitItem())

            repository.addFavorite(movie(1))
            assertEquals(listOf(movie(1)), awaitItem())

            repository.addFavorite(movie(2))
            assertEquals(listOf(movie(2), movie(1)), awaitItem())
        }
    }

    @Test
    fun observeIsFavorite_reflectsAddAndRemove() = runTest {
        val repository = newRepository()

        repository.observeIsFavorite(1).test {
            assertFalse(awaitItem())

            repository.addFavorite(movie(1))
            assertTrue(awaitItem())

            repository.removeFavorite(1)
            assertFalse(awaitItem())
        }
    }

    @Test
    fun toggleFavorite_addsWhenNotCurrentlyFavorite() = runTest {
        val repository = newRepository()

        repository.toggleFavorite(movie(1), isFavorite = false)

        repository.observeIsFavorite(1).test {
            assertTrue(awaitItem())
        }
    }

    @Test
    fun toggleFavorite_removesWhenCurrentlyFavorite() = runTest {
        val repository = newRepository()
        repository.addFavorite(movie(1))

        repository.toggleFavorite(movie(1), isFavorite = true)

        repository.observeIsFavorite(1).test {
            assertFalse(awaitItem())
        }
    }
}
