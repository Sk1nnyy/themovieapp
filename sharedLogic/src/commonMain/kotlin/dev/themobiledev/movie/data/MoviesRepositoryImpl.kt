package dev.themobiledev.movie.data

import dev.themobiledev.movie.db.MovieAppDatabase
import dev.themobiledev.movie.domain.Movie
import dev.themobiledev.movie.domain.MovieDetails
import dev.themobiledev.movie.domain.MoviesPage
import dev.themobiledev.movie.domain.MoviesRepository
import dev.themobiledev.movie.network.dto.GenreDto
import dev.themobiledev.movie.network.dto.MoviesResponseDto
import dev.themobiledev.movie.network.service.MoviesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

/**
 * DB-first with a 7-day TTL. Every read emits whatever is cached (fresh or stale) first, if
 * anything - callers that want to paint instantly can collect that; callers that only care
 * about the final state can just take the last emission. A fresh cache short-circuits the
 * network call unless [forceRefresh] is set; otherwise a fetch runs and repopulates the cache
 * on success. If the fetch fails, whatever is cached (even past TTL) is emitted instead, marked
 * [MoviesPage.isStale] / [MovieDetails.isStale], so the app stays usable offline.
 */
class MoviesRepositoryImpl(
    private val moviesApi: MoviesApi,
    database: MovieAppDatabase,
    private val clock: Clock = Clock.System,
) : MoviesRepository {

    private val queries = database.moviesCacheQueries
    private val json = Json { ignoreUnknownKeys = true }

    override fun getPopularMovies(page: Int, forceRefresh: Boolean): Flow<Result<MoviesPage>> =
        getMoviesPage(CATEGORY_POPULAR, page, forceRefresh) { moviesApi.getPopularMovies(page) }

    override fun getUpcomingMovies(page: Int, forceRefresh: Boolean): Flow<Result<MoviesPage>> =
        getMoviesPage(CATEGORY_UPCOMING, page, forceRefresh) { moviesApi.getUpcomingMovies(page) }

    override fun getTopRatedMovies(page: Int, forceRefresh: Boolean): Flow<Result<MoviesPage>> =
        getMoviesPage(CATEGORY_TOP_RATED, page, forceRefresh) { moviesApi.getTopRatedMovies(page) }

    override fun getNowPlayingMovies(page: Int, forceRefresh: Boolean): Flow<Result<MoviesPage>> =
        getMoviesPage(CATEGORY_NOW_PLAYING, page, forceRefresh) { moviesApi.getNowPlayingMovies(page) }

    override fun getMovieDetails(movieId: Long, forceRefresh: Boolean): Flow<Result<MovieDetails>> = flow {
        val cached = readCachedDetails(movieId)
        val fresh = cached != null && isFresh(cached.cachedAt)
        if (cached != null) {
            emit(Result.success(cached.details.copy(isStale = !fresh)))
            if (fresh && !forceRefresh) return@flow
        }

        moviesApi.getMovieDetails(movieId).fold(
            onSuccess = { dto ->
                val details = dto.toDomain()
                writeCachedDetails(details)
                emit(Result.success(details))
            },
            onFailure = { error ->
                // Stale cache was already emitted above; nothing more to add on failure.
                if (cached == null) emit(Result.failure(error))
            },
        )
    }.flowOn(Dispatchers.Default)

    private fun getMoviesPage(
        category: String,
        page: Int,
        forceRefresh: Boolean,
        fetch: suspend () -> Result<MoviesResponseDto>,
    ): Flow<Result<MoviesPage>> = flow {
        val cached = readCachedPage(category, page)
        val fresh = cached != null && isFresh(cached.cachedAt)
        if (cached != null) {
            emit(Result.success(cached.page.copy(isStale = !fresh)))
            if (fresh && !forceRefresh) return@flow
        }

        fetch().fold(
            onSuccess = { dto ->
                val moviesPage = dto.toDomain()
                writeCachedPage(category, page, moviesPage)
                emit(Result.success(moviesPage))
            },
            onFailure = { error ->
                // Stale cache was already emitted above; nothing more to add on failure.
                if (cached == null) emit(Result.failure(error))
            },
        )
    }.flowOn(Dispatchers.Default)

    private fun readCachedPage(category: String, page: Int): CachedPage? {
        val meta = queries.selectPageMeta(category, page.toLong()) { totalPages, totalResults, cachedAt ->
            PageMeta(totalPages, totalResults, cachedAt)
        }.executeAsOneOrNull() ?: return null

        val movies = queries.selectPageItems(category, page.toLong()) {
                movieId, title, overview, posterPath, releaseDate, voteAverage ->
            Movie(
                id = movieId,
                title = title,
                overview = overview,
                posterPath = posterPath,
                releaseDate = releaseDate,
                voteAverage = voteAverage,
            )
        }.executeAsList()

        val moviesPage = MoviesPage(
            movies = movies,
            page = page,
            totalPages = meta.totalPages.toInt(),
            totalResults = meta.totalResults.toInt(),
        )
        return CachedPage(moviesPage, meta.cachedAt)
    }

    private fun writeCachedPage(category: String, page: Int, moviesPage: MoviesPage) {
        val cachedAt = clock.now().toEpochMilliseconds()
        queries.transaction {
            queries.deletePageItems(category, page.toLong())
            moviesPage.movies.forEachIndexed { index, movie ->
                queries.insertPageItem(
                    category = category,
                    page = page.toLong(),
                    position = index.toLong(),
                    movieId = movie.id,
                    title = movie.title,
                    overview = movie.overview,
                    posterPath = movie.posterPath,
                    releaseDate = movie.releaseDate,
                    voteAverage = movie.voteAverage,
                )
            }
            queries.upsertPageMeta(
                category = category,
                page = page.toLong(),
                totalPages = moviesPage.totalPages.toLong(),
                totalResults = moviesPage.totalResults.toLong(),
                cachedAt = cachedAt,
            )
        }
    }

    private fun readCachedDetails(movieId: Long): CachedDetails? =
        queries.selectMovieDetails(movieId) {
                title, overview, tagline, runtime, budget, revenue, homepage, genresJson,
                posterPath, backdropPath, releaseDate, voteAverage, voteCount, trailerKey, cachedAt ->
            val details = MovieDetails(
                id = movieId,
                title = title,
                overview = overview,
                tagline = tagline,
                runtime = runtime?.toInt(),
                budget = budget,
                revenue = revenue,
                homepage = homepage,
                genres = json.decodeFromString(ListSerializer(GenreDto.serializer()), genresJson).map { it.toDomain() },
                posterPath = posterPath,
                backdropPath = backdropPath,
                releaseDate = releaseDate,
                voteAverage = voteAverage,
                voteCount = voteCount.toInt(),
                trailerKey = trailerKey,
            )
            CachedDetails(details, cachedAt)
        }.executeAsOneOrNull()

    private fun writeCachedDetails(details: MovieDetails) {
        queries.upsertMovieDetails(
            movieId = details.id,
            title = details.title,
            overview = details.overview,
            tagline = details.tagline,
            runtime = details.runtime?.toLong(),
            budget = details.budget,
            revenue = details.revenue,
            homepage = details.homepage,
            genresJson = json.encodeToString(
                ListSerializer(GenreDto.serializer()),
                details.genres.map { GenreDto(id = it.id, name = it.name) },
            ),
            posterPath = details.posterPath,
            backdropPath = details.backdropPath,
            releaseDate = details.releaseDate,
            voteAverage = details.voteAverage,
            voteCount = details.voteCount.toLong(),
            trailerKey = details.trailerKey,
            cachedAt = clock.now().toEpochMilliseconds(),
        )
    }

    private fun isFresh(cachedAt: Long): Boolean =
        clock.now() - Instant.fromEpochMilliseconds(cachedAt) < TTL

    private data class PageMeta(val totalPages: Long, val totalResults: Long, val cachedAt: Long)
    private data class CachedPage(val page: MoviesPage, val cachedAt: Long)
    private data class CachedDetails(val details: MovieDetails, val cachedAt: Long)

    companion object {
        private const val CATEGORY_POPULAR = "popular"
        private const val CATEGORY_UPCOMING = "upcoming"
        private const val CATEGORY_TOP_RATED = "top_rated"
        private const val CATEGORY_NOW_PLAYING = "now_playing"
        private val TTL = 7.days
    }
}
