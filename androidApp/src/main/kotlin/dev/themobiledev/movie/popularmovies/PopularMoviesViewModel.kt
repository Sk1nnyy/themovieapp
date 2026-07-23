package dev.themobiledev.movie.popularmovies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.themobiledev.movie.domain.FavoritesRepository
import dev.themobiledev.movie.domain.Movie
import dev.themobiledev.movie.domain.MoviesPage
import dev.themobiledev.movie.domain.MoviesRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PopularMoviesViewModel(
    private val moviesRepository: MoviesRepository,
    private val favoritesRepository: FavoritesRepository,
) : ViewModel() {

    val state: StateFlow<PopularMoviesState>
        field = MutableStateFlow(PopularMoviesState())

    val effect: SharedFlow<PopularMoviesEffect>
        field = MutableSharedFlow(extraBufferCapacity = 1)

    private var loadJob: Job? = null

    init {
        handleIntent(PopularMoviesIntent.LoadPopularMovies)
        favoritesRepository.observeFavorites()
            .onEach { favorites -> state.update { it.copy(favoriteIds = favorites.mapTo(HashSet()) { movie -> movie.id }) } }
            .launchIn(viewModelScope)
    }

    fun handleIntent(intent: PopularMoviesIntent) {
        when (intent) {
            PopularMoviesIntent.LoadPopularMovies,
            PopularMoviesIntent.Retry,
                -> loadMovies(page = 1)

            PopularMoviesIntent.LoadNextPage -> loadNextPage()

            PopularMoviesIntent.ShowFilters -> state.update { it.copy(isFilterSheetVisible = true) }
            PopularMoviesIntent.HideFilters -> state.update { it.copy(isFilterSheetVisible = false) }
            is PopularMoviesIntent.OnFilterSelected -> onFilterSelected(intent.filter)
            is PopularMoviesIntent.OnMovieClicked -> viewModelScope.launch {
                effect.emit(PopularMoviesEffect.NavigateToDetail(intent.movie))
            }
            is PopularMoviesIntent.OnFavoriteClicked -> toggleFavorite(intent.movie)
        }
    }

    private fun toggleFavorite(movie: Movie) {
        viewModelScope.launch {
            favoritesRepository.toggleFavorite(movie, movie.id in state.value.favoriteIds)
        }
    }

    private fun onFilterSelected(filter: MovieFilter) {
        state.update {
            it.copy(
                selectedFilter = filter,
                isFilterSheetVisible = false,
                movies = emptyList(),
                currentPage = 0,
                totalPages = 1,
            )
        }
        loadMovies(page = 1)
    }

    private fun loadNextPage() {
        val current = state.value
        if (current.isLoading || current.isLoadingMore || !current.canLoadMore) return
        loadMovies(page = current.currentPage + 1)
    }

    private fun loadMovies(page: Int) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val filter = state.value.selectedFilter

            state.update {
                if (page == 1) it.copy(isLoading = true, error = null) else it.copy(isLoadingMore = true, error = null)
            }

            val flow = when (filter) {
                MovieFilter.Popular -> moviesRepository.getPopularMovies(page = page)
                MovieFilter.Upcoming -> moviesRepository.getUpcomingMovies(page = page)
                MovieFilter.TopRated -> moviesRepository.getTopRatedMovies(page = page)
                MovieFilter.NowPlaying -> moviesRepository.getNowPlayingMovies(page = page)
            }

            flow.collect { result ->
                result
                    .onSuccess { moviesPage -> applyMoviesPage(moviesPage, page) }
                    .onFailure { throwable ->
                        val message = throwable.message ?: "Unable to load movies"
                        state.update { it.copy(isLoading = false, isLoadingMore = false, error = message) }
                        effect.emit(PopularMoviesEffect.ShowError(message))
                    }
            }
        }
    }

    private fun applyMoviesPage(moviesPage: MoviesPage, requestedPage: Int) {
        state.update {
            it.copy(
                isLoading = false,
                isLoadingMore = false,
                movies = if (requestedPage == 1) {
                    moviesPage.movies
                } else {
                    val existingIds = it.movies.mapTo(HashSet()) { movie -> movie.id }
                    it.movies + moviesPage.movies.filterNot { movie -> movie.id in existingIds }
                },
                currentPage = moviesPage.page,
                totalPages = moviesPage.totalPages,
                error = null,
            )
        }
    }
}
