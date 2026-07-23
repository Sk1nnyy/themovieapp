import SwiftUI
import SharedLogic
import ComposableArchitecture

@main
struct iOSApp: App {
    static let store = Store(initialState: AppFeature.State()) {
        AppFeature()
    }

    init() {
        KoinInitKt.doInitKoin(config: { _ in })
    }

    var body: some Scene {
        WindowGroup {
            if ProcessInfo.processInfo.environment["XCTestConfigurationFilePath"] != nil {
                EmptyView()
            } else {
                RootView(store: iOSApp.store)
            }
        }
    }
}