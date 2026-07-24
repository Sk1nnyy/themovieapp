package dev.themobiledev.movie.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MovieDetailsDto(
    val id: Long,
    val title: String,
    val overview: String,
    val tagline: String? = null,
    val runtime: Int? = null,
    val budget: Long = 0,
    val revenue: Long = 0,
    val homepage: String? = null,
    val genres: List<GenreDto> = emptyList(),
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    @SerialName("vote_count") val voteCount: Int = 0,
    val videos: VideosDto? = null,
)

@Serializable
data class VideosDto(
    val results: List<VideoDto> = emptyList(),
)

@Serializable
data class VideoDto(
    val key: String,
    val site: String,
    val type: String,
    val official: Boolean = false,
)
