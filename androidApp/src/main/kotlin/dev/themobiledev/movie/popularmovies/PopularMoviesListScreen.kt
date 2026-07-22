package dev.themobiledev.movie.popularmovies

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.themobiledev.movie.R
import dev.themobiledev.movie.popularmovies.components.PopularMovieItem
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PopularMoviesListScreen(
    viewModel: PopularMoviesViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    PopularMoviesListContent(
        state = state,
        onIntent = viewModel::handleIntent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PopularMoviesListContent(
    state: PopularMoviesState,
    onIntent: (PopularMoviesIntent) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(state.selectedFilter.labelRes),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
            )

            IconButton(onClick = { onIntent(PopularMoviesIntent.ShowFilters) }) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = stringResource(R.string.content_description_filters_button),
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.isLoading && state.movies.isEmpty() -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                state.error != null && state.movies.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(text = state.error)
                        Button(onClick = { onIntent(PopularMoviesIntent.Retry) }) {
                            Text(stringResource(R.string.action_retry))
                        }
                    }
                }

                else -> {
                    val gridState = rememberLazyGridState()
                    val shouldLoadMore by remember {
                        derivedStateOf {
                            val lastVisibleIndex = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                            lastVisibleIndex >= state.movies.size - 4
                        }
                    }

                    LaunchedEffect(shouldLoadMore, state.movies.size) {
                        if (shouldLoadMore) {
                            onIntent(PopularMoviesIntent.LoadNextPage)
                        }
                    }

                    LazyVerticalGrid(
                        GridCells.Fixed(2),
                        state = gridState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(state.movies, key = { it.id }) { movie ->
                            PopularMovieItem(movie)
                        }

                        if (state.isLoadingMore) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Box(modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)) {
                                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (state.isFilterSheetVisible) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { onIntent(PopularMoviesIntent.HideFilters) },
            sheetState = sheetState,
        ) {
            FilterSheetContent(
                selectedFilter = state.selectedFilter,
                onFilterSelected = { onIntent(PopularMoviesIntent.OnFilterSelected(it)) },
            )
        }
    }
}

@Composable
private fun FilterSheetContent(
    selectedFilter: MovieFilter,
    onFilterSelected: (MovieFilter) -> Unit,
) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 16.dp)) {
        Text(
            text = stringResource(R.string.filters_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        MovieFilter.entries.forEach { filter ->
            val selected = filter == selectedFilter
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(selected = selected, onClick = { onFilterSelected(filter) })
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                RadioButton(selected = selected, onClick = { onFilterSelected(filter) })
                Text(text = stringResource(filter.labelRes))
            }
        }
    }
}
