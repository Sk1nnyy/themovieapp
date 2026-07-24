package dev.themobiledev.movie.data

import dev.themobiledev.movie.network.dto.GenreDto
import dev.themobiledev.movie.network.dto.MovieDetailsDto
import dev.themobiledev.movie.network.dto.MovieDto
import dev.themobiledev.movie.network.dto.MoviesResponseDto
import dev.themobiledev.movie.network.dto.VideoDto
import dev.themobiledev.movie.network.dto.VideosDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MovieMappersTest {

    @Test
    fun movieDto_toDomain_mapsAllFields() {
        val dto = MovieDto(
            id = 1,
            title = "Title",
            overview = "Overview",
            posterPath = "/poster.jpg",
            releaseDate = "2024-01-01",
            voteAverage = 7.5,
        )

        val movie = dto.toDomain()

        assertEquals(dto.id, movie.id)
        assertEquals(dto.title, movie.title)
        assertEquals(dto.overview, movie.overview)
        assertEquals(dto.posterPath, movie.posterPath)
        assertEquals(dto.releaseDate, movie.releaseDate)
        assertEquals(dto.voteAverage, movie.voteAverage)
    }

    @Test
    fun moviesResponseDto_toDomain_mapsPageAndResults() {
        val dto = MoviesResponseDto(
            page = 2,
            results = listOf(
                MovieDto(id = 1, title = "A", overview = "OA"),
                MovieDto(id = 2, title = "B", overview = "OB", posterPath = "/b.jpg", releaseDate = "2023-01-01", voteAverage = 6.2),
            ),
            totalPages = 10,
            totalResults = 200,
        )

        val page = dto.toDomain()

        assertEquals(2, page.page)
        assertEquals(10, page.totalPages)
        assertEquals(200, page.totalResults)
        assertEquals(listOf(1L, 2L), page.movies.map { it.id })
        assertEquals(listOf("A", "B"), page.movies.map { it.title })
    }

    @Test
    fun genreDto_toDomain_mapsFields() {
        val dto = GenreDto(id = 28, name = "Action")

        val genre = dto.toDomain()

        assertEquals(28, genre.id)
        assertEquals("Action", genre.name)
    }

    @Test
    fun movieDetailsDto_toDomain_mapsAllFieldsIncludingGenres() {
        val dto = MovieDetailsDto(
            id = 42,
            title = "Details",
            overview = "Overview",
            tagline = "Tagline",
            runtime = 120,
            budget = 1_000_000,
            revenue = 5_000_000,
            homepage = "https://example.com",
            genres = listOf(GenreDto(id = 1, name = "Action"), GenreDto(id = 2, name = "Drama")),
            posterPath = "/poster.jpg",
            backdropPath = "/backdrop.jpg",
            releaseDate = "2024-05-01",
            voteAverage = 8.1,
            voteCount = 900,
        )

        val details = dto.toDomain()

        assertEquals(dto.id, details.id)
        assertEquals(dto.title, details.title)
        assertEquals(dto.tagline, details.tagline)
        assertEquals(dto.runtime, details.runtime)
        assertEquals(dto.budget, details.budget)
        assertEquals(dto.revenue, details.revenue)
        assertEquals(dto.homepage, details.homepage)
        assertEquals(listOf("Action", "Drama"), details.genres.map { it.name })
        assertEquals(dto.posterPath, details.posterPath)
        assertEquals(dto.backdropPath, details.backdropPath)
        assertEquals(dto.releaseDate, details.releaseDate)
        assertEquals(dto.voteAverage, details.voteAverage)
        assertEquals(dto.voteCount, details.voteCount)
    }

    @Test
    fun movieDetailsDto_toDomain_handlesNullableDefaults() {
        val dto = MovieDetailsDto(id = 1, title = "Title", overview = "Overview")

        val details = dto.toDomain()

        assertEquals(null, details.tagline)
        assertEquals(null, details.runtime)
        assertEquals(emptyList(), details.genres)
        assertNull(details.trailerKey)
    }

    @Test
    fun movieDetailsDto_toDomain_prefersOfficialYouTubeTrailerOverEverythingElse() {
        val dto = baseDetailsDto(
            videos = VideosDto(
                results = listOf(
                    VideoDto(key = "teaser", site = "YouTube", type = "Teaser", official = true),
                    VideoDto(key = "vimeoTrailer", site = "Vimeo", type = "Trailer", official = true),
                    VideoDto(key = "unofficialTrailer", site = "YouTube", type = "Trailer", official = false),
                    VideoDto(key = "officialTrailer", site = "YouTube", type = "Trailer", official = true),
                ),
            ),
        )

        assertEquals("officialTrailer", dto.toDomain().trailerKey)
    }

    @Test
    fun movieDetailsDto_toDomain_fallsBackToUnofficialYouTubeTrailerWhenNoOfficialOneExists() {
        val dto = baseDetailsDto(
            videos = VideosDto(
                results = listOf(
                    VideoDto(key = "vimeoTrailer", site = "Vimeo", type = "Trailer", official = true),
                    VideoDto(key = "unofficialTrailer", site = "YouTube", type = "Trailer", official = false),
                ),
            ),
        )

        assertEquals("unofficialTrailer", dto.toDomain().trailerKey)
    }

    @Test
    fun movieDetailsDto_toDomain_fallsBackToYouTubeTeaserWhenNoTrailerExists() {
        val dto = baseDetailsDto(
            videos = VideosDto(results = listOf(VideoDto(key = "teaser", site = "YouTube", type = "Teaser", official = true))),
        )

        assertEquals("teaser", dto.toDomain().trailerKey)
    }

    @Test
    fun movieDetailsDto_toDomain_noYouTubeVideos_trailerKeyIsNull() {
        val dto = baseDetailsDto(
            videos = VideosDto(results = listOf(VideoDto(key = "vimeoTrailer", site = "Vimeo", type = "Trailer", official = true))),
        )

        assertNull(dto.toDomain().trailerKey)
    }

    private fun baseDetailsDto(videos: VideosDto?) = MovieDetailsDto(
        id = 1,
        title = "Title",
        overview = "Overview",
        videos = videos,
    )
}
