import Foundation
import SwiftUI

enum Destination: String, CaseIterable, Identifiable {
    case home = "Home"
    case journal = "Journal"
    case library = "Library"
    case insights = "Insights"

    var id: String { rawValue }

    var systemImage: String {
        switch self {
        case .home: return "house.fill"
        case .journal: return "doc.text.fill"
        case .library: return "folder.fill"
        case .insights: return "chart.bar.xaxis"
        }
    }
}

struct Memory: Identifiable, Codable, Hashable {
    var id: String
    var title: String
    var type: String
    var time: String
    var summary: String
    var action: String?
    var accentHex: String
    var attachmentPath: String?

    var accent: Color { Color(hex: accentHex) }
}

struct Reminder: Identifiable, Codable, Hashable {
    var id: Int64
    var title: String
    var notes: String
    var at: Date
}

struct AuthResult: Codable {
    let token: String
    let name: String?
    let email: String?
    let phone: String?
}

struct RemoteMemory: Codable {
    let id: String
    let title: String
    let content: String
    let reason: String?
    let createdAt: String
    let privateMemory: Bool

    enum CodingKeys: String, CodingKey {
        case id, title, content, reason
        case createdAt = "created_at"
        case privateMemory = "private_memory"
    }
}

extension Color {
    static let rekolNavy = Color(hex: "071B4A")
    static let rekolBlue = Color(hex: "087CFF")
    static let rekolPurple = Color(hex: "7836FF")
    static let rekolCyan = Color(hex: "16D8D0")
    static let rekolCoral = Color(hex: "FF5A7A")
    static let rekolCanvas = Color(hex: "F8FAFF")
    static let rekolLine = Color(hex: "E8EDF7")

    init(hex: String) {
        let cleaned = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: cleaned).scanHexInt64(&int)
        let r, g, b: UInt64
        switch cleaned.count {
        case 6:
            (r, g, b) = ((int >> 16) & 255, (int >> 8) & 255, int & 255)
        default:
            (r, g, b) = (7, 27, 74)
        }
        self.init(.sRGB, red: Double(r) / 255, green: Double(g) / 255, blue: Double(b) / 255, opacity: 1)
    }
}

let sampleMemories = [
    Memory(id: "sample-report", title: "UAE Sustainability Report", type: "Article", time: "10:24 AM", summary: "Overview of UAE's net zero strategy and opportunities for businesses.", action: "Read and share with team", accentHex: "087CFF"),
    Memory(id: "sample-site", title: "Project Site Photos", type: "Photos", time: "9:41 AM", summary: "Site progress looks good. Check pending items before next meeting.", action: "Review and schedule meeting", accentHex: "13B77A"),
    Memory(id: "sample-client", title: "Client Meeting Notes", type: "Note", time: "8:30 AM", summary: "Discussed ERP rollout timeline and next steps.", action: "Set reminder for follow-up", accentHex: "FFA117")
]
