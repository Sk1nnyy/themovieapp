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

    func testFavoriteToggledFromPopularMoviesGrid_resyncsFavoritesTab() async {
        let movie = Movie.mock(id: 1)
        let store = TestStore(initialState: AppFeature.State()) {
            AppFeature()
        } withDependencies: {
            $0.favoritesClient.toggleFavorite = { _, _ in }
            $0.favoritesClient.getFavorites = { [movie] }
        }

        await store.send(.popularMovies(.favoriteTapped(movie))) {
            $0.popularMovies.favoriteIds = [1]
        }
        await store.receive(.popularMovies(.toggleFavoriteResponse(id: 1, wasFavorite: false, result: .success(EquatableVoid()))))
        await store.receive(.favorites(.task)) {
            $0.favorites.isLoading = true
        }
        await store.receive(.favorites(.favoritesResponse(.success([movie])))) {
            $0.favorites.isLoading = false
            $0.favorites.favorites = [movie]
        }
    }

    func testFavoriteToggledFromPopularMoviesDetailPush_resyncsFavoritesTab() async {
        let movie = Movie.mock(id: 1)
        let detailState = MovieDetailFeature.State(movie: movie, isFavorite: false)
        let store = TestStore(
            initialState: AppFeature.State(
                popularMovies: PopularMoviesFeature.State(path: StackState([.detail(detailState)]))
            )
        ) {
            AppFeature()
        } withDependencies: {
            $0.favoritesClient.getFavorites = { [movie] }
        }

        await store.send(
            .popularMovies(.path(.element(id: 0, action: .detail(.toggleFavoriteResponse(wasFavorite: false, result: .success(EquatableVoid()))))))
        ) {
            $0.popularMovies.favoriteIds = [1]
        }
        await store.receive(.favorites(.task)) {
            $0.favorites.isLoading = true
        }
        await store.receive(.favorites(.favoritesResponse(.success([movie])))) {
            $0.favorites.isLoading = false
            $0.favorites.favorites = [movie]
        }
    }

    func testFavoriteRemovedFromFavoritesTab_resyncsPopularMoviesFavoriteIds() async {
        let movie = Movie.mock(id: 1)
        let store = TestStore(
            initialState: AppFeature.State(
                popularMovies: PopularMoviesFeature.State(favoriteIds: [1]),
                favorites: FavoritesFeature.State(favorites: [movie])
            )
        ) {
            AppFeature()
        } withDependencies: {
            $0.favoritesClient.toggleFavorite = { _, _ in }
            $0.favoritesClient.getFavoriteIds = { [] }
        }

        await store.send(.favorites(.removeTapped(movie))) {
            $0.favorites.favorites = []
        }
        await store.receive(.favorites(.removeFavoriteResponse(.success(EquatableVoid()))))
        await store.receive(.popularMovies(.refreshFavoriteIds))
        await store.receive(.popularMovies(.favoriteIdsResponse(.success([])))) {
            $0.popularMovies.favoriteIds = []
        }
    }

    func testFavoriteToggledFromFavoritesDetailPush_resyncsPopularMoviesFavoriteIds() async {
        let movie = Movie.mock(id: 1)
        let detailState = MovieDetailFeature.State(movie: movie, isFavorite: true)
        let store = TestStore(
            initialState: AppFeature.State(
                popularMovies: PopularMoviesFeature.State(favoriteIds: [1]),
                favorites: FavoritesFeature.State(favorites: [movie], path: StackState([.detail(detailState)]))
            )
        ) {
            AppFeature()
        } withDependencies: {
            $0.favoritesClient.getFavoriteIds = { [] }
        }

        await store.send(
            .favorites(.path(.element(id: 0, action: .detail(.toggleFavoriteResponse(wasFavorite: true, result: .success(EquatableVoid()))))))
        ) {
            $0.favorites.favorites = []
        }
        await store.receive(.popularMovies(.refreshFavoriteIds))
        await store.receive(.popularMovies(.favoriteIdsResponse(.success([])))) {
            $0.popularMovies.favoriteIds = []
        }
    }
}
