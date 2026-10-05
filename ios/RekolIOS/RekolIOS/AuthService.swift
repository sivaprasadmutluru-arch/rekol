import Foundation

struct OtpChallenge {
    let phone: String
    let verificationID: String
}

struct AuthService {
    static let shared = AuthService()

    private let api = APIClient.shared
    private let phoneAuth = FirebasePhoneAuthService.shared

    func login(email: String, password: String) async throws -> AuthResult {
        try await api.login(email: email, password: password)
    }

    func requestLoginOtp(phone: String) async throws -> OtpChallenge {
        let verificationID = try await phoneAuth.sendOtp(to: phone)
        return OtpChallenge(phone: phone, verificationID: verificationID)
    }

    func registerAndRequestOtp(name: String, email: String, phone: String, password: String) async throws -> OtpChallenge {
        _ = try await api.register(name: name, email: email, phone: phone, password: password)
        let verificationID = try await phoneAuth.sendOtp(to: phone)
        return OtpChallenge(phone: phone, verificationID: verificationID)
    }

    func verifyOtp(challenge: OtpChallenge, code: String) async throws -> AuthResult {
        try await phoneAuth.confirmOtp(verificationID: challenge.verificationID, code: code)
        return try await api.verifyOtp(phone: challenge.phone, code: code)
    }

    func refreshSession(token: String) async throws -> AuthResult {
        try await api.me(token: token)
    }
}
