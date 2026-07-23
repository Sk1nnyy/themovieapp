import ComposableArchitecture

@Reducer
struct AppFeature {
    enum Tab: Equatable {
        case popularMovies
        case favorites
    }

    @ObservableState
    struct State: Equatable {
        var selectedTab: Tab = .popularMovies
        var popularMovies = PopularMoviesFeature.State()
        var favorites = FavoritesFeature.State()
    }

    enum Action: Equatable {
        case tabSelected(Tab)
        case popularMovies(PopularMoviesFeature.Action)
        case favorites(FavoritesFeature.Action)
    }

    var body: some ReducerOf<Self> {
        Scope(state: \.popularMovies, action: \.popularMovies) {
            PopularMoviesFeature()
        }
        Scope(state: \.favorites, action: \.favorites) {
            FavoritesFeature()
        }
        Reduce { state, action in
            switch action {
            case let .tabSelected(tab):
                state.selectedTab = tab
                if tab == .favorites {
                    return .send(.favorites(.task))
                }
                return .none

            case .popularMovies, .favorites:
                return .none
            }
        }
    }
}
