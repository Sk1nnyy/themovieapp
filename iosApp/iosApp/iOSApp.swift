import SwiftUI
import SharedLogic

@main
struct iOSApp: App {
    init() {
        KoinInitKt.initKoin(config: { _ in })
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}