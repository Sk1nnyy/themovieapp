import ComposableArchitecture

@Reducer
struct FavoritesFeature {
    @ObservableState
    struct State: Equatable {
        var isLoading = false
        var favorites: [Movie] = []
        var path = StackState<MoviesPath.State>()
    }

    enum Action: Equatable {
        case task
        case removeTapped(Movie)
        case movieTapped(Movie)
        case path(StackActionOf<MoviesPath>)

        case favoritesResponse(Result<[Movie], EquatableError>)
        case removeFavoriteResponse(Result<EquatableVoid, EquatableError>)
    }

    private enum CancelID { case load }

    @Dependency(\.favoritesClient) var favoritesClient

    var body: some ReducerOf<Self> {
        Reduce { state, action in
            switch action {
            case .task:
                state.isLoading = state.favorites.isEmpty
                return loadFavorites()

            case let .removeTapped(movie):
                state.favorites.removeAll { $0.id == movie.id }
                return .run { send in
                    do {
                        try await favoritesClient.toggleFavorite(movie, true)
                        await send(.removeFavoriteResponse(.success(EquatableVoid())))
                    } catch {
                        await send(.removeFavoriteResponse(.failure(error.equatable)))
                    }
                }

            case let .favoritesResponse(.success(favorites)):
                state.isLoading = false
                state.favorites = favorites
                return .none

            case .favoritesResponse(.failure):
                state.isLoading = false
                return .none

            case .removeFavoriteResponse(.success):
                return .none

            case .removeFavoriteResponse(.failure):
                return loadFavorites()

            case let .movieTapped(movie):
                state.path.append(.detail(MovieDetailFeature.State(movie: movie, isFavorite: true)))
                return .none

            case let .path(.element(id: id, action: .detail(.toggleFavoriteResponse(wasFavorite, .success)))):
                guard case let .detail(detailState) = state.path[id: id], wasFavorite else { return .none }
                state.favorites.removeAll { $0.id == detailState.movie.id }
                return .none

            case .path:
                return .none
            }
        }
        .forEach(\.path, action: \.path)
    }

    private func loadFavorites() -> Effect<Action> {
        .run { send in
            do {
                let favorites = try await favoritesClient.getFavorites()
                await send(.favoritesResponse(.success(favorites)))
            } catch {
                await send(.favoritesResponse(.failure(error.equatable)))
            }
        }
        .cancellable(id: CancelID.load, cancelInFlight: true)
    }
}
