import SwiftUI
import YouTubeiOSPlayerHelper

struct TrailerPlayer: UIViewRepresentable {
    let videoKey: String

    final class Coordinator {
        var loadedVideoKey: String?
    }

    func makeCoordinator() -> Coordinator {
        Coordinator()
    }

    func makeUIView(context: Context) -> YTPlayerView {
        YTPlayerView()
    }

    func updateUIView(_ playerView: YTPlayerView, context: Context) {
        guard context.coordinator.loadedVideoKey != videoKey else { return }
        context.coordinator.loadedVideoKey = videoKey
        playerView.load(withVideoId: videoKey, playerVars: ["playsinline": 1])
    }
}

#Preview {
    TrailerPlayer(videoKey: "dQw4w9WgXcQ")
        .aspectRatio(16.0 / 9.0, contentMode: .fit)
}
