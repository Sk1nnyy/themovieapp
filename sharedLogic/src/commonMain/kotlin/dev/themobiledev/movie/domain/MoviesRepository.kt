package dev.themobiledev.movie.domain

import kotlinx.coroutines.flow.Flow

interface MoviesRepository {

    /**
     * DB-first with a 7-day TTL. If a cached value exists, it's emitted first (marked stale if
     * past the TTL), before anything else happens. A fresh cache short-circuits the network call
     * entirely unless [forceRefresh] is true; otherwise a network fetch follows, emitting its
     * result - fresh data on success, or the cached value (marked stale) as a fallback if the
     * network call fails and nothing else is available.
     */
    fun getPopularMovies(page: Int = 1, forceRefresh: Boolean = false): Flow<Result<MoviesPage>>

    fun getUpcomingMovies(page: Int = 1, forceRefresh: Boolean = false): Flow<Result<MoviesPage>>

    fun getTopRatedMovies(page: Int = 1, forceRefresh: Boolean = false): Flow<Result<MoviesPage>>

    fun getNowPlayingMovies(page: Int = 1, forceRefresh: Boolean = false): Flow<Result<MoviesPage>>

    fun getMovieDetails(movieId: Long, forceRefresh: Boolean = false): Flow<Result<MovieDetails>>
}
