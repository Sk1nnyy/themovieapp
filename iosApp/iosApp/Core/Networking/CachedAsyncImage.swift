import SwiftUI

/// Backs `CachedAsyncImage` with an actual disk-backed `URLCache` and an app-controlled 1-day
/// expiration. SwiftUI's built-in `AsyncImage` can't be handed a custom `URLSession`/`URLCache`,
/// so it was previously relying on whatever `URLSession`'s default shared cache happens to do -
/// governed by TMDB's own response headers, not a 1-day TTL the app actually controls.
actor PosterImageCache {
    static let shared = PosterImageCache()

    private let urlCache: URLCache
    private let session: URLSession
    private let maxAge: TimeInterval = 24 * 60 * 60
    private let fetchedAtKey = "fetchedAt"

    private init() {
        let cache = URLCache(memoryCapacity: 20 * 1024 * 1024, diskCapacity: 100 * 1024 * 1024, diskPath: "poster_image_cache")
        let configuration = URLSessionConfiguration.default
        configuration.urlCache = cache
        urlCache = cache
        session = URLSession(configuration: configuration)
    }

    func image(for url: URL) async throws -> UIImage {
        let request = URLRequest(url: url)

        if let cached = urlCache.cachedResponse(for: request), isFresh(cached), let image = UIImage(data: cached.data) {
            return image
        }

        let (data, response) = try await session.data(for: request)
        guard let image = UIImage(data: data) else { throw URLError(.cannotDecodeContentData) }

        urlCache.storeCachedResponse(
            CachedURLResponse(response: response, data: data, userInfo: [fetchedAtKey: Date()], storagePolicy: .allowed),
            for: request
        )
        return image
    }

    // `userInfo` on a CachedURLResponse isn't guaranteed to survive every OS's disk-cache
    // persistence; if it's missing (e.g. after a relaunch on an OS where it didn't round-trip),
    // treating that as stale just re-fetches and re-stamps it, which is self-correcting rather
    // than a correctness bug - it only costs a redundant network round-trip in that edge case.
    private func isFresh(_ cached: CachedURLResponse) -> Bool {
        guard let fetchedAt = cached.userInfo?[fetchedAtKey] as? Date else { return false }
        return Date().timeIntervalSince(fetchedAt) < maxAge
    }
}

/// Drop-in replacement for SwiftUI's `AsyncImage` that routes through `PosterImageCache` instead
/// of the uncontrolled default `URLSession` cache, so posters actually honor a 1-day expiration.
struct CachedAsyncImage<Content: View>: View {
    private let url: URL?
    private let content: (AsyncImagePhase) -> Content

    init(url: URL?, @ViewBuilder content: @escaping (AsyncImagePhase) -> Content) {
        self.url = url
        self.content = content
    }

    @State private var phase: AsyncImagePhase = .empty

    var body: some View {
        content(phase)
            .task(id: url) {
                await load()
            }
    }

    private func load() async {
        guard let url else {
            phase = .empty
            return
        }
        do {
            let image = try await PosterImageCache.shared.image(for: url)
            phase = .success(Image(uiImage: image))
        } catch {
            phase = .failure(error)
        }
    }
}
