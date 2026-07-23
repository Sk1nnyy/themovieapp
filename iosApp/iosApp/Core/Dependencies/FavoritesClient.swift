import Dependencies
import DependenciesMacros
import SharedLogic

@DependencyClient
struct FavoritesClient: Sendable {
    var getFavoriteIds: @Sendable () async throws -> Set<Int64> = { [] }
    var getFavorites: @Sendable () async throws -> [Movie] = { [] }
    var isFavorite: @Sendable (_ movieId: Int64) async throws -> Bool
    var toggleFavorite: @Sendable (_ movie: Movie, _ isFavorite: Bool) async throws -> Void
}

extension FavoritesClient: DependencyKey {
    static let liveValue: FavoritesClient = {
        let repository = IOSDependencies.shared.favoritesRepository
        return FavoritesClient(
            getFavoriteIds: {
                let ids = try await repository.getFavoriteIds()
                return Set(ids.map(\.int64Value))
            },
            getFavorites: {
                try await repository.getFavorites().map(Movie.init)
            },
            isFavorite: { movieId in
                try await repository.isFavorite(movieId: movieId).boolValue
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
