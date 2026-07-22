package dev.themobiledev.movie.di

import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

/**
 * Starts the single, process-wide Koin instance. Platforms fold any
 * platform-specific setup (e.g. Android's androidContext()) into
 * [config] rather than ever calling startKoin() a second time.
 */
fun initKoin(config: KoinAppDeclaration = {}) {
    startKoin {
        config()
        modules(networkModule, repositoryModule, databaseModule, platformDatabaseModule)
    }
}
