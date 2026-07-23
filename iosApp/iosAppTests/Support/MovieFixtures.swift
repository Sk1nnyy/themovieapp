@testable import MovieApp

extension Movie {
    static func mock(
        id: Int64,
        title: String = "Test Movie",
        overview: String = "Overview",
        posterPath: String? = nil,
        releaseDate: String? = "2024-01-01",
        voteAverage: Double = 7.0
    ) -> Movie {
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
