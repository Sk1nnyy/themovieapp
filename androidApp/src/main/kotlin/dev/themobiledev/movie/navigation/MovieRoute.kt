package dev.themobiledev.movie.navigation

sealed interface MovieRoute {
    data object PopularMoviesList : MovieRoute
    data object Favorites : MovieRoute
    data class MovieDetail(val movieId: Int, val posterPath: String?, val title: String) : MovieRoute
}
