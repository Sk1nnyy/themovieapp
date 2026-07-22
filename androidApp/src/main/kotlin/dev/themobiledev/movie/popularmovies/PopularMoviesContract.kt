package dev.themobiledev.movie.popularmovies

import dev.themobiledev.movie.R
import dev.themobiledev.movie.domain.Movie

enum class MovieFilter(val labelRes: Int) {
    Popular(R.string.filter_popular),
    Upcoming(R.string.filter_upcoming),
    TopRated(R.string.filter_top_rated),
    NowPlaying(R.string.filter_now_playing),
}

data class PopularMoviesState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val movies: List<Movie> = emptyList(),
    val currentPage: Int = 0,
    val totalPages: Int = 1,
    val error: String? = null,
    val selectedFilter: MovieFilter = MovieFilter.Popular,
    val isFilterSheetVisible: Boolean = false,
    val favoriteIds: Set<Long> = emptySet(),
) {
    val canLoadMore: Boolean get() = currentPage < totalPages
}

sealed interface PopularMoviesIntent {
    data object LoadPopularMovies : PopularMoviesIntent
    data object Retry : PopularMoviesIntent
    data object LoadNextPage : PopularMoviesIntent
    data object ShowFilters : PopularMoviesIntent
    data object HideFilters : PopularMoviesIntent
    data class OnFilterSelected(val filter: MovieFilter) : PopularMoviesIntent
    data class OnMovieClicked(val movie: Movie) : PopularMoviesIntent
    data class OnFavoriteClicked(val movie: Movie) : PopularMoviesIntent
}

sealed interface PopularMoviesEffect {
    data class ShowError(val message: String) : PopularMoviesEffect
    data class NavigateToDetail(val movie: Movie) : PopularMoviesEffect
}
