import Foundation

enum MovieFilter: Equatable, CaseIterable, Sendable {
    case popular
    case upcoming
    case topRated
    case nowPlaying

    /// A plain `String` (used in `.navigationTitle(_: some StringProtocol)`, which doesn't
    /// auto-localize a `LocalizedStringKey` literal the way `Text`/`Label` do) - localized
    /// explicitly via a named key instead.
    var label: String {
        switch self {
        case .popular: String(localized: "movie_filter_popular", defaultValue: "Popular")
        case .upcoming: String(localized: "movie_filter_upcoming", defaultValue: "Upcoming")
        case .topRated: String(localized: "movie_filter_top_rated", defaultValue: "Top Rated")
        case .nowPlaying: String(localized: "movie_filter_now_playing", defaultValue: "Now Playing")
        }
    }
}
