import SwiftUI

/// Surfaces `MoviesPage.isStale` / `MovieDetails.isStale` so users can tell they're looking at
/// cached data after a failed refresh, instead of the app silently showing (possibly old)
/// content with no indication anything went wrong.
struct OfflineBanner: View {
    var body: some View {
        HStack(spacing: 8) {
            Image(systemName: "wifi.slash")
            Text("You're offline - showing saved data")
                .font(.footnote)
        }
        .foregroundStyle(.secondary)
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 16)
        .padding(.vertical, 8)
        .background(Color.secondary.opacity(0.12))
    }
}

#Preview {
    OfflineBanner()
}
