import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    init() {
        // Initialize Koin dependency injection
        MainViewControllerKt.doInitKoin()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    MainViewControllerKt.handleDeepLink(urlStr: url.absoluteString)
                }
        }
    }
}