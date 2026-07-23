import ComposableArchitecture

@Reducer
enum MoviesPath {
    case detail(MovieDetailFeature)
}

extension MoviesPath.State: Equatable {}
extension MoviesPath.Action: Equatable {}
