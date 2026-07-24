package dev.themobiledev.movie.ui.presentation.popularmovies.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import dev.themobiledev.movie.Constants.POSTER_BASE_URL
import dev.themobiledev.movie.R
import dev.themobiledev.movie.domain.Movie
import dev.themobiledev.movie.navigation.moviePosterSharedElementKey
import dev.themobiledev.movie.navigation.movieTitleSharedElementKey
import dev.themobiledev.movie.theme.FavoriteRed
import dev.themobiledev.movie.theme.MovieTheme

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PopularMovieItem(
    movie: Movie,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.clickable(onClick = onClick).fillMaxWidth()) {
        with(sharedTransitionScope) {
            Box {
                SubcomposeAsyncImage(
                    model = movie.posterPath?.let { POSTER_BASE_URL + it },
                    contentDescription = stringResource(R.string.content_description_movie_poster_image),
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(2f / 3f)
                        .clip(RoundedCornerShape(12.dp))
                        .sharedElement(
                            rememberSharedContentState(key = moviePosterSharedElementKey(movie.id)),
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

                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(36.dp)
                        .background(Color.Black.copy(alpha = 0.35f), CircleShape),
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = stringResource(
                            if (isFavorite) {
                                R.string.content_description_remove_favorite
                            } else {
                                R.string.content_description_add_favorite
                            },
                        ),
                        tint = if (isFavorite) FavoriteRed else Color.White,
                    )
                }
            }

            Text(
                text = movie.title,
                modifier = Modifier
                    .padding(16.dp)
                    .sharedBounds(
                        rememberSharedContentState(key = movieTitleSharedElementKey(movie.id)),
                        animatedVisibilityScope = animatedVisibilityScope,
                    ),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Preview
@Composable
private fun PopularMovieItemPreview(@PreviewParameter(MoviePreviewParameters::class) movie: Movie) {
    MovieTheme {
        SharedTransitionLayout {
            AnimatedVisibility(visible = true) {
                PopularMovieItem(
                    movie = movie,
                    isFavorite = false,
                    onClick = {},
                    onFavoriteClick = {},
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@AnimatedVisibility,
                )
            }
        }
    }
}

private val previewMovie = Movie(
    id = 1,
    title = "The great Bruno",
    overview = "The best movie in history",
    posterPath = null,
    releaseDate = "30/01/1996",
    voteAverage = 5.0,
)

class MoviePreviewParameters : PreviewParameterProvider<Movie> {
    override val values: Sequence<Movie>
        get() = sequenceOf(
            previewMovie
        )
}