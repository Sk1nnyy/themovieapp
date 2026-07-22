package dev.themobiledev.movie.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import dev.themobiledev.movie.favorites.FavoritesScreen
import dev.themobiledev.movie.popularmovies.PopularMoviesListScreen

@Composable
fun MovieNavHost(modifier: Modifier = Modifier) {
    val backStack = rememberSaveable { mutableStateListOf<MovieRoute>(MovieRoute.PopularMoviesList) }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            MovieBottomNavigationBar(
                currentRoute = backStack.lastOrNull(),
                onTabSelected = { route ->
                    if (backStack.lastOrNull() != route) {
                        backStack.clear()
                        backStack.add(route)
                    }
                },
            )
        },
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeLastOrNull() },
                entryProvider = { route ->
                    when (route) {
                        MovieRoute.PopularMoviesList -> NavEntry(route) {
                            PopularMoviesListScreen()
                        }

                        MovieRoute.Favorites -> NavEntry(route) {
                            FavoritesScreen()
                        }
                    }
                },
            )
        }
    }
}
