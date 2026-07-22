package dev.themobiledev.movie.popularmovies.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import dev.themobiledev.movie.theme.MovieTheme


@Composable
fun PopularMovieItem(movie: Movie, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth()) {

            SubcomposeAsyncImage(
                model = movie.posterPath?.let { POSTER_BASE_URL + it },
                contentDescription = stringResource(R.string.content_description_movie_poster_image),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(.8f),
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

            Text(text = movie.title, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium, maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Preview
@Composable
private fun PopularMovieItemPreview(@PreviewParameter(MoviePreviewParameters::class) movie: Movie) {
    MovieTheme {
        PopularMovieItem(movie = movie)
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