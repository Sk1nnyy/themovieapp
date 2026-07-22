package dev.themobiledev.movie.navigation

sealed interface MovieRoute {
    data object PopularMoviesList : MovieRoute
    data object Favorites : MovieRoute
}
