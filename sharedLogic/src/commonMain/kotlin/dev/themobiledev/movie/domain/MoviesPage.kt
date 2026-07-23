package dev.themobiledev.movie.domain

data class MoviesPage(
    val movies: List<Movie>,
    val page: Int,
    val totalPages: Int,
    val totalResults: Int,
    val isStale: Boolean = false,
)
