package dev.themobiledev.movie.ios

import com.rickclephas.kmp.nativecoroutines.NativeCoroutines
import dev.themobiledev.movie.domain.FavoritesRepository
import dev.themobiledev.movie.domain.Movie
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class IOSFavoritesRepository(private val repository: FavoritesRepository) {

    @NativeCoroutines
    fun observeFavoriteIds(): Flow<Set<Long>> =
        repository.observeFavorites().map { movies -> movies.mapTo(mutableSetOf()) { it.id } }

    @NativeCoroutines
    fun observeFavorites(): Flow<List<Movie>> = repository.observeFavorites()

    @NativeCoroutines
    fun observeIsFavorite(movieId: Long): Flow<Boolean> = repository.observeIsFavorite(movieId)

    @Throws(CancellationException::class)
    suspend fun toggleFavorite(movie: Movie, isFavorite: Boolean) {
        repository.toggleFavorite(movie, isFavorite)
    }
}
