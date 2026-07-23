package dev.themobiledev.movie.domain

interface MoviesRepository {
    suspend fun getPopularMovies(page: Int = 1): Result<MoviesPage>
    suspend fun getUpcomingMovies(page: Int = 1): Result<MoviesPage>
    suspend fun getTopRatedMovies(page: Int = 1): Result<MoviesPage>
    suspend fun getNowPlayingMovies(page: Int = 1): Result<MoviesPage>
    suspend fun getMovieDetails(movieId: Long): Result<MovieDetails>
}
