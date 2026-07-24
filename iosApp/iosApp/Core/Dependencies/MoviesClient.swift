import Dependencies
import DependenciesMacros
import SharedLogic

@DependencyClient
struct MoviesClient: Sendable {
    var getPopularMovies: @Sendable (_ page: Int, _ forceRefresh: Bool) -> AsyncThrowingStream<MoviesPage, Error> =
        { _, _ in AsyncThrowingStream { $0.finish() } }
    var getUpcomingMovies: @Sendable (_ page: Int, _ forceRefresh: Bool) -> AsyncThrowingStream<MoviesPage, Error> =
        { _, _ in AsyncThrowingStream { $0.finish() } }
    var getTopRatedMovies: @Sendable (_ page: Int, _ forceRefresh: Bool) -> AsyncThrowingStream<MoviesPage, Error> =
        { _, _ in AsyncThrowingStream { $0.finish() } }
    var getNowPlayingMovies: @Sendable (_ page: Int, _ forceRefresh: Bool) -> AsyncThrowingStream<MoviesPage, Error> =
        { _, _ in AsyncThrowingStream { $0.finish() } }
    var getMovieDetails: @Sendable (_ movieId: Int64, _ forceRefresh: Bool) -> AsyncThrowingStream<MovieDetails, Error> =
        { _, _ in AsyncThrowingStream { $0.finish() } }
}

extension MoviesClient: DependencyKey {
    static let liveValue: MoviesClient = {
        let repository = IOSDependencies.shared.moviesRepository
        return MoviesClient(
            getPopularMovies: { page, forceRefresh in
                nativeStream(for: repository.getPopularMovies(page: Int32(page), forceRefresh: forceRefresh), map: MoviesPage.init)
            },
            getUpcomingMovies: { page, forceRefresh in
                nativeStream(for: repository.getUpcomingMovies(page: Int32(page), forceRefresh: forceRefresh), map: MoviesPage.init)
            },
            getTopRatedMovies: { page, forceRefresh in
                nativeStream(for: repository.getTopRatedMovies(page: Int32(page), forceRefresh: forceRefresh), map: MoviesPage.init)
            },
            getNowPlayingMovies: { page, forceRefresh in
                nativeStream(for: repository.getNowPlayingMovies(page: Int32(page), forceRefresh: forceRefresh), map: MoviesPage.init)
            },
            getMovieDetails: { movieId, forceRefresh in
                nativeStream(for: repository.getMovieDetails(movieId: movieId, forceRefresh: forceRefresh), map: MovieDetails.init)
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
