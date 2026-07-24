package dev.themobiledev.movie.ui.presentation.favorites

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.themobiledev.movie.R
import dev.themobiledev.movie.domain.Movie
import dev.themobiledev.movie.presentation.favorites.components.FavoriteMovieItem
import dev.themobiledev.movie.theme.MovieTheme
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun FavoritesScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onNavigateToDetail: (Movie) -> Unit,
    viewModel: FavoritesViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is FavoritesEffect.NavigateToDetail -> onNavigateToDetail(effect.movie)
            }
        }
    }

    FavoritesContent(
        state = state,
        onIntent = viewModel::handleIntent,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun FavoritesContent(
    state: FavoritesState,
    onIntent: (FavoritesIntent) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    if (state.favorites.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.favorites_empty),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(state.favorites, key = { it.id }) { movie ->
                FavoriteMovieItem(
                    movie = movie,
                    onClick = { onIntent(FavoritesIntent.OnMovieClicked(movie)) },
                    onRemoveClick = { onIntent(FavoritesIntent.OnRemoveClicked(movie)) },
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Preview
@Composable
private fun FavoritesScreenPreview(
    @PreviewParameter(FavoritesStatePreviewParameters::class) state: FavoritesState,
) {
    MovieTheme {
        SharedTransitionLayout {
            AnimatedVisibility(visible = true) {
                FavoritesContent(
                    state = state,
                    onIntent = {},
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@AnimatedVisibility,
                )
            }
        }
    }
}

private class FavoritesStatePreviewParameters : PreviewParameterProvider<FavoritesState> {
    override val values: Sequence<FavoritesState>
        get() = sequenceOf(
            FavoritesState(
                favorites = listOf(
                    Movie(
                        id = 1,
                        title = "The great Bruno",
                        overview = "The best movie in history",
                        posterPath = null,
                        releaseDate = "1996-01-30",
                        voteAverage = 5.0,
                    ),
                    Movie(
                        id = 2,
                        title = "Another Favorite",
                        overview = "Overview",
                        posterPath = null,
                        releaseDate = "2020-05-01",
                        voteAverage = 7.2,
                    ),
                ),
            ),
            FavoritesState(),
        )
}
