package dev.themobiledev.movie.ui.presentation.moviedetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.themobiledev.movie.data.toMovie
import dev.themobiledev.movie.domain.FavoritesRepository
import dev.themobiledev.movie.domain.MoviesRepository
import dev.themobiledev.movie.utils.userFacingMessageRes
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MovieDetailsViewModel(
    private val movieId: Long,
    private val moviesRepository: MoviesRepository,
    private val favoritesRepository: FavoritesRepository,
) : ViewModel() {

    val state: StateFlow<MovieDetailsState>
        field = MutableStateFlow(MovieDetailsState())

    val effect: SharedFlow<MovieDetailsEffect>
        field = MutableSharedFlow(extraBufferCapacity = 1)

    private var loadJob: Job? = null

    init {
        handleIntent(MovieDetailsIntent.Load)
        favoritesRepository.observeIsFavorite(movieId)
            .onEach { isFavorite -> state.update { it.copy(isFavorite = isFavorite) } }
            .launchIn(viewModelScope)
    }

    fun handleIntent(intent: MovieDetailsIntent) {
        when (intent) {
            MovieDetailsIntent.Load,
            MovieDetailsIntent.Retry,
                -> loadDetails()

            MovieDetailsIntent.OnBackClicked -> viewModelScope.launch {
                effect.emit(MovieDetailsEffect.NavigateBack)
            }

            MovieDetailsIntent.ToggleFavorite -> toggleFavorite()
        }
    }

    private fun toggleFavorite() {
        val details = state.value.movieDetails ?: return
        viewModelScope.launch {
            favoritesRepository.toggleFavorite(details.toMovie(), state.value.isFavorite)
        }
    }

    private fun loadDetails() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            state.update { it.copy(isLoading = true, errorRes = null) }

            moviesRepository.getMovieDetails(movieId).collect { result ->
                result
                    .onSuccess { details ->
                        state.update {
                            it.copy(isLoading = false, movieDetails = details, errorRes = null, isOffline = details.isStale)
                        }
                    }
                    .onFailure { throwable ->
                        state.update { it.copy(isLoading = false, errorRes = throwable.userFacingMessageRes()) }
                    }
            }
        }
    }
}
