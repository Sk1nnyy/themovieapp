package dev.themobiledev.movie.ui.presentation.moviedetails

import androidx.annotation.StringRes
import dev.themobiledev.movie.domain.MovieDetails

data class MovieDetailsState(
    val isLoading: Boolean = false,
    val movieDetails: MovieDetails? = null,
    val isFavorite: Boolean = false,
    @StringRes val errorRes: Int? = null,
    val isOffline: Boolean = false,
)

sealed interface MovieDetailsIntent {
    data object Load : MovieDetailsIntent
    data object Retry : MovieDetailsIntent
    data object OnBackClicked : MovieDetailsIntent
    data object ToggleFavorite : MovieDetailsIntent
}

sealed interface MovieDetailsEffect {
    data object NavigateBack : MovieDetailsEffect
}
