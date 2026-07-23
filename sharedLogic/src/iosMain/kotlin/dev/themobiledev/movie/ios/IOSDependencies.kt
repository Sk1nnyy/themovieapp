package dev.themobiledev.movie.ios

import dev.themobiledev.movie.domain.FavoritesRepository
import dev.themobiledev.movie.domain.MoviesRepository
import org.koin.mp.KoinPlatform

object IOSDependencies {
    val moviesRepository: IOSMoviesRepository
        get() = IOSMoviesRepository(KoinPlatform.getKoin().get<MoviesRepository>())

    val favoritesRepository: IOSFavoritesRepository
        get() = IOSFavoritesRepository(KoinPlatform.getKoin().get<FavoritesRepository>())
}
