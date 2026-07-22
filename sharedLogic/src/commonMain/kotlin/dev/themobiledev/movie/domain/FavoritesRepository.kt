package dev.themobiledev.movie.domain

import kotlinx.coroutines.flow.Flow

interface FavoritesRepository {
    fun observeFavorites(): Flow<List<Movie>>
    fun observeIsFavorite(movieId: Long): Flow<Boolean>
    suspend fun addFavorite(movie: Movie)
    suspend fun removeFavorite(movieId: Long)

    suspend fun toggleFavorite(movie: Movie, isFavorite: Boolean) {
        if (isFavorite) removeFavorite(movie.id) else addFavorite(movie)
    }
}
