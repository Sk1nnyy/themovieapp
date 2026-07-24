import Dependencies
import DependenciesMacros
import SharedLogic

@DependencyClient
struct FavoritesClient: Sendable {
    var observeFavoriteIds: @Sendable () -> AsyncThrowingStream<Set<Int64>, Error> =
        { AsyncThrowingStream { $0.finish() } }
    var observeFavorites: @Sendable () -> AsyncThrowingStream<[Movie], Error> =
        { AsyncThrowingStream { $0.finish() } }
    var observeIsFavorite: @Sendable (_ movieId: Int64) -> AsyncThrowingStream<Bool, Error> =
        { _ in AsyncThrowingStream { $0.finish() } }
    var toggleFavorite: @Sendable (_ movie: Movie, _ isFavorite: Bool) async throws -> Void
}

extension FavoritesClient: DependencyKey {
    static let liveValue: FavoritesClient = {
        let repository = IOSDependencies.shared.favoritesRepository
        return FavoritesClient(
            observeFavoriteIds: {
                nativeStream(for: repository.observeFavoriteIds()) { ids in Set(ids.map(\.int64Value)) }
            },
            observeFavorites: {
                nativeStream(for: repository.observeFavorites()) { movies in movies.map(Movie.init) }
            },
            observeIsFavorite: { movieId in
                nativeStream(for: repository.observeIsFavorite(movieId: movieId), map: \.boolValue)
            },
            toggleFavorite: { movie, isFavorite in
                try await repository.toggleFavorite(movie: movie.kotlin, isFavorite: isFavorite)
            }
        )
    }()
}

extension DependencyValues {
    var favoritesClient: FavoritesClient {
        get { self[FavoritesClient.self] }
        set { self[FavoritesClient.self] = newValue }
    }
}
