package dev.themobiledev.movie.di

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import dev.themobiledev.movie.db.MovieAppDatabase
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformDatabaseModule: Module = module {
    single<SqlDriver> { AndroidSqliteDriver(MovieAppDatabase.Schema, get<Context>(), "movieapp.db") }
}
