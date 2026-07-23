package dev.themobiledev.movie.ios

import dev.themobiledev.movie.domain.FavoritesRepository
import dev.themobiledev.movie.domain.Movie
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.first

class IOSFavoritesRepository(private val repository: FavoritesRepository) {

    @Throws(CancellationException::class)
    suspend fun getFavoriteIds(): Set<Long> =
        repository.observeFavorites().first().mapTo(mutableSetOf()) { it.id }

    @Throws(CancellationException::class)
    suspend fun getFavorites(): List<Movie> =
        repository.observeFavorites().first()

    @Throws(CancellationException::class)
    suspend fun isFavorite(movieId: Long): Boolean =
        repository.observeIsFavorite(movieId).first()

    @Throws(CancellationException::class)
    suspend fun toggleFavorite(movie: Movie, isFavorite: Boolean) {
        repository.toggleFavorite(movie, isFavorite)
    }
}
