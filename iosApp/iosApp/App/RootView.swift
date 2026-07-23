import SwiftUI
import ComposableArchitecture

struct RootView: View {
    @Bindable var store: StoreOf<AppFeature>

    var body: some View {
        TabView(selection: $store.selectedTab.sending(\.tabSelected)) {
            PopularMoviesView(
                store: store.scope(state: \.popularMovies, action: \.popularMovies)
            )
            .tabItem {
                Label("Popular", systemImage: "popcorn.fill")
            }
            .tag(AppFeature.Tab.popularMovies)

            FavoritesView(
                store: store.scope(state: \.favorites, action: \.favorites)
            )
            .tabItem {
                Label("Favorites", systemImage: "star.fill")
            }
            .tag(AppFeature.Tab.favorites)
        }
    }
}

#Preview {
    RootView(
        store: Store(initialState: AppFeature.State()) {
            AppFeature()
        }
    )
}
