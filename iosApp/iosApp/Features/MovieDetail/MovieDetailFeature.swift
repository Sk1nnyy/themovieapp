import ComposableArchitecture

@Reducer
struct MovieDetailFeature {
    @ObservableState
    struct State: Equatable {
        let movie: Movie
        var isLoading = false
        var movieDetails: MovieDetails?
        var isFavorite = false
        var errorMessage: String?
        var isOffline = false
    }

    enum Action: Equatable {
        case task
        case retryTapped
        case toggleFavoriteTapped

        case detailsResponse(Result<MovieDetails, EquatableError>)
        case isFavoriteResponse(Result<Bool, EquatableError>)
        case toggleFavoriteResponse(Result<EquatableVoid, EquatableError>)
    }

    private enum CancelID { case load, observeIsFavorite }

    @Dependency(\.moviesClient) var moviesClient
    @Dependency(\.favoritesClient) var favoritesClient

    var body: some ReducerOf<Self> {
        Reduce { state, action in
            switch action {
            case .task:
                return .merge(
                    loadDetails(state: &state),
                    observeIsFavorite(movieId: state.movie.id)
                )

            case .retryTapped:
                return loadDetails(state: &state)

            case .toggleFavoriteTapped:
                guard let details = state.movieDetails else { return .none }
                let wasFavorite = state.isFavorite
                return .run { send in
                    do {
                        try await favoritesClient.toggleFavorite(details.asMovie, wasFavorite)
                        await send(.toggleFavoriteResponse(.success(EquatableVoid())))
                    } catch {
                        await send(.toggleFavoriteResponse(.failure(error.equatable)))
                    }
                }

            case let .detailsResponse(.success(details)):
                state.isLoading = false
                state.movieDetails = details
                state.errorMessage = nil
                state.isOffline = details.isStale
                return .none

            case let .detailsResponse(.failure(error)):
                state.isLoading = false
                state.errorMessage = error.underlying.localizedDescription
                return .none

            case let .isFavoriteResponse(.success(isFavorite)):
                state.isFavorite = isFavorite
                return .none

            case .isFavoriteResponse(.failure):
                return .none

            case .toggleFavoriteResponse:
                return .none
            }
        }
    }

    private func observeIsFavorite(movieId: Int64) -> Effect<Action> {
        .run { send in
            do {
                for try await isFavorite in favoritesClient.observeIsFavorite(movieId) {
                    await send(.isFavoriteResponse(.success(isFavorite)))
                }
            } catch {
                await send(.isFavoriteResponse(.failure(error.equatable)))
            }
        }
        .cancellable(id: CancelID.observeIsFavorite, cancelInFlight: true)
    }

    private func loadDetails(state: inout State) -> Effect<Action> {
        state.isLoading = true
        state.errorMessage = nil
        return .run { [movieId = state.movie.id] send in
            do {
                for try await details in moviesClient.getMovieDetails(movieId, false) {
                    await send(.detailsResponse(.success(details)))
                }
            } catch {
                await send(.detailsResponse(.failure(error.equatable)))
            }
        }
        .cancellable(id: CancelID.load, cancelInFlight: true)
    }
}
