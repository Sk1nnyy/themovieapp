package dev.themobiledev.movie.data

import dev.themobiledev.movie.domain.MovieDetails
import dev.themobiledev.movie.domain.MoviesPage
import dev.themobiledev.movie.domain.MoviesRepository
import dev.themobiledev.movie.network.service.MoviesApi

class MoviesRepositoryImpl(private val moviesApi: MoviesApi) : MoviesRepository {

    override suspend fun getPopularMovies(page: Int): Result<MoviesPage> =
        moviesApi.getPopularMovies(page).map { it.toDomain() }

    override suspend fun getUpcomingMovies(page: Int): Result<MoviesPage> =
        moviesApi.getUpcomingMovies(page).map { it.toDomain() }

    override suspend fun getTopRatedMovies(page: Int): Result<MoviesPage> =
        moviesApi.getTopRatedMovies(page).map { it.toDomain() }

    override suspend fun getNowPlayingMovies(page: Int): Result<MoviesPage> =
        moviesApi.getNowPlayingMovies(page).map { it.toDomain() }

    override suspend fun getMovieDetails(movieId: Long): Result<MovieDetails> =
        moviesApi.getMovieDetails(movieId).map { it.toDomain() }
}
