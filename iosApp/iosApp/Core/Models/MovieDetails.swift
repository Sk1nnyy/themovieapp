import Foundation
import SharedLogic

struct Genre: Equatable, Hashable, Identifiable, Sendable {
    let id: Int32
    let name: String
}

extension Genre {
    init(_ kotlin: SharedLogic.Genre) {
        self.init(id: kotlin.id, name: kotlin.name)
    }
}

struct MovieDetails: Equatable, Sendable {
    let id: Int64
    let title: String
    let overview: String
    let tagline: String?
    let runtime: Int?
    let budget: Int64
    let revenue: Int64
    let homepage: String?
    let genres: [Genre]
    let posterPath: String?
    let backdropPath: String?
    let releaseDate: String?
    let voteAverage: Double
    let voteCount: Int

    var posterURL: URL? {
        guard let posterPath else { return nil }
        return URL(string: SharedLogic.Constants.shared.POSTER_BASE_URL + posterPath)
    }
}

extension MovieDetails {
    init(_ kotlin: SharedLogic.MovieDetails) {
        self.init(
            id: kotlin.id,
            title: kotlin.title,
            overview: kotlin.overview,
            tagline: kotlin.tagline,
            runtime: kotlin.runtime.map { Int($0.int32Value) },
            budget: kotlin.budget,
            revenue: kotlin.revenue,
            homepage: kotlin.homepage,
            genres: kotlin.genres.map(Genre.init),
            posterPath: kotlin.posterPath,
            backdropPath: kotlin.backdropPath,
            releaseDate: kotlin.releaseDate,
            voteAverage: kotlin.voteAverage,
            voteCount: Int(kotlin.voteCount)
        )
    }

    var asMovie: Movie {
        Movie(
            id: id,
            title: title,
            overview: overview,
            posterPath: posterPath,
            releaseDate: releaseDate,
            voteAverage: voteAverage
        )
    }
}
