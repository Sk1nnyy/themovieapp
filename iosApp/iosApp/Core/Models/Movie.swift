import Foundation
import SharedLogic

struct Movie: Equatable, Hashable, Identifiable, Sendable {
    let id: Int64
    let title: String
    let overview: String
    let posterPath: String?
    let releaseDate: String?
    let voteAverage: Double

    var posterURL: URL? {
        guard let posterPath else { return nil }
        return URL(string: SharedLogic.Constants.shared.POSTER_BASE_URL + posterPath)
    }
}

extension Movie {
    init(_ kotlin: SharedLogic.Movie) {
        self.init(
            id: kotlin.id,
            title: kotlin.title,
            overview: kotlin.overview,
            posterPath: kotlin.posterPath,
            releaseDate: kotlin.releaseDate,
            voteAverage: kotlin.voteAverage
        )
    }

    var kotlin: SharedLogic.Movie {
        SharedLogic.Movie(
            id: id,
            title: title,
            overview: overview,
            posterPath: posterPath,
            releaseDate: releaseDate,
            voteAverage: voteAverage
        )
    }
}

struct MoviesPage: Equatable, Sendable {
    let movies: [Movie]
    let page: Int
    let totalPages: Int
    let totalResults: Int
    let isStale: Bool

    init(movies: [Movie], page: Int, totalPages: Int, totalResults: Int, isStale: Bool = false) {
        self.movies = movies
        self.page = page
        self.totalPages = totalPages
        self.totalResults = totalResults
        self.isStale = isStale
    }
}

extension MoviesPage {
    init(_ kotlin: SharedLogic.MoviesPage) {
        self.init(
            movies: kotlin.movies.map(Movie.init),
            page: Int(kotlin.page),
            totalPages: Int(kotlin.totalPages),
            totalResults: Int(kotlin.totalResults),
            isStale: kotlin.isStale
        )
    }
}
