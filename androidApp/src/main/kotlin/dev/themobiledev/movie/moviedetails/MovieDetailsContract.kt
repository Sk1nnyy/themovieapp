package dev.themobiledev.movie.moviedetails

import dev.themobiledev.movie.domain.MovieDetails

data class MovieDetailsState(
    val isLoading: Boolean = false,
    val movieDetails: MovieDetails? = null,
    val error: String? = null,
)

sealed interface MovieDetailsIntent {
    data object Load : MovieDetailsIntent
    data object Retry : MovieDetailsIntent
    data object OnBackClicked : MovieDetailsIntent
}

sealed interface MovieDetailsEffect {
    data object NavigateBack : MovieDetailsEffect
}
