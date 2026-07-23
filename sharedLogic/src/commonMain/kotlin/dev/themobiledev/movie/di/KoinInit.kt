@file:OptIn(ExperimentalAtomicApi::class)

package dev.themobiledev.movie.di

import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

fun initKoin(config: KoinAppDeclaration = {}) {
    startKoin {
        config()
        modules(networkModule, repositoryModule, databaseModule, platformDatabaseModule)
    }
}

private val koinStarted = AtomicInt(0)

fun doInitKoin(config: KoinAppDeclaration = {}) {
    if (koinStarted.compareAndSet(0, 1)) {
        initKoin(config)
    }
}
