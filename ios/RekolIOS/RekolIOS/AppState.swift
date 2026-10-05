import Foundation
import SwiftUI
import UserNotifications

@MainActor
final class RekolAppState: ObservableObject {
    @AppStorage("access_token") var accessToken = ""
    @AppStorage("profile_name") var profileName = "Rekol user"
    @AppStorage("profile_email") var profileEmail = ""
    @AppStorage("profile_phone") var profilePhone = ""
    @AppStorage("profile_location") var profileLocation = "Dubai, UAE"
    @AppStorage("profile_photo_path") var profilePhotoPath = ""
    @AppStorage("did_onboard") var didOnboard = false

    @Published var selected: Destination = .home
    @Published var showWelcome = true
    @Published var showAuth = false
    @Published var authMode = "login"
    @Published var showCapture = false
    @Published var showProfile = false
    @Published var showSettings = false
    @Published var showNotifications = false
    @Published var showConnections = false
    @Published var showReminders = false
    @Published var selectedMemory: Memory?
    @Published var memories: [Memory] = []
    @Published var reminders: [Reminder] = []
    @Published var isAnalyzing = false
    @Published var alertMessage = ""

    private let memoriesKey = "local_memories"
    private let remindersKey = "local_reminders"

    func bootstrap() async {
        loadLocal()
        showWelcome = accessToken.isEmpty
        if !accessToken.isEmpty {
            await refreshProfileAndMemories()
        }
    }

    func authenticate(_ result: AuthResult) async {
        accessToken = result.token
        profileName = result.name?.isEmpty == false ? result.name! : profileName
        profileEmail = result.email?.isEmpty == false ? result.email! : profileEmail
        profilePhone = result.phone?.isEmpty == false ? result.phone! : profilePhone
        showAuth = false
        showWelcome = false
        await refreshProfileAndMemories()
    }

    func signOut() {
        accessToken = ""
        showProfile = false
        showSettings = false
        showWelcome = true
        showAuth = false
        authMode = "login"
    }

    func refreshProfileAndMemories() async {
        guard !accessToken.isEmpty else { return }
        do {
            let me = try await AuthService.shared.refreshSession(token: accessToken)
            profileName = me.name?.isEmpty == false ? me.name! : profileName
            profileEmail = me.email?.isEmpty == false ? me.email! : profileEmail
            profilePhone = me.phone?.isEmpty == false ? me.phone! : profilePhone
            let remote = try await APIClient.shared.memories(token: accessToken)
            let mapped = remote.map {
                Memory(
                    id: $0.id,
                    title: $0.title.isEmpty ? "Untitled memory" : $0.title,
                    type: "Note",
                    time: $0.createdAt.replacingOccurrences(of: "T", with: " ").prefix(16).description,
                    summary: $0.content,
                    action: $0.reason,
                    accentHex: "7836FF"
                )
            }
            memories = mapped
            saveLocal()
        } catch APIError.authInvalid {
            accessToken = ""
            showWelcome = true
        } catch {
            if memories.isEmpty { memories = sampleMemories }
        }
    }

    func saveMemory(content: String, reason: String, privateMemory: Bool, attachmentURL: URL?) async {
        let trimmed = content.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        isAnalyzing = true
        defer { isAnalyzing = false }

        let analysis = localAnalysis(for: trimmed)
        let enriched = "\(trimmed)\n\nRekol AI analysis:\n\(analysis)"

        if !accessToken.isEmpty {
            do {
                let saved = try await APIClient.shared.createMemory(token: accessToken, content: enriched, reason: reason, privateMemory: privateMemory)
                let memory = Memory(id: saved.id, title: saved.title, type: "Note", time: "Just now", summary: saved.content, action: saved.reason, accentHex: "7836FF", attachmentPath: attachmentURL?.path)
                memories.insert(memory, at: 0)
                selectedMemory = memory
                saveLocal()
                notify(title: "Memory saved", body: saved.title)
                return
            } catch {
                alertMessage = error.localizedDescription
            }
        }

        let memory = Memory(id: UUID().uuidString, title: titleFrom(trimmed), type: "Note", time: "Just now", summary: enriched, action: reason.isEmpty ? nil : reason, accentHex: "7836FF", attachmentPath: attachmentURL?.path)
        memories.insert(memory, at: 0)
        selectedMemory = memory
        saveLocal()
        notify(title: "Memory saved locally", body: memory.title)
    }

    func addReminder(title: String, notes: String, at: Date) {
        let reminder = Reminder(id: Int64(Date().timeIntervalSince1970 * 1000), title: title, notes: notes, at: at)
        reminders.append(reminder)
        reminders.sort { $0.at < $1.at }
        saveLocal()

        let content = UNMutableNotificationContent()
        content.title = title
        content.body = notes.isEmpty ? "Rekol reminder" : notes
        content.sound = .default
        let trigger = UNCalendarNotificationTrigger(dateMatching: Calendar.current.dateComponents([.year, .month, .day, .hour, .minute], from: at), repeats: false)
        UNUserNotificationCenter.current().add(UNNotificationRequest(identifier: "\(reminder.id)", content: content, trigger: trigger))
    }

    private func loadLocal() {
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        if let data = UserDefaults.standard.data(forKey: memoriesKey), let decoded = try? decoder.decode([Memory].self, from: data) {
            memories = decoded
        }
        if memories.isEmpty && accessToken.isEmpty {
            memories = sampleMemories
        }
        if let data = UserDefaults.standard.data(forKey: remindersKey), let decoded = try? decoder.decode([Reminder].self, from: data) {
            reminders = decoded.sorted { $0.at < $1.at }
        }
    }

    private func saveLocal() {
        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601
        UserDefaults.standard.set(try? encoder.encode(memories), forKey: memoriesKey)
        UserDefaults.standard.set(try? encoder.encode(reminders), forKey: remindersKey)
    }

    private func titleFrom(_ text: String) -> String {
        let firstLine = text.components(separatedBy: .newlines).first ?? "New memory"
        return String(firstLine.prefix(48)).isEmpty ? "New memory" : String(firstLine.prefix(48))
    }

    private func localAnalysis(for text: String) -> String {
        """
        SUMMARY:
        \(String(text.prefix(180)))
        IMPORTANT DETAILS:
        Saved from iOS capture.
        RISKS OR QUESTIONS:
        Not identified.
        SUGGESTED ACTIONS:
        Review, tag, and add a reminder if follow-up is needed.
        NEXT REMINDER:
        Not identified.
        """
    }

    private func notify(title: String, body: String) {
        let content = UNMutableNotificationContent()
        content.title = title
        content.body = body
        content.sound = .default
        UNUserNotificationCenter.current().add(UNNotificationRequest(identifier: UUID().uuidString, content: content, trigger: nil))
    }
}
