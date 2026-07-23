package dev.themobiledev.movie.ios

import com.rickclephas.kmp.nativecoroutines.NativeCoroutines
import dev.themobiledev.movie.domain.MovieDetails
import dev.themobiledev.movie.domain.MoviesPage
import dev.themobiledev.movie.domain.MoviesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class IOSMoviesRepository(private val repository: MoviesRepository) {

    @NativeCoroutines
    fun getPopularMovies(page: Int, forceRefresh: Boolean): Flow<MoviesPage> =
        repository.getPopularMovies(page, forceRefresh).map { it.getOrThrow() }

    @NativeCoroutines
    fun getUpcomingMovies(page: Int, forceRefresh: Boolean): Flow<MoviesPage> =
        repository.getUpcomingMovies(page, forceRefresh).map { it.getOrThrow() }

    @NativeCoroutines
    fun getTopRatedMovies(page: Int, forceRefresh: Boolean): Flow<MoviesPage> =
        repository.getTopRatedMovies(page, forceRefresh).map { it.getOrThrow() }

    @NativeCoroutines
    fun getNowPlayingMovies(page: Int, forceRefresh: Boolean): Flow<MoviesPage> =
        repository.getNowPlayingMovies(page, forceRefresh).map { it.getOrThrow() }

    @NativeCoroutines
    fun getMovieDetails(movieId: Long, forceRefresh: Boolean): Flow<MovieDetails> =
        repository.getMovieDetails(movieId, forceRefresh).map { it.getOrThrow() }
}
