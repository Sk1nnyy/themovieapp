import SwiftUI

struct MovieGridItem: View {
    let movie: Movie
    let isFavorite: Bool
    let onTap: () -> Void
    let onFavoriteTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            VStack(alignment: .leading, spacing: 8) {
                ZStack(alignment: .topTrailing) {
                    posterImage
                    favoriteButton
                        .padding(6)
                        .accessibilityHidden(true)
                }

                Text(movie.title)
                    .font(.subheadline.weight(.medium))
                    .foregroundStyle(.primary)
                    .lineLimit(2)
                    .multilineTextAlignment(.leading)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .combine)
        .accessibilityValue(isFavorite ? "Favorited" : "")
        .accessibilityAction(named: Text(isFavorite ? "Remove from Favorites" : "Add to Favorites")) {
            onFavoriteTap()
        }
    }

    @ViewBuilder
    private var posterImage: some View {
        CachedAsyncImage(url: movie.posterURL) { phase in
            switch phase {
            case .success(let image):
                image.resizable().aspectRatio(2.0 / 3.0, contentMode: .fill)
            case .failure:
                posterPlaceholder {
                    Text("No Image")
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                }
            default:
                posterPlaceholder {
                    ProgressView()
                }
            }
        }
        .aspectRatio(2.0 / 3.0, contentMode: .fit)
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }

    private func posterPlaceholder<Content: View>(@ViewBuilder content: () -> Content) -> some View {
        ZStack {
            Color.secondary.opacity(0.15)
            content()
        }
    }

    private var favoriteButton: some View {
        Button(action: onFavoriteTap) {
            Image(systemName: isFavorite ? "heart.fill" : "heart")
                .font(.subheadline)
                .foregroundStyle(isFavorite ? Color.red : Color.white)
                .padding(6)
                .background(.black.opacity(0.35), in: Circle())
        }
        .buttonStyle(.plain)
    }
}
