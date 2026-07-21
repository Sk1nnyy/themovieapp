package dev.themobiledev.movie.di

import dev.themobiledev.movie.network.createHttpClient
import dev.themobiledev.movie.network.createPlatformEngine
import dev.themobiledev.movie.network.service.MoviesApi
import org.koin.dsl.module

val networkModule = module {
    single { createPlatformEngine() }
    single { createHttpClient(get()) }
    single { MoviesApi(get()) }
}
