import ComposableArchitecture
import Foundation

@Reducer
struct PopularMoviesFeature {
    @ObservableState
    struct State: Equatable {
        var isLoading = false
        var isLoadingMore = false
        var movies: [Movie] = []
        var pagesByNumber: [Int: [Movie]] = [:]
        var currentPage = 0
        var totalPages = 1
        var errorMessage: LocalizedStringResource?
        var selectedFilter: MovieFilter = .popular
        var isFilterSheetPresented = false
        var favoriteIds: Set<Int64> = []
        var isOffline = false
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
        case toggleFavoriteResponse(Result<EquatableVoid, EquatableError>)
    }

    private enum CancelID: Hashable {
        case load
        case observeFavorites
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
                    observeFavoriteIds()
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
                state.pagesByNumber = [:]
                state.currentPage = 0
                state.totalPages = 1
                return loadMovies(page: 1, state: &state)

            case let .favoriteTapped(movie):
                let wasFavorite = state.favoriteIds.contains(movie.id)
                return .run { send in
                    do {
                        try await favoritesClient.toggleFavorite(movie, wasFavorite)
                        await send(.toggleFavoriteResponse(.success(EquatableVoid())))
                    } catch is CancellationError {
                    } catch {
                        await send(.toggleFavoriteResponse(.failure(error.equatable)))
                    }
                }
                .cancellable(id: CancelID.toggleFavorite(movie.id), cancelInFlight: true)

            case let .movieTapped(movie):
                state.path.append(
                    .detail(MovieDetailFeature.State(movie: movie, isFavorite: state.favoriteIds.contains(movie.id)))
                )
                return .none

            case .path:
                return .none

            case let .moviesResponse(.success(page)):
                state.isLoading = false
                state.isLoadingMore = false
                if page.page <= 1 {
                    state.pagesByNumber = [page.page: page.movies]
                } else {
                    state.pagesByNumber[page.page] = page.movies
                }
                state.movies = state.pagesByNumber
                    .sorted { $0.key < $1.key }
                    .flatMap(\.value)
                    .distinctByID()
                state.currentPage = page.page
                state.totalPages = page.totalPages
                state.errorMessage = nil
                state.isOffline = page.isStale
                return .none

            case let .moviesResponse(.failure(error)):
                state.isLoading = false
                state.isLoadingMore = false
                state.errorMessage = error.userFacingMessage
                return .none

            case let .favoriteIdsResponse(.success(ids)):
                state.favoriteIds = ids
                return .none

            case .favoriteIdsResponse(.failure):
                return .none

            case .toggleFavoriteResponse:
                return .none
            }
        }
        .forEach(\.path, action: \.path)
    }

    private func observeFavoriteIds() -> Effect<Action> {
        .run { send in
            do {
                for try await ids in favoritesClient.observeFavoriteIds() {
                    await send(.favoriteIdsResponse(.success(ids)))
                }
            } catch {
                await send(.favoriteIdsResponse(.failure(error.equatable)))
            }
        }
        .cancellable(id: CancelID.observeFavorites, cancelInFlight: true)
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
                switch filter {
                case .popular:
                    for try await page in moviesClient.getPopularMovies(page, false) {
                        await send(.moviesResponse(.success(page)))
                    }
                case .upcoming:
                    for try await page in moviesClient.getUpcomingMovies(page, false) {
                        await send(.moviesResponse(.success(page)))
                    }
                case .topRated:
                    for try await page in moviesClient.getTopRatedMovies(page, false) {
                        await send(.moviesResponse(.success(page)))
                    }
                case .nowPlaying:
                    for try await page in moviesClient.getNowPlayingMovies(page, false) {
                        await send(.moviesResponse(.success(page)))
                    }
                }
            } catch {
                await send(.moviesResponse(.failure(error.equatable)))
            }
        }
        .cancellable(id: CancelID.load, cancelInFlight: true)
    }
}

private extension Sequence where Element == Movie {
    func distinctByID() -> [Movie] {
        var seenIDs = Set<Int64>()
        return filter { seenIDs.insert($0.id).inserted }
    }
}
