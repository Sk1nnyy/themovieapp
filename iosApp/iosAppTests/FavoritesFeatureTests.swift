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
            $0.favoritesClient.observeFavorites = { AsyncThrowingStream { $0.yield(favorites); $0.finish() } }
        }

        await store.send(.task) {
            $0.isLoading = true
        }
        await store.receive(.favoritesResponse(.success(favorites))) {
            $0.isLoading = false
            $0.favorites = favorites
        }
    }

    func testRemoveTapped_callsToggleFavoriteWithTrue() async {
        let movie = Movie.mock(id: 1)
        let toggleCalls = CallRecorder<Bool>()
        let store = TestStore(
            initialState: FavoritesFeature.State(favorites: [movie])
        ) {
            FavoritesFeature()
        } withDependencies: {
            $0.favoritesClient.toggleFavorite = { _, isFavorite in
                await toggleCalls.record(isFavorite)
            }
        }

        await store.send(.removeTapped(movie))
        await store.receive(.removeFavoriteResponse(.success(EquatableVoid())))

        let values = await toggleCalls.values
        XCTAssertEqual(values, [true])
    }

    func testRemoveTapped_failure_sendsFailureResponse() async {
        let movie = Movie.mock(id: 1)
        struct TestError: Error {}
        let store = TestStore(
            initialState: FavoritesFeature.State(favorites: [movie])
        ) {
            FavoritesFeature()
        } withDependencies: {
            $0.favoritesClient.toggleFavorite = { _, _ in throw TestError() }
        }

        await store.send(.removeTapped(movie))
        await store.receive(.removeFavoriteResponse(.failure(TestError().equatable)))
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

    func testObserveFavorites_laterEmissionRemovesEntry() async {
        let movie = Movie.mock(id: 1)
        let (favoritesStream, favoritesContinuation) = AsyncThrowingStream<[Movie], Error>.makeStream()
        let store = TestStore(initialState: FavoritesFeature.State()) {
            FavoritesFeature()
        } withDependencies: {
            $0.favoritesClient.observeFavorites = { favoritesStream }
        }

        let task = await store.send(.task) {
            $0.isLoading = true
        }

        favoritesContinuation.yield([movie])
        await store.receive(.favoritesResponse(.success([movie]))) {
            $0.isLoading = false
            $0.favorites = [movie]
        }

        // A favorite removed anywhere else (e.g. a detail screen pushed under this same list, or
        // the Popular Movies tab) writes to the same underlying table this subscription observes,
        // so the list picks it up without needing its own path- or tab-specific resync logic.
        favoritesContinuation.yield([])
        await store.receive(.favoritesResponse(.success([]))) {
            $0.favorites = []
        }

        favoritesContinuation.finish()
        await task.finish(timeout: .seconds(5))
    }
}
