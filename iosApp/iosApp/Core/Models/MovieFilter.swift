enum MovieFilter: Equatable, CaseIterable, Sendable {
    case popular
    case upcoming
    case topRated
    case nowPlaying

    var label: String {
        switch self {
        case .popular: "Popular"
        case .upcoming: "Upcoming"
        case .topRated: "Top Rated"
        case .nowPlaying: "Now Playing"
        }
    }
}
