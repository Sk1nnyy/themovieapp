package dev.themobiledev.movie

import android.app.Application
import dev.themobiledev.movie.di.initKoin

class MovieApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin()
    }
}
