import ComposableArchitecture
import XCTest
@testable import MovieApp

@MainActor
final class FavoritesFeatureTests: XCTestCase {
    func testTask_reflectsFavoritesFromRepository() async {
        let favorites = [Movie.mock(id: 1), Movie.mock(id: 2)]
        let store = TestStore(initialState: FavoritesFeature.State()) {
            FavoritesFeature()
        } withDependencies: {
            $0.favoritesClient.getFavorites = { favorites }
        }

        await store.send(.task) {
            $0.isLoading = true
        }
        await store.receive(.favoritesResponse(.success(favorites))) {
            $0.isLoading = false
            $0.favorites = favorites
        }
    }

    func testRemoveTapped_removesFavoriteFromRepository() async {
        let movie = Movie.mock(id: 1)
        let store = TestStore(
            initialState: FavoritesFeature.State(favorites: [movie])
        ) {
            FavoritesFeature()
        } withDependencies: {
            $0.favoritesClient.toggleFavorite = { _, _ in }
        }

        await store.send(.removeTapped(movie)) {
            $0.favorites = []
        }
        await store.receive(.removeFavoriteResponse(.success(EquatableVoid())))
    }

    func testRemoveTapped_failure_resyncsFromSource() async {
        let movie = Movie.mock(id: 1)
        struct TestError: Error {}
        let refreshed = [movie]
        let store = TestStore(
            initialState: FavoritesFeature.State(favorites: [movie])
        ) {
            FavoritesFeature()
        } withDependencies: {
            $0.favoritesClient.toggleFavorite = { _, _ in throw TestError() }
            $0.favoritesClient.getFavorites = { refreshed }
        }

        await store.send(.removeTapped(movie)) {
            $0.favorites = []
        }
        await store.receive(.removeFavoriteResponse(.failure(TestError().equatable)))
        await store.receive(.favoritesResponse(.success(refreshed))) {
            $0.favorites = refreshed
        }
    }

    func testMovieTapped_pushesDetailOntoPath() async {
        let movie = Movie.mock(id: 1)
        let store = TestStore(initialState: FavoritesFeature.State(favorites: [movie])) {
            FavoritesFeature()
        }

        await store.send(.movieTapped(movie)) {
            $0.path.append(.detail(MovieDetailFeature.State(movie: movie, isFavorite: true)))
        }
    }

    func testPathToggleFavoriteResponse_removesFromFavoritesList() async {
        let movie = Movie.mock(id: 1)
        let detailState = MovieDetailFeature.State(movie: movie, isFavorite: true)
        let store = TestStore(
            initialState: FavoritesFeature.State(favorites: [movie], path: StackState([.detail(detailState)]))
        ) {
            FavoritesFeature()
        }

        await store.send(
            .path(.element(id: 0, action: .detail(.toggleFavoriteResponse(wasFavorite: true, result: .success(EquatableVoid())))))
        ) {
            $0.favorites = []
        }
    }
}
