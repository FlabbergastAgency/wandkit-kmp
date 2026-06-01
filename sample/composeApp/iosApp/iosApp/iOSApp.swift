import SwiftUI
import Shared

@main
struct iOSApp: App {
    init() {
        InitWandkitKt.doInitWandkit(apiKey: "abc")
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
