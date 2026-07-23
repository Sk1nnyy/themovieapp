package dev.themobiledev.movie.di

import dev.themobiledev.movie.data.MoviesRepositoryImpl
import dev.themobiledev.movie.domain.MoviesRepository
import org.koin.dsl.module

val repositoryModule = module {
    single<MoviesRepository> { MoviesRepositoryImpl(get(), get()) }
}
