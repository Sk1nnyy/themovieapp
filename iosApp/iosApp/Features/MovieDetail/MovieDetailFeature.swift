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
        case toggleFavoriteResponse(wasFavorite: Bool, result: Result<EquatableVoid, EquatableError>)
    }

    private enum CancelID { case load }

    @Dependency(\.moviesClient) var moviesClient
    @Dependency(\.favoritesClient) var favoritesClient

    var body: some ReducerOf<Self> {
        Reduce { state, action in
            switch action {
            case .task:
                return .merge(
                    loadDetails(state: &state),
                    .run { [movieId = state.movie.id] send in
                        do {
                            let isFavorite = try await favoritesClient.isFavorite(movieId)
                            await send(.isFavoriteResponse(.success(isFavorite)))
                        } catch {
                            await send(.isFavoriteResponse(.failure(error.equatable)))
                        }
                    }
                )

            case .retryTapped:
                return loadDetails(state: &state)

            case .toggleFavoriteTapped:
                guard let details = state.movieDetails else { return .none }
                let wasFavorite = state.isFavorite
                state.isFavorite.toggle()
                return .run { send in
                    do {
                        try await favoritesClient.toggleFavorite(details.asMovie, wasFavorite)
                        await send(.toggleFavoriteResponse(wasFavorite: wasFavorite, result: .success(EquatableVoid())))
                    } catch {
                        await send(.toggleFavoriteResponse(wasFavorite: wasFavorite, result: .failure(error.equatable)))
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

            case let .toggleFavoriteResponse(wasFavorite, .failure):
                state.isFavorite = wasFavorite
                return .none

            case .toggleFavoriteResponse(_, .success):
                return .none
            }
        }
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
