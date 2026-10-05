import Foundation

enum APIError: LocalizedError {
    case invalidURL
    case invalidCredentials
    case accountNotFound
    case accountAlreadyExists
    case authInvalid
    case server(String)

    var errorDescription: String? {
        switch self {
        case .invalidURL: return "The Rekol API URL is invalid."
        case .invalidCredentials: return "Invalid email or password."
        case .accountNotFound: return "Account not found."
        case .accountAlreadyExists: return "An account with these details already exists."
        case .authInvalid: return "Please sign in again."
        case .server(let message): return message
        }
    }
}

struct APIClient {
    static let shared = APIClient()
    private let baseURL = APIClientConfig.baseURL

    func login(email: String, password: String) async throws -> AuthResult {
        try await post("/api/v1/auth/login", body: ["email": email, "password": password])
    }

    func register(name: String, email: String, phone: String, password: String) async throws -> AuthResult {
        try await post("/api/v1/auth/register", body: ["name": name, "email": email, "phone": phone, "password": password])
    }

    func verifyOtp(phone: String, code: String) async throws -> AuthResult {
        try await post("/api/v1/auth/otp/verify", body: ["phone": phone, "code": code])
    }

    func me(token: String) async throws -> AuthResult {
        var request = URLRequest(url: baseURL.appending(path: "/api/v1/auth/me"))
        request.httpMethod = "GET"
        request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        let (data, response) = try await URLSession.shared.data(for: request)
        try validate(response: response, data: data)
        let user = try JSONSerialization.jsonObject(with: data) as? [String: Any]
        return AuthResult(token: token, name: user?["name"] as? String, email: user?["email"] as? String, phone: user?["phone"] as? String)
    }

    func memories(token: String) async throws -> [RemoteMemory] {
        var request = URLRequest(url: baseURL.appending(path: "/api/v1/memories"))
        request.httpMethod = "GET"
        request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        let (data, response) = try await URLSession.shared.data(for: request)
        try validate(response: response, data: data)
        return try JSONDecoder().decode([RemoteMemory].self, from: data)
    }

    func createMemory(token: String, content: String, reason: String, privateMemory: Bool) async throws -> RemoteMemory {
        var request = URLRequest(url: baseURL.appending(path: "/api/v1/memories"))
        request.httpMethod = "POST"
        request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONSerialization.data(withJSONObject: [
            "content": content,
            "reason": reason,
            "privateMemory": privateMemory
        ])
        let (data, response) = try await URLSession.shared.data(for: request)
        try validate(response: response, data: data)
        return try JSONDecoder().decode(RemoteMemory.self, from: data)
    }

    private func post(_ path: String, body: [String: Any]) async throws -> AuthResult {
        var request = URLRequest(url: baseURL.appending(path: path))
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONSerialization.data(withJSONObject: body)
        let (data, response) = try await URLSession.shared.data(for: request)
        try validate(response: response, data: data)
        let json = try JSONSerialization.jsonObject(with: data) as? [String: Any]
        let user = json?["user"] as? [String: Any]
        return AuthResult(
            token: json?["access_token"] as? String ?? "",
            name: user?["name"] as? String,
            email: user?["email"] as? String,
            phone: user?["phone"] as? String
        )
    }

    private func validate(response: URLResponse, data: Data) throws {
        guard let http = response as? HTTPURLResponse else { return }
        guard (200...299).contains(http.statusCode) else {
            if http.statusCode == 401 { throw APIError.authInvalid }
            if http.statusCode == 404 { throw APIError.accountNotFound }
            if http.statusCode == 409 { throw APIError.accountAlreadyExists }
            let message = ((try? JSONSerialization.jsonObject(with: data)) as? [String: Any])
                .flatMap { ($0["detail"] as? String) ?? ($0["message"] as? String) ?? ($0["error"] as? String) }
            throw APIError.server(message ?? "Account service returned HTTP \(http.statusCode)")
        }
    }
}

enum APIClientConfig {
    static let baseURL: URL = {
        if let value = Bundle.main.object(forInfoDictionaryKey: "REKOL_API_URL") as? String,
           let url = URL(string: value.trimmingCharacters(in: CharacterSet(charactersIn: "/"))) {
            return url
        }
        return URL(string: "https://wefyx.pro/rekol-api")!
    }()
}
