package dev.themobiledev.movie.di

import dev.themobiledev.movie.ui.presentation.favorites.FavoritesViewModel
import dev.themobiledev.movie.ui.presentation.moviedetails.MovieDetailsViewModel
import dev.themobiledev.movie.ui.presentation.popularmovies.PopularMoviesViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule = module {
    viewModelOf(::PopularMoviesViewModel)
    viewModel { (movieId: Long) ->
        MovieDetailsViewModel(
            movieId,
            get(),
            get()
        )
    }
    viewModelOf(::FavoritesViewModel)
}
