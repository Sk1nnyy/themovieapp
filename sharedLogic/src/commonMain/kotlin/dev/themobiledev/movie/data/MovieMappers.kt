package dev.themobiledev.movie.data

import dev.themobiledev.movie.domain.Genre
import dev.themobiledev.movie.domain.Movie
import dev.themobiledev.movie.domain.MovieDetails
import dev.themobiledev.movie.domain.MoviesPage
import dev.themobiledev.movie.network.dto.GenreDto
import dev.themobiledev.movie.network.dto.MovieDetailsDto
import dev.themobiledev.movie.network.dto.MovieDto
import dev.themobiledev.movie.network.dto.MoviesResponseDto

fun MovieDto.toDomain(): Movie =
    Movie(
        id = id,
        title = title,
        overview = overview,
        posterPath = posterPath,
        releaseDate = releaseDate,
        voteAverage = voteAverage,
    )

fun MoviesResponseDto.toDomain(): MoviesPage =
    MoviesPage(
        movies = results.map { it.toDomain() },
        page = page,
        totalPages = totalPages,
        totalResults = totalResults,
    )

fun GenreDto.toDomain(): Genre = Genre(id = id, name = name)

fun MovieDetails.toMovie(): Movie =
    Movie(
        id = id,
        title = title,
        overview = overview,
        posterPath = posterPath,
        releaseDate = releaseDate,
        voteAverage = voteAverage,
    )

fun MovieDetailsDto.toDomain(): MovieDetails =
    MovieDetails(
        id = id,
        title = title,
        overview = overview,
        tagline = tagline,
        runtime = runtime,
        budget = budget,
        revenue = revenue,
        homepage = homepage,
        genres = genres.map { it.toDomain() },
        posterPath = posterPath,
        backdropPath = backdropPath,
        releaseDate = releaseDate,
        voteAverage = voteAverage,
        voteCount = voteCount,
    )
