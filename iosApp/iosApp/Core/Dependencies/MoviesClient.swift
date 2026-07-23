import Dependencies
import DependenciesMacros
import SharedLogic

@DependencyClient
struct MoviesClient: Sendable {
    var getPopularMovies: @Sendable (_ page: Int) async throws -> MoviesPage
    var getUpcomingMovies: @Sendable (_ page: Int) async throws -> MoviesPage
    var getTopRatedMovies: @Sendable (_ page: Int) async throws -> MoviesPage
    var getNowPlayingMovies: @Sendable (_ page: Int) async throws -> MoviesPage
    var getMovieDetails: @Sendable (_ movieId: Int64) async throws -> MovieDetails
}

extension MoviesClient: DependencyKey {
    static let liveValue: MoviesClient = {
        let repository = IOSDependencies.shared.moviesRepository
        return MoviesClient(
            getPopularMovies: { page in
                MoviesPage(try await repository.getPopularMovies(page: Int32(page)))
            },
            getUpcomingMovies: { page in
                MoviesPage(try await repository.getUpcomingMovies(page: Int32(page)))
            },
            getTopRatedMovies: { page in
                MoviesPage(try await repository.getTopRatedMovies(page: Int32(page)))
            },
            getNowPlayingMovies: { page in
                MoviesPage(try await repository.getNowPlayingMovies(page: Int32(page)))
            },
            getMovieDetails: { movieId in
                MovieDetails(try await repository.getMovieDetails(movieId: movieId))
            }
        )
    }()
}

extension DependencyValues {
    var moviesClient: MoviesClient {
        get { self[MoviesClient.self] }
        set { self[MoviesClient.self] = newValue }
    }
}
