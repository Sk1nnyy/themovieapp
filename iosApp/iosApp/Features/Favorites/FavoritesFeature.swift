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

    private enum CancelID { case observeFavorites }

    @Dependency(\.favoritesClient) var favoritesClient

    var body: some ReducerOf<Self> {
        Reduce { state, action in
            switch action {
            case .task:
                state.isLoading = state.favorites.isEmpty
                return observeFavorites()

            case let .removeTapped(movie):
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

            case .removeFavoriteResponse:
                return .none

            case let .movieTapped(movie):
                state.path.append(.detail(MovieDetailFeature.State(movie: movie, isFavorite: true)))
                return .none

            case .path:
                return .none
            }
        }
        .forEach(\.path, action: \.path)
    }

    private func observeFavorites() -> Effect<Action> {
        .run { send in
            do {
                for try await favorites in favoritesClient.observeFavorites() {
                    await send(.favoritesResponse(.success(favorites)))
                }
            } catch {
                await send(.favoritesResponse(.failure(error.equatable)))
            }
        }
        .cancellable(id: CancelID.observeFavorites, cancelInFlight: true)
    }
}
