import ComposableArchitecture
import Foundation

@Reducer
struct PopularMoviesFeature {
    @ObservableState
    struct State: Equatable {
        var isLoading = false
        var isLoadingMore = false
        var movies: [Movie] = []
        var currentPage = 0
        var totalPages = 1
        var errorMessage: String?
        var selectedFilter: MovieFilter = .popular
        var isFilterSheetPresented = false
        var favoriteIds: Set<Int64> = []
        var path = StackState<MoviesPath.State>()

        var canLoadMore: Bool { currentPage < totalPages }
    }

    enum Action: Equatable {
        case task
        case retryTapped
        case loadNextPageIfNeeded(currentItem: Movie)
        case setFilterSheetPresented(Bool)
        case filterSelected(MovieFilter)
        case favoriteTapped(Movie)
        case movieTapped(Movie)
        case path(StackActionOf<MoviesPath>)

        case moviesResponse(Result<MoviesPage, EquatableError>)
        case favoriteIdsResponse(Result<Set<Int64>, EquatableError>)
        case toggleFavoriteResponse(id: Int64, wasFavorite: Bool, result: Result<EquatableVoid, EquatableError>)
    }

    private enum CancelID: Hashable {
        case load
        case toggleFavorite(Int64)
    }

    @Dependency(\.moviesClient) var moviesClient
    @Dependency(\.favoritesClient) var favoritesClient

    var body: some ReducerOf<Self> {
        Reduce { state, action in
            switch action {
            case .task:
                return .merge(
                    loadMovies(page: 1, state: &state),
                    .run { send in
                        do {
                            let ids = try await favoritesClient.getFavoriteIds()
                            await send(.favoriteIdsResponse(.success(ids)))
                        } catch {
                            await send(.favoriteIdsResponse(.failure(error.equatable)))
                        }
                    }
                )

            case .retryTapped:
                return loadMovies(page: 1, state: &state)

            case let .loadNextPageIfNeeded(currentItem):
                guard shouldLoadMore(after: currentItem, state: state) else { return .none }
                return loadMovies(page: state.currentPage + 1, state: &state)

            case let .setFilterSheetPresented(isPresented):
                state.isFilterSheetPresented = isPresented
                return .none

            case let .filterSelected(filter):
                guard filter != state.selectedFilter else {
                    state.isFilterSheetPresented = false
                    return .none
                }
                state.selectedFilter = filter
                state.isFilterSheetPresented = false
                state.movies = []
                state.currentPage = 0
                state.totalPages = 1
                return loadMovies(page: 1, state: &state)

            case let .favoriteTapped(movie):
                let wasFavorite = state.favoriteIds.contains(movie.id)
                if wasFavorite {
                    state.favoriteIds.remove(movie.id)
                } else {
                    state.favoriteIds.insert(movie.id)
                }
                return .run { send in
                    do {
                        try await favoritesClient.toggleFavorite(movie, wasFavorite)
                        await send(.toggleFavoriteResponse(id: movie.id, wasFavorite: wasFavorite, result: .success(EquatableVoid())))
                    } catch is CancellationError {
                    } catch {
                        await send(.toggleFavoriteResponse(id: movie.id, wasFavorite: wasFavorite, result: .failure(error.equatable)))
                    }
                }
                .cancellable(id: CancelID.toggleFavorite(movie.id), cancelInFlight: true)

            case let .movieTapped(movie):
                state.path.append(
                    .detail(MovieDetailFeature.State(movie: movie, isFavorite: state.favoriteIds.contains(movie.id)))
                )
                return .none

            case let .path(.element(id: id, action: .detail(.toggleFavoriteResponse(wasFavorite, .success)))):
                guard case let .detail(detailState) = state.path[id: id] else { return .none }
                if wasFavorite {
                    state.favoriteIds.remove(detailState.movie.id)
                } else {
                    state.favoriteIds.insert(detailState.movie.id)
                }
                return .none

            case .path:
                return .none

            case let .moviesResponse(.success(page)):
                state.isLoading = false
                state.isLoadingMore = false
                if page.page <= 1 {
                    state.movies = page.movies
                } else {
                    let existingIds = Set(state.movies.map(\.id))
                    state.movies += page.movies.filter { !existingIds.contains($0.id) }
                }
                state.currentPage = page.page
                state.totalPages = page.totalPages
                state.errorMessage = nil
                return .none

            case let .moviesResponse(.failure(error)):
                state.isLoading = false
                state.isLoadingMore = false
                state.errorMessage = error.underlying.localizedDescription
                return .none

            case let .favoriteIdsResponse(.success(ids)):
                state.favoriteIds = ids
                return .none

            case .favoriteIdsResponse(.failure):
                return .none

            case let .toggleFavoriteResponse(id, wasFavorite, .failure):
                if wasFavorite {
                    state.favoriteIds.insert(id)
                } else {
                    state.favoriteIds.remove(id)
                }
                return .none

            case .toggleFavoriteResponse(_, _, .success):
                return .none
            }
        }
        .forEach(\.path, action: \.path)
    }

    private func shouldLoadMore(after movie: Movie, state: State) -> Bool {
        guard !state.isLoading, !state.isLoadingMore, state.canLoadMore else { return false }
        guard let index = state.movies.firstIndex(of: movie) else { return false }
        return index >= state.movies.count - 4
    }

    private func loadMovies(page: Int, state: inout State) -> Effect<Action> {
        let filter = state.selectedFilter
        if page <= 1 {
            state.isLoading = true
        } else {
            state.isLoadingMore = true
        }
        state.errorMessage = nil

        return .run { send in
            do {
                let result: MoviesPage
                switch filter {
                case .popular: result = try await moviesClient.getPopularMovies(page)
                case .upcoming: result = try await moviesClient.getUpcomingMovies(page)
                case .topRated: result = try await moviesClient.getTopRatedMovies(page)
                case .nowPlaying: result = try await moviesClient.getNowPlayingMovies(page)
                }
                await send(.moviesResponse(.success(result)))
            } catch {
                await send(.moviesResponse(.failure(error.equatable)))
            }
        }
        .cancellable(id: CancelID.load, cancelInFlight: true)
    }
}
