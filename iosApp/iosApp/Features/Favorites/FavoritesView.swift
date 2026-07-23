import SwiftUI
import ComposableArchitecture

struct FavoritesView: View {
    @Bindable var store: StoreOf<FavoritesFeature>

    @Namespace private var heroTransition

    private let columns = [GridItem(.flexible(), spacing: 16), GridItem(.flexible(), spacing: 16)]

    var body: some View {
        NavigationStack(path: $store.scope(\.path, action: \.path)) {
            content
                .navigationTitle("Favorites")
        } destination: { store in
            switch store.case {
            case let .detail(store):
                MovieDetailView(store: store)
                    .navigationTransition(.zoom(sourceID: store.movie.id, in: heroTransition))
            }
        }
        .task { await store.send(.task).finish() }
    }

    @ViewBuilder
    private var content: some View {
        if store.isLoading && store.favorites.isEmpty {
            ProgressView()
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        } else if store.favorites.isEmpty {
            ContentUnavailableView(
                "No Favorites Yet",
                systemImage: "star",
                description: Text("Movies you favorite will show up here.")
            )
        } else {
            ScrollView {
                LazyVGrid(columns: columns, spacing: 24) {
                    ForEach(store.favorites) { movie in
                        MovieGridItem(
                            movie: movie,
                            isFavorite: true,
                            onTap: { store.send(.movieTapped(movie)) },
                            onFavoriteTap: { store.send(.removeTapped(movie)) }
                        )
                        .matchedTransitionSource(id: movie.id, in: heroTransition)
                    }
                }
                .padding(16)
            }
        }
    }
}

#Preview {
    FavoritesView(
        store: Store(initialState: FavoritesFeature.State()) {
            FavoritesFeature()
        }
    )
}
