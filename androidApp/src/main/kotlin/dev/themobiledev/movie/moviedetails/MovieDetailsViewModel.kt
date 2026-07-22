package dev.themobiledev.movie.moviedetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.themobiledev.movie.domain.MoviesRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MovieDetailsViewModel(
    private val movieId: Int,
    private val moviesRepository: MoviesRepository,
) : ViewModel() {

    val state: StateFlow<MovieDetailsState>
        field = MutableStateFlow(MovieDetailsState())

    val effect: SharedFlow<MovieDetailsEffect>
        field = MutableSharedFlow(extraBufferCapacity = 1)

    private var loadJob: Job? = null

    init {
        handleIntent(MovieDetailsIntent.Load)
    }

    fun handleIntent(intent: MovieDetailsIntent) {
        when (intent) {
            MovieDetailsIntent.Load,
            MovieDetailsIntent.Retry,
                -> loadDetails()

            MovieDetailsIntent.OnBackClicked -> viewModelScope.launch {
                effect.emit(MovieDetailsEffect.NavigateBack)
            }
        }
    }

    private fun loadDetails() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            state.update { it.copy(isLoading = true, error = null) }

            moviesRepository.getMovieDetails(movieId)
                .onSuccess { details ->
                    state.update { it.copy(isLoading = false, movieDetails = details, error = null) }
                }
                .onFailure { throwable ->
                    val message = throwable.message ?: "Unable to load movie details"
                    state.update { it.copy(isLoading = false, error = message) }
                }
        }
    }
}
