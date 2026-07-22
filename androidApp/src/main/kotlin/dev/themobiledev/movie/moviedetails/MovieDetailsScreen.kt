package dev.themobiledev.movie.moviedetails

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.SubcomposeAsyncImage
import dev.themobiledev.movie.Constants.POSTER_BASE_URL
import dev.themobiledev.movie.R
import dev.themobiledev.movie.domain.Genre
import dev.themobiledev.movie.domain.MovieDetails
import dev.themobiledev.movie.navigation.moviePosterSharedElementKey
import dev.themobiledev.movie.navigation.movieTitleSharedElementKey
import dev.themobiledev.movie.theme.MovieTheme
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MovieDetailsScreen(
    movieId: Long,
    posterPath: String?,
    title: String,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onBackClick: () -> Unit,
    viewModel: MovieDetailsViewModel = koinViewModel(parameters = { parametersOf(movieId) }),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                MovieDetailsEffect.NavigateBack -> onBackClick()
            }
        }
    }

    MovieDetailsContent(
        movieId = movieId,
        posterPath = posterPath,
        title = title,
        state = state,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
        onIntent = viewModel::handleIntent,
    )
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun MovieDetailsContent(
    movieId: Long,
    posterPath: String?,
    title: String,
    state: MovieDetailsState,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onIntent: (MovieDetailsIntent) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = { onIntent(MovieDetailsIntent.OnBackClicked) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.content_description_back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onIntent(MovieDetailsIntent.ToggleFavorite) }) {
                        Icon(
                            imageVector = if (state.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = stringResource(
                                if (state.isFavorite) {
                                    R.string.content_description_remove_favorite
                                } else {
                                    R.string.content_description_add_favorite
                                },
                            ),
                            tint = if (state.isFavorite) Color.Red else LocalContentColor.current,
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
        ) {
            // The poster and title are rendered immediately from what the list screen already
            // knew, so the shared-element transition always has a target to animate into on the
            // very first navigation, instead of waiting on the network fetch below to resolve.
            MovieHero(
                movieId = movieId,
                posterPath = posterPath,
                title = title,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
            )

            when {
                state.isLoading && state.movieDetails == null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                state.error != null && state.movieDetails == null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(text = state.error)
                        Button(onClick = { onIntent(MovieDetailsIntent.Retry) }) {
                            Text(stringResource(R.string.action_retry))
                        }
                    }
                }

                state.movieDetails != null -> {
                    MovieDetailsMetadata(details = state.movieDetails)
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun MovieHero(
    movieId: Long,
    posterPath: String?,
    title: String,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        with(sharedTransitionScope) {
            SubcomposeAsyncImage(
                model = posterPath?.let { POSTER_BASE_URL + it },
                contentDescription = stringResource(R.string.content_description_movie_poster_image),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(.8f)
                    .sharedElement(
                        rememberSharedContentState(key = moviePosterSharedElementKey(movieId)),
                        animatedVisibilityScope = animatedVisibilityScope,
                    ),
                contentScale = ContentScale.Crop,
                loading = {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                },
                error = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = stringResource(R.string.error_no_image), style = MaterialTheme.typography.bodySmall)
                    }
                },
            )

            // sharedBounds (not sharedElement) because the title's content differs between the
            // two-line ellipsized card and the full single-line headline: sharedElement tries to
            // reflow the live text through intermediate widths, which clips it mid-animation.
            // sharedBounds instead cross-fades between the two rendered looks while the bounds move.
            Text(
                text = title,
                modifier = Modifier
                    .padding(16.dp)
                    .sharedBounds(
                        rememberSharedContentState(key = movieTitleSharedElementKey(movieId)),
                        animatedVisibilityScope = animatedVisibilityScope,
                    ),
                style = MaterialTheme.typography.headlineSmall,
            )
        }
    }
}

@Composable
private fun MovieDetailsMetadata(details: MovieDetails) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = stringResource(R.string.movie_details_rating, details.voteAverage),
                style = MaterialTheme.typography.bodyMedium,
            )
            details.releaseDate?.let {
                Text(text = it, style = MaterialTheme.typography.bodyMedium)
            }
            details.runtime?.let {
                Text(
                    text = stringResource(R.string.movie_details_runtime_minutes, it),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        if (details.genres.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                details.genres.forEach { genre ->
                    SuggestionChip(onClick = {}, label = { Text(genre.name) })
                }
            }
        }

        Text(text = details.overview, style = MaterialTheme.typography.bodyLarge)
    }
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun MovieDetailsScreenPreview(
    @PreviewParameter(MovieDetailsStatePreviewParameters::class) state: MovieDetailsState,
) {
    MovieTheme {
        SharedTransitionLayout {
            AnimatedVisibility(visible = true) {
                MovieDetailsContent(
                    movieId = state.movieDetails?.id ?: 0L,
                    posterPath = state.movieDetails?.posterPath.orEmpty(),
                    title = state.movieDetails?.title.orEmpty(),
                    state = state,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@AnimatedVisibility,
                    onIntent = {},
                )
            }
        }
    }
}


private class MovieDetailsStatePreviewParameters : PreviewParameterProvider<MovieDetailsState> {
    override val values: Sequence<MovieDetailsState>
        get() = sequenceOf(
            MovieDetailsState(movieDetails =  MovieDetails(
                id = 1,
                title = "The great Bruno",
                overview = "The best movie in history",
                tagline = "An epic tale",
                runtime = 120,
                budget = 1_000_000,
                revenue = 5_000_000,
                homepage = null,
                genres = listOf(Genre(id = 1, name = "Action"), Genre(id = 2, name = "Drama")),
                posterPath = null,
                backdropPath = null,
                releaseDate = "1996-01-30",
                voteAverage = 8.5,
                voteCount = 900,
            ), isFavorite = true),
            MovieDetailsState(isLoading = true),
        )
}
