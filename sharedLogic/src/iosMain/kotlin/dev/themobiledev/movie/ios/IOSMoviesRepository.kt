package dev.themobiledev.movie.ios

import dev.themobiledev.movie.domain.MovieDetails
import dev.themobiledev.movie.domain.MoviesPage
import dev.themobiledev.movie.domain.MoviesRepository
import dev.themobiledev.movie.network.ApiException
import kotlin.coroutines.cancellation.CancellationException

class IOSMoviesRepository(private val repository: MoviesRepository) {

    @Throws(ApiException::class, CancellationException::class)
    suspend fun getPopularMovies(page: Int): MoviesPage =
        repository.getPopularMovies(page).getOrThrow()

    @Throws(ApiException::class, CancellationException::class)
    suspend fun getUpcomingMovies(page: Int): MoviesPage =
        repository.getUpcomingMovies(page).getOrThrow()

    @Throws(ApiException::class, CancellationException::class)
    suspend fun getTopRatedMovies(page: Int): MoviesPage =
        repository.getTopRatedMovies(page).getOrThrow()

    @Throws(ApiException::class, CancellationException::class)
    suspend fun getNowPlayingMovies(page: Int): MoviesPage =
        repository.getNowPlayingMovies(page).getOrThrow()

    @Throws(ApiException::class, CancellationException::class)
    suspend fun getMovieDetails(movieId: Long): MovieDetails =
        repository.getMovieDetails(movieId).getOrThrow()
}
