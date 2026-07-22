package dev.themobiledev.movie.di

import dev.themobiledev.movie.favorites.FavoritesViewModel
import dev.themobiledev.movie.moviedetails.MovieDetailsViewModel
import dev.themobiledev.movie.popularmovies.PopularMoviesViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule = module {
    viewModelOf(::PopularMoviesViewModel)
    viewModel { (movieId: Long) -> MovieDetailsViewModel(movieId, get(), get()) }
    viewModelOf(::FavoritesViewModel)
}
