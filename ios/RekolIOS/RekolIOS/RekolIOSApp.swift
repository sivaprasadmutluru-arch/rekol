import SwiftUI
import UIKit

#if canImport(FirebaseAuth)
import FirebaseAuth
#endif

final class RekolAppDelegate: NSObject, UIApplicationDelegate {
    func application(_ application: UIApplication,
                     didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil) -> Bool {
        FirebasePhoneAuthService.shared.configureIfAvailable()
        #if !targetEnvironment(simulator)
        application.registerForRemoteNotifications()
        #endif
        return true
    }

    func application(_ application: UIApplication,
                     didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        #if canImport(FirebaseAuth)
        #if !targetEnvironment(simulator)
        #if DEBUG
        Auth.auth().setAPNSToken(deviceToken, type: .sandbox)
        #else
        Auth.auth().setAPNSToken(deviceToken, type: .prod)
        #endif
        #endif
        #endif
    }

    func application(_ application: UIApplication,
                     didFailToRegisterForRemoteNotificationsWithError error: Error) {
        #if DEBUG
        print("Remote notification registration failed: \(error.localizedDescription)")
        #endif
    }

    func application(_ application: UIApplication,
                     didReceiveRemoteNotification userInfo: [AnyHashable: Any]) {
        #if canImport(FirebaseAuth)
        _ = Auth.auth().canHandleNotification(userInfo)
        #endif
    }

    func application(_ application: UIApplication,
                     didReceiveRemoteNotification userInfo: [AnyHashable: Any],
                     fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void) {
        #if canImport(FirebaseAuth)
        if Auth.auth().canHandleNotification(userInfo) {
            completionHandler(.noData)
            return
        }
        #endif
        completionHandler(.noData)
    }

    func application(_ application: UIApplication,
                     open url: URL,
                     options: [UIApplication.OpenURLOptionsKey: Any] = [:]) -> Bool {
        #if canImport(FirebaseAuth)
        if Auth.auth().canHandle(url) {
            return true
        }
        #endif
        return false
    }
}

@main
struct RekolIOSApp: App {
    @UIApplicationDelegateAdaptor(RekolAppDelegate.self) private var appDelegate
    @StateObject private var state = RekolAppState()

    init() {
        FirebasePhoneAuthService.shared.configureIfAvailable()
    }

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(state)
                .task {
                    await state.bootstrap()
                }
        }
    }
}
