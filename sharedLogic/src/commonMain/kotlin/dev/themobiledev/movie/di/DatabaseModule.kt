package dev.themobiledev.movie.di

import dev.themobiledev.movie.data.FavoritesRepositoryImpl
import dev.themobiledev.movie.db.MovieAppDatabase
import dev.themobiledev.movie.domain.FavoritesRepository
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Provides the [app.cash.sqldelight.db.SqlDriver] single. Each platform's driver needs
 * different construction arguments (Android needs a Context, iOS doesn't), so the driver
 * itself is bound here rather than in [databaseModule].
 */
expect val platformDatabaseModule: Module

val databaseModule = module {
    single { MovieAppDatabase(get()) }
    single<FavoritesRepository> { FavoritesRepositoryImpl(get()) }
}
