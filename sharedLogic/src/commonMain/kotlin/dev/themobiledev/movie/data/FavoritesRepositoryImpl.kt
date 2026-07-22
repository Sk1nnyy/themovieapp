package dev.themobiledev.movie.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrDefault
import dev.themobiledev.movie.db.MovieAppDatabase
import dev.themobiledev.movie.domain.FavoritesRepository
import dev.themobiledev.movie.domain.Movie
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class FavoritesRepositoryImpl(
    database: MovieAppDatabase,
) : FavoritesRepository {

    private val queries = database.favoriteQueries

    override fun observeFavorites(): Flow<List<Movie>> =
        queries.selectAllFavorites { id, title, overview, posterPath, releaseDate, voteAverage ->
            Movie(
                id = id,
                title = title,
                overview = overview,
                posterPath = posterPath,
                releaseDate = releaseDate,
                voteAverage = voteAverage,
            )
        }.asFlow().mapToList(Dispatchers.Default)

    override fun observeIsFavorite(movieId: Long): Flow<Boolean> =
        queries.isFavorite(movieId).asFlow().mapToOneOrDefault(false, Dispatchers.Default)

    override suspend fun addFavorite(movie: Movie) {
        withContext(Dispatchers.Default) {
            queries.insertFavorite(
                id = movie.id,
                title = movie.title,
                overview = movie.overview,
                posterPath = movie.posterPath,
                releaseDate = movie.releaseDate,
                voteAverage = movie.voteAverage,
            )
        }
    }

    override suspend fun removeFavorite(movieId: Long) {
        withContext(Dispatchers.Default) {
            queries.removeFavorite(movieId)
        }
    }
}
