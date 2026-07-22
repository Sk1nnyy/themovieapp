package dev.themobiledev.movie.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.metadata
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.NavDisplay
import dev.themobiledev.movie.favorites.FavoritesScreen
import dev.themobiledev.movie.moviedetails.MovieDetailsScreen
import dev.themobiledev.movie.popularmovies.PopularMoviesListScreen

private val movieDetailEnterTransition: AnimatedContentTransitionScope<Scene<*>>.() -> ContentTransform = {
    slideInHorizontally(animationSpec = tween(300)) { fullWidth -> fullWidth } togetherWith
        slideOutHorizontally(animationSpec = tween(300)) { fullWidth -> -fullWidth / 4 }
}

private val movieDetailPopTransition: AnimatedContentTransitionScope<Scene<*>>.() -> ContentTransform = {
    slideInHorizontally(animationSpec = tween(300)) { fullWidth -> -fullWidth / 4 } togetherWith
        slideOutHorizontally(animationSpec = tween(300)) { fullWidth -> fullWidth }
}

// Tap-triggered back (the TopAppBar arrow, system back button) reads PopTransitionKey; the
// edge-swipe predictive-back gesture reads this separate key instead, so both must be set.
private val movieDetailPredictivePopTransition: AnimatedContentTransitionScope<Scene<*>>.(Int) -> ContentTransform? =
    { _ -> movieDetailPopTransition() }

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MovieNavHost(modifier: Modifier = Modifier) {
    val backStack = rememberSaveable { mutableStateListOf<MovieRoute>(MovieRoute.PopularMoviesList) }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (backStack.lastOrNull() !is MovieRoute.MovieDetail) {
                MovieBottomNavigationBar(
                    currentRoute = backStack.lastOrNull(),
                    onTabSelected = { route ->
                        if (backStack.lastOrNull() != route) {
                            backStack.clear()
                            backStack.add(route)
                        }
                    },
                )
            }
        },
    ) { paddingValues ->
        SharedTransitionLayout {
            Box(
                modifier = Modifier
                    .padding(paddingValues)
                    .consumeWindowInsets(paddingValues),
            ) {
                NavDisplay(
                    backStack = backStack,
                    onBack = { backStack.removeLastOrNull() },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    entryDecorators = listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator(),
                    ),
                    entryProvider = { route ->
                        when (route) {
                            MovieRoute.PopularMoviesList -> NavEntry(route) {
                                PopularMoviesListScreen(
                                    sharedTransitionScope = this@SharedTransitionLayout,
                                    animatedVisibilityScope = LocalNavAnimatedContentScope.current,
                                    onNavigateToDetail = { movie ->
                                        val detail = MovieRoute.MovieDetail(movie.id, movie.posterPath, movie.title)
                                        if (backStack.lastOrNull() != detail) {
                                            backStack.add(detail)
                                        }
                                    },
                                )
                            }

                            MovieRoute.Favorites -> NavEntry(route) {
                                FavoritesScreen()
                            }

                            is MovieRoute.MovieDetail -> NavEntry(
                                route,
                                metadata = metadata {
                                    put(NavDisplay.TransitionKey, movieDetailEnterTransition)
                                    put(NavDisplay.PopTransitionKey, movieDetailPopTransition)
                                    put(NavDisplay.PredictivePopTransitionKey, movieDetailPredictivePopTransition)
                                },
                            ) {
                                MovieDetailsScreen(
                                    movieId = route.movieId,
                                    posterPath = route.posterPath,
                                    title = route.title,
                                    sharedTransitionScope = this@SharedTransitionLayout,
                                    animatedVisibilityScope = LocalNavAnimatedContentScope.current,
                                    onBackClick = { backStack.removeLastOrNull() },
                                )
                            }
                        }
                    },
                )
            }
        }
    }
}
