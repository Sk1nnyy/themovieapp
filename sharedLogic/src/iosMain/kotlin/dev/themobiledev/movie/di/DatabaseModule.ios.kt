package dev.themobiledev.movie.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import dev.themobiledev.movie.db.MovieAppDatabase
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformDatabaseModule: Module = module {
    single<SqlDriver> { NativeSqliteDriver(MovieAppDatabase.Schema, "movieapp.db") }
}
