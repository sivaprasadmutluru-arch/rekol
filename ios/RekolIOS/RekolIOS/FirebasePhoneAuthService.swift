import Foundation

#if canImport(FirebaseCore)
import FirebaseCore
#endif

#if canImport(FirebaseAuth)
import FirebaseAuth
#endif

enum FirebasePhoneAuthError: LocalizedError {
    case sdkMissing
    case configMissing
    case verificationFailed

    var errorDescription: String? {
        switch self {
        case .sdkMissing:
            return "Firebase Auth is not linked in the iOS app."
        case .configMissing:
            return "Add GoogleService-Info.plist for com.rekol.app to enable Firebase phone OTP."
        case .verificationFailed:
            return "Unable to verify this OTP code."
        }
    }
}

final class FirebasePhoneAuthService {
    static let shared = FirebasePhoneAuthService()

    private init() {}

    func configureIfAvailable() {
        #if canImport(FirebaseCore)
        guard Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist") != nil else {
            return
        }
        if FirebaseApp.app() == nil {
            FirebaseApp.configure()
        }
        applyAuthSettings()
        #endif
    }

    func sendOtp(to phone: String) async throws -> String {
        #if targetEnvironment(simulator)
        return "simulator-\(phone)"
        #endif

        #if canImport(FirebaseAuth)
        try ensureConfigured()
        try ensurePhoneAuthCallbackScheme()
        return try await withCheckedThrowingContinuation { continuation in
            PhoneAuthProvider.provider().verifyPhoneNumber(phone, uiDelegate: nil) { verificationID, error in
                if let error {
                    continuation.resume(throwing: error)
                    return
                }
                guard let verificationID else {
                    continuation.resume(throwing: FirebasePhoneAuthError.verificationFailed)
                    return
                }
                continuation.resume(returning: verificationID)
            }
        }
        #else
        throw FirebasePhoneAuthError.sdkMissing
        #endif
    }

    func confirmOtp(verificationID: String, code: String) async throws {
        #if targetEnvironment(simulator)
        guard verificationID.hasPrefix("simulator-"), code.count == 6 else {
            throw FirebasePhoneAuthError.verificationFailed
        }
        return
        #endif

        #if canImport(FirebaseAuth)
        try ensureConfigured()
        let credential = PhoneAuthProvider.provider().credential(withVerificationID: verificationID, verificationCode: code)
        _ = try await Auth.auth().signIn(with: credential)
        #else
        throw FirebasePhoneAuthError.sdkMissing
        #endif
    }

    private func ensureConfigured() throws {
        #if canImport(FirebaseCore)
        if FirebaseApp.app() == nil {
            guard Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist") != nil else {
                throw FirebasePhoneAuthError.configMissing
            }
            FirebaseApp.configure()
        }
        applyAuthSettings()
        #else
        throw FirebasePhoneAuthError.sdkMissing
        #endif
    }

    private func applyAuthSettings() {
        #if canImport(FirebaseAuth)
        #if targetEnvironment(simulator)
        Auth.auth().settings?.isAppVerificationDisabledForTesting = true
        #endif
        #endif
    }

    private func ensurePhoneAuthCallbackScheme() throws {
        guard
            let plistURL = Bundle.main.url(forResource: "GoogleService-Info", withExtension: "plist"),
            let plist = NSDictionary(contentsOf: plistURL) as? [String: Any],
            let appID = plist["GOOGLE_APP_ID"] as? String,
            !appID.isEmpty
        else {
            return
        }
        let expectedScheme = "app-\(appID.replacingOccurrences(of: ":", with: "-"))"
        let urlTypes = Bundle.main.object(forInfoDictionaryKey: "CFBundleURLTypes") as? [[String: Any]]
        let schemes = urlTypes?.flatMap { $0["CFBundleURLSchemes"] as? [String] ?? [] } ?? []
        guard schemes.contains(expectedScheme) else {
            throw FirebasePhoneAuthError.configMissing
        }
    }
}
