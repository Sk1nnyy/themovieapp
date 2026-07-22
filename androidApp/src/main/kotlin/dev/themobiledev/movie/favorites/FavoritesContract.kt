package dev.themobiledev.movie.favorites

import dev.themobiledev.movie.domain.Movie

data class FavoritesState(
    val favorites: List<Movie> = emptyList(),
)

sealed interface FavoritesIntent {
    data class OnMovieClicked(val movie: Movie) : FavoritesIntent
    data class OnRemoveClicked(val movie: Movie) : FavoritesIntent
}

sealed interface FavoritesEffect {
    data class NavigateToDetail(val movie: Movie) : FavoritesEffect
}
