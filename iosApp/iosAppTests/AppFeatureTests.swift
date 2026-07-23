import ComposableArchitecture
import XCTest
@testable import MovieApp

@MainActor
final class AppFeatureTests: XCTestCase {
    func testTabSelected_favorites_triggersReload() async {
        let store = TestStore(initialState: AppFeature.State()) {
            AppFeature()
        } withDependencies: {
            $0.favoritesClient.getFavorites = { [] }
        }

        await store.send(.tabSelected(.favorites)) {
            $0.selectedTab = .favorites
        }
        await store.receive(.favorites(.task)) {
            $0.favorites.isLoading = true
        }
        await store.receive(.favorites(.favoritesResponse(.success([])))) {
            $0.favorites.isLoading = false
        }
    }

    func testTabSelected_popularMovies_doesNotReload() async {
        let store = TestStore(
            initialState: AppFeature.State(selectedTab: .favorites)
        ) {
            AppFeature()
        }

        await store.send(.tabSelected(.popularMovies)) {
            $0.selectedTab = .popularMovies
        }
    }
}
