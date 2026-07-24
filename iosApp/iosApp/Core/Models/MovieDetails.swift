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
    let trailerKey: String?
    let isStale: Bool

    init(
        id: Int64,
        title: String,
        overview: String,
        tagline: String?,
        runtime: Int?,
        budget: Int64,
        revenue: Int64,
        homepage: String?,
        genres: [Genre],
        posterPath: String?,
        backdropPath: String?,
        releaseDate: String?,
        voteAverage: Double,
        voteCount: Int,
        trailerKey: String? = nil,
        isStale: Bool = false
    ) {
        self.id = id
        self.title = title
        self.overview = overview
        self.tagline = tagline
        self.runtime = runtime
        self.budget = budget
        self.revenue = revenue
        self.homepage = homepage
        self.genres = genres
        self.posterPath = posterPath
        self.backdropPath = backdropPath
        self.releaseDate = releaseDate
        self.voteAverage = voteAverage
        self.voteCount = voteCount
        self.trailerKey = trailerKey
        self.isStale = isStale
    }

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
            voteCount: Int(kotlin.voteCount),
            trailerKey: kotlin.trailerKey,
            isStale: kotlin.isStale
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
