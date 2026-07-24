import SwiftUI
import ComposableArchitecture

struct MovieDetailView: View {
    let store: StoreOf<MovieDetailFeature>

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                posterImage
                    .frame(maxWidth: .infinity)

                if store.isOffline && store.movieDetails != nil {
                    OfflineBanner()
                }

                Text(store.movie.title)
                    .font(.title2.bold())

                HStack(spacing: 12) {
                    Label(String(format: "%.1f", store.movie.voteAverage), systemImage: "star.fill")
                        .foregroundStyle(.yellow)
                        .fontWeight(.medium)

                    if let releaseDate = store.movie.releaseDate, !releaseDate.isEmpty {
                        Text(releaseDate)
                            .foregroundStyle(.secondary)
                    }

                    if let runtime = store.movieDetails?.runtime {
                        Text("\(runtime) min")
                            .foregroundStyle(.secondary)
                    }
                }
                .font(.subheadline)

                if let tagline = store.movieDetails?.tagline, !tagline.isEmpty {
                    Text(tagline)
                        .font(.subheadline.italic())
                        .foregroundStyle(.secondary)
                }

                if let trailerKey = store.movieDetails?.trailerKey {
                    TrailerPlayer(videoKey: trailerKey)
                        .aspectRatio(16.0 / 9.0, contentMode: .fit)
                        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                        .accessibilityLabel("Movie trailer player")

                    if let watchURL = URL(string: "https://www.youtube.com/watch?v=\(trailerKey)") {
                        Link("Watch on YouTube", destination: watchURL)
                            .font(.subheadline)
                    }
                }

                if let genres = store.movieDetails?.genres, !genres.isEmpty {
                    genresRow(genres)
                }

                let overview = store.movieDetails?.overview ?? store.movie.overview
                if !overview.isEmpty {
                    Text(overview)
                        .font(.body)
                        .foregroundStyle(.secondary)
                }

                if let details = store.movieDetails {
                    additionalInfo(details)
                }

                if store.isLoading && store.movieDetails == nil {
                    ProgressView()
                        .frame(maxWidth: .infinity)
                        .padding()
                } else if let errorMessage = store.errorMessage, store.movieDetails == nil {
                    ContentUnavailableView {
                        Label("Unable to Load Details", systemImage: "exclamationmark.triangle")
                    } description: {
                        Text(errorMessage)
                    } actions: {
                        Button("Retry") { store.send(.retryTapped) }
                            .buttonStyle(.borderedProminent)
                    }
                    .frame(maxWidth: .infinity)
                }
            }
            .padding()
        }
        .navigationTitle(store.movie.title)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    store.send(.toggleFavoriteTapped)
                } label: {
                    Image(systemName: store.isFavorite ? "heart.fill" : "heart")
                        .foregroundStyle(store.isFavorite ? Color.red : Color.primary)
                }
                .accessibilityLabel(store.isFavorite ? "Remove from favorites" : "Add to favorites")
            }
        }
        .task { await store.send(.task).finish() }
    }

    @ViewBuilder
    private var posterImage: some View {
        CachedAsyncImage(url: store.movieDetails?.posterURL ?? store.movie.posterURL) { phase in
            switch phase {
            case .success(let image):
                image.resizable().aspectRatio(2.0 / 3.0, contentMode: .fit)
            case .failure:
                posterPlaceholder {
                    Image(systemName: "photo")
                        .font(.largeTitle)
                        .foregroundStyle(.secondary)
                }
            default:
                posterPlaceholder {
                    ProgressView()
                }
            }
        }
        .frame(maxHeight: 420)
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        .shadow(color: .black.opacity(0.15), radius: 8, y: 4)
    }

    private func posterPlaceholder<Content: View>(@ViewBuilder content: () -> Content) -> some View {
        RoundedRectangle(cornerRadius: 16, style: .continuous)
            .fill(Color.secondary.opacity(0.15))
            .aspectRatio(2.0 / 3.0, contentMode: .fit)
            .overlay(content())
    }

    private func genresRow(_ genres: [Genre]) -> some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(genres) { genre in
                    Text(genre.name)
                        .font(.caption.weight(.medium))
                        .padding(.horizontal, 10)
                        .padding(.vertical, 4)
                        .background(Color.secondary.opacity(0.15), in: Capsule())
                }
            }
        }
    }

    @ViewBuilder
    private func additionalInfo(_ details: MovieDetails) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            if details.budget > 0 {
                LabeledContent("Budget", value: details.budget, format: .currency(code: "USD"))
            }
            if details.revenue > 0 {
                LabeledContent("Revenue", value: details.revenue, format: .currency(code: "USD"))
            }
            if let homepage = details.homepage, let url = URL(string: homepage) {
                Link("Homepage", destination: url)
            }
        }
        .font(.subheadline)
        .padding(.top, 8)
    }
}

#Preview {
    NavigationStack {
        MovieDetailView(
            store: Store(
                initialState: MovieDetailFeature.State(
                    movie: Movie(
                        id: 1,
                        title: "Preview Movie",
                        overview: "Overview",
                        posterPath: nil,
                        releaseDate: "2024-01-01",
                        voteAverage: 7.5
                    )
                )
            ) {
                MovieDetailFeature()
            }
        )
    }
}
