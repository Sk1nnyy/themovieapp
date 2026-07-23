import SwiftUI
import ComposableArchitecture

struct PopularMoviesView: View {
    @Bindable var store: StoreOf<PopularMoviesFeature>

    @Namespace private var heroTransition

    private let columns = [GridItem(.flexible(), spacing: 16), GridItem(.flexible(), spacing: 16)]

    var body: some View {
        NavigationStack(path: $store.scope(\.path, action: \.path)) {
            content
                .navigationTitle(store.selectedFilter.label)
                .toolbar {
                    ToolbarItem(placement: .topBarTrailing) {
                        Button {
                            store.send(.setFilterSheetPresented(true))
                        } label: {
                            Label("Filters", systemImage: "line.3.horizontal.decrease.circle")
                        }
                    }
                }
        } destination: { store in
            switch store.case {
            case let .detail(store):
                MovieDetailView(store: store)
                    .navigationTransition(.zoom(sourceID: store.movie.id, in: heroTransition))
            }
        }
        .sheet(isPresented: filterSheetBinding) {
            FilterSheetView(
                selectedFilter: store.selectedFilter,
                onSelect: { store.send(.filterSelected($0)) }
            )
            .presentationDetents([.medium])
        }
        .task { await store.send(.task).finish() }
    }

    private var filterSheetBinding: Binding<Bool> {
        Binding(
            get: { store.isFilterSheetPresented },
            set: { store.send(.setFilterSheetPresented($0)) }
        )
    }

    @ViewBuilder
    private var content: some View {
        if store.isLoading && store.movies.isEmpty {
            ProgressView()
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        } else if let errorMessage = store.errorMessage, store.movies.isEmpty {
            ContentUnavailableView {
                Label("Unable to Load Movies", systemImage: "exclamationmark.triangle")
            } description: {
                Text(errorMessage)
            } actions: {
                Button("Retry") { store.send(.retryTapped) }
                    .buttonStyle(.borderedProminent)
            }
        } else {
            VStack(spacing: 0) {
                if store.isOffline {
                    OfflineBanner()
                }

                ScrollView {
                    LazyVGrid(columns: columns, spacing: 24) {
                        ForEach(store.movies) { movie in
                            MovieGridItem(
                                movie: movie,
                                isFavorite: store.favoriteIds.contains(movie.id),
                                onTap: { store.send(.movieTapped(movie)) },
                                onFavoriteTap: { store.send(.favoriteTapped(movie)) }
                            )
                            .matchedTransitionSource(id: movie.id, in: heroTransition)
                            .onAppear { store.send(.loadNextPageIfNeeded(currentItem: movie)) }
                        }
                    }
                    .padding(16)

                    if store.isLoadingMore {
                        ProgressView()
                            .padding()
                            .frame(maxWidth: .infinity)
                    }
                }
            }
        }
    }
}

#Preview {
    PopularMoviesView(
        store: Store(initialState: PopularMoviesFeature.State()) {
            PopularMoviesFeature()
        }
    )
}
