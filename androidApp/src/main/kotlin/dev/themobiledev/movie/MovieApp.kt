package dev.themobiledev.movie

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import dev.themobiledev.movie.di.initKoin
import dev.themobiledev.movie.di.viewModelModule
import dev.themobiledev.movie.utils.MovieImageLoaderFactory
import org.koin.android.ext.koin.androidContext

class MovieApp : Application(), SingletonImageLoader.Factory {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@MovieApp)
            modules(viewModelModule)
        }
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        MovieImageLoaderFactory().newImageLoader(context)
}
