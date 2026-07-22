package dev.themobiledev.movie.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.themobiledev.movie.domain.FavoritesRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FavoritesViewModel(
    private val favoritesRepository: FavoritesRepository,
) : ViewModel() {

    val state: StateFlow<FavoritesState> = favoritesRepository.observeFavorites()
        .map { FavoritesState(favorites = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FavoritesState())

    val effect: SharedFlow<FavoritesEffect>
        field = MutableSharedFlow(extraBufferCapacity = 1)

    fun handleIntent(intent: FavoritesIntent) {
        when (intent) {
            is FavoritesIntent.OnMovieClicked -> viewModelScope.launch {
                effect.emit(FavoritesEffect.NavigateToDetail(intent.movie))
            }

            is FavoritesIntent.OnRemoveClicked -> viewModelScope.launch {
                favoritesRepository.removeFavorite(intent.movie.id)
            }
        }
    }
}
