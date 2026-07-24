import ComposableArchitecture
import XCTest
@testable import MovieApp

@MainActor
final class MovieDetailFeatureTests: XCTestCase {
    private func details(id: Int64, title: String? = nil, isStale: Bool = false) -> MovieDetails {
        MovieDetails(
            id: id,
            title: title ?? "Movie \(id)",
            overview: "overview",
            tagline: nil,
            runtime: nil,
            budget: 0,
            revenue: 0,
            homepage: nil,
            genres: [],
            posterPath: nil,
            backdropPath: nil,
            releaseDate: nil,
            voteAverage: 5.0,
            voteCount: 0,
            isStale: isStale
        )
    }

    func testTask_emitsLoadingThenDetails() async {
        let movie = Movie.mock(id: 1)
        let movieDetails = details(id: 1)
        let isFavoriteGate = Gate()
        let store = TestStore(initialState: MovieDetailFeature.State(movie: movie)) {
            MovieDetailFeature()
        } withDependencies: {
            $0.moviesClient.getMovieDetails = { _, _ in AsyncThrowingStream { $0.yield(movieDetails); $0.finish() } }
            $0.favoritesClient.observeIsFavorite = { _ in
                AsyncThrowingStream { continuation in
                    Task {
                        await isFavoriteGate.wait()
                        continuation.yield(false)
                        continuation.finish()
                    }
                }
            }
        }

        await store.send(.task) {
            $0.isLoading = true
        }
        await store.receive(.detailsResponse(.success(movieDetails))) {
            $0.isLoading = false
            $0.movieDetails = movieDetails
        }

        await isFavoriteGate.open()
        await store.receive(.isFavoriteResponse(.success(false)))
    }

    func testTask_failureSetsError() async {
        struct TestError: LocalizedError {
            var errorDescription: String? { "boom" }
        }
        let movie = Movie.mock(id: 1)
        let isFavoriteGate = Gate()
        let store = TestStore(initialState: MovieDetailFeature.State(movie: movie)) {
            MovieDetailFeature()
        } withDependencies: {
            $0.moviesClient.getMovieDetails = { _, _ in AsyncThrowingStream { $0.finish(throwing: TestError()) } }
            $0.favoritesClient.observeIsFavorite = { _ in
                AsyncThrowingStream { continuation in
                    Task {
                        await isFavoriteGate.wait()
                        continuation.yield(false)
                        continuation.finish()
                    }
                }
            }
        }

        await store.send(.task) {
            $0.isLoading = true
        }
        await store.receive(.detailsResponse(.failure(TestError().equatable))) {
            $0.isLoading = false
            $0.errorMessage = "boom"
        }

        await isFavoriteGate.open()
        await store.receive(.isFavoriteResponse(.success(false)))
    }

    func testRetryTapped_reloadsSuccessfullyAfterFailure() async {
        struct TestError: Error {}
        let movie = Movie.mock(id: 1)
        let movieDetails = details(id: 1)
        let store = TestStore(
            initialState: MovieDetailFeature.State(movie: movie, errorMessage: "boom")
        ) {
            MovieDetailFeature()
        } withDependencies: {
            $0.moviesClient.getMovieDetails = { _, _ in AsyncThrowingStream { $0.yield(movieDetails); $0.finish() } }
        }

        await store.send(.retryTapped) {
            $0.isLoading = true
            $0.errorMessage = nil
        }
        await store.receive(.detailsResponse(.success(movieDetails))) {
            $0.isLoading = false
            $0.movieDetails = movieDetails
        }
    }

    func testRetryTapped_cachedDetailsShownImmediately_thenUpdatedByNetwork() async {
        let movie = Movie.mock(id: 1)
        let cachedDetails = details(id: 1, title: "Cached Title")
        let freshDetails = details(id: 1, title: "Fresh Title")
        let store = TestStore(
            initialState: MovieDetailFeature.State(movie: movie, errorMessage: "boom")
        ) {
            MovieDetailFeature()
        } withDependencies: {
            $0.moviesClient.getMovieDetails = { _, _ in
                AsyncThrowingStream { continuation in
                    continuation.yield(cachedDetails)
                    continuation.yield(freshDetails)
                    continuation.finish()
                }
            }
        }

        await store.send(.retryTapped) {
            $0.isLoading = true
            $0.errorMessage = nil
        }
        await store.receive(.detailsResponse(.success(cachedDetails))) {
            $0.isLoading = false
            $0.movieDetails = cachedDetails
        }
        await store.receive(.detailsResponse(.success(freshDetails))) {
            $0.movieDetails = freshDetails
        }
    }

    func testTask_staleCachedDetails_setsIsOffline_clearedOnceFreshDetailsArrive() async {
        let movie = Movie.mock(id: 1)
        let staleDetails = details(id: 1, title: "Stale Title", isStale: true)
        let freshDetails = details(id: 1, title: "Fresh Title")
        let isFavoriteGate = Gate()
        let store = TestStore(initialState: MovieDetailFeature.State(movie: movie)) {
            MovieDetailFeature()
        } withDependencies: {
            $0.moviesClient.getMovieDetails = { _, _ in
                AsyncThrowingStream { continuation in
                    continuation.yield(staleDetails)
                    continuation.yield(freshDetails)
                    continuation.finish()
                }
            }
            $0.favoritesClient.observeIsFavorite = { _ in
                AsyncThrowingStream { continuation in
                    Task {
                        await isFavoriteGate.wait()
                        continuation.yield(false)
                        continuation.finish()
                    }
                }
            }
        }

        await store.send(.task) {
            $0.isLoading = true
        }
        await store.receive(.detailsResponse(.success(staleDetails))) {
            $0.isLoading = false
            $0.movieDetails = staleDetails
            $0.isOffline = true
        }
        await store.receive(.detailsResponse(.success(freshDetails))) {
            $0.movieDetails = freshDetails
            $0.isOffline = false
        }

        await isFavoriteGate.open()
        await store.receive(.isFavoriteResponse(.success(false)))
    }

    func testToggleFavoriteTapped_callsToggleFavoriteWithCurrentState() async {
        let movie = Movie.mock(id: 1)
        let movieDetails = details(id: 1)
        let toggleCalls = CallRecorder<Bool>()
        let store = TestStore(
            initialState: MovieDetailFeature.State(movie: movie, movieDetails: movieDetails, isFavorite: true)
        ) {
            MovieDetailFeature()
        } withDependencies: {
            $0.favoritesClient.toggleFavorite = { _, wasFavorite in
                await toggleCalls.record(wasFavorite)
            }
        }

        await store.send(.toggleFavoriteTapped)
        await store.receive(.toggleFavoriteResponse(.success(EquatableVoid())))

        let values = await toggleCalls.values
        XCTAssertEqual(values, [true])
    }

    func testToggleFavoriteTapped_noOpsWhenDetailsNotLoaded() async {
        let movie = Movie.mock(id: 1)
        let store = TestStore(initialState: MovieDetailFeature.State(movie: movie)) {
            MovieDetailFeature()
        }

        await store.send(.toggleFavoriteTapped)
    }

    func testToggleFavoriteTapped_failure_sendsFailureResponse() async {
        struct TestError: Error {}
        let movie = Movie.mock(id: 1)
        let movieDetails = details(id: 1)
        let store = TestStore(
            initialState: MovieDetailFeature.State(movie: movie, movieDetails: movieDetails, isFavorite: false)
        ) {
            MovieDetailFeature()
        } withDependencies: {
            $0.favoritesClient.toggleFavorite = { _, _ in throw TestError() }
        }

        await store.send(.toggleFavoriteTapped)
        await store.receive(.toggleFavoriteResponse(.failure(TestError().equatable)))
    }

    func testObserveIsFavorite_laterEmissionUpdatesIsFavorite() async {
        let movie = Movie.mock(id: 1)
        let (isFavoriteStream, isFavoriteContinuation) = AsyncThrowingStream<Bool, Error>.makeStream()
        let store = TestStore(initialState: MovieDetailFeature.State(movie: movie)) {
            MovieDetailFeature()
        } withDependencies: {
            $0.moviesClient.getMovieDetails = { _, _ in AsyncThrowingStream { $0.finish() } }
            $0.favoritesClient.observeIsFavorite = { _ in isFavoriteStream }
        }

        let task = await store.send(.task) {
            $0.isLoading = true
        }

        isFavoriteContinuation.yield(false)
        await store.receive(.isFavoriteResponse(.success(false)))

        // Favoriting this movie from elsewhere (e.g. the Popular Movies grid) flows back through
        // this same live subscription, tied to this screen's own StackState lifetime.
        isFavoriteContinuation.yield(true)
        await store.receive(.isFavoriteResponse(.success(true))) {
            $0.isFavorite = true
        }

        isFavoriteContinuation.finish()
        await task.finish(timeout: .seconds(5))
    }
}
