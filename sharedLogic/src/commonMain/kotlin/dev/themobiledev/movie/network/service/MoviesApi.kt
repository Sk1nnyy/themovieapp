package dev.themobiledev.movie.network.service

import dev.themobiledev.movie.network.ApiClient
import dev.themobiledev.movie.network.dto.MovieDetailsDto
import dev.themobiledev.movie.network.dto.MoviesResponseDto
import io.ktor.client.HttpClient

class MoviesApi(httpClient: HttpClient) : ApiClient(httpClient) {

    suspend fun getPopularMovies(page: Int = 1): Result<MoviesResponseDto> =
        get("movie/popular", parameters = mapOf("page" to page))

    suspend fun getUpcomingMovies(page: Int = 1): Result<MoviesResponseDto> =
        get("movie/upcoming", parameters = mapOf("page" to page))

    suspend fun getTopRatedMovies(page: Int = 1): Result<MoviesResponseDto> =
        get("movie/top_rated", parameters = mapOf("page" to page))

    suspend fun getNowPlayingMovies(page: Int = 1): Result<MoviesResponseDto> =
        get("movie/now_playing", parameters = mapOf("page" to page))

    suspend fun getMovieDetails(movieId: Int): Result<MovieDetailsDto> =
        get("movie/$movieId")

    suspend fun searchMovies(query: String, page: Int = 1): Result<MoviesResponseDto> =
        get("search/movie", parameters = mapOf("query" to query, "page" to page))
}
