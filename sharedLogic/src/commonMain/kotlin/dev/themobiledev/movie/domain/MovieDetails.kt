package dev.themobiledev.movie.domain

data class MovieDetails(
    val id: Int,
    val title: String,
    val overview: String,
    val tagline: String?,
    val runtime: Int?,
    val budget: Long,
    val revenue: Long,
    val homepage: String?,
    val genres: List<Genre>,
    val posterPath: String?,
    val backdropPath: String?,
    val releaseDate: String?,
    val voteAverage: Double,
    val voteCount: Int,
)
