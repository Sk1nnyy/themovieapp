import ComposableArchitecture
import XCTest
@testable import MovieApp

@MainActor
final class MovieDetailFeatureTests: XCTestCase {
    private func details(id: Int64, title: String? = nil) -> MovieDetails {
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
            voteCount: 0
        )
    }

    func testTask_emitsLoadingThenDetails() async {
        let movie = Movie.mock(id: 1)
        let movieDetails = details(id: 1)
        let store = TestStore(initialState: MovieDetailFeature.State(movie: movie)) {
            MovieDetailFeature()
        } withDependencies: {
            $0.moviesClient.getMovieDetails = { _, _ in AsyncThrowingStream { $0.yield(movieDetails); $0.finish() } }
            $0.favoritesClient.isFavorite = { _ in false }
        }

        await store.send(.task) {
            $0.isLoading = true
        }
        await store.receive(.isFavoriteResponse(.success(false)))
        await store.receive(.detailsResponse(.success(movieDetails))) {
            $0.isLoading = false
            $0.movieDetails = movieDetails
        }
    }

    func testTask_failureSetsError() async {
        struct TestError: LocalizedError {
            var errorDescription: String? { "boom" }
        }
        let movie = Movie.mock(id: 1)
        let store = TestStore(initialState: MovieDetailFeature.State(movie: movie)) {
            MovieDetailFeature()
        } withDependencies: {
            $0.moviesClient.getMovieDetails = { _, _ in AsyncThrowingStream { $0.finish(throwing: TestError()) } }
            $0.favoritesClient.isFavorite = { _ in false }
        }

        await store.send(.task) {
            $0.isLoading = true
        }
        await store.receive(.isFavoriteResponse(.success(false)))
        await store.receive(.detailsResponse(.failure(TestError().equatable))) {
            $0.isLoading = false
            $0.errorMessage = "boom"
        }
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

    func testToggleFavoriteTapped_delegatesCurrentFavoriteStateToRepository() async {
        let movie = Movie.mock(id: 1)
        let movieDetails = details(id: 1)
        let store = TestStore(
            initialState: MovieDetailFeature.State(movie: movie, movieDetails: movieDetails, isFavorite: true)
        ) {
            MovieDetailFeature()
        } withDependencies: {
            $0.favoritesClient.toggleFavorite = { _, _ in }
        }

        await store.send(.toggleFavoriteTapped) {
            $0.isFavorite = false
        }
        await store.receive(.toggleFavoriteResponse(wasFavorite: true, result: .success(EquatableVoid())))
    }

    func testToggleFavoriteTapped_noOpsWhenDetailsNotLoaded() async {
        let movie = Movie.mock(id: 1)
        let store = TestStore(initialState: MovieDetailFeature.State(movie: movie)) {
            MovieDetailFeature()
        }

        await store.send(.toggleFavoriteTapped)
    }

    func testToggleFavoriteTapped_revertsOptimisticUpdateOnFailure() async {
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

        await store.send(.toggleFavoriteTapped) {
            $0.isFavorite = true
        }
        await store.receive(.toggleFavoriteResponse(wasFavorite: false, result: .failure(TestError().equatable))) {
            $0.isFavorite = false
        }
    }
}
