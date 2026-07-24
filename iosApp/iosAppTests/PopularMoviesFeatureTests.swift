import ComposableArchitecture
import XCTest
@testable import MovieApp

@MainActor
final class PopularMoviesFeatureTests: XCTestCase {
    func testTask_emitsLoadingThenPopularMoviesAndFavorites() async {
        let page = MoviesPage(movies: [.mock(id: 1), .mock(id: 2)], page: 1, totalPages: 3, totalResults: 40)
        let store = TestStore(initialState: PopularMoviesFeature.State()) {
            PopularMoviesFeature()
        } withDependencies: {
            $0.moviesClient.getPopularMovies = { _, _ in AsyncThrowingStream { $0.yield(page); $0.finish() } }
            $0.favoritesClient.getFavoriteIds = { [1] }
        }
        await store.send(.task) {
            $0.isLoading = true
        }
        await store.receive(.favoriteIdsResponse(.success([1]))) {
            $0.favoriteIds = [1]
        }
        await store.receive(.moviesResponse(.success(page))) {
            $0.isLoading = false
            $0.pagesByNumber = [1: page.movies]
            $0.movies = page.movies
            $0.currentPage = 1
            $0.totalPages = 3
        }
    }

    func testRetryTapped_reloadsMovies() async {
        let page = MoviesPage(movies: [.mock(id: 5)], page: 1, totalPages: 1, totalResults: 1)
        let store = TestStore(
            initialState: PopularMoviesFeature.State(errorMessage: "boom")
        ) {
            PopularMoviesFeature()
        } withDependencies: {
            $0.moviesClient.getPopularMovies = { _, _ in AsyncThrowingStream { $0.yield(page); $0.finish() } }
        }

        await store.send(.retryTapped) {
            $0.isLoading = true
            $0.errorMessage = nil
        }
        await store.receive(.moviesResponse(.success(page))) {
            $0.isLoading = false
            $0.pagesByNumber = [1: page.movies]
            $0.movies = page.movies
            $0.currentPage = 1
            $0.totalPages = 1
        }
    }

    func testRetryTapped_cachedMoviesShownImmediately_thenUpdatedByNetwork() async {
        let cachedPage = MoviesPage(movies: [.mock(id: 1, title: "Cached Movie")], page: 1, totalPages: 1, totalResults: 1)
        let freshPage = MoviesPage(movies: [.mock(id: 1, title: "Fresh Movie")], page: 1, totalPages: 1, totalResults: 1)
        let store = TestStore(
            initialState: PopularMoviesFeature.State(errorMessage: "boom")
        ) {
            PopularMoviesFeature()
        } withDependencies: {
            $0.moviesClient.getPopularMovies = { _, _ in
                AsyncThrowingStream { continuation in
                    continuation.yield(cachedPage)
                    continuation.yield(freshPage)
                    continuation.finish()
                }
            }
        }

        await store.send(.retryTapped) {
            $0.isLoading = true
            $0.errorMessage = nil
        }
        await store.receive(.moviesResponse(.success(cachedPage))) {
            $0.isLoading = false
            $0.pagesByNumber = [1: cachedPage.movies]
            $0.movies = cachedPage.movies
            $0.currentPage = 1
            $0.totalPages = 1
        }
        await store.receive(.moviesResponse(.success(freshPage))) {
            $0.pagesByNumber = [1: freshPage.movies]
            $0.movies = freshPage.movies
        }
    }

    func testFilterSelected_whileRequestInFlight_ignoresStaleResult() async {
        let popularGate = Gate()
        let popularPage = MoviesPage(movies: [.mock(id: 1, title: "Popular Movie")], page: 1, totalPages: 1, totalResults: 1)
        let upcomingPage = MoviesPage(movies: [.mock(id: 2, title: "Upcoming Movie")], page: 1, totalPages: 1, totalResults: 1)

        let store = TestStore(
            initialState: PopularMoviesFeature.State(errorMessage: nil)
        ) {
            PopularMoviesFeature()
        } withDependencies: {
            $0.moviesClient.getPopularMovies = { _, _ in
                AsyncThrowingStream { continuation in
                    Task {
                        await popularGate.wait()
                        continuation.yield(popularPage)
                        continuation.finish()
                    }
                }
            }
            $0.moviesClient.getUpcomingMovies = { _, _ in AsyncThrowingStream { $0.yield(upcomingPage); $0.finish() } }
        }

        let staleLoad = await store.send(.retryTapped) {
            $0.isLoading = true
        }

        await store.send(.filterSelected(.upcoming)) {
            $0.selectedFilter = .upcoming
            $0.movies = []
            $0.currentPage = 0
            $0.totalPages = 1
        }
        await store.receive(.moviesResponse(.success(upcomingPage))) {
            $0.isLoading = false
            $0.pagesByNumber = [1: upcomingPage.movies]
            $0.movies = upcomingPage.movies
            $0.currentPage = 1
            $0.totalPages = 1
        }

        await popularGate.open()
        await staleLoad.finish(timeout: .seconds(5))
    }

    func testFilterSelected_sameFilter_justDismissesSheet() async {
        let store = TestStore(
            initialState: PopularMoviesFeature.State(isFilterSheetPresented: true)
        ) {
            PopularMoviesFeature()
        }

        await store.send(.filterSelected(.popular)) {
            $0.isFilterSheetPresented = false
        }
    }

    func testLoadNextPageIfNeeded_emitsLoadingMoreThenAppendsDedupedMovies() async {
        let existing = (1...10).map { Movie.mock(id: Int64($0)) }
        let nextPage = MoviesPage(
            movies: [.mock(id: 9), .mock(id: 11)],
            page: 2,
            totalPages: 3,
            totalResults: 30
        )
        let store = TestStore(
            initialState: PopularMoviesFeature.State(
                movies: existing,
                pagesByNumber: [1: existing],
                currentPage: 1,
                totalPages: 3
            )
        ) {
            PopularMoviesFeature()
        } withDependencies: {
            $0.moviesClient.getPopularMovies = { _, _ in AsyncThrowingStream { $0.yield(nextPage); $0.finish() } }
        }

        await store.send(.loadNextPageIfNeeded(currentItem: existing[7])) {
            $0.isLoadingMore = true
        }
        await store.receive(.moviesResponse(.success(nextPage))) {
            $0.isLoadingMore = false
            $0.pagesByNumber[2] = nextPage.movies
            $0.movies = existing + [.mock(id: 11)]
            $0.currentPage = 2
            $0.totalPages = 3
        }
    }

    func testLoadNextPageIfNeeded_notNearEnd_doesNothing() async {
        let existing = (1...10).map { Movie.mock(id: Int64($0)) }
        let store = TestStore(
            initialState: PopularMoviesFeature.State(
                movies: existing,
                pagesByNumber: [1: existing],
                currentPage: 1,
                totalPages: 3
            )
        ) {
            PopularMoviesFeature()
        }

        await store.send(.loadNextPageIfNeeded(currentItem: existing[0]))
    }

    func testLoadNextPageIfNeeded_staleThenFreshEmissionForSamePage_replacesRatherThanAccumulates() async {
        let existing = [Movie.mock(id: 1), Movie.mock(id: 2)]
        let stalePage = MoviesPage(movies: [.mock(id: 3), .mock(id: 4)], page: 2, totalPages: 2, totalResults: 4, isStale: true)
        let freshPage = MoviesPage(movies: [.mock(id: 4), .mock(id: 5)], page: 2, totalPages: 2, totalResults: 4)
        let staleGate = Gate()

        let store = TestStore(
            initialState: PopularMoviesFeature.State(
                movies: existing,
                pagesByNumber: [1: existing],
                currentPage: 1,
                totalPages: 2
            )
        ) {
            PopularMoviesFeature()
        } withDependencies: {
            $0.moviesClient.getPopularMovies = { _, _ in
                AsyncThrowingStream { continuation in
                    continuation.yield(stalePage)
                    Task {
                        await staleGate.wait()
                        continuation.yield(freshPage)
                        continuation.finish()
                    }
                }
            }
        }

        await store.send(.loadNextPageIfNeeded(currentItem: existing[1])) {
            $0.isLoadingMore = true
        }
        await store.receive(.moviesResponse(.success(stalePage))) {
            $0.isLoadingMore = false
            $0.pagesByNumber[2] = stalePage.movies
            $0.movies = existing + stalePage.movies
            $0.currentPage = 2
            $0.isOffline = true
        }

        await staleGate.open()
        await store.receive(.moviesResponse(.success(freshPage))) {
            $0.pagesByNumber[2] = freshPage.movies
            $0.movies = existing + freshPage.movies
            $0.isOffline = false
        }
    }

    func testFavoriteTapped_togglesToFavoritedWhenNotAlreadyFavorited() async {
        let movie = Movie.mock(id: 1)
        let store = TestStore(initialState: PopularMoviesFeature.State()) {
            PopularMoviesFeature()
        } withDependencies: {
            $0.favoritesClient.toggleFavorite = { _, _ in }
        }

        await store.send(.favoriteTapped(movie)) {
            $0.favoriteIds = [1]
        }
        await store.receive(.toggleFavoriteResponse(id: 1, wasFavorite: false, result: .success(EquatableVoid())))
    }

    func testFavoriteTapped_togglesToNotFavoritedWhenAlreadyFavorited() async {
        let movie = Movie.mock(id: 1)
        let store = TestStore(
            initialState: PopularMoviesFeature.State(favoriteIds: [1])
        ) {
            PopularMoviesFeature()
        } withDependencies: {
            $0.favoritesClient.toggleFavorite = { _, _ in }
        }

        await store.send(.favoriteTapped(movie)) {
            $0.favoriteIds = []
        }
        await store.receive(.toggleFavoriteResponse(id: 1, wasFavorite: true, result: .success(EquatableVoid())))
    }

    func testFavoriteTapped_revertsOptimisticUpdateOnFailure() async {
        let movie = Movie.mock(id: 1)
        struct TestError: Error {}
        let store = TestStore(
            initialState: PopularMoviesFeature.State(favoriteIds: [1])
        ) {
            PopularMoviesFeature()
        } withDependencies: {
            $0.favoritesClient.toggleFavorite = { _, _ in throw TestError() }
        }

        await store.send(.favoriteTapped(movie)) {
            $0.favoriteIds = []
        }
        await store.receive(.toggleFavoriteResponse(id: 1, wasFavorite: true, result: .failure(TestError().equatable))) {
            $0.favoriteIds = [1]
        }
    }

    func testMoviesResponse_failureSetsErrorMessage() async {
        struct TestError: LocalizedError {
            var errorDescription: String? { "Network unreachable" }
        }
        let store = TestStore(initialState: PopularMoviesFeature.State(isLoading: true)) {
            PopularMoviesFeature()
        }

        await store.send(.moviesResponse(.failure(TestError().equatable))) {
            $0.isLoading = false
            $0.errorMessage = "Network unreachable"
        }
    }

    func testMovieTapped_pushesDetailOntoPath() async {
        let movie = Movie.mock(id: 1)
        let store = TestStore(
            initialState: PopularMoviesFeature.State(favoriteIds: [1])
        ) {
            PopularMoviesFeature()
        }

        await store.send(.movieTapped(movie)) {
            $0.path.append(.detail(MovieDetailFeature.State(movie: movie, isFavorite: true)))
        }
    }

    func testRetryTapped_staleCachedMovies_setsIsOffline_clearedOnceFreshMoviesArrive() async {
        let stalePage = MoviesPage(movies: [.mock(id: 1, title: "Stale Movie")], page: 1, totalPages: 1, totalResults: 1, isStale: true)
        let freshPage = MoviesPage(movies: [.mock(id: 1, title: "Fresh Movie")], page: 1, totalPages: 1, totalResults: 1)
        let store = TestStore(initialState: PopularMoviesFeature.State()) {
            PopularMoviesFeature()
        } withDependencies: {
            $0.moviesClient.getPopularMovies = { _, _ in
                AsyncThrowingStream { continuation in
                    continuation.yield(stalePage)
                    continuation.yield(freshPage)
                    continuation.finish()
                }
            }
        }

        await store.send(.retryTapped) {
            $0.isLoading = true
        }
        await store.receive(.moviesResponse(.success(stalePage))) {
            $0.isLoading = false
            $0.pagesByNumber = [1: stalePage.movies]
            $0.movies = stalePage.movies
            $0.currentPage = 1
            $0.totalPages = 1
            $0.isOffline = true
        }
        await store.receive(.moviesResponse(.success(freshPage))) {
            $0.pagesByNumber = [1: freshPage.movies]
            $0.movies = freshPage.movies
            $0.isOffline = false
        }
    }

    func testPathToggleFavoriteResponse_syncsGridFavoriteIds() async {
        let movie = Movie.mock(id: 1)
        let detailState = MovieDetailFeature.State(movie: movie, isFavorite: false)
        let store = TestStore(
            initialState: PopularMoviesFeature.State(
                favoriteIds: [],
                path: StackState([.detail(detailState)])
            )
        ) {
            PopularMoviesFeature()
        }

        await store.send(
            .path(.element(id: 0, action: .detail(.toggleFavoriteResponse(wasFavorite: false, result: .success(EquatableVoid())))))
        ) {
            $0.favoriteIds = [1]
        }
    }
}
