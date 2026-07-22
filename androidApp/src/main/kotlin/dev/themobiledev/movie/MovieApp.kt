package dev.themobiledev.movie

import android.app.Application
import dev.themobiledev.movie.di.initKoin
import dev.themobiledev.movie.di.viewModelModule
import org.koin.android.ext.koin.androidContext

class MovieApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@MovieApp)
            modules(viewModelModule)
        }
    }
}
