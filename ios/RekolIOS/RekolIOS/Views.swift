import SwiftUI
import PhotosUI
import UniformTypeIdentifiers
import UIKit
import AVFoundation
import UserNotifications

private func bundledImage(_ resource: String) -> Image {
    if let image = UIImage(named: resource) {
        return Image(uiImage: image)
    }
    if let url = Bundle.main.url(forResource: resource, withExtension: "png"),
       let image = UIImage(contentsOfFile: url.path) {
        return Image(uiImage: image)
    }
    return Image(systemName: "photo")
}

struct RootView: View {
    @EnvironmentObject private var state: RekolAppState
    @State private var showSplash = true

    var body: some View {
        ZStack {
            if showSplash {
                SplashView()
            } else if state.showWelcome {
                WelcomeView()
            } else if state.showAuth {
                AuthView()
            } else if !state.didOnboard {
                OnboardingView()
            } else {
                MainShellView()
            }

            if state.isAnalyzing {
                AnalysisOverlay()
            }
        }
        .tint(.rekolBlue)
        .font(.system(.body, design: .rounded))
        .alert("Rekol", isPresented: Binding(get: { !state.alertMessage.isEmpty }, set: { if !$0 { state.alertMessage = "" } })) {
            Button("OK", role: .cancel) { state.alertMessage = "" }
        } message: {
            Text(state.alertMessage)
        }
        .task {
            try? await Task.sleep(nanoseconds: 1_100_000_000)
            showSplash = false
        }
    }
}

struct MainShellView: View {
    @EnvironmentObject private var state: RekolAppState

    var body: some View {
        ZStack(alignment: .bottom) {
            Color.rekolCanvas.ignoresSafeArea()
            Group {
                if state.showProfile {
                    ProfileView()
                } else if state.showSettings {
                    SettingsView()
                } else if state.showNotifications {
                    NotificationsView()
                } else if state.showConnections {
                    ConnectionsView()
                } else if state.showReminders {
                    RemindersView()
                } else if let memory = state.selectedMemory {
                    MemoryDetailView(memory: memory)
                } else {
                    switch state.selected {
                    case .home: HomeView()
                    case .journal: JournalView()
                    case .library: LibraryView()
                    case .insights: InsightsView()
                    }
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)

            if shouldShowTabs {
                TabBar()
            }
        }
        .fullScreenCover(isPresented: $state.showCapture) {
            CaptureView()
        }
    }

    private var shouldShowTabs: Bool {
        !state.showProfile && !state.showSettings && !state.showNotifications && !state.showConnections && !state.showReminders && state.selectedMemory == nil
    }
}

struct WelcomeView: View {
    @EnvironmentObject private var state: RekolAppState

    var body: some View {
        RekolAuthBackground {
            ScrollView {
                VStack(spacing: 16) {
                    RekolLogoLockup(iconSize: 64, wordSize: 34)
                        .padding(.top, 12)

                    CaptureOrbit()
                        .frame(height: 275)

                    Text("Welcome to Rekol")
                        .font(.system(size: 39, weight: .bold, design: .rounded))
                        .foregroundStyle(Color.rekolNavy)
                        .multilineTextAlignment(.center)
                        .lineLimit(2)
                        .minimumScaleFactor(0.78)

                    Text("Your life, organized intelligently.")
                        .font(.title3.weight(.semibold))
                        .foregroundStyle(Color.rekolNavy)
                        .multilineTextAlignment(.center)

                    Text("Capture what matters, understand it with AI,\nand find it whenever you need it.")
                        .font(.subheadline)
                        .lineSpacing(4)
                        .foregroundStyle(Color(hex: "66738D"))
                        .multilineTextAlignment(.center)

                    Button {
                        state.authMode = "create"
                        state.showWelcome = false
                        state.showAuth = true
                    } label: {
                        HStack {
                            Spacer()
                            Text("Create account")
                            Spacer()
                            Image(systemName: "arrow.right")
                        }
                    }
                    .buttonStyle(SolidBlueButtonStyle())
                    .padding(.top, 8)

                    Button {
                        state.authMode = "login"
                        state.showWelcome = false
                        state.showAuth = true
                    } label: {
                        Text("Sign in").frame(maxWidth: .infinity)
                    }
                    .buttonStyle(OutlineButtonStyle(height: 56))

                    SeparatorLabel("or continue with")

                    HStack(spacing: 12) {
                        BrandButton(title: "Google", imageName: "google_logo", systemName: "g.circle.fill")
                        BrandButton(title: "Apple", imageName: nil, systemName: "apple.logo")
                    }

                    TermsText()
                    DesignerCredit()
                }
                .padding(.horizontal, 28)
                .padding(.bottom, 28)
                .frame(maxWidth: 430)
                .frame(maxWidth: .infinity)
            }
            .scrollIndicators(.hidden)
        }
    }

    private var line: some View { Rectangle().fill(Color.rekolLine).frame(height: 1) }
}

struct SplashView: View {
    @State private var animate = false

    var body: some View {
        RekolAuthBackground {
            VStack(spacing: 22) {
                Spacer()
                RekolLogoLockup(iconSize: 132, wordSize: 56)
                Text("Capture Today. Remember Tomorrow.")
                    .font(.system(size: 18, weight: .semibold, design: .rounded))
                    .tracking(2.2)
                    .foregroundStyle(Color.rekolNavy)
                    .multilineTextAlignment(.center)
                HStack(spacing: 12) {
                    ForEach(0..<3) { index in
                        Circle()
                            .fill([Color.rekolBlue, Color.rekolPurple, Color(hex: "FF8A55")][index])
                            .frame(width: 13, height: 13)
                            .scaleEffect(animate ? 1.25 : 0.75)
                            .opacity(animate ? 1 : 0.45)
                            .animation(.easeInOut(duration: 0.55).repeatForever().delay(Double(index) * 0.18), value: animate)
                    }
                }
                Spacer()
                DesignerCredit()
                    .padding(.bottom, 22)
            }
            .padding(.horizontal, 28)
            .onAppear { animate = true }
        }
    }
}

struct SocialButton: View {
    var title: String
    var symbol: String

    var body: some View {
        Button { } label: {
            HStack {
                Image(systemName: symbol)
                Text(title)
            }
            .frame(maxWidth: .infinity)
        }
        .buttonStyle(OutlineButtonStyle(height: 52))
    }
}

struct AuthView: View {
    @EnvironmentObject private var state: RekolAppState
    @State private var page: AuthPage = .login
    @State private var emailOrPhone = ""
    @State private var password = ""
    @State private var name = ""
    @State private var phone = ""
    @State private var email = ""
    @State private var createPassword = ""
    @State private var otp = ""
    @State private var countryCode = "+971"
    @State private var agreeTerms = true
    @State private var busy = false
    @State private var errorText = ""
    @State private var rememberMe = true
    @State private var productUpdates = false
    @State private var resetMode = 0
    @State private var resendSeconds = 28
    @State private var otpChallenge: OtpChallenge?
    @State private var verifyReturnPage: AuthPage = .create
    @FocusState private var otpFocused: Bool

    var body: some View {
        RekolAuthBackground {
            ScrollView {
                VStack(spacing: 18) {
                    HStack {
                        Button { goBack() } label: {
                            Image(systemName: "chevron.left")
                                .font(.system(size: 24, weight: .semibold))
                                .foregroundStyle(Color.rekolNavy)
                                .frame(width: 48, height: 48)
                        }
                        Spacer()
                    }
                    .frame(height: 52)

                    RekolLogoLockup(iconSize: 58, wordSize: 31)

                    switch page {
                    case .login: loginContent
                    case .create: createContent
                    case .verify: verifyContent
                    case .reset: resetContent
                    }

                    DesignerCredit()
                }
                .padding(.horizontal, 28)
                .padding(.bottom, 26)
                .frame(maxWidth: 430)
                .frame(maxWidth: .infinity)
            }
            .scrollIndicators(.hidden)
        }
        .onAppear {
            page = AuthPage(rawValue: state.authMode) ?? .login
        }
        .onChange(of: state.authMode) { newValue in
            page = AuthPage(rawValue: newValue) ?? .login
        }
    }

    private var loginContent: some View {
        VStack(spacing: 15) {
            AuthHeader(title: "Welcome back", subtitle: "Sign in to capture, organize and\nremember what matters.")
            LoginIdentifierField(text: $emailOrPhone, countryCode: $countryCode)
            if !isPhoneLogin {
                LabeledSecureField(title: "Password", placeholder: "Enter your password", text: $password)
            }
            HStack {
                CheckRow(title: "Remember me", isOn: $rememberMe)
                Spacer()
                Button("Forgot password?") { page = .reset }
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Color.rekolBlue)
            }
            AuthErrorText(errorText)
            Button { Task { await submit() } } label: {
                HStack {
                    Spacer()
                    Text(busy ? "Please wait..." : "Sign in")
                    Image(systemName: "arrow.right")
                    Spacer()
                }
            }
            .buttonStyle(SolidBlueButtonStyle())
            .disabled(busy)
            SeparatorLabel("or continue with")
            HStack(spacing: 12) {
                BrandButton(title: "Apple", imageName: nil, systemName: "apple.logo")
                BrandButton(title: "Google", imageName: "google_logo", systemName: "g.circle.fill")
            }
            Button { } label: {
                HStack {
                    Image(systemName: "touchid")
                        .font(.title2)
                    Text("Sign in with biometrics")
                }
                .frame(maxWidth: .infinity)
            }
            .buttonStyle(OutlineButtonStyle(height: 54))
            Button {
                errorText = ""
                state.authMode = "create"
                page = .create
            } label: {
                HStack(spacing: 4) {
                    Text("New to Rekol?")
                        .foregroundColor(Color(hex: "66738D"))
                    Text("Create account")
                        .foregroundColor(Color.rekolBlue)
                        .fontWeight(.semibold)
                }
                .font(.headline)
                .frame(maxWidth: .infinity, minHeight: 44)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            TrustNote(text: "Secure sign-in. Your data stays protected.")
        }
    }

    private var createContent: some View {
        VStack(spacing: 14) {
            AuthHeader(title: "Create your account", subtitle: "Start building your personal memory.")
            StepProgress(current: 1)
            LabeledField(title: "Full name", placeholder: "Full name", text: $name, symbol: "person")
            PhoneField(title: "Phone number", placeholder: "50 123 4567", phone: $phone, countryCode: $countryCode)
            Text("We'll send a verification code.")
                .font(.caption)
                .foregroundStyle(Color(hex: "66738D"))
                .frame(maxWidth: .infinity, alignment: .leading)
            LabeledField(title: "Email address", placeholder: "you@example.com", text: $email, symbol: "envelope")
                .keyboardType(.emailAddress)
                .textInputAutocapitalization(.never)
            LabeledSecureField(title: "Create password", placeholder: "At least 6 characters", text: $createPassword)
            LabeledField(title: "", placeholder: "Referral code (optional)", text: .constant(""), symbol: "gift")
            CheckRow(title: "I agree to the Terms of Service and Privacy Policy.", isOn: $agreeTerms)
            CheckRow(title: "Send me product updates and helpful tips.", isOn: $productUpdates)
            AuthErrorText(errorText)
            Button { Task { await submit() } } label: {
                HStack {
                    Spacer()
                    Text(busy ? "Please wait..." : "Continue to verification")
                    Image(systemName: "arrow.right")
                    Spacer()
                }
            }
            .buttonStyle(SolidBlueButtonStyle())
            .disabled(busy)
            Button { page = .login } label: {
                Text("Already have an account? ")
                    .foregroundColor(Color(hex: "66738D"))
                + Text("Sign in")
                    .foregroundColor(Color.rekolBlue)
                    .fontWeight(.semibold)
            }
        }
    }

    private var verifyContent: some View {
        VStack(spacing: 22) {
            AuthHeader(title: "Verify your phone", subtitle: "Enter the 6-digit code sent to\n\(verificationDisplayPhone)")
            Button("Change number") { page = verifyReturnPage }
                .font(.headline)
                .foregroundStyle(Color.rekolBlue)
            StepProgress(current: 2)
            OtpInput(code: $otp, focused: $otpFocused)
            AuthErrorText(errorText)
            if otp.count == 6 {
                TrustNote(text: "Code entered securely", color: Color(hex: "20C77A"))
            } else {
                TrustNote(text: "Enter the code sent to your phone.")
            }
            Text(resendSeconds > 0 ? "Resend code in **00:\(String(format: "%02d", resendSeconds))**" : "You can request a new code now.")
                .foregroundStyle(Color(hex: "66738D"))
            Button { Task { await submit() } } label: {
                HStack {
                    Spacer()
                    Text(busy ? "Please wait..." : "Verify and continue")
                    Image(systemName: "arrow.right")
                    Spacer()
                }
            }
            .buttonStyle(SolidBlueButtonStyle())
            .disabled(busy || otp.count < 6)
            VStack(spacing: 8) {
                Text("Didn't receive a code?")
                    .foregroundStyle(Color.rekolNavy)
                Button("Resend code") {
                    Task { await resendOtp() }
                }
                .foregroundStyle(resendSeconds > 0 ? Color(hex: "A0A8B8") : Color.rekolBlue)
                .disabled(resendSeconds > 0 || busy)
            }
            TrustNote(text: "Your number is used only to protect your account.")
        }
        .onAppear {
            otpFocused = true
            if resendSeconds == 0 {
                resendSeconds = 28
            }
        }
        .task(id: resendSeconds) {
            guard page == .verify, resendSeconds > 0 else { return }
            try? await Task.sleep(nanoseconds: 1_000_000_000)
            guard page == .verify, resendSeconds > 0 else { return }
            resendSeconds -= 1
        }
    }

    private var resetContent: some View {
        VStack(spacing: 18) {
            AuthHeader(title: "Access your account", subtitle: "Reset your password or sign in securely\nwith a one-time code.")
            Picker("", selection: $resetMode) {
                Text("Reset password").tag(0)
                Text("Login with OTP").tag(1)
            }
            .pickerStyle(.segmented)
            Image(systemName: resetMode == 0 ? "key.fill" : "message.badge.fill")
                .font(.system(size: 45, weight: .bold))
                .foregroundStyle(.white)
                .frame(width: 105, height: 105)
                .background(LinearGradient(colors: [.rekolCyan, .rekolBlue, .rekolPurple], startPoint: .topLeading, endPoint: .bottomTrailing), in: Circle())
                .shadow(color: Color.rekolPurple.opacity(0.18), radius: 18)
            AuthHeader(title: resetMode == 0 ? "Reset your password" : "Sign in without a password", subtitle: resetMode == 0 ? "Enter your registered email or phone number.\nWe'll send you a verification code." : "Receive a secure OTP on your\nregistered phone.")
            LoginIdentifierField(text: $emailOrPhone, countryCode: $countryCode)
            Button { Task { await submit() } } label: {
                HStack {
                    Spacer()
                    Text(busy ? "Please wait..." : "Send verification code")
                    Image(systemName: "arrow.right")
                    Spacer()
                }
            }
            .buttonStyle(SolidBlueButtonStyle())
            .disabled(busy)
            Button { page = .login } label: {
                Text("Remember your password? ")
                    .foregroundColor(Color(hex: "66738D"))
                + Text("Back to sign in")
                    .foregroundColor(Color.rekolBlue)
                    .fontWeight(.semibold)
            }
            Button { resetMode = 1 } label: {
                HStack(spacing: 14) {
                    Image(systemName: "iphone.gen3.badge.play")
                        .font(.title2)
                        .foregroundStyle(Color.rekolBlue)
                        .frame(width: 54, height: 54)
                        .background(Color(hex: "EDF6FF"), in: Circle())
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Sign in without a password")
                            .font(.headline)
                            .foregroundStyle(Color.rekolNavy)
                        Text("Receive a secure OTP on your registered phone.")
                            .font(.caption)
                            .foregroundStyle(Color(hex: "66738D"))
                    }
                    Spacer()
                    Text("Login with OTP")
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(Color.rekolBlue)
                    Image(systemName: "arrow.right")
                        .foregroundStyle(Color.rekolBlue)
                }
                .padding(14)
                .background(.white, in: RoundedRectangle(cornerRadius: 14))
                .overlay(RoundedRectangle(cornerRadius: 14).stroke(Color.rekolLine))
            }
            TrustNote(text: "Codes expire in 5 minutes for your security.")
        }
    }

    private var isPhoneLogin: Bool {
        let trimmed = emailOrPhone.trimmingCharacters(in: .whitespacesAndNewlines)
        return !trimmed.isEmpty && trimmed.allSatisfy(\.isNumber)
    }

    private var phoneSuffix: String {
        String((phone.isEmpty ? emailOrPhone : phone).suffix(4))
    }

    private func goBack() {
        switch page {
        case .login:
            state.showAuth = false
            state.showWelcome = true
        case .create:
            state.showAuth = false
            state.showWelcome = true
        case .verify:
            resendSeconds = 28
            page = verifyReturnPage
        case .reset:
            page = .login
        }
    }

    private func submit() async {
        errorText = ""
        busy = true
        defer { busy = false }
        do {
            if page == .login {
                if isPhoneLogin {
                    phone = emailOrPhone.filter(\.isNumber)
                    let challenge = try await AuthService.shared.requestLoginOtp(phone: fullPhone)
                    beginVerification(challenge: challenge, returnTo: .login)
                    return
                }
                guard !emailOrPhone.isEmpty, !password.isEmpty else { errorText = "Enter your email and password."; return }
                let result = try await AuthService.shared.login(email: emailOrPhone, password: password)
                await state.authenticate(result)
            } else if page == .verify {
                guard let otpChallenge else { errorText = "Request a verification code first."; return }
                guard otp.count == 6 else { errorText = "Enter the 6-digit verification code."; return }
                let result = try await AuthService.shared.verifyOtp(challenge: otpChallenge, code: otp)
                state.didOnboard = false
                await state.authenticate(result)
            } else if page == .reset {
                guard isPhoneLogin else {
                    errorText = "Password reset by email needs a backend reset endpoint. Use a phone number for OTP sign-in."
                    return
                }
                phone = emailOrPhone.filter(\.isNumber)
                let challenge = try await AuthService.shared.requestLoginOtp(phone: fullPhone)
                beginVerification(challenge: challenge, returnTo: .reset)
            } else {
                guard agreeTerms, !name.isEmpty, email.contains("@"), createPassword.count >= 6 else {
                    errorText = "Complete the required details and use a 6-character password."
                    return
                }
                let challenge = try await AuthService.shared.registerAndRequestOtp(name: name, email: email, phone: fullPhone, password: createPassword)
                beginVerification(challenge: challenge, returnTo: .create)
            }
        } catch {
            errorText = error.localizedDescription
        }
    }

    private var fullPhone: String {
        "\(countryCode)\((phone.isEmpty ? emailOrPhone : phone).filter(\.isNumber))"
    }

    private var verificationDisplayPhone: String {
        let value = otpChallenge?.phone ?? fullPhone
        return "\(countryCode) • • • • \(String(value.suffix(4)))"
    }

    private func beginVerification(challenge: OtpChallenge, returnTo: AuthPage) {
        otpChallenge = challenge
        verifyReturnPage = returnTo
        otp = ""
        errorText = ""
        resendSeconds = 28
        state.authMode = "verify"
        page = .verify
        otpFocused = true
    }

    private func resendOtp() async {
        guard let current = otpChallenge else {
            errorText = "Enter your phone number to request a code."
            return
        }
        errorText = ""
        busy = true
        defer { busy = false }
        do {
            let challenge = try await AuthService.shared.requestLoginOtp(phone: current.phone)
            beginVerification(challenge: challenge, returnTo: verifyReturnPage)
        } catch {
            errorText = error.localizedDescription
        }
    }
}

enum AuthPage {
    init?(rawValue: String) {
        switch rawValue {
        case "login": self = .login
        case "create": self = .create
        case "verify": self = .verify
        case "reset": self = .reset
        default: return nil
        }
    }

    case login
    case create
    case verify
    case reset
}

struct RekolAuthBackground<Content: View>: View {
    @ViewBuilder var content: Content

    var body: some View {
        ZStack {
            LinearGradient(colors: [Color(hex: "F7FCFF"), Color(hex: "EAF7FF"), Color(hex: "FFF7FF")], startPoint: .top, endPoint: .bottom)
                .ignoresSafeArea()
            bundledImage("rekol_welcome_background")
                .resizable()
                .scaledToFill()
                .opacity(0.96)
                .ignoresSafeArea()
            content
        }
    }
}

struct RekolLogoLockup: View {
    var iconSize: CGFloat
    var wordSize: CGFloat

    var body: some View {
        VStack(spacing: 4) {
            bundledImage("rekol_mobile_icon")
                .resizable()
                .scaledToFit()
                .frame(width: iconSize, height: iconSize)
                .clipShape(RoundedRectangle(cornerRadius: iconSize * 0.2, style: .continuous))
                .shadow(color: Color.rekolBlue.opacity(0.16), radius: 12, y: 6)
            Text("rekol")
                .font(.system(size: wordSize, weight: .heavy, design: .rounded))
                .foregroundStyle(Color.rekolNavy)
                .lineLimit(1)
                .minimumScaleFactor(0.8)
        }
        .accessibilityLabel("Rekol")
    }
}

struct CaptureOrbit: View {
    private let items: [(String, String, Color, CGFloat, CGFloat)] = [
        ("camera.fill", "Photo", .rekolCyan, -105, -15),
        ("doc.text.fill", "Note", .rekolPurple, 0, -95),
        ("link", "Link", .rekolBlue, 105, -10),
        ("mic.fill", "Voice", .rekolCoral, -82, 90),
        ("doc.fill", "File", Color(hex: "FFA117"), 90, 86)
    ]

    var body: some View {
        ZStack {
            Circle()
                .stroke(Color.rekolBlue.opacity(0.18), lineWidth: 2)
                .frame(width: 210, height: 210)
            bundledImage("rekol_mobile_icon")
                .resizable()
                .scaledToFit()
                .frame(width: 112, height: 112)
                .background(Circle().fill(Color.white.opacity(0.78)).shadow(color: Color.rekolBlue.opacity(0.18), radius: 20))
            ForEach(items.indices, id: \.self) { index in
                let item = items[index]
                VStack(spacing: 8) {
                    Image(systemName: item.0)
                        .font(.system(size: 23, weight: .bold))
                        .foregroundStyle(item.2)
                    Text(item.1)
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(Color.rekolNavy)
                }
                .frame(width: 82, height: 82)
                .background(.white.opacity(0.82), in: RoundedRectangle(cornerRadius: 20, style: .continuous))
                .shadow(color: item.2.opacity(0.18), radius: 12)
                .offset(x: item.3, y: item.4)
            }
        }
    }
}

struct AuthHeader: View {
    var title: String
    var subtitle: String

    var body: some View {
        VStack(spacing: 8) {
            Text(title)
                .font(.system(size: 35, weight: .bold, design: .rounded))
                .foregroundStyle(Color.rekolNavy)
                .multilineTextAlignment(.center)
                .lineLimit(2)
                .minimumScaleFactor(0.72)
            Text(subtitle)
                .font(.system(size: 18, weight: .medium, design: .rounded))
                .lineSpacing(4)
                .foregroundStyle(Color(hex: "66738D"))
                .multilineTextAlignment(.center)
        }
    }
}

struct StepProgress: View {
    var current: Int

    var body: some View {
        HStack(spacing: 12) {
            StepDot(number: 1, label: "Details", state: current > 1 ? .done : current == 1 ? .active : .idle)
            Rectangle().fill(Color.rekolLine).frame(height: 2)
            StepDot(number: 2, label: "Verify", state: current > 2 ? .done : current == 2 ? .active : .idle)
            Rectangle().fill(Color.rekolLine).frame(height: 2)
            StepDot(number: 3, label: "Secure", state: current == 3 ? .active : .idle)
        }
        .padding(.vertical, 8)
    }
}

private enum StepState { case active, done, idle }

private struct StepDot: View {
    var number: Int
    var label: String
    var state: StepState

    var body: some View {
        VStack(spacing: 7) {
            ZStack {
                Circle()
                    .fill(state == .done ? Color(hex: "20C77A") : state == .active ? Color.rekolBlue : Color.white)
                    .frame(width: 42, height: 42)
                    .overlay(Circle().stroke(Color.rekolLine))
                    .shadow(color: state == .active ? Color.rekolBlue.opacity(0.22) : .clear, radius: 8)
                Image(systemName: state == .done ? "checkmark" : "\(number).circle.fill")
                    .font(.system(size: state == .done ? 18 : 26, weight: .bold))
                    .foregroundStyle(state == .idle ? Color(hex: "8A94A8") : .white)
            }
            Text(label)
                .font(.caption.weight(.semibold))
                .foregroundStyle(state == .active ? Color.rekolBlue : Color(hex: "66738D"))
        }
        .frame(width: 66)
    }
}

struct OtpInput: View {
    @Binding var code: String
    var focused: FocusState<Bool>.Binding

    private var digits: [Character] {
        Array(code.padding(toLength: 6, withPad: " ", startingAt: 0).prefix(6))
    }

    var body: some View {
        ZStack {
            TextField("", text: Binding(
                get: { code },
                set: { code = String($0.filter(\.isNumber).prefix(6)) }
            ))
            .keyboardType(.numberPad)
            .textContentType(.oneTimeCode)
            .focused(focused)
            .frame(width: 1, height: 1)
            .opacity(0.01)
            .accessibilityLabel("Verification code")

            HStack(spacing: 9) {
                ForEach(Array(digits.enumerated()), id: \.offset) { index, digit in
                    Text(digit == " " ? "" : String(digit))
                        .font(.system(size: 31, weight: .bold, design: .rounded))
                        .foregroundStyle(Color.rekolNavy)
                        .frame(width: 50, height: 64)
                        .background(.white, in: RoundedRectangle(cornerRadius: 12))
                        .overlay(RoundedRectangle(cornerRadius: 12).stroke(activeIndex == index ? Color.rekolBlue : Color.rekolLine, lineWidth: 1.4))
                        .shadow(color: activeIndex == index ? Color.rekolPurple.opacity(0.26) : .clear, radius: 7)
                }
            }
            .contentShape(Rectangle())
            .onTapGesture {
                focused.wrappedValue = true
            }
        }
    }

    private var activeIndex: Int {
        min(code.count, 5)
    }
}

struct LabeledField: View {
    var title: String
    var placeholder: String
    @Binding var text: String
    var symbol: String

    var body: some View {
        VStack(alignment: .leading, spacing: 7) {
            if !title.isEmpty {
                Text(title)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Color.rekolNavy)
            }
            HStack(spacing: 12) {
                Image(systemName: symbol)
                    .font(.title3)
                    .foregroundStyle(Color(hex: "4D5A7A"))
                    .frame(width: 28)
                TextField(placeholder, text: $text)
                    .font(.body)
                    .textInputAutocapitalization(.never)
            }
            .padding(.horizontal, 14)
            .frame(height: 58)
            .background(.white.opacity(0.95), in: RoundedRectangle(cornerRadius: 12))
            .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.rekolLine))
        }
    }
}

private let countryOptions: [(code: String, flag: String, name: String)] = [
    ("+971", "🇦🇪", "United Arab Emirates"),
    ("+91", "🇮🇳", "India"),
    ("+1", "🇺🇸", "United States"),
    ("+44", "🇬🇧", "United Kingdom"),
    ("+966", "🇸🇦", "Saudi Arabia"),
    ("+974", "🇶🇦", "Qatar"),
    ("+965", "🇰🇼", "Kuwait"),
    ("+973", "🇧🇭", "Bahrain"),
    ("+968", "🇴🇲", "Oman"),
    ("+92", "🇵🇰", "Pakistan")
]

private func flag(for countryCode: String) -> String {
    countryOptions.first { $0.code == countryCode }?.flag ?? "🌐"
}

struct CountryCodePicker: View {
    @Binding var countryCode: String
    @State private var showCountries = false

    var body: some View {
        Button {
            showCountries = true
        } label: {
            HStack(spacing: 5) {
                Text(flag(for: countryCode))
                    .font(.system(size: 20))
                Text(countryCode)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Color.rekolNavy)
                Image(systemName: "chevron.down")
                    .font(.caption.weight(.bold))
                    .foregroundStyle(Color(hex: "66738D"))
            }
            .frame(minWidth: 88, alignment: .leading)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .sheet(isPresented: $showCountries) {
            CountryCodeSheet(countryCode: $countryCode)
                .presentationDetents([.medium])
        }
    }
}

struct CountryCodeSheet: View {
    @Binding var countryCode: String
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            List(countryOptions, id: \.code) { country in
                Button {
                    countryCode = country.code
                    dismiss()
                } label: {
                    HStack(spacing: 12) {
                        Text(country.flag)
                            .font(.title3)
                        VStack(alignment: .leading, spacing: 2) {
                            Text(country.name)
                                .font(.body.weight(.semibold))
                                .foregroundStyle(Color.rekolNavy)
                            Text(country.code)
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                        Spacer()
                        if country.code == countryCode {
                            Image(systemName: "checkmark.circle.fill")
                                .foregroundStyle(Color.rekolBlue)
                        }
                    }
                    .padding(.vertical, 4)
                }
            }
            .navigationTitle("Country code")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Done") { dismiss() }
                }
            }
        }
    }
}

struct PhoneField: View {
    var title: String
    var placeholder: String
    @Binding var phone: String
    @Binding var countryCode: String

    var body: some View {
        VStack(alignment: .leading, spacing: 7) {
            if !title.isEmpty {
                Text(title)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Color.rekolNavy)
            }
            HStack(spacing: 12) {
                CountryCodePicker(countryCode: $countryCode)
                Rectangle()
                    .fill(Color.rekolLine)
                    .frame(width: 1, height: 28)
                TextField(placeholder, text: Binding(
                    get: { phone },
                    set: { phone = String($0.filter(\.isNumber).prefix(12)) }
                ))
                .font(.body)
                .keyboardType(.phonePad)
            }
            .padding(.horizontal, 14)
            .frame(height: 58)
            .background(.white.opacity(0.95), in: RoundedRectangle(cornerRadius: 12))
            .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.rekolLine))
        }
    }
}

struct LoginIdentifierField: View {
    @Binding var text: String
    @Binding var countryCode: String

    private var isPhone: Bool {
        let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
        return !trimmed.isEmpty && trimmed.allSatisfy(\.isNumber)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 7) {
            Text("Email or phone number")
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(Color.rekolNavy)
            HStack(spacing: 12) {
                if isPhone {
                    CountryCodePicker(countryCode: $countryCode)
                    Rectangle()
                        .fill(Color.rekolLine)
                        .frame(width: 1, height: 28)
                } else {
                    Image(systemName: "person")
                        .font(.title3)
                        .foregroundStyle(Color(hex: "4D5A7A"))
                        .frame(width: 28)
                }
                TextField("Enter email or phone number", text: Binding(
                    get: { text },
                    set: { value in
                        if value.allSatisfy(\.isNumber) {
                            text = String(value.filter(\.isNumber).prefix(12))
                        } else {
                            text = value
                        }
                    }
                ))
                .font(.body)
                .keyboardType(isPhone ? .phonePad : .emailAddress)
                .textInputAutocapitalization(.never)
            }
            .padding(.horizontal, 14)
            .frame(height: 58)
            .background(.white.opacity(0.95), in: RoundedRectangle(cornerRadius: 12))
            .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.rekolLine))
        }
    }
}

struct LabeledSecureField: View {
    var title: String
    var placeholder: String
    @Binding var text: String
    @State private var hidden = true

    var body: some View {
        VStack(alignment: .leading, spacing: 7) {
            Text(title)
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(Color.rekolNavy)
            HStack(spacing: 12) {
                Image(systemName: "lock")
                    .font(.title3)
                    .foregroundStyle(Color(hex: "4D5A7A"))
                    .frame(width: 28)
                Group {
                    if hidden {
                        SecureField(placeholder, text: $text)
                    } else {
                        TextField(placeholder, text: $text)
                    }
                }
                Button { hidden.toggle() } label: {
                    Image(systemName: hidden ? "eye.slash" : "eye")
                        .foregroundStyle(Color(hex: "4D5A7A"))
                }
            }
            .padding(.horizontal, 14)
            .frame(height: 58)
            .background(.white.opacity(0.95), in: RoundedRectangle(cornerRadius: 12))
            .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.rekolLine))
        }
    }
}

struct CheckRow: View {
    var title: String
    @Binding var isOn: Bool

    var body: some View {
        Button { isOn.toggle() } label: {
            HStack(spacing: 12) {
                Image(systemName: isOn ? "checkmark.square.fill" : "square")
                    .font(.title2)
                    .foregroundStyle(isOn ? Color.rekolBlue : Color(hex: "AAB4C4"))
                Text(title)
                    .font(.subheadline)
                    .foregroundStyle(Color.rekolNavy)
                    .multilineTextAlignment(.leading)
                Spacer(minLength: 0)
            }
        }
        .buttonStyle(.plain)
    }
}

struct SeparatorLabel: View {
    var text: String

    init(_ text: String) {
        self.text = text
    }

    var body: some View {
        HStack {
            Rectangle().fill(Color.rekolLine).frame(height: 1)
            Text(text)
                .font(.caption)
                .foregroundStyle(Color(hex: "66738D"))
                .padding(.horizontal, 10)
            Rectangle().fill(Color.rekolLine).frame(height: 1)
        }
        .padding(.vertical, 2)
    }
}

struct BrandButton: View {
    var title: String
    var imageName: String?
    var systemName: String

    var body: some View {
        Button { } label: {
            HStack(spacing: 10) {
                if let imageName {
                    bundledImage(imageName)
                        .resizable()
                        .renderingMode(.original)
                        .scaledToFit()
                        .frame(width: 23, height: 23)
                } else {
                    Image(systemName: systemName)
                        .font(.title2)
                }
                Text(title)
                    .font(.headline)
            }
            .foregroundStyle(Color.rekolNavy)
            .frame(maxWidth: .infinity)
        }
        .buttonStyle(OutlineButtonStyle(height: 54))
    }
}

struct TermsText: View {
    var body: some View {
        Text("By continuing, you agree to our Terms of Service and Privacy Policy.")
            .font(.caption)
            .foregroundStyle(Color(hex: "66738D"))
            .multilineTextAlignment(.center)
            .padding(.horizontal, 8)
    }
}

struct DesignerCredit: View {
    var body: some View {
        HStack(spacing: 3) {
            Text("Crafted and designed by")
                .foregroundStyle(Color(hex: "66738D"))
            Text("PrabhaTech")
                .foregroundStyle(Color.rekolBlue)
                .fontWeight(.semibold)
        }
        .font(.caption)
    }
}

struct AuthErrorText: View {
    var text: String

    init(_ text: String) {
        self.text = text
    }

    var body: some View {
        if !text.isEmpty {
            Text(text)
                .font(.caption)
                .foregroundStyle(Color.rekolCoral)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
    }
}

struct TrustNote: View {
    var text: String
    var color: Color = Color(hex: "4D5A7A")

    var body: some View {
        HStack(spacing: 10) {
            Image(systemName: "checkmark.shield")
                .font(.title3)
                .foregroundStyle(color)
            Text(text)
                .font(.subheadline)
                .foregroundStyle(Color(hex: "66738D"))
            Spacer(minLength: 0)
        }
        .padding(.top, 6)
    }
}

struct OnboardingView: View {
    @EnvironmentObject private var state: RekolAppState
    @State private var page = 0

    private let titles = ["Welcome to Rekol", "Capture anything", "Understand automatically", "Find it anytime", "Turn information into action"]
    private let messages = [
        "Your personal intelligence companion.",
        "Save notes, links, screenshots, photos, audio and documents in one tap.",
        "Rekol extracts context, topics and useful summaries while you continue your day.",
        "Search naturally across your private memory library whenever you need it.",
        "Turn saved information into reminders, calendar events and follow-ups."
    ]
    private let symbols = ["sparkles", "camera.fill", "brain.head.profile", "magnifyingglass", "checkmark.circle.fill"]

    var body: some View {
        VStack(spacing: 22) {
            RekolLogoLockup(iconSize: 68, wordSize: 34)
            Spacer()
            Image(systemName: symbols[page])
                .font(.system(size: 52, weight: .semibold))
                .foregroundStyle(.white)
                .frame(width: 118, height: 118)
                .background(LinearGradient(colors: [.rekolBlue, .rekolCyan, .rekolPurple], startPoint: .topLeading, endPoint: .bottomTrailing), in: Circle())
            Text(titles[page])
                .font(.title.bold())
                .foregroundStyle(Color.rekolNavy)
            Text(messages[page])
                .font(.body)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
            Spacer()
            HStack(spacing: 7) {
                ForEach(titles.indices, id: \.self) { index in
                    Capsule()
                        .fill(index == page ? Color.rekolBlue : Color.rekolLine)
                        .frame(width: index == page ? 25 : 7, height: 7)
                }
            }
            Button(page == titles.indices.last ? "Start using Rekol" : "Continue") {
                requestPermissionIfNeeded(for: page)
                if page == titles.indices.last {
                    completeOnboarding()
                } else {
                    page += 1
                }
            }
            .buttonStyle(PrimaryButtonStyle())
            Button("Skip for now") {
                completeOnboarding()
            }
            .foregroundStyle(.secondary)
        }
        .padding(24)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(LinearGradient(colors: [.white, Color(hex: "F0F6FF"), Color(hex: "EAF9F7")], startPoint: .top, endPoint: .bottom))
    }

    private func completeOnboarding() {
        state.didOnboard = true
        state.showCapture = true
    }

    private func requestPermissionIfNeeded(for page: Int) {
        switch page {
        case 1:
            AVCaptureDevice.requestAccess(for: .video) { _ in }
        case 2:
            AVCaptureDevice.requestAccess(for: .audio) { _ in }
        case 3:
            UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound]) { _, _ in }
        default:
            break
        }
    }
}

struct HomeView: View {
    @EnvironmentObject private var state: RekolAppState

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 22) {
                HStack {
                    RekolLogoLockup(iconSize: 48, wordSize: 24)
                        .frame(width: 72, alignment: .leading)
                    Spacer()
                    Button { state.showReminders = true } label: {
                        Image(systemName: "calendar").iconButton()
                    }
                    Button { state.showNotifications = true } label: {
                        Image(systemName: "bell.fill").iconButton()
                    }
                    Button { state.showProfile = true } label: {
                        Text(initials)
                            .font(.headline)
                            .foregroundStyle(.white)
                            .frame(width: 42, height: 42)
                            .background(LinearGradient(colors: [.rekolBlue, .rekolPurple], startPoint: .topLeading, endPoint: .bottomTrailing), in: Circle())
                    }
                }
                .padding(.top, 18)

                Text("Let's capture what\nmatters today.")
                    .font(.system(size: 34, weight: .heavy, design: .rounded))
                    .foregroundStyle(Color.rekolNavy)
                    .lineSpacing(2)

                Button { state.showCapture = true } label: {
                    HStack(spacing: 14) {
                        Image(systemName: "magnifyingglass")
                            .font(.title2)
                            .foregroundStyle(.secondary)
                        Text("Ask or capture anything...")
                            .font(.headline)
                            .foregroundStyle(.secondary)
                        Spacer()
                        Image(systemName: "mic.fill")
                            .font(.title2)
                            .foregroundStyle(Color.rekolNavy)
                    }
                    .padding(.horizontal, 18)
                    .frame(height: 62)
                    .background(.white, in: RoundedRectangle(cornerRadius: 22, style: .continuous))
                    .shadow(color: .black.opacity(0.10), radius: 12, y: 5)
                }
                .buttonStyle(.plain)

                HStack(spacing: 10) {
                    HomeCaptureShortcut(symbol: "doc.text.fill", label: "Note", color: .rekolPurple)
                    HomeCaptureShortcut(symbol: "camera.fill", label: "Photo", color: Color(hex: "15BEA8"))
                    HomeCaptureShortcut(symbol: "link", label: "Link", color: .rekolBlue)
                    HomeCaptureShortcut(symbol: "mic.fill", label: "Audio", color: .rekolCoral)
                    HomeCaptureShortcut(symbol: "doc.fill", label: "File", color: .rekolPurple)
                    HomeCaptureShortcut(symbol: "viewfinder", label: "Screenshot", color: .rekolBlue)
                }
                .onTapGesture {
                    state.showCapture = true
                }

                Button { state.showReminders = true } label: {
                    HStack(spacing: 14) {
                        Image(systemName: "calendar")
                            .font(.title2.weight(.bold))
                            .foregroundStyle(Color.rekolBlue)
                            .frame(width: 52, height: 52)
                            .background(Color(hex: "E9F4FF"), in: RoundedRectangle(cornerRadius: 16))
                        VStack(alignment: .leading, spacing: 3) {
                            Text("Calendar & reminders")
                                .font(.headline)
                                .foregroundStyle(Color.rekolNavy)
                            Text("Schedule follow-ups and get notified on time")
                                .font(.subheadline)
                                .foregroundStyle(.secondary)
                                .lineLimit(1)
                        }
                        Spacer()
                        Image(systemName: "chevron.right")
                            .font(.headline.weight(.bold))
                            .foregroundStyle(Color.rekolBlue)
                    }
                    .padding(16)
                    .background(.white, in: RoundedRectangle(cornerRadius: 20, style: .continuous))
                    .overlay(RoundedRectangle(cornerRadius: 20).stroke(Color.rekolLine))
                    .shadow(color: .black.opacity(0.05), radius: 8, y: 3)
                }
                .buttonStyle(.plain)

                Text("Today's Snapshot")
                    .font(.title2.bold())
                    .foregroundStyle(Color.rekolNavy)

                HStack(spacing: 14) {
                    SnapshotCard(value: "\(state.memories.count)", label: "items captured", color: .rekolPurple)
                    SnapshotCard(value: "\(state.memories.filter { $0.action != nil }.count)", label: "smart actions", color: Color(hex: "FFA117"))
                }

                if let latest = state.memories.first {
                    Button { state.selectedMemory = latest } label: {
                        HStack(spacing: 14) {
                            Image(systemName: "sparkles")
                                .font(.title2.weight(.bold))
                                .foregroundStyle(Color.rekolPurple)
                                .frame(width: 50, height: 50)
                                .background(.white, in: RoundedRectangle(cornerRadius: 14))
                            VStack(alignment: .leading, spacing: 4) {
                                Text("Smart insight")
                                    .font(.headline)
                                    .foregroundStyle(Color.rekolNavy)
                                Text("Rekol analyzed your latest capture: \(latest.title)")
                                    .font(.subheadline)
                                    .foregroundStyle(.secondary)
                                    .lineLimit(2)
                                if let action = latest.action {
                                    Text("Suggested next step: \(action)")
                                        .font(.caption.weight(.semibold))
                                        .foregroundStyle(Color.rekolBlue)
                                }
                            }
                            Spacer()
                            Image(systemName: "chevron.right")
                                .font(.headline.weight(.bold))
                                .foregroundStyle(Color.rekolBlue)
                        }
                        .padding(16)
                        .background(Color(hex: "F3EDFF"), in: RoundedRectangle(cornerRadius: 20))
                        .overlay(RoundedRectangle(cornerRadius: 20).stroke(Color(hex: "CBA9FF")))
                    }
                    .buttonStyle(.plain)
                }

                HStack {
                    Text("Recent Captures")
                        .font(.title2.bold())
                        .foregroundStyle(Color.rekolNavy)
                    Spacer()
                }

                ForEach(state.memories.prefix(4)) { memory in
                    CompactMemoryRow(memory: memory) { state.selectedMemory = memory }
                }
            }
            .padding(.horizontal, 18)
            .padding(.bottom, 120)
        }
    }

    private var initials: String {
        String(state.profileName.trimmingCharacters(in: .whitespaces).first ?? "R").uppercased()
    }
}

struct HomeCaptureShortcut: View {
    var symbol: String
    var label: String
    var color: Color

    var body: some View {
        VStack(spacing: 8) {
            Image(systemName: symbol)
                .font(.headline.weight(.bold))
                .foregroundStyle(color)
                .frame(width: 46, height: 46)
                .background(color.opacity(0.12), in: RoundedRectangle(cornerRadius: 15))
            Text(label)
                .font(.caption2.weight(.medium))
                .foregroundStyle(Color.rekolNavy)
                .lineLimit(1)
                .minimumScaleFactor(0.75)
        }
        .frame(maxWidth: .infinity)
    }
}

struct SnapshotCard: View {
    var value: String
    var label: String
    var color: Color

    var body: some View {
        HStack(alignment: .firstTextBaseline, spacing: 10) {
            Text(value)
                .font(.system(size: 28, weight: .heavy, design: .rounded))
                .foregroundStyle(color)
            Text(label)
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .lineLimit(1)
                .minimumScaleFactor(0.8)
            Spacer(minLength: 0)
        }
        .padding(16)
        .frame(maxWidth: .infinity, minHeight: 74)
        .background(.white, in: RoundedRectangle(cornerRadius: 18))
        .overlay(RoundedRectangle(cornerRadius: 18).stroke(Color.rekolLine))
    }
}

struct CompactMemoryRow: View {
    var memory: Memory
    var action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 14) {
                Image(systemName: icon(for: memory))
                    .font(.title3.weight(.bold))
                    .foregroundStyle(Color.rekolBlue)
                    .frame(width: 52, height: 52)
                    .background(Color(hex: "E2F0FF"), in: RoundedRectangle(cornerRadius: 16))
                VStack(alignment: .leading, spacing: 3) {
                    Text(memory.title)
                        .font(.headline)
                        .foregroundStyle(Color.rekolNavy)
                        .lineLimit(1)
                    Text("\(memory.type) • \(memory.time)")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.headline.weight(.bold))
                    .foregroundStyle(.secondary)
            }
            .padding(14)
            .background(.white, in: RoundedRectangle(cornerRadius: 18))
        }
        .buttonStyle(.plain)
    }

    private func icon(for memory: Memory) -> String {
        switch memory.type.lowercased() {
        case "photos", "photo", "image": return "photo.fill"
        case "link", "article": return "doc.text.fill"
        case "audio", "voice": return "mic.fill"
        default: return "doc.text.fill"
        }
    }
}

struct TabBar: View {
    @EnvironmentObject private var state: RekolAppState

    var body: some View {
        ZStack(alignment: .top) {
            HStack {
                tab(.home)
                tab(.journal)
                Spacer(minLength: 70)
                tab(.library)
                tab(.insights)
            }
            .padding(.horizontal, 14)
            .padding(.top, 18)
            .padding(.bottom, 24)

            Button {
                state.showCapture = true
            } label: {
                Image(systemName: "sparkles")
                    .font(.title3.weight(.bold))
                    .foregroundStyle(.white)
                    .frame(width: 54, height: 54)
                    .background(LinearGradient(colors: [.rekolBlue, .rekolPurple], startPoint: .topLeading, endPoint: .bottomTrailing), in: Circle())
                    .overlay(Circle().stroke(.white, lineWidth: 6))
            }
            .offset(y: -24)
        }
        .frame(maxWidth: .infinity)
        .background {
            UnevenRoundedRectangle(topLeadingRadius: 28, topTrailingRadius: 28)
                .fill(.white)
                .ignoresSafeArea(edges: .bottom)
                .shadow(color: .black.opacity(0.08), radius: 12, y: -2)
        }
        .ignoresSafeArea(edges: .bottom)
    }

    private func tab(_ destination: Destination) -> some View {
        Button {
            state.selected = destination
        } label: {
            VStack(spacing: 4) {
                Image(systemName: destination.systemImage)
                    .font(.headline)
                Text(destination.rawValue)
                    .font(.caption2.weight(.semibold))
            }
            .foregroundStyle(state.selected == destination ? Color.rekolBlue : Color(hex: "66708B"))
            .frame(maxWidth: .infinity)
        }
    }
}

struct CaptureView: View {
    @EnvironmentObject private var state: RekolAppState
    @Environment(\.dismiss) private var dismiss
    @State private var content = ""
    @State private var reason = ""
    @State private var privateMemory = false
    @State private var selectedItem: PhotosPickerItem?
    @State private var showFileImporter = false
    @State private var attachmentURL: URL?
    @State private var capturedImage: UIImage?

    var body: some View {
        ZStack {
            CameraPreviewView(capturedImage: $capturedImage, attachmentURL: $attachmentURL, content: $content)
                .ignoresSafeArea()

            LinearGradient(colors: [Color.black.opacity(0.78), .clear], startPoint: .top, endPoint: .bottom)
                .frame(height: 185)
                .frame(maxHeight: .infinity, alignment: .top)
                .ignoresSafeArea()

            LinearGradient(colors: [.clear, Color.black.opacity(0.86)], startPoint: .top, endPoint: .bottom)
                .frame(height: 230)
                .frame(maxHeight: .infinity, alignment: .bottom)
                .ignoresSafeArea()

            VStack(spacing: 0) {
                HStack {
                    Spacer()
                    Button {
                        state.showCapture = false
                        dismiss()
                    } label: {
                        Image(systemName: "xmark")
                            .font(.headline.weight(.bold))
                            .foregroundStyle(.white)
                            .frame(width: 42, height: 42)
                            .background(Color.black.opacity(0.34), in: Circle())
                            .overlay(Circle().stroke(Color.white.opacity(0.45), lineWidth: 1))
                    }
                    .accessibilityLabel("Close capture")
                }
                .padding(.top, 8)
                .padding(.horizontal, 18)

                HStack(spacing: 12) {
                    Image(systemName: "sparkles")
                        .font(.headline)
                        .foregroundStyle(.white)
                    TextField("Add a description...", text: $content)
                        .foregroundStyle(.white)
                        .tint(.white)
                        .textInputAutocapitalization(.sentences)
                    if !content.isEmpty {
                        Button { content = "" } label: {
                            Image(systemName: "xmark.circle.fill")
                                .foregroundStyle(.white.opacity(0.85))
                        }
                    }
                }
                .padding(.horizontal, 16)
                .frame(height: 52)
                .background(Color.white.opacity(0.14), in: RoundedRectangle(cornerRadius: 24))
                .overlay(RoundedRectangle(cornerRadius: 24).stroke(Color.white.opacity(0.8)))
                .padding(.top, 8)
                .padding(.horizontal, 18)

                Spacer()

                if attachmentURL != nil || capturedImage != nil {
                    Button {
                        Task { await saveCapture() }
                    } label: {
                        Label("Analyze and save", systemImage: "sparkles")
                            .font(.headline)
                            .padding(.horizontal, 20)
                            .frame(height: 52)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(.rekolBlue)
                    .clipShape(Capsule())
                    .disabled(content.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && attachmentURL == nil)
                    .padding(.bottom, 22)
                }

                HStack(alignment: .center) {
                    PhotosPicker(selection: $selectedItem, matching: .any(of: [.images, .videos])) {
                        CameraCircleButton(symbol: "photo.on.rectangle", label: "Gallery")
                    }

                    Spacer()

                    Button {
                        NotificationCenter.default.post(name: .rekolCapturePhoto, object: nil)
                    } label: {
                        ZStack {
                            Circle()
                                .fill(.white)
                                .frame(width: 78, height: 78)
                            Circle()
                                .stroke(Color.white.opacity(0.8), lineWidth: 5)
                                .frame(width: 92, height: 92)
                            Circle()
                                .fill(LinearGradient(colors: [.rekolBlue, .rekolPurple], startPoint: .topLeading, endPoint: .bottomTrailing))
                                .frame(width: 64, height: 64)
                            Image(systemName: "camera.fill")
                                .font(.title2.weight(.bold))
                                .foregroundStyle(.white)
                        }
                    }

                    Spacer()

                    Button { showFileImporter = true } label: {
                        CameraCircleButton(symbol: "doc.fill", label: "File")
                    }
                }
                .padding(.horizontal, 30)
                .padding(.bottom, 30)
            }
        }
        .background(Color.black)
        .onChange(of: selectedItem) { item in
            Task { await importPhoto(item) }
        }
        .fileImporter(isPresented: $showFileImporter, allowedContentTypes: [.item], allowsMultipleSelection: false) { result in
            if let url = try? result.get().first {
                attachmentURL = url
                if content.isEmpty { content = "Attached file: \(url.lastPathComponent)" }
            }
        }
    }

    private func saveCapture() async {
        if let image = capturedImage, attachmentURL == nil {
            let target = FileManager.default.temporaryDirectory.appendingPathComponent("capture_\(Int(Date().timeIntervalSince1970)).jpg")
            if let data = image.jpegData(compressionQuality: 0.9) {
                try? data.write(to: target)
                attachmentURL = target
            }
        }
        let saveText: String
        if content.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty, let attachmentURL {
            saveText = "Photo captured: \(attachmentURL.lastPathComponent)"
        } else {
            saveText = content
        }
        state.showCapture = false
        dismiss()
        await state.saveMemory(content: saveText, reason: reason, privateMemory: privateMemory, attachmentURL: attachmentURL)
    }

    private func importPhoto(_ item: PhotosPickerItem?) async {
        guard let item, let data = try? await item.loadTransferable(type: Data.self) else { return }
        let target = FileManager.default.temporaryDirectory.appendingPathComponent("capture_\(Int(Date().timeIntervalSince1970)).jpg")
        try? data.write(to: target)
        attachmentURL = target
        if content.isEmpty { content = "Attached media: \(target.lastPathComponent)" }
    }
}

extension Notification.Name {
    static let rekolCapturePhoto = Notification.Name("rekolCapturePhoto")
}

struct CameraCircleButton: View {
    var symbol: String
    var label: String

    var body: some View {
        VStack(spacing: 6) {
            Image(systemName: symbol)
                .font(.title3.weight(.bold))
                .foregroundStyle(Color.rekolNavy)
                .frame(width: 56, height: 56)
                .background(Color.white.opacity(0.94), in: Circle())
            Text(label)
                .font(.caption2.weight(.semibold))
                .foregroundStyle(.white)
        }
    }
}

struct CameraPreviewView: UIViewRepresentable {
    @Binding var capturedImage: UIImage?
    @Binding var attachmentURL: URL?
    @Binding var content: String

    func makeCoordinator() -> Coordinator {
        Coordinator(capturedImage: $capturedImage, attachmentURL: $attachmentURL, content: $content)
    }

    func makeUIView(context: Context) -> CameraContainerView {
        let view = CameraContainerView()
        context.coordinator.configure(in: view)
        return view
    }

    func updateUIView(_ uiView: CameraContainerView, context: Context) {
        context.coordinator.previewLayer?.frame = uiView.bounds
    }

    final class Coordinator: NSObject, AVCapturePhotoCaptureDelegate {
        @Binding var capturedImage: UIImage?
        @Binding var attachmentURL: URL?
        @Binding var content: String
        private let session = AVCaptureSession()
        private let photoOutput = AVCapturePhotoOutput()
        var previewLayer: AVCaptureVideoPreviewLayer?
        private var observer: NSObjectProtocol?

        init(capturedImage: Binding<UIImage?>, attachmentURL: Binding<URL?>, content: Binding<String>) {
            _capturedImage = capturedImage
            _attachmentURL = attachmentURL
            _content = content
            super.init()
            observer = NotificationCenter.default.addObserver(forName: .rekolCapturePhoto, object: nil, queue: .main) { [weak self] _ in
                self?.capture()
            }
        }

        deinit {
            if let observer { NotificationCenter.default.removeObserver(observer) }
            session.stopRunning()
        }

        func configure(in view: CameraContainerView) {
            guard UIImagePickerController.isSourceTypeAvailable(.camera),
                  let device = AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: .back),
                  let input = try? AVCaptureDeviceInput(device: device) else {
                view.showUnavailableMessage()
                return
            }

            session.beginConfiguration()
            session.sessionPreset = .photo
            if session.canAddInput(input) { session.addInput(input) }
            if session.canAddOutput(photoOutput) { session.addOutput(photoOutput) }
            session.commitConfiguration()

            let layer = AVCaptureVideoPreviewLayer(session: session)
            layer.videoGravity = .resizeAspectFill
            layer.frame = view.bounds
            view.layer.insertSublayer(layer, at: 0)
            previewLayer = layer

            DispatchQueue.global(qos: .userInitiated).async { [session] in
                session.startRunning()
            }
        }

        func capture() {
            guard session.isRunning else {
                content = content.isEmpty ? "Photo captured" : content
                return
            }
            photoOutput.capturePhoto(with: AVCapturePhotoSettings(), delegate: self)
        }

        func photoOutput(_ output: AVCapturePhotoOutput, didFinishProcessingPhoto photo: AVCapturePhoto, error: Error?) {
            guard let data = photo.fileDataRepresentation(), let image = UIImage(data: data) else { return }
            capturedImage = image
            let target = FileManager.default.temporaryDirectory.appendingPathComponent("capture_\(Int(Date().timeIntervalSince1970)).jpg")
            try? data.write(to: target)
            attachmentURL = target
            if content.isEmpty { content = "Photo captured: \(target.lastPathComponent)" }
        }
    }
}

final class CameraContainerView: UIView {
    override func layoutSubviews() {
        super.layoutSubviews()
        layer.sublayers?.forEach { $0.frame = bounds }
    }

    func showUnavailableMessage() {
        backgroundColor = .black
        let label = UILabel()
        label.text = "Camera preview is available on a real iPhone"
        label.textColor = .white
        label.textAlignment = .center
        label.numberOfLines = 0
        label.font = .systemFont(ofSize: 17, weight: .semibold)
        label.translatesAutoresizingMaskIntoConstraints = false
        addSubview(label)
        NSLayoutConstraint.activate([
            label.centerXAnchor.constraint(equalTo: centerXAnchor),
            label.centerYAnchor.constraint(equalTo: centerYAnchor),
            label.leadingAnchor.constraint(greaterThanOrEqualTo: leadingAnchor, constant: 30),
            label.trailingAnchor.constraint(lessThanOrEqualTo: trailingAnchor, constant: -30)
        ])
    }
}

struct CaptureModeTile: View {
    var symbol: String
    var label: String
    var color: Color
    var selected: Bool

    var body: some View {
        VStack(spacing: 8) {
            Image(systemName: symbol)
                .font(.title3.weight(.bold))
            Text(label)
                .font(.caption.weight(.semibold))
                .lineLimit(1)
                .minimumScaleFactor(0.8)
        }
        .foregroundStyle(selected ? .white : color)
        .frame(maxWidth: .infinity, minHeight: 76)
        .background(selected ? color : color.opacity(0.11), in: RoundedRectangle(cornerRadius: 16))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(selected ? color : Color.rekolLine))
    }
}

struct CaptureOption: View {
    var symbol: String
    var label: String

    var body: some View {
        VStack(spacing: 8) {
            Image(systemName: symbol)
                .font(.title3)
            Text(label)
                .font(.caption.weight(.semibold))
        }
        .foregroundStyle(Color.rekolBlue)
        .frame(maxWidth: .infinity, minHeight: 72)
        .background(Color(hex: "F3F7FF"), in: RoundedRectangle(cornerRadius: 16))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.rekolLine))
    }
}

struct JournalView: View {
    @EnvironmentObject private var state: RekolAppState
    @State private var filter = "All"
    private let filters = ["All", "Notes", "Media", "Links", "Files", "Actions"]

    var filtered: [Memory] {
        state.memories.filter { filter == "All" || category($0) == filter }
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                PageHeader(title: "Today", subtitle: "Tue, 22 Sep 2026", symbol: "calendar")
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack {
                        ForEach(filters, id: \.self) { item in
                            Button(item) { filter = item }
                                .chip(selected: filter == item)
                        }
                    }
                }

                if filtered.isEmpty {
                    EmptyState(title: "No \(filter.lowercased()) captured yet", message: "Capture a note, link, photo, audio, or document and it will appear here automatically.")
                } else {
                    ForEach(filtered) { memory in
                        HStack(alignment: .top, spacing: 12) {
                            VStack {
                                Circle().fill(memory.accent).frame(width: 9, height: 9)
                                Rectangle().fill(Color.rekolLine).frame(width: 2, height: 120)
                            }
                            MemoryRow(memory: memory) { state.selectedMemory = memory }
                        }
                    }
                }
            }
            .padding(.horizontal, 18)
            .padding(.bottom, 120)
        }
    }
}

struct LibraryView: View {
    @EnvironmentObject private var state: RekolAppState
    @State private var search = ""

    var collections: [(String, Int)] {
        Dictionary(grouping: state.memories, by: { $0.type.isEmpty ? "Notes" : $0.type })
            .map { ($0.key, $0.value.count) }
            .sorted { $0.1 > $1.1 }
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                Text("Library")
                    .font(.largeTitle.bold())
                    .foregroundStyle(Color.rekolNavy)
                    .padding(.top, 20)
                Text("Everything you've captured, organized by Rekol.")
                    .foregroundStyle(.secondary)
                TextField("Search your memories...", text: $search)
                    .fieldStyle(symbol: "magnifyingglass")

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack {
                        ForEach(["All", "Notes", "Images", "Videos", "Audio", "Docs", "Links"], id: \.self) { item in
                            Text(item).chip(selected: item == "All")
                        }
                    }
                }

                if collections.isEmpty {
                    EmptyState(title: "Your library is empty", message: "Capture a note, link, photo, or file to create your first collection.")
                } else {
                    ForEach(collections, id: \.0) { title, count in
                        CollectionRow(title: title, subtitle: "\(count) item\(count == 1 ? "" : "s")")
                    }
                }

                Text("Saved memories")
                    .font(.title3.bold())
                    .foregroundStyle(Color.rekolNavy)
                    .padding(.top, 8)
                ForEach(state.memories) { memory in
                    MemoryRow(memory: memory) { state.selectedMemory = memory }
                }
            }
            .padding(.horizontal, 18)
            .padding(.bottom, 120)
        }
    }
}

struct InsightsView: View {
    @EnvironmentObject private var state: RekolAppState

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                PageHeader(title: "Insights")
                InsightCard(title: "Your Memories") {
                    ZStack {
                        Circle()
                            .fill(AngularGradient(colors: [.rekolBlue, .rekolCyan, .yellow, .rekolCoral, .rekolPurple, .rekolBlue], center: .center))
                            .frame(width: 160, height: 160)
                        Circle().fill(.white).frame(width: 98, height: 98)
                        VStack {
                            Text("\(state.memories.count)")
                                .font(.title.bold())
                            Text("items")
                                .font(.caption)
                        }
                    }
                }
                InsightCard(title: "Your Topics") {
                    FlowTags(tags: Array(Array(Set(state.memories.flatMap { [$0.type, $0.action ?? ""] }.filter { !$0.isEmpty })).prefix(8)))
                }
                InsightCard(title: "Actions Overview") {
                    HStack {
                        MetricCard(value: "\(state.memories.filter { $0.action != nil }.count)", label: "With actions", color: Color(hex: "11B98A"))
                        MetricCard(value: "\(state.memories.filter { $0.action == nil }.count)", label: "Without actions", color: Color(hex: "FF8A22"))
                        MetricCard(value: "\(state.memories.count)", label: "Total", color: .rekolPurple)
                    }
                }
                HStack {
                    bundledImage("rekol_mobile_icon")
                        .resizable()
                        .scaledToFit()
                        .frame(width: 46, height: 46)
                    Text("You capture ideas.\nWe help you make them happen.")
                        .font(.headline)
                }
                .padding(16)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Color(hex: "EDF8FF"), in: RoundedRectangle(cornerRadius: 18))
            }
            .padding(.horizontal, 18)
            .padding(.bottom, 120)
        }
    }
}

struct ProfileView: View {
    @EnvironmentObject private var state: RekolAppState
    @State private var confirmSignOut = false
    @State private var showPhotoPicker = false
    @State private var showEditProfile = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                TopBackRow(title: "Profile") { state.showProfile = false }
                HStack(spacing: 14) {
                    Button {
                        showPhotoPicker = true
                    } label: {
                        ZStack(alignment: .bottomTrailing) {
                            ProfileAvatar(name: state.profileName, imagePath: state.profilePhotoPath)
                            Image(systemName: "camera.fill")
                                .font(.caption.weight(.bold))
                                .foregroundStyle(.white)
                                .frame(width: 26, height: 26)
                                .background(Color.rekolBlue, in: Circle())
                                .overlay(Circle().stroke(.white, lineWidth: 2))
                        }
                    }
                    .buttonStyle(.plain)
                    VStack(alignment: .leading, spacing: 3) {
                        Text(state.profileName.isEmpty ? "Rekol user" : state.profileName).font(.headline)
                        Text(state.profileEmail.isEmpty ? "No email provided" : state.profileEmail).font(.caption).foregroundStyle(.secondary)
                        Text(state.profilePhone.isEmpty ? state.profileLocation : state.profilePhone).font(.caption).foregroundStyle(.secondary)
                        Text(state.profileLocation.isEmpty ? "No location provided" : state.profileLocation).font(.caption).foregroundStyle(.secondary)
                        Text("Verified").font(.caption.weight(.semibold)).foregroundStyle(Color(hex: "13B77A"))
                    }
                    Spacer()
                    VStack(spacing: 8) {
                        Button {
                            showPhotoPicker = true
                        } label: {
                            Text(state.profilePhotoPath.isEmpty ? "Add photo" : "Change")
                        }
                        .buttonStyle(.bordered)
                        Button {
                            showEditProfile = true
                        } label: {
                            Text("Edit")
                        }
                        .buttonStyle(.borderedProminent)
                        .tint(.rekolBlue)
                    }
                }
                .card()

                ProBanner()
                HStack {
                    ProfileStat(value: "\(state.memories.count)", label: "Memories", symbol: "doc.text.fill", color: .rekolPurple)
                    ProfileStat(value: "\(state.memories.filter { $0.action != nil }.count)", label: "Actions", symbol: "paperplane.fill", color: Color(hex: "FFA117"))
                    ProfileStat(value: "14", label: "Day streak", symbol: "flame.fill", color: .rekolCoral)
                }
                .card()

                ProfileSection(title: "Preferences", rows: [
                    ("Notifications", "Smart alerts on", "bell.fill"),
                    ("Connections", "3 apps connected", "link"),
                    ("Appearance", "System default", "circle.lefthalf.filled"),
                    ("Language", "English", "globe")
                ]) { index in
                    if index == 0 { state.showProfile = false; state.showNotifications = true }
                    if index == 1 { state.showProfile = false; state.showConnections = true }
                }
                ProfileSection(title: "Privacy & security", rows: [
                    ("Account security", "Password and biometrics", "lock.shield.fill"),
                    ("Privacy controls", "Manage AI and data settings", "hand.raised.fill"),
                    ("Data & storage", "Export or delete your data", "internaldrive.fill"),
                    ("Active sessions", "2 devices", "iphone")
                ])
                ProfileSection(title: "Support", rows: [
                    ("Help center", "", "questionmark.circle.fill"),
                    ("Contact support", "", "headphones"),
                    ("About Rekol", "Version 1.0.0", "info.circle.fill")
                ])
                Button {
                    confirmSignOut = true
                } label: {
                    Label("Sign out", systemImage: "rectangle.portrait.and.arrow.right")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(OutlineButtonStyle())
                .foregroundStyle(Color.rekolCoral)
                Text("Capture Today. Remember Tomorrow.\nCrafted and designed by PrabhaTech")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
                    .frame(maxWidth: .infinity)
            }
            .padding(20)
        }
        .background(LinearGradient(colors: [Color(hex: "F8FCFF"), Color(hex: "EAF6FF")], startPoint: .top, endPoint: .bottom))
        .alert("Sign out?", isPresented: $confirmSignOut) {
            Button("Cancel", role: .cancel) { }
            Button("Sign out", role: .destructive) { state.signOut() }
        } message: {
            Text("Do you want to sign out of your Rekol account?")
        }
        .sheet(isPresented: $showPhotoPicker) {
            ProfilePhotoPicker { image in
                saveProfilePhoto(image)
            }
        }
        .sheet(isPresented: $showEditProfile) {
            EditProfileView()
                .environmentObject(state)
        }
    }

    private func saveProfilePhoto(_ image: UIImage) {
        guard let jpeg = image.jpegData(compressionQuality: 0.86) else { return }
        let directory = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first
        guard let directory else { return }
        let target = directory.appendingPathComponent("rekol-profile-photo.jpg")
        do {
            try jpeg.write(to: target, options: .atomic)
            state.profilePhotoPath = target.path
        } catch {
            state.alertMessage = error.localizedDescription
        }
    }
}

struct ProfileAvatar: View {
    var name: String
    var imagePath: String

    var body: some View {
        Group {
            if !imagePath.isEmpty, let image = UIImage(contentsOfFile: imagePath) {
                Image(uiImage: image)
                    .resizable()
                    .scaledToFill()
            } else {
                Text(String(name.trimmingCharacters(in: .whitespacesAndNewlines).first ?? "R").uppercased())
                    .font(.title.bold())
                    .foregroundStyle(.white)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .background(LinearGradient(colors: [.rekolBlue, .rekolPurple], startPoint: .topLeading, endPoint: .bottomTrailing))
            }
        }
        .frame(width: 76, height: 76)
        .clipShape(Circle())
    }
}

struct ProfilePhotoPicker: UIViewControllerRepresentable {
    var onSelect: (UIImage) -> Void
    @Environment(\.dismiss) private var dismiss

    func makeUIViewController(context: Context) -> PHPickerViewController {
        var configuration = PHPickerConfiguration(photoLibrary: .shared())
        configuration.filter = .images
        configuration.selectionLimit = 1
        let picker = PHPickerViewController(configuration: configuration)
        picker.delegate = context.coordinator
        return picker
    }

    func updateUIViewController(_ uiViewController: PHPickerViewController, context: Context) {}

    func makeCoordinator() -> Coordinator {
        Coordinator(onSelect: onSelect, dismiss: dismiss)
    }

    final class Coordinator: NSObject, PHPickerViewControllerDelegate {
        private let onSelect: (UIImage) -> Void
        private let dismiss: DismissAction

        init(onSelect: @escaping (UIImage) -> Void, dismiss: DismissAction) {
            self.onSelect = onSelect
            self.dismiss = dismiss
        }

        func picker(_ picker: PHPickerViewController, didFinishPicking results: [PHPickerResult]) {
            dismiss()
            guard let provider = results.first?.itemProvider, provider.canLoadObject(ofClass: UIImage.self) else { return }
            provider.loadObject(ofClass: UIImage.self) { object, _ in
                guard let image = object as? UIImage else { return }
                DispatchQueue.main.async {
                    self.onSelect(image)
                }
            }
        }
    }
}

struct EditProfileView: View {
    @EnvironmentObject private var state: RekolAppState
    @Environment(\.dismiss) private var dismiss
    @State private var name = ""
    @State private var email = ""
    @State private var phone = ""
    @State private var location = ""

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 16) {
                    LabeledField(title: "Full name", placeholder: "Full name", text: $name, symbol: "person")
                    LabeledField(title: "Email address", placeholder: "you@example.com", text: $email, symbol: "envelope")
                        .keyboardType(.emailAddress)
                        .textInputAutocapitalization(.never)
                    LabeledField(title: "Phone number", placeholder: "+971 50 123 4567", text: $phone, symbol: "phone")
                        .keyboardType(.phonePad)
                    LabeledField(title: "Location", placeholder: "City, country", text: $location, symbol: "mappin.and.ellipse")
                    Text("These details update your Rekol profile on this device.")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
                .padding(20)
            }
            .background(Color.rekolCanvas)
            .navigationTitle("Edit profile")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Save") {
                        save()
                    }
                    .fontWeight(.semibold)
                }
            }
            .onAppear {
                name = state.profileName
                email = state.profileEmail
                phone = state.profilePhone
                location = state.profileLocation
            }
        }
    }

    private func save() {
        let trimmedName = name.trimmingCharacters(in: .whitespacesAndNewlines)
        let trimmedEmail = email.trimmingCharacters(in: .whitespacesAndNewlines)
        let trimmedPhone = phone.trimmingCharacters(in: .whitespacesAndNewlines)
        let trimmedLocation = location.trimmingCharacters(in: .whitespacesAndNewlines)
        state.profileName = trimmedName.isEmpty ? "Rekol user" : trimmedName
        state.profileEmail = trimmedEmail
        state.profilePhone = trimmedPhone
        state.profileLocation = trimmedLocation.isEmpty ? "Dubai, UAE" : trimmedLocation
        dismiss()
    }
}

struct SettingsView: View {
    @EnvironmentObject private var state: RekolAppState
    @AppStorage("ai_analysis") private var aiAnalysis = true
    @AppStorage("personalized_suggestions") private var suggestions = true
    @AppStorage("automatic_tagging") private var autoTagging = true
    @AppStorage("offline_capture") private var offlineCapture = true
    @AppStorage("smart_notifications") private var smartNotifications = true
    @AppStorage("action_reminders") private var actionReminders = true
    @AppStorage("high_contrast") private var highContrast = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                TopBackRow(title: "Settings", subtitle: "Make Rekol work the way you do.") { state.showSettings = false }
                SettingsToggleSection(title: "Rekol AI", rows: [
                    ("AI analysis", $aiAnalysis, "sparkles"),
                    ("Personalized suggestions", $suggestions, "brain.head.profile")
                ])
                ProfileSection(title: "Rekol AI", rows: [
                    ("Analysis language", "Auto-detect", "translate"),
                    ("AI privacy controls", "Choose what Rekol can analyze", "lock.shield.fill")
                ])
                SettingsToggleSection(title: "Capture & organization", rows: [
                    ("Automatic tagging", $autoTagging, "tag.fill"),
                    ("Offline capture", $offlineCapture, "icloud.and.arrow.down.fill")
                ])
                SettingsToggleSection(title: "Notifications", rows: [
                    ("Smart notifications", $smartNotifications, "bell.fill"),
                    ("Action reminders", $actionReminders, "calendar.badge.clock")
                ])
                SettingsToggleSection(title: "Appearance & accessibility", rows: [
                    ("High contrast", $highContrast, "circle.lefthalf.filled")
                ])
                ProfileSection(title: "Data & security", rows: [
                    ("Connections", "3 apps connected", "link"),
                    ("Security", "Password and biometrics", "lock.fill"),
                    ("Storage", "2.4 GB of 10 GB", "internaldrive.fill"),
                    ("Export your data", "", "square.and.arrow.down.fill")
                ]) { index in
                    if index == 0 { state.showSettings = false; state.showConnections = true }
                }
            }
            .padding(20)
        }
        .background(LinearGradient(colors: [Color(hex: "F8FCFF"), Color(hex: "EAF6FF")], startPoint: .top, endPoint: .bottom))
    }
}

struct NotificationsView: View {
    @EnvironmentObject private var state: RekolAppState
    @State private var tab = "All 7"

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                TopBackRow(title: "Notifications", subtitle: "Stay updated without the noise.") { state.showNotifications = false }
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack {
                        ForEach(["All 7", "Actions", "Memories", "AI updates", "System"], id: \.self) { item in
                            Button(item) { tab = item }.chip(selected: tab == item)
                        }
                    }
                }
                NotificationCard(symbol: "sparkles", title: "Memory analysis ready", message: "Rekol found useful next steps in your latest capture.", time: "Now", action: "Review")
                NotificationCard(symbol: "calendar.badge.clock", title: "Follow-up reminder", message: "Client meeting notes may need a follow-up today.", time: "12m", action: "Open reminders")
                ForEach(state.memories.prefix(4)) { memory in
                    NotificationCard(symbol: "doc.text.fill", title: memory.title, message: memory.summary, time: memory.time, action: memory.action)
                }
            }
            .padding(20)
        }
        .background(LinearGradient(colors: [Color(hex: "F8FCFF"), Color(hex: "EAF6FF")], startPoint: .top, endPoint: .bottom))
    }
}

struct ConnectionsView: View {
    @EnvironmentObject private var state: RekolAppState
    @State private var connected = [true, true, false]

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                TopBackRow(title: "Connections", subtitle: "Bring more context into Rekol.") { state.showConnections = false }
                ForEach(Array(["Google Drive", "Calendar", "Slack"].enumerated()), id: \.offset) { index, name in
                    HStack {
                        Image(systemName: ["folder.fill", "calendar", "message.fill"][index])
                            .foregroundStyle(Color.rekolBlue)
                            .frame(width: 34, height: 34)
                            .background(Color(hex: "EAF3FF"), in: Circle())
                        VStack(alignment: .leading) {
                            Text(name).font(.headline)
                            Text(index == 0 ? "Import documents and media" : index == 1 ? "Create follow-up reminders" : "Capture decisions from messages")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                        Spacer()
                        Toggle("", isOn: $connected[index])
                            .labelsHidden()
                    }
                    .card()
                }
                ProfileSection(title: "Available", rows: [
                    ("Gmail", "Save important email context", "envelope.fill"),
                    ("Microsoft 365", "Connect files and calendar", "square.grid.2x2.fill"),
                    ("Notion", "Sync project notes", "doc.richtext.fill")
                ])
            }
            .padding(20)
        }
        .background(LinearGradient(colors: [Color(hex: "F8FCFF"), Color(hex: "EAF6FF")], startPoint: .top, endPoint: .bottom))
    }
}

struct RemindersView: View {
    @EnvironmentObject private var state: RekolAppState
    @State private var title = ""
    @State private var notes = ""
    @State private var at = Date().addingTimeInterval(3600)

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                TopBackRow(title: "Reminders", subtitle: "Turn memory into action.") { state.showReminders = false }
                VStack(alignment: .leading, spacing: 10) {
                    TextField("Reminder title", text: $title).fieldStyle()
                    TextField("Notes", text: $notes).fieldStyle()
                    DatePicker("When", selection: $at, displayedComponents: [.date, .hourAndMinute])
                    Button("Create reminder") {
                        state.addReminder(title: title.isEmpty ? "Rekol reminder" : title, notes: notes, at: at)
                        title = ""
                        notes = ""
                    }
                    .buttonStyle(PrimaryButtonStyle())
                }
                .card()

                ForEach(state.reminders) { reminder in
                    VStack(alignment: .leading, spacing: 6) {
                        Text(reminder.title).font(.headline)
                        Text(reminder.notes.isEmpty ? "No notes" : reminder.notes).font(.subheadline).foregroundStyle(.secondary)
                        Text(reminder.at.formatted(date: .abbreviated, time: .shortened)).font(.caption).foregroundStyle(Color.rekolBlue)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .card()
                }
            }
            .padding(20)
        }
        .background(LinearGradient(colors: [Color(hex: "F8FCFF"), Color(hex: "EAF6FF")], startPoint: .top, endPoint: .bottom))
    }
}

struct MemoryDetailView: View {
    @EnvironmentObject private var state: RekolAppState
    var memory: Memory

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                TopBackRow(title: "Memory") { state.selectedMemory = nil }
                VStack(alignment: .leading, spacing: 10) {
                    HStack {
                        Circle().fill(memory.accent).frame(width: 12, height: 12)
                        Text(memory.type).font(.caption.weight(.semibold)).foregroundStyle(.secondary)
                        Spacer()
                        Text(memory.time).font(.caption).foregroundStyle(.secondary)
                    }
                    Text(memory.title)
                        .font(.title2.bold())
                        .foregroundStyle(Color.rekolNavy)
                    Text(memory.summary)
                        .font(.body)
                        .foregroundStyle(Color.rekolNavy)
                    if let action = memory.action, !action.isEmpty {
                        Text("Action: \(action)")
                            .font(.subheadline.weight(.semibold))
                            .foregroundStyle(Color.rekolBlue)
                            .padding(10)
                            .background(Color(hex: "E9F4FF"), in: RoundedRectangle(cornerRadius: 10))
                    }
                    if let path = memory.attachmentPath {
                        Text("Attachment: \(URL(fileURLWithPath: path).lastPathComponent)")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                }
                .card()
            }
            .padding(20)
        }
        .background(Color.rekolCanvas)
    }
}

struct AnalysisOverlay: View {
    var body: some View {
        ZStack {
            Color.white.opacity(0.95).ignoresSafeArea()
            VStack(spacing: 18) {
                bundledImage("rekol_mobile_icon")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 100, height: 100)
                Text("Analyzing...")
                    .font(.title.bold())
                ProgressView()
                    .scaleEffect(1.2)
                VStack(alignment: .leading, spacing: 10) {
                    ForEach(["Securing original content", "Extracting or transcribing", "Understanding context", "Creating summary", "Detecting possible actions", "Adding to your library"], id: \.self) { item in
                        Label(item, systemImage: "checkmark.circle.fill")
                            .foregroundStyle(Color(hex: "20C77A"))
                    }
                }
                .padding()
                .background(.white, in: RoundedRectangle(cornerRadius: 18))
            }
            .padding()
        }
    }
}

struct MemoryRow: View {
    var memory: Memory
    var action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(alignment: .leading, spacing: 8) {
                HStack {
                    Text(memory.time)
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                    Spacer()
                    Text(category(memory))
                        .font(.caption2.weight(.semibold))
                        .padding(.horizontal, 9)
                        .padding(.vertical, 5)
                        .background(Color(hex: "EEF4FF"), in: Capsule())
                }
                Text(memory.title)
                    .font(.headline)
                    .foregroundStyle(Color.rekolNavy)
                    .lineLimit(1)
                Text(memory.summary)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .lineLimit(3)
                if let action = memory.action, !action.isEmpty {
                    Text("Action: \(action)")
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(Color.rekolBlue)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .card()
        }
        .buttonStyle(.plain)
    }
}

struct MetricCard: View {
    var value: String
    var label: String
    var color: Color

    var body: some View {
        VStack(spacing: 4) {
            Text(value)
                .font(.title2.bold())
                .foregroundStyle(color)
            Text(label)
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity)
        .padding(13)
        .background(color.opacity(0.1), in: RoundedRectangle(cornerRadius: 13))
    }
}

struct PageHeader: View {
    var title: String
    var subtitle: String?
    var symbol: String?

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 3) {
                Text(title)
                    .font(.largeTitle.bold())
                    .foregroundStyle(Color.rekolNavy)
                if let subtitle {
                    Text(subtitle).font(.subheadline).foregroundStyle(.secondary)
                }
            }
            Spacer()
            if let symbol {
                Image(systemName: symbol).iconButton()
            }
        }
        .padding(.top, 18)
    }
}

struct TopBackRow: View {
    var title: String
    var subtitle: String?
    var onBack: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Button(action: onBack) { Image(systemName: "chevron.left").iconButton() }
                Spacer()
            }
            Text(title)
                .font(.largeTitle.bold())
                .foregroundStyle(Color.rekolNavy)
            if let subtitle {
                Text(subtitle).foregroundStyle(.secondary)
            }
        }
    }
}

struct ProBanner: View {
    var body: some View {
        HStack {
            Image(systemName: "star.fill")
                .foregroundStyle(Color.rekolPurple)
                .frame(width: 42, height: 42)
                .background(Color(hex: "ECE2FF"), in: RoundedRectangle(cornerRadius: 12))
            VStack(alignment: .leading) {
                Text("Rekol Pro").font(.headline)
                Text("Your memories, fully unlocked").font(.caption).foregroundStyle(.secondary)
                Text("Renews 23 Oct 2026").font(.caption2).foregroundStyle(.secondary)
            }
            Spacer()
            Text("Active")
                .font(.caption.weight(.semibold))
                .foregroundStyle(Color(hex: "13B77A"))
        }
        .padding(14)
        .background(Color(hex: "F7F1FF"), in: RoundedRectangle(cornerRadius: 16))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color(hex: "E4D9FF")))
    }
}

struct ProfileStat: View {
    var value: String
    var label: String
    var symbol: String
    var color: Color

    var body: some View {
        VStack(spacing: 4) {
            Image(systemName: symbol)
                .foregroundStyle(color)
                .frame(width: 38, height: 38)
                .background(color.opacity(0.12), in: RoundedRectangle(cornerRadius: 12))
            Text(value).font(.headline)
            Text(label).font(.caption2).foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity)
    }
}

struct ProfileSection: View {
    var title: String
    var rows: [(String, String, String)]
    var onTap: ((Int) -> Void)?

    init(title: String, rows: [(String, String, String)], onTap: ((Int) -> Void)? = nil) {
        self.title = title
        self.rows = rows
        self.onTap = onTap
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title).font(.headline).foregroundStyle(Color.rekolNavy)
            VStack(spacing: 0) {
                ForEach(rows.indices, id: \.self) { index in
                    Button {
                        onTap?(index)
                    } label: {
                        HStack {
                            Image(systemName: rows[index].2)
                                .foregroundStyle(Color.rekolBlue)
                                .frame(width: 31, height: 31)
                                .background(Color(hex: "EAF3FF"), in: Circle())
                            Text(rows[index].0)
                                .foregroundStyle(Color.rekolNavy)
                                .font(.subheadline)
                            Spacer()
                            if !rows[index].1.isEmpty {
                                Text(rows[index].1)
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                            Image(systemName: "chevron.right")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                        .padding(.vertical, 10)
                    }
                    .buttonStyle(.plain)
                    if index < rows.indices.last! { Divider() }
                }
            }
            .padding(.horizontal, 12)
            .background(.white, in: RoundedRectangle(cornerRadius: 15))
            .overlay(RoundedRectangle(cornerRadius: 15).stroke(Color.rekolLine))
        }
    }
}

struct SettingsToggleSection: View {
    var title: String
    var rows: [(String, Binding<Bool>, String)]

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title).font(.headline).foregroundStyle(Color.rekolNavy)
            VStack(spacing: 0) {
                ForEach(rows.indices, id: \.self) { index in
                    HStack {
                        Image(systemName: rows[index].2)
                            .foregroundStyle(Color.rekolBlue)
                            .frame(width: 34, height: 34)
                            .background(Color(hex: "EAF3FF"), in: Circle())
                        Text(rows[index].0)
                            .font(.subheadline)
                        Spacer()
                        Toggle("", isOn: rows[index].1).labelsHidden()
                    }
                    .padding(.vertical, 8)
                    if index < rows.indices.last! { Divider() }
                }
            }
            .padding(.horizontal, 12)
            .background(.white, in: RoundedRectangle(cornerRadius: 15))
            .overlay(RoundedRectangle(cornerRadius: 15).stroke(Color.rekolLine))
        }
    }
}

struct NotificationCard: View {
    var symbol: String
    var title: String
    var message: String
    var time: String
    var action: String?

    var content: some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: symbol)
                .foregroundStyle(Color.rekolBlue)
                .frame(width: 38, height: 38)
                .background(Color(hex: "EAF3FF"), in: Circle())
            VStack(alignment: .leading, spacing: 5) {
                HStack {
                    Text(title).font(.headline).foregroundStyle(Color.rekolNavy)
                    Spacer()
                    Text(time).font(.caption2).foregroundStyle(.secondary)
                }
                Text(message).font(.subheadline).foregroundStyle(.secondary).lineLimit(2)
                if let action, !action.isEmpty {
                    Text(action).font(.caption.weight(.semibold)).foregroundStyle(Color.rekolBlue)
                }
            }
        }
    }

    var body: some View { content.card() }
}

struct CollectionRow: View {
    var title: String
    var subtitle: String

    var body: some View {
        HStack {
            Image(systemName: "rectangle.stack.fill")
                .font(.title2)
                .foregroundStyle(.white)
                .frame(width: 62, height: 62)
                .background(LinearGradient(colors: [.rekolBlue.opacity(0.75), .rekolCyan.opacity(0.55)], startPoint: .topLeading, endPoint: .bottomTrailing), in: RoundedRectangle(cornerRadius: 12))
            VStack(alignment: .leading) {
                Text(title).font(.headline)
                Text(subtitle).font(.caption).foregroundStyle(.secondary)
            }
            Spacer()
            Image(systemName: "chevron.right").foregroundStyle(.secondary)
        }
        .card()
    }
}

struct InsightCard<Content: View>: View {
    var title: String
    @ViewBuilder var content: Content

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text(title).font(.title3.bold())
            content.frame(maxWidth: .infinity)
        }
        .card()
    }
}

struct FlowTags: View {
    var tags: [String]

    var body: some View {
        if tags.isEmpty {
            Text("Topics will appear as you capture memories.")
                .foregroundStyle(.secondary)
        } else {
            LazyVGrid(columns: [GridItem(.adaptive(minimum: 90), spacing: 8)], spacing: 8) {
                ForEach(tags, id: \.self) { tag in
                    Text(tag).chip(selected: false)
                }
            }
        }
    }
}

struct EmptyState: View {
    var title: String
    var message: String

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title).font(.headline).foregroundStyle(Color.rekolNavy)
            Text(message).foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .card()
    }
}

func category(_ memory: Memory) -> String {
    let content = memory.summary.lowercased()
    let ext = (memory.attachmentPath ?? "").split(separator: ".").last.map(String.init)?.lowercased() ?? ""
    if ["jpg", "jpeg", "png", "gif", "webp", "heic", "mp4", "mov", "mp3", "wav", "m4a"].contains(ext) || content.contains("photo captured") || content.contains("attached media") { return "Media" }
    if ["pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "csv"].contains(ext) || content.contains("attached file") { return "Files" }
    if content.contains("http://") || content.contains("https://") || content.contains("www.") { return "Links" }
    if memory.action?.isEmpty == false { return "Actions" }
    return "Notes"
}

struct PrimaryButtonStyle: ButtonStyle {
    var height: CGFloat = 56

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.headline)
            .foregroundStyle(.white)
            .frame(height: height)
            .padding(.horizontal, 16)
            .background(LinearGradient(colors: [.rekolCyan, .rekolBlue, .rekolPurple], startPoint: .leading, endPoint: .trailing), in: Capsule())
            .opacity(configuration.isPressed ? 0.8 : 1)
    }
}

struct SolidBlueButtonStyle: ButtonStyle {
    var height: CGFloat = 58

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.headline)
            .foregroundStyle(.white)
            .frame(height: height)
            .padding(.horizontal, 18)
            .background(Color.rekolBlue, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
            .shadow(color: Color.rekolBlue.opacity(configuration.isPressed ? 0.12 : 0.25), radius: configuration.isPressed ? 5 : 12, y: 6)
            .opacity(configuration.isPressed ? 0.82 : 1)
    }
}

struct OutlineButtonStyle: ButtonStyle {
    var height: CGFloat = 56

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.headline)
            .foregroundStyle(Color.rekolBlue)
            .frame(height: height)
            .padding(.horizontal, 16)
            .background(.white, in: Capsule())
            .overlay(Capsule().stroke(Color.rekolLine))
            .opacity(configuration.isPressed ? 0.8 : 1)
    }
}

extension View {
    func card() -> some View {
        self
            .padding(14)
            .background(.white, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
            .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.rekolLine))
    }

    func iconButton() -> some View {
        self
            .foregroundStyle(Color.rekolNavy)
            .frame(width: 42, height: 42)
            .background(.white, in: Circle())
            .overlay(Circle().stroke(Color.rekolLine))
    }

    func chip(selected: Bool) -> some View {
        self
            .font(.caption.weight(.semibold))
            .foregroundStyle(selected ? .white : Color.rekolNavy)
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(selected ? Color.rekolBlue : Color.white, in: Capsule())
            .overlay(Capsule().stroke(selected ? Color.rekolBlue : Color.rekolLine))
    }

    func fieldStyle(symbol: String? = nil) -> some View {
        HStack {
            if let symbol {
                Image(systemName: symbol).foregroundStyle(.secondary)
            }
            self
        }
        .padding(14)
        .background(.white, in: RoundedRectangle(cornerRadius: 14))
        .overlay(RoundedRectangle(cornerRadius: 14).stroke(Color.rekolLine))
    }
}
