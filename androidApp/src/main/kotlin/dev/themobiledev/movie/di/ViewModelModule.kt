package dev.themobiledev.movie.di

import dev.themobiledev.movie.popularmovies.PopularMoviesViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule = module {
    viewModelOf(::PopularMoviesViewModel)
}
