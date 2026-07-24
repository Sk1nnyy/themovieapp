import ComposableArchitecture
import XCTest
@testable import MovieApp

@MainActor
final class AppFeatureTests: XCTestCase {
    func testTabSelected_favorites_justUpdatesSelectedTab() async {
        let store = TestStore(initialState: AppFeature.State()) {
            AppFeature()
        }

        await store.send(.tabSelected(.favorites)) {
            $0.selectedTab = .favorites
        }
    }

    func testTabSelected_popularMovies_justUpdatesSelectedTab() async {
        let store = TestStore(
            initialState: AppFeature.State(selectedTab: .favorites)
        ) {
            AppFeature()
        }

        await store.send(.tabSelected(.popularMovies)) {
            $0.selectedTab = .popularMovies
        }
    }

    func testBothTabsStayInSyncViaIndependentLiveSubscriptions() async {
        let movie = Movie.mock(id: 1)
        let (idsStream, idsContinuation) = AsyncThrowingStream<Set<Int64>, Error>.makeStream()
        let (favoritesStream, favoritesContinuation) = AsyncThrowingStream<[Movie], Error>.makeStream()

        let store = TestStore(initialState: AppFeature.State()) {
            AppFeature()
        } withDependencies: {
            $0.moviesClient.getPopularMovies = { _, _ in AsyncThrowingStream { $0.finish() } }
            $0.favoritesClient.observeFavoriteIds = { idsStream }
            $0.favoritesClient.observeFavorites = { favoritesStream }
        }

        let popularMoviesTask = await store.send(.popularMovies(.task)) {
            $0.popularMovies.isLoading = true
        }
        let favoritesTask = await store.send(.favorites(.task)) {
            $0.favorites.isLoading = true
        }

        // Both tabs are independently subscribed to the same underlying favorites table -
        // AppFeature doesn't forward anything between them for a change to show up on both.
        idsContinuation.yield([1])
        await store.receive(.popularMovies(.favoriteIdsResponse(.success([1])))) {
            $0.popularMovies.favoriteIds = [1]
        }

        favoritesContinuation.yield([movie])
        await store.receive(.favorites(.favoritesResponse(.success([movie])))) {
            $0.favorites.isLoading = false
            $0.favorites.favorites = [movie]
        }

        idsContinuation.finish()
        favoritesContinuation.finish()
        await popularMoviesTask.finish(timeout: .seconds(5))
        await favoritesTask.finish(timeout: .seconds(5))
    }
}
