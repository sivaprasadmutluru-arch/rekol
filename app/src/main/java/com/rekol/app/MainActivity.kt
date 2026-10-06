package com.rekol.app

import android.content.Intent
import android.app.Activity
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.Manifest
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import androidx.core.content.FileProvider
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.analytics.FirebaseAnalytics
import java.util.concurrent.TimeUnit
import java.util.Calendar
import org.json.JSONArray
import org.json.JSONObject

private val Navy = Color(0xFF071B4A)
private val Blue = Color(0xFF087CFF)
private val Purple = Color(0xFF7836FF)
private val Cyan = Color(0xFF16D8D0)
private val Coral = Color(0xFFFF5A7A)
private val RekolFont = FontFamily(Font(R.font.rekol_regular, FontWeight.Normal), Font(R.font.rekol_semibold, FontWeight.SemiBold), Font(R.font.rekol_bold, FontWeight.Bold))
private const val PLAY_TEST_PHONE = "9618810386"
private const val PLAY_TEST_OTP = "123456"

private fun isPlayStoreTestPhone(countryCode: String, phone: String): Boolean {
    return phone.filter(Char::isDigit) == PLAY_TEST_PHONE
}

private fun countryFlag(code: String): String = mapOf(
    "+1" to "🇺🇸", "+7" to "🇷🇺", "+20" to "🇪🇬", "+27" to "🇿🇦", "+30" to "🇬🇷", "+31" to "🇳🇱", "+32" to "🇧🇪", "+33" to "🇫🇷", "+34" to "🇪🇸", "+39" to "🇮🇹", "+44" to "🇬🇧", "+49" to "🇩🇪", "+55" to "🇧🇷", "+60" to "🇲🇾", "+61" to "🇦🇺", "+62" to "🇮🇩", "+63" to "🇵🇭", "+64" to "🇳🇿", "+65" to "🇸🇬", "+66" to "🇹🇭", "+81" to "🇯🇵", "+82" to "🇰🇷", "+84" to "🇻🇳", "+86" to "🇨🇳", "+90" to "🇹🇷", "+91" to "🇮🇳", "+92" to "🇵🇰", "+93" to "🇦🇫", "+94" to "🇱🇰", "+95" to "🇲🇲", "+98" to "🇮🇷", "+971" to "🇦🇪", "+966" to "🇸🇦", "+974" to "🇶🇦", "+965" to "🇰🇼", "+973" to "🇧🇭", "+968" to "🇴🇲"
)[code] ?: "🌐"
private val Canvas = Color(0xFFF8FAFF)
private val Line = Color(0xFFE8EDF7)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.WHITE
        if (android.os.Build.VERSION.SDK_INT >= 28) {
            window.setNavigationBarDividerColor(android.graphics.Color.WHITE)
        }
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            window.isNavigationBarContrastEnforced = false
        }
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        val sharedText = if (intent?.action == Intent.ACTION_SEND) intent.getStringExtra(Intent.EXTRA_TEXT) else null
        setContent { RekolTheme { RekolApp(sharedText) } }
    }
}

@Composable
private fun RekolTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(primary = Blue, secondary = Purple, background = Canvas, surface = Color.White, onSurface = Navy),
        typography = Typography(bodyLarge = androidx.compose.ui.text.TextStyle(color = Navy, fontFamily = RekolFont), titleLarge = androidx.compose.ui.text.TextStyle(fontFamily = RekolFont), headlineLarge = androidx.compose.ui.text.TextStyle(fontFamily = RekolFont), labelLarge = androidx.compose.ui.text.TextStyle(fontFamily = RekolFont)),
        content = content
    )
}

private enum class Destination(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Default.Home), Journal("Journal", Icons.Default.Description),
    Library("Library", Icons.Default.Folder), Insights("Insights", Icons.Default.BarChart)
}

data class Memory(val title: String, val type: String, val time: String, val summary: String, val action: String? = null, val accent: Color = Blue, val id: String? = null)
data class Reminder(val id: Long, val title: String, val notes: String, val at: Long)

private fun loadReminders(prefs: android.content.SharedPreferences): List<Reminder> = runCatching {
    val json = JSONArray(prefs.getString("reminders", "[]"))
    List(json.length()) { index -> json.getJSONObject(index).let { Reminder(it.getLong("id"), it.getString("title"), it.optString("notes"), it.getLong("at")) } }
}.getOrDefault(emptyList()).sortedBy { it.at }

private fun saveReminders(prefs: android.content.SharedPreferences, reminders: List<Reminder>) {
    val json = JSONArray(); reminders.forEach { json.put(JSONObject().apply { put("id", it.id); put("title", it.title); put("notes", it.notes); put("at", it.at) }) }
    prefs.edit().putString("reminders", json.toString()).apply()
}

private fun scheduleReminder(context: android.content.Context, reminder: Reminder) {
    val intent = Intent(context, ReminderReceiver::class.java).apply { putExtra("title", reminder.title); putExtra("notes", reminder.notes) }
    val pending = android.app.PendingIntent.getBroadcast(context, reminder.id.toInt(), intent, android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE)
    val alarm = context.getSystemService(AlarmManager::class.java)
    alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.at, pending)
}

private val sampleMemories = listOf(
    Memory("UAE Sustainability Report", "Article", "10:24 AM", "Overview of UAE's net zero strategy and opportunities for businesses.", "Read & share with team", Blue),
    Memory("Project Site Photos", "Photos", "9:41 AM", "Site progress looks good. Check pending items before next meeting.", "Review & schedule meeting", Color(0xFF13B77A)),
    Memory("Client Meeting Notes", "Note", "8:30 AM", "Discussed ERP rollout timeline and next steps.", "Set reminder for follow-up", Color(0xFFFFA117))
)

private suspend fun extractLocalContent(context: android.content.Context, file: File, sourceUri: Uri): String = withContext(Dispatchers.IO) {
    val mime = context.contentResolver.getType(sourceUri).orEmpty()
    val ext = file.extension.lowercase()
    if (mime.startsWith("text/") || ext in setOf("txt", "md", "csv", "json", "xml")) {
        return@withContext file.readText().take(12000)
    }
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    fun ocr(bitmap: Bitmap): String = runCatching { com.google.android.gms.tasks.Tasks.await(recognizer.process(InputImage.fromBitmap(bitmap, 0))).text }.getOrDefault("")
    if (mime.startsWith("image/") || ext in setOf("png", "jpg", "jpeg", "webp")) {
        val result = BitmapFactory.decodeFile(file.absolutePath)?.let { ocr(it).take(12000) }.orEmpty()
        recognizer.close()
        return@withContext result
    }
    if (mime == "application/pdf" || ext == "pdf") {
        val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(descriptor)
        val text = buildString {
            val pages = minOf(renderer.pageCount, 3)
            repeat(pages) { index ->
                renderer.openPage(index).use { page ->
                    val bitmap = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(android.graphics.Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    append(ocr(bitmap)).append('\n')
                    bitmap.recycle()
                }
            }
        }
        renderer.close(); descriptor.close(); recognizer.close()
        return@withContext text.trim().take(12000)
    }
    recognizer.close()
    ""
}

@Composable
private fun RekolApp(sharedText: String?) {
    val context = LocalContext.current
    val analytics = remember { FirebaseAnalytics.getInstance(context) }
    val networkOnline = rememberNetworkStatus()
    val prefs = remember { context.getSharedPreferences("rekol_profile", android.content.Context.MODE_PRIVATE) }
    var accessToken by remember { mutableStateOf(prefs.getString("access_token", null)) }
    var showSplash by remember { mutableStateOf(false) }
    var showWelcome by remember { mutableStateOf(sharedText == null && accessToken.isNullOrBlank()) }
    var showReferenceLogin by remember { mutableStateOf(false) }
    var showResetReference by remember { mutableStateOf(false) }
    var showVerifyReference by remember { mutableStateOf(false) }
    var showCreateReference by remember { mutableStateOf(false) }
    var showAuth by remember { mutableStateOf(sharedText == null && accessToken.isNullOrBlank()) }
    var authStartPage by remember { mutableIntStateOf(0) }
    var showOnboarding by remember { mutableStateOf(sharedText == null && accessToken.isNullOrBlank()) }
    var profileName by remember { mutableStateOf(prefs.getString("profile_name", "Rekol user") ?: "Rekol user") }
    var profileEmail by remember { mutableStateOf(prefs.getString("profile_email", "") ?: "") }
    var profilePhone by remember { mutableStateOf(prefs.getString("profile_phone", "") ?: "") }
    var selected by remember { mutableStateOf(Destination.Home) }
    var showCapture by remember { mutableStateOf(sharedText != null) }
    var cameraOpenedOnEntry by remember { mutableStateOf(false) }
    var pendingAttachment by remember { mutableStateOf<String?>(null) }
    var pendingAttachmentPath by remember { mutableStateOf<String?>(null) }
    var attachmentFiles by remember {
        mutableStateOf(prefs.all.filterKeys { it.startsWith("attachment_") }.mapKeys { it.key.removePrefix("attachment_") }.mapValues { it.value.toString() })
    }
    var analyzing by remember { mutableStateOf(false) }
    var analyzingLabel by remember { mutableStateOf("your capture") }
    var drawerOpen by remember { mutableStateOf(false) }
    var profileOpen by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    var notificationsOpen by remember { mutableStateOf(false) }
    var connectionsOpen by remember { mutableStateOf(false) }
    var remindersOpen by remember { mutableStateOf(false) }
    var reminders by remember { mutableStateOf(loadReminders(prefs)) }
    var selectedMemory by remember { mutableStateOf<Memory?>(null) }
    var memories by remember { mutableStateOf(sampleMemories) }
    val scope = rememberCoroutineScope()
    var profileUriString by remember { mutableStateOf(prefs.getString("profile_uri", null)) }
    var capturedProfile by remember {
        mutableStateOf<Bitmap?>(runCatching { File(context.filesDir, "profile_photo.png").takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.absolutePath) } }.getOrNull())
    }
    var cameraReady by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) }
    val profileUri = profileUriString?.let(Uri::parse)
    val picker = androidx.activity.compose.rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        profileUriString = uri?.toString()
        prefs.edit().putString("profile_uri", profileUriString).apply()
    }
    fun importCapture(selectedUri: Uri) {
            val mime = context.contentResolver.getType(selectedUri).orEmpty()
            val extension = android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)?.let { ".${it}" } ?: ""
            val name = "capture_${System.currentTimeMillis()}$extension"
            val target = File(context.filesDir, "captures").apply { mkdirs() }.resolve(name)
            runCatching { context.contentResolver.openInputStream(selectedUri)?.use { input -> target.outputStream().use { input.copyTo(it) } } }
            pendingAttachmentPath = target.absolutePath
            scope.launch {
                val extracted = runCatching { extractLocalContent(context, target, selectedUri) }.getOrDefault("")
                pendingAttachment = extracted.ifBlank { "Attached media: ${target.name}\nNo readable text was detected. You can add a description before saving." }
                showCapture = true
            }
    }
    val galleryPicker = androidx.activity.compose.rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { showCapture = false; importCapture(it) }
    }
    val filePicker = androidx.activity.compose.rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { showCapture = false; importCapture(it) }
    }
    val cameraLauncher = androidx.activity.compose.rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            capturedProfile = bitmap
            runCatching { File(context.filesDir, "profile_photo.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } }
        }
    }
    val cameraPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) cameraLauncher.launch(null)
    }
    val captureCameraLauncher = androidx.activity.compose.rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            val target = File(context.filesDir, "captures").apply { mkdirs() }.resolve("capture_${System.currentTimeMillis()}.jpg")
            runCatching { target.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it) } }
            pendingAttachmentPath = target.absolutePath
            pendingAttachment = "Photo captured: ${target.name}"
            showCapture = true
        }
    }
    val captureCameraPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        cameraReady = granted
        if (!granted) Toast.makeText(context, "Camera access is needed to take a photo", Toast.LENGTH_SHORT).show()
    }
    fun openGallery() {
        galleryPicker.launch("image/*")
    }
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }
    LaunchedEffect(showSplash) { if (showSplash) { delay(1400); showSplash = false } }
    LaunchedEffect(Unit) {
        analytics.setAnalyticsCollectionEnabled(true)
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            prefs.edit().putString("fcm_token", token).apply()
        }
    }
    LaunchedEffect(selected) {
        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, selected.label)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, "MainActivity")
        })
    }
    LaunchedEffect(accessToken, showOnboarding) {
        if (!accessToken.isNullOrBlank() && !showOnboarding && !cameraOpenedOnEntry) {
            cameraOpenedOnEntry = true
            delay(350)
            showCapture = true
            captureCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    LaunchedEffect(accessToken) {
        if (!accessToken.isNullOrBlank()) {
            runCatching { ApiClient.me(accessToken!!) }.onSuccess { result ->
                result.name?.takeIf { it.isNotBlank() }?.let { profileName = it; prefs.edit().putString("profile_name", it).apply() }
                result.email?.takeIf { it.isNotBlank() }?.let { profileEmail = it; prefs.edit().putString("profile_email", it).apply() }
                result.phone?.takeIf { it.isNotBlank() }?.let { profilePhone = it; prefs.edit().putString("profile_phone", it).apply() }
            }.onFailure { error ->
                if (error.message == "AUTH_INVALID") {
                    prefs.edit().remove("access_token").apply()
                    showWelcome = true
                    showAuth = false
                }
            }
            runCatching { ApiClient.memories(accessToken!!) }.onSuccess { remote ->
                memories = remote.map { Memory(it.title, "Note", it.createdAt.replace('T',' ').take(16), it.content, it.reason, Purple, it.id) }
            }
        }
    }

    if (showSplash) { SplashScreen(); return }
    if (showWelcome) { WelcomeScreen(onCreate = { authStartPage = 2; showWelcome = false; showAuth = true }, onSignIn = { authStartPage = 0; showWelcome = false; showAuth = true }); return }
    if (showCreateReference) { ReferenceCreateScreen(onBack = { showCreateReference = false; showWelcome = true }, onVerify = { showCreateReference = false; showVerifyReference = true }, onSignIn = { showCreateReference = false; showReferenceLogin = true }); return }
    if (showResetReference) { ReferenceResetScreen(onBack = { showResetReference = false; showReferenceLogin = true }, onOtp = { showResetReference = false; showVerifyReference = true }); return }
    if (showVerifyReference) { ReferenceVerifyScreen(onBack = { showVerifyReference = false; showResetReference = true }, onContinue = { showVerifyReference = false; authStartPage = 2; showAuth = true }); return }
    if (showReferenceLogin) { ReferenceLoginScreen(onSignIn = { showReferenceLogin = false; authStartPage = 0; showAuth = true }, onCreate = { showReferenceLogin = false; authStartPage = 2; showAuth = true }, onForgot = { showReferenceLogin = false; showResetReference = true }); return }
    if (showAuth) { AuthScreen(onAuthenticated = { result ->
            profileName = result.name?.takeIf { it.isNotBlank() } ?: profileName
            profileEmail = result.email?.takeIf { it.isNotBlank() } ?: profileEmail
            profilePhone = result.phone?.takeIf { it.isNotBlank() } ?: profilePhone
            accessToken = result.token
            prefs.edit().putString("access_token", result.token).putString("profile_name", profileName).putString("profile_email", profileEmail).putString("profile_phone", profilePhone).apply()
            analytics.logEvent(FirebaseAnalytics.Event.LOGIN, Bundle().apply { putString(FirebaseAnalytics.Param.METHOD, if (result.phone.isNullOrBlank()) "password" else "phone_otp") })
            showAuth = false
        }, onSkip = { showAuth = false }, onBack = { showAuth = false; showWelcome = true }, initialPage = authStartPage); return }
    if (showOnboarding) { OnboardingScreen(onComplete = { showOnboarding = false }, requestPermissions = { permissions -> permissionLauncher.launch(permissions) }); return }
    Scaffold(
        containerColor = Canvas,
        bottomBar = {
            if (!showCapture && !profileOpen && !settingsOpen && !notificationsOpen && !connectionsOpen && !remindersOpen) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(112.dp)
                        .background(Color.White)
                ) {
                Surface(
                    Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .align(Alignment.BottomCenter),
                    color = Color.White,
                    shadowElevation = 4.dp,
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                ) {
                Row(Modifier.fillMaxWidth().height(96.dp), verticalAlignment = Alignment.CenterVertically) {
                    listOf<Destination?>(Destination.Home, Destination.Journal, null, Destination.Library, Destination.Insights).forEachIndexed { index, destination ->
                        if (destination == null) {
                            Column(
                                Modifier.weight(1f).fillMaxHeight().padding(top = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Spacer(Modifier.size(25.dp))
                                Text("Capture", fontSize = 11.sp, color = if (showCapture) Blue else Color(0xFF66708B), modifier = Modifier.padding(top = 3.dp))
                            }
                        } else {
                            Column(Modifier.weight(1f).fillMaxHeight().clickable { profileOpen = false; showCapture = false; selected = destination }.padding(top = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(destination.icon, null, tint = if (selected == destination) Blue else Color(0xFF66708B), modifier = Modifier.size(25.dp))
                                Text(destination.label, fontSize = 11.sp, color = if (selected == destination) Blue else Color(0xFF66708B), modifier = Modifier.padding(top = 3.dp))
                            }
                        }
                    }
                }
                }
                FloatingActionButton(
                    onClick = { profileOpen = false; showCapture = true; analytics.logEvent("capture_started", null); captureCameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                    modifier = Modifier.align(Alignment.TopCenter).size(54.dp),
                    shape = CircleShape,
                    containerColor = Color.White,
                    contentColor = Blue
                ) {
                    Box(Modifier.size(42.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Blue, Purple))), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.AutoAwesome, "Capture", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
                }
            }
        },
        floatingActionButton = {}
    ) { padding ->
            Box((if (showCapture) Modifier.fillMaxSize() else Modifier.padding(padding).fillMaxSize())) {
            when {
                notificationsOpen -> NotificationsScreen(memories, onBack = { notificationsOpen = false })
                remindersOpen -> RemindersScreen(reminders, onBack = { remindersOpen = false }, onCreate = { title, notes, at -> val reminder = Reminder(System.currentTimeMillis(), title, notes, at); reminders = (reminders + reminder).sortedBy { it.at }; saveReminders(prefs, reminders); scheduleReminder(context, reminder) })
                connectionsOpen -> ConnectionsScreen(onBack = { connectionsOpen = false })
                settingsOpen -> SettingsScreen(profileName, profileEmail, onBack = { settingsOpen = false; profileOpen = false; selected = Destination.Home }, onNotifications = { notificationsOpen = true; settingsOpen = false }, onConnections = { connectionsOpen = true; settingsOpen = false })
                profileOpen -> ProfileScreen(
                    profileUri,
                    capturedBitmap = capturedProfile,
                    profileName = profileName,
                    profileEmail = profileEmail,
                    profilePhone = profilePhone,
                    onPick = { picker.launch("image/*") },
                    onCapture = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                    onSettings = { profileOpen = false; settingsOpen = true },
                    onNotifications = { profileOpen = false; notificationsOpen = true },
                    onConnections = { profileOpen = false; connectionsOpen = true },
                    onBack = { profileOpen = false },
                    onSignOut = {
                        accessToken = null
                        prefs.edit().remove("access_token").apply()
                        profileOpen = false
                        showWelcome = true
                        showAuth = false
                        showOnboarding = false
                    }
                )
                analyzing -> AnalysisScreen(analyzingLabel)
                selectedMemory != null -> MemoryDetailScreen(selectedMemory!!, attachmentPath = selectedMemory?.id?.let { attachmentFiles[it] }, onOpenAttachment = { path -> openLocalAttachment(context, path) }, onBack = { selectedMemory = null })
                else -> when (selected) { Destination.Home -> HomeScreen(profileName = profileName, memories = memories, networkOnline = networkOnline, onProfile = { profileOpen = true }, onNotifications = { notificationsOpen = true }, onReminders = { remindersOpen = true }, openCapture = { showCapture = true }, onMemory = { selectedMemory = it }); Destination.Journal -> JournalScreen(memories, attachmentFiles, onMemory = { selectedMemory = it }); Destination.Library -> LibraryScreen(memories, attachmentFiles, onOpenAttachment = { path -> openLocalAttachment(context, path) }); Destination.Insights -> InsightsScreen(memories) }
            }
            AnimatedVisibility(showCapture) {
                CaptureSheet(
                    sharedText ?: pendingAttachment,
                    onDismiss = { showCapture = false; pendingAttachment = null; pendingAttachmentPath = null },
                    cameraReady = cameraReady,
                    onCamera = { showCapture = true; captureCameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                    onGallery = { openGallery() },
                    onFile = { filePicker.launch("*/*") },
                    onPhotoCaptured = { path -> pendingAttachmentPath = path; pendingAttachment = "Photo captured: ${File(path).name}" },
                    onSave = { content, reason ->
                    showCapture = false
                    selectedMemory = null
                    analyzingLabel = content.trim().take(80).ifBlank { "your capture" }
                    val token = accessToken
                    val attachmentPath = pendingAttachmentPath
                    if (token.isNullOrBlank()) { analyzing = false } else scope.launch {
                        analyzing = true
                        val aiAnalysis = runCatching { GeminiAnalyzer.analyze(content, attachmentPath?.let { File(it) }) }.getOrNull().orEmpty()
                        val enrichedContent = if (aiAnalysis.isBlank()) content else "$content\n\nRekol AI analysis:\n$aiAnalysis"
                        runCatching { ApiClient.createMemory(token, enrichedContent, reason, false) }.onSuccess { saved ->
                            val savedMemory = Memory(saved.title, "Note", "Just now", saved.content, saved.reason, Purple, saved.id)
                            memories = listOf(savedMemory) + memories
                            selectedMemory = savedMemory
                            analyzing = false
                            pendingAttachmentPath?.let { path -> attachmentFiles = attachmentFiles + (saved.id to path); prefs.edit().putString("attachment_${saved.id}", path).apply() }
                            pendingAttachmentPath = null
                            RekolNotificationHelper.show(context, "Memory saved", saved.title)
                            analytics.logEvent("memory_saved", Bundle().apply { putString("memory_id", saved.id); putString("capture_reason", reason ?: "") })
                        }.onFailure {
                            analyzing = false
                            Toast.makeText(context, "Unable to save this memory", Toast.LENGTH_SHORT).show()
                        }
                    }
                    }
                )
            }
        }
    }
}

@Composable
private fun WelcomeScreen(onCreate: () -> Unit, onSignIn: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFF6FBFF), Color(0xFFEAF5FF), Color(0xFFF7F0FF))))) {
        Image(painterResource(R.drawable.rekol_welcome_background), "Welcome background", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.size(360.dp).offset(x = (-90).dp, y = (-80).dp).clip(CircleShape).background(Color(0xFFBDE9FF).copy(alpha = .45f)))
        Box(Modifier.size(330.dp).align(Alignment.TopEnd).offset(x = 120.dp, y = (-60).dp).clip(CircleShape).background(Color(0xFFE7C7FF).copy(alpha = .42f)))
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            // Keep the content above the decorative footer on compact phones.
            Spacer(Modifier.height(285.dp))
            Text("Welcome to Rekol", color = Navy, fontSize = 32.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(top = 12.dp))
            Text("Your life, organized intelligently.", color = Navy, fontSize = 18.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
            Text("Capture what matters, understand it with AI,\nand find it whenever you need it.", color = Color(0xFF66738D), fontSize = 14.sp, lineHeight = 20.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(top = 12.dp, bottom = 24.dp))
            Button(onClick = onCreate, Modifier.fillMaxWidth().height(58.dp), shape = RoundedCornerShape(28.dp), colors = ButtonDefaults.buttonColors(containerColor = Blue)) { Text("Create account  →", fontSize = 17.sp, fontWeight = FontWeight.SemiBold) }
            OutlinedButton(onClick = onSignIn, Modifier.fillMaxWidth().padding(top = 12.dp).height(58.dp), shape = RoundedCornerShape(28.dp)) { Text("Sign in", color = Blue, fontSize = 17.sp, fontWeight = FontWeight.SemiBold) }
            Row(Modifier.fillMaxWidth().padding(top = 22.dp), verticalAlignment = Alignment.CenterVertically) { HorizontalDivider(Modifier.weight(1f), color = Color(0xFFCBD5E1)); Text("or continue with", color = Color(0xFF66738D), fontSize = 13.sp, modifier = Modifier.padding(horizontal = 12.dp)); HorizontalDivider(Modifier.weight(1f), color = Color(0xFFCBD5E1)) }
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) { OutlinedButton(onClick = onSignIn, Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(16.dp)) { Image(painterResource(R.drawable.google_logo), "Google", Modifier.size(23.dp)); Text("  Google", color = Navy) }; OutlinedButton(onClick = onSignIn, Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(16.dp)) { Image(painterResource(R.drawable.apple_logo), "Apple", Modifier.size(23.dp)); Text("  Apple", color = Navy) } }
            Text(buildAnnotatedString { append("By continuing, you agree to our "); withStyle(androidx.compose.ui.text.SpanStyle(color = Blue, textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)) { append("Terms of Service") }; append(" and "); withStyle(androidx.compose.ui.text.SpanStyle(color = Blue, textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)) { append("Privacy Policy") }; append(".") }, color = Color(0xFF66738D), fontSize = 11.sp, lineHeight = 16.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 14.dp, start = 12.dp, end = 12.dp))
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ReferenceLoginScreen(onSignIn: () -> Unit, onCreate: () -> Unit, onForgot: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Image(painterResource(R.drawable.rekol_login_reference), "Welcome back", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxWidth().height(90.dp).align(Alignment.BottomCenter).padding(horizontal = 54.dp).offset(y = (-930).dp).clickable(onClick = onSignIn))
        Box(Modifier.fillMaxWidth().height(90.dp).align(Alignment.BottomCenter).padding(horizontal = 54.dp).offset(y = (-650).dp).clickable(onClick = onSignIn))
        Box(Modifier.fillMaxWidth().height(55.dp).align(Alignment.BottomCenter).padding(horizontal = 54.dp).offset(y = (-735).dp).clickable(onClick = onForgot))
        Box(Modifier.fillMaxWidth().height(70.dp).align(Alignment.BottomCenter).padding(horizontal = 80.dp).offset(y = (-92).dp).clickable(onClick = onCreate))
    }
}

@Composable
private fun ReferenceResetScreen(onBack: () -> Unit, onOtp: () -> Unit) {
    Box(Modifier.fillMaxSize()) { Image(painterResource(R.drawable.rekol_reset_reference), "Reset password", Modifier.fillMaxSize(), contentScale = ContentScale.Crop); Box(Modifier.fillMaxWidth().height(70.dp).align(Alignment.BottomCenter).padding(horizontal = 46.dp).offset(y = (-470).dp).clickable(onClick = onOtp)); Box(Modifier.size(80.dp).align(Alignment.TopStart).padding(top = 55.dp, start = 28.dp).clickable(onClick = onBack)) }
}

@Composable
private fun ReferenceVerifyScreen(onBack: () -> Unit, onContinue: () -> Unit) {
    Box(Modifier.fillMaxSize()) { Image(painterResource(R.drawable.rekol_verify_reference), "Verify phone", Modifier.fillMaxSize(), contentScale = ContentScale.Crop); Box(Modifier.fillMaxWidth().height(72.dp).align(Alignment.BottomCenter).padding(horizontal = 50.dp).offset(y = (-390).dp).clickable(onClick = onContinue)); Box(Modifier.size(80.dp).align(Alignment.TopStart).padding(top = 55.dp, start = 28.dp).clickable(onClick = onBack)) }
}

@Composable
private fun ReferenceCreateScreen(onBack: () -> Unit, onVerify: () -> Unit, onSignIn: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Image(painterResource(R.drawable.rekol_create_reference), "Create your account", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.size(80.dp).align(Alignment.TopStart).padding(top = 55.dp, start = 28.dp).clickable(onClick = onBack))
        Box(Modifier.fillMaxWidth().height(78.dp).align(Alignment.BottomCenter).padding(horizontal = 52.dp).offset(y = (-170).dp).clickable(onClick = onVerify))
        Box(Modifier.fillMaxWidth().height(50.dp).align(Alignment.BottomCenter).padding(horizontal = 190.dp).offset(y = (-58).dp).clickable(onClick = onSignIn))
    }
}

private fun friendlyAuthError(error: Throwable): String = when (error.message) {
    "INVALID_CREDENTIALS" -> "Email/phone or password is incorrect. Check your details and try again."
    "ACCOUNT_NOT_FOUND" -> "No Rekol account is registered with this phone number. Create an account first."
    "ACCOUNT_ALREADY_EXISTS" -> "An account already exists with these details. Try signing in instead."
    "AUTH_INVALID" -> "Your session has expired. Please sign in again."
    else -> error.message?.takeIf { it.isNotBlank() } ?: "Unable to sign in. Please try again."
}

private fun friendlyOtpError(error: Throwable): String = when (error.message) {
    "INVALID_CREDENTIALS", "ACCOUNT_NOT_FOUND" -> "This phone number is not registered with Rekol. Create an account first."
    else -> error.message?.takeIf { it.isNotBlank() } ?: "Unable to verify this phone number."
}

@Composable
private fun AuthScreen(onAuthenticated: (AuthResult) -> Unit, onSkip: () -> Unit, onBack: () -> Unit, initialPage: Int = 0) {
    var page by remember { mutableIntStateOf(initialPage) }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var createPassword by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(true) }
    var passwordVisible by remember { mutableStateOf(false) }
    var createPasswordVisible by remember { mutableStateOf(false) }
    var countryCode by remember { mutableStateOf("+91") }
    var countryMenu by remember { mutableStateOf(false) }
    var otp by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var referral by remember { mutableStateOf("") }
    var agreeTerms by remember { mutableStateOf(true) }
    var productUpdates by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var verificationId by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val activity = context as? Activity
    val firebaseAuth = remember { FirebaseAuth.getInstance() }
    fun authenticatePlayStoreTestPhone() {
        busy = true
        error = ""
        scope.launch {
            runCatching { ApiClient.verifyOtp("$countryCode$PLAY_TEST_PHONE", PLAY_TEST_OTP) }
                .onSuccess { result -> onAuthenticated(result) }
                .onFailure { error = friendlyOtpError(it) }
            busy = false
        }
    }
    fun requestFirebaseOtp() {
        if (isPlayStoreTestPhone(countryCode, phone)) {
            authenticatePlayStoreTestPhone()
            return
        }
        val host = activity
        if (host == null) { error = "Unable to start phone verification"; return }
        val fullPhone = "$countryCode$phone"
        busy = true
        val options = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(fullPhone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(host)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    firebaseAuth.signInWithCredential(credential)
                    busy = false
                }
                override fun onVerificationFailed(exception: FirebaseException) {
                    busy = false
                    error = exception.localizedMessage ?: "Unable to send verification code"
                }
                override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                    verificationId = id
                    busy = false
                    page = 1
                    error = ""
                }
            }).build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }
    val title = when (page) { 0 -> "Welcome back"; 1 -> "Verify your phone"; else -> "Create your account" }
    val subtitle = when (page) { 0 -> "Sign in to capture, organize and remember what matters."; 1 -> "Enter the 6-digit code sent to"; else -> "Start building your personal memory." }
    Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFFF7FBFF), Color(0xFFEAF4FF), Color(0xFFF7F0FF))))) {
        Box(Modifier.size(430.dp).offset(x = (-150).dp, y = (-110).dp).clip(CircleShape).background(Color(0xFFB9E5FF).copy(alpha = .42f)))
        Box(Modifier.size(390.dp).align(Alignment.TopEnd).offset(x = 130.dp, y = (-80).dp).clip(CircleShape).background(Color(0xFFF3C6FF).copy(alpha = .36f)))
        Box(Modifier.size(460.dp).align(Alignment.BottomStart).offset(x = (-180).dp, y = 110.dp).clip(CircleShape).background(Color(0xFFB8D9FF).copy(alpha = .36f)))
        Box(Modifier.size(420.dp).align(Alignment.BottomEnd).offset(x = 170.dp, y = 130.dp).clip(CircleShape).background(Color(0xFFFFC4D8).copy(alpha = .32f)))
        Column(Modifier.fillMaxSize().statusBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp).padding(top = if (page == 2) 18.dp else 34.dp, bottom = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(onClick = { if (page == 0) onBack() else { page = 0; error = "" } }, modifier = Modifier.align(Alignment.Start).padding(start = 2.dp)) { Icon(Icons.Default.ArrowBack, "Back", tint = Navy, modifier = Modifier.size(32.dp)) }
            Row(Modifier.fillMaxWidth().height(if (page == 2) 68.dp else 86.dp).padding(top = if (page == 2) 0.dp else 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) { Image(painterResource(R.drawable.rekol_logo_transparent), "Rekol", Modifier.width(if (page == 2) 140.dp else 110.dp).height(if (page == 2) 76.dp else 58.dp), contentScale = ContentScale.Fit) }
            Column(Modifier.fillMaxWidth().padding(top = if (page == 2) 8.dp else 18.dp, bottom = 12.dp).padding(horizontal = 2.dp)) {
                    if (page != 0) {
                        Text(title, color = Navy, fontSize = if (page == 2) 30.sp else 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = if (page == 2) androidx.compose.ui.text.style.TextAlign.Center else androidx.compose.ui.text.style.TextAlign.Start)
                        Text(subtitle, color = Color(0xFF66738D), fontSize = if (page == 2) 17.sp else 14.sp, modifier = Modifier.fillMaxWidth().padding(top = 5.dp, bottom = 16.dp), textAlign = if (page == 2) androidx.compose.ui.text.style.TextAlign.Center else androidx.compose.ui.text.style.TextAlign.Start)
                        if (page == 1) {
                            Text("$countryCode ••• ••• ${phone.takeLast(4)}", color = Navy, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("Change number", color = Blue, fontSize = 14.sp, modifier = Modifier.clickable { page = 0 }.padding(top = 6.dp, bottom = 18.dp))
                        }
                        if (page == 2) {
                            Row(Modifier.fillMaxWidth().padding(bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) { listOf("Details", "Verify", "Secure").forEachIndexed { i, label -> Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) { Box(Modifier.size(34.dp).clip(CircleShape).background(if (i == 0) Blue else Color(0xFFF4F7FB)).border(1.dp, if (i == 0) Blue else Color(0xFFCBD5E1), CircleShape), contentAlignment = Alignment.Center) { Text(if (i == 0) "1" else (i + 1).toString(), color = if (i == 0) Color.White else Color(0xFF66738D), fontWeight = FontWeight.Bold) }; Text(label, color = if (i == 0) Blue else Color(0xFF6F7B98), fontSize = 12.sp, fontWeight = if (i == 0) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.padding(top = 4.dp)) }; if (i < 2) HorizontalDivider(color = Color(0xFFCBD5E1), thickness = 1.dp, modifier = Modifier.width(42.dp).padding(bottom = 20.dp)) } }
                        }
                    }
                    when (page) {
                        0 -> Column(Modifier.fillMaxWidth()) {
                            Text("Welcome back", color = Navy, fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                            Text("Email or phone number", color = Color(0xFF59616D), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            val isPhone = phone.isNotEmpty() && phone.all(Char::isDigit)
                            OutlinedTextField(phone, { value -> phone = if (value.all(Char::isDigit)) value.take(10) else value }, Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text("Enter email or phone number") }, leadingIcon = { if (isPhone) Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { countryMenu = true }.padding(start = 3.dp)) { Text(countryFlag(countryCode), fontSize = 20.sp); Text("  $countryCode", fontSize = 14.sp, color = Navy); Icon(Icons.Default.ArrowDropDown, "Select country", tint = Color(0xFF59616D), modifier = Modifier.size(18.dp)); VerticalDivider(modifier = Modifier.height(24.dp).padding(start = 8.dp), color = Color(0xFFCBD5E1)) } else Icon(Icons.Default.Person, null) }, trailingIcon = { if (isPhone && phone.isNotEmpty()) IconButton(onClick = { phone = "" }) { Icon(Icons.Default.Clear, "Clear number") } }, singleLine = true, shape = RoundedCornerShape(14.dp), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = if (isPhone) androidx.compose.ui.text.input.KeyboardType.Phone else androidx.compose.ui.text.input.KeyboardType.Email))
                            DropdownMenu(expanded = countryMenu, onDismissRequest = { countryMenu = false }) {
                                listOf("+971  UAE", "+91  India", "+1  United States / Canada", "+44  United Kingdom").forEach { entry ->
                                    DropdownMenuItem(text = { Text("${countryFlag(entry.substringBefore("  "))}  $entry") }, onClick = { countryCode = entry.substringBefore("  "); countryMenu = false })
                                }
                            }
                            if (!isPhone) {
                                OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth().padding(top = 12.dp), label = { Text("Password") }, placeholder = { Text("Enter your password") }, leadingIcon = { Icon(Icons.Default.Lock, null) }, trailingIcon = { IconButton(onClick = { passwordVisible = !passwordVisible }) { Icon(if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, "Show password") } }, visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(), singleLine = true, shape = RoundedCornerShape(14.dp))
                                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) { Checkbox(rememberMe, { rememberMe = it }); Text("Remember me", fontSize = 14.sp, color = Navy, modifier = Modifier.weight(1f)); TextButton(onClick = { error = "Password reset is available from account recovery" }) { Text("Forgot password?", color = Blue) } }
                            }
                        }                        1 -> OutlinedTextField(otp, { otp = it.filter(Char::isDigit).take(6) }, Modifier.fillMaxWidth(), label = { Text("6-digit verification code") }, placeholder = { Text("••••••") }, leadingIcon = { Icon(Icons.Default.VerifiedUser, null) }, singleLine = true, shape = RoundedCornerShape(14.dp))
                        else -> {
                            OutlinedTextField(fullName, { fullName = it }, Modifier.fillMaxWidth(), label = { Text("Full name") }, leadingIcon = { Icon(Icons.Default.Person, null) }, singleLine = true, shape = RoundedCornerShape(14.dp))
                            OutlinedTextField(phone, { phone = it.filter(Char::isDigit).take(10) }, Modifier.fillMaxWidth().padding(top = 10.dp), label = { Text("Phone number") }, leadingIcon = { Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { countryMenu = true }.padding(start = 6.dp)) { Text(countryFlag(countryCode), fontSize = 22.sp); Text("  $countryCode", fontSize = 14.sp, color = Navy); Icon(Icons.Default.ArrowDropDown, "Select country", tint = Color(0xFF59616D), modifier = Modifier.size(18.dp)) } }, singleLine = true, shape = RoundedCornerShape(14.dp))
                            DropdownMenu(expanded = countryMenu, onDismissRequest = { countryMenu = false }) {
                                listOf("+971  UAE", "+91  India", "+1  United States / Canada", "+44  United Kingdom").forEach { entry ->
                                    DropdownMenuItem(text = { Text("${countryFlag(entry.substringBefore("  "))}  $entry") }, onClick = { countryCode = entry.substringBefore("  "); countryMenu = false })
                                }
                            }
                            Text("Phone verification code will be sent.", color = Color(0xFF66738D), fontSize = 12.sp)
                            OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth().padding(top = 10.dp), label = { Text("Email address") }, leadingIcon = { Icon(Icons.Default.Email, null) }, singleLine = true, shape = RoundedCornerShape(14.dp))
                            OutlinedTextField(createPassword, { createPassword = it }, Modifier.fillMaxWidth().padding(top = 10.dp), label = { Text("Create password") }, placeholder = { Text("At least 6 characters") }, leadingIcon = { Icon(Icons.Default.Lock, null) }, trailingIcon = { IconButton(onClick = { createPasswordVisible = !createPasswordVisible }) { Icon(if (createPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, "Show password") } }, visualTransformation = if (createPasswordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(), singleLine = true, shape = RoundedCornerShape(14.dp))
                            OutlinedTextField(referral, { referral = it }, Modifier.fillMaxWidth().padding(top = 10.dp), label = { Text("Referral code (optional)") }, singleLine = true, shape = RoundedCornerShape(14.dp))
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Checkbox(agreeTerms, { agreeTerms = it }); Text("I agree to the Terms of Service and Privacy Policy.", fontSize = 12.sp) }
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Checkbox(productUpdates, { productUpdates = it }); Text("Send me product updates and helpful tips.", fontSize = 12.sp) }
                        }
                    }
                    Spacer(Modifier.height(if (page == 2) 12.dp else 0.dp))
                    if (error.isNotEmpty()) Text(error, color = Coral, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                    if (page == 0) Text("By continuing, you agree to our Terms & Conditions and Privacy Policy.", color = Color(0xFF59616D), fontSize = 12.sp, lineHeight = 16.sp, modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
                    Button(enabled = !busy, onClick = {
                        when (page) {
                            0 -> if (phone.isBlank()) error = "Enter your email or phone number" else if (phone.all(Char::isDigit)) { requestFirebaseOtp() } else if (password.isBlank()) error = "Enter your password" else {
                                busy = true
                                scope.launch { runCatching { ApiClient.login(phone, password) }.onSuccess { result -> onAuthenticated(result) }.onFailure { error = friendlyAuthError(it) }; busy = false }
                            }
                            1 -> if (otp.isBlank()) error = "Enter the verification code" else if (verificationId.isNullOrBlank()) error = "Request a verification code first" else {
                                busy = true
                                val credential = PhoneAuthProvider.getCredential(verificationId!!, otp)
                                firebaseAuth.signInWithCredential(credential)
                                    .addOnSuccessListener {
                                        scope.launch { runCatching { ApiClient.verifyOtp("$countryCode$phone", otp) }.onSuccess { result -> onAuthenticated(result) }.onFailure { error = friendlyOtpError(it) }; busy = false }
                                    }
                                    .addOnFailureListener { error = it.localizedMessage ?: "Invalid verification code"; busy = false }
                            }
                            else -> if (!agreeTerms || fullName.isBlank() || phone.length != 10 || !email.contains("@") || createPassword.length < 6) error = "Complete the required details and use a 6-character password" else {
                                busy = true
                                scope.launch {
                                    runCatching { ApiClient.register(fullName, email, "$countryCode$phone", createPassword) }
                                        .onSuccess { requestFirebaseOtp() }
                                        .onFailure { error = friendlyAuthError(it); busy = false }
                                }
                            }
                        }
                    }, modifier = Modifier.fillMaxWidth().padding(top = if (page == 0) 10.dp else 16.dp).height(56.dp).clip(RoundedCornerShape(28.dp)).background(Brush.horizontalGradient(listOf(Color(0xFF12BCEB), Color(0xFF1C6DED), Color(0xFFB72CFF)))), shape = RoundedCornerShape(28.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)) { Text(if (busy) "Please wait…" else if (page == 0) "Sign in  →" else if (page == 1) "Verify and continue" else "Continue to verification", fontSize = 17.sp, fontWeight = FontWeight.SemiBold) }
                    if (page == 0) {
                        Text("or continue with", color = Color(0xFF66738D), modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 14.dp))
                        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) { OutlinedButton(onClick = { page = 1 }, Modifier.weight(1f).height(48.dp)) { Image(painterResource(R.drawable.apple_logo), "Apple", Modifier.size(23.dp)); Text("  Apple", color = Navy) }; OutlinedButton(onClick = { page = 1 }, Modifier.weight(1f).height(48.dp)) { Image(painterResource(R.drawable.google_logo), "Google", Modifier.size(23.dp)); Text("  Google", color = Navy) } }
                        OutlinedButton(onClick = { page = 1 }, Modifier.fillMaxWidth().padding(top = 10.dp).height(54.dp)) { Icon(Icons.Default.Fingerprint, "Biometrics", tint = Blue, modifier = Modifier.size(32.dp)); Text("  Sign in with biometrics", color = Navy, fontSize = 15.sp) }
                        TextButton(onClick = { page = 2; error = "" }, Modifier.align(Alignment.CenterHorizontally)) { Text("New to Rekol?  Create account", color = Blue) }
                        Text("♢  Secure sign-in. Your data stays protected.", color = Color(0xFF66738D), fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp))
                        Text("Crafted and designed by PrabhaTech", color = Color(0xFF66738D), fontSize = 11.sp, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 14.dp, bottom = 8.dp))
                    }
                    if (page == 1) TextButton(onClick = { page = 2; error = "" }, Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp)) { Text("New to Rekol? Create an account", color = Blue) }
            }
        }
    }
}

@Composable
private fun OnboardingScreen(onComplete: () -> Unit, requestPermissions: (Array<String>) -> Unit) {
    var page by remember { mutableIntStateOf(0) }
    val titles = listOf("Welcome to Rekol", "Capture anything", "Understand automatically", "Find it anytime", "Turn information into action")
    val messages = listOf("Your personal intelligence companion.", "Save notes, links, screenshots, photos, audio and documents in one tap.", "Rekol extracts context, topics and useful summaries while you continue your day.", "Search naturally across your private memory library whenever you need it.", "Turn saved information into reminders, calendar events and follow-ups.")
    val icons = listOf(Icons.Default.AutoAwesome, Icons.Default.CameraAlt, Icons.Default.Psychology, Icons.Default.Search, Icons.Default.TaskAlt)
    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.White, Color(0xFFF0F6FF), Color(0xFFEAF9F7)))).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(42.dp)); Image(painterResource(R.drawable.rekol_logo_transparent), "Rekol", Modifier.width(190.dp).height(115.dp), contentScale = ContentScale.Fit)
        Spacer(Modifier.height(28.dp)); Box(Modifier.size(118.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Blue, Cyan, Purple))), contentAlignment = Alignment.Center) { Icon(icons[page], null, tint = Color.White, modifier = Modifier.size(52.dp)) }
        Text(titles[page], color = Navy, fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 30.dp))
        Text(messages[page], color = Color(0xFF66738D), fontSize = 16.sp, lineHeight = 24.sp, modifier = Modifier.padding(top = 12.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.weight(1f)); Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.height(12.dp), verticalAlignment = Alignment.CenterVertically) { titles.indices.forEach { i -> Box(Modifier.width(if (i == page) 25.dp else 7.dp).height(7.dp).clip(CircleShape).background(if (i == page) Blue else Line)) } }
        Spacer(Modifier.height(14.dp))
        Button(onClick = {
            if (page == 1) requestPermissions(arrayOf(Manifest.permission.CAMERA))
            if (page == 2) requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO))
            if (page == 3) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
            if (page == titles.lastIndex) onComplete() else page++
        }, Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(15.dp)) { Text(if (page == titles.lastIndex) "Start using Rekol" else "Continue", fontSize = 16.sp) }
        TextButton(onClick = onComplete, Modifier.height(48.dp)) { Text("Skip for now", color = Color(0xFF66738D)) }
    }
}

@Composable
private fun SplashScreen() {
    val transition = rememberInfiniteTransition(label = "splash dots")
    val phase by transition.animateFloat(initialValue = 0f, targetValue = 1f, animationSpec = androidx.compose.animation.core.infiniteRepeatable(animation = androidx.compose.animation.core.tween(1200), repeatMode = androidx.compose.animation.core.RepeatMode.Restart), label = "dot phase")
    Box(Modifier.fillMaxSize()) {
        Image(painterResource(R.drawable.rekol_splash_nodots), "Rekol splash", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Row(Modifier.align(Alignment.Center).offset(y = 154.dp).background(Color(0xFFF7FBFF)).padding(horizontal = 10.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(Color(0xFF168CFF), Color(0xFFB52CFF), Color(0xFFFF805A)).forEachIndexed { index, color ->
                val distance = kotlin.math.abs(((phase * 3f - index + 3f) % 3f) - 1f)
                Box(Modifier.size((8f + (1f - distance) * 4f).dp).clip(CircleShape).background(color.copy(alpha = .65f + (1f - distance) * .35f)))
            }
        }
    }
}

@Composable
private fun PageHeader(title: String, subtitle: String? = null, action: @Composable (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(title, fontSize = 27.sp, fontWeight = FontWeight.Bold, color = Navy); subtitle?.let { Text(it, color = Color(0xFF6F7B98), fontSize = 13.sp) } }
        action?.invoke()
    }
}

@Composable
private fun RekolDrawer(selected: Destination, onSelect: (Destination) -> Unit, onDismiss: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0x66000000)).clickable(onClick = onDismiss)) {
        Surface(Modifier.fillMaxHeight().width(292.dp).clickable(enabled = false) {}, color = Color.White) {
            Column(Modifier.padding(22.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) { Image(painterResource(R.drawable.rekol_mobile_icon), null, Modifier.size(46.dp)); Text("rekol", color = Navy, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 10.dp)) }
                Text("YOUR MEMORY COMPANION", color = Color.Gray, fontSize = 10.sp, modifier = Modifier.padding(top = 34.dp, bottom = 12.dp))
                Destination.entries.forEach { destination ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(if (selected == destination) Color(0xFFEAF3FF) else Color.Transparent).clickable { onSelect(destination) }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(destination.icon, null, tint = if (selected == destination) Blue else Navy); Text(destination.label, color = Navy, fontSize = 15.sp, modifier = Modifier.padding(start = 14.dp))
                    }
                }
                Spacer(Modifier.weight(1f)); HorizontalDivider(color = Line); Row(Modifier.padding(top = 18.dp).clickable(onClick = onDismiss), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Settings, null, tint = Navy); Text("Settings", color = Navy, modifier = Modifier.padding(start = 14.dp)) }
            }
        }
    }
}

@Composable
private fun ProfileScreen(profileUri: Uri?, capturedBitmap: Bitmap?, profileName: String, profileEmail: String, profilePhone: String, onPick: () -> Unit, onCapture: () -> Unit, onSettings: () -> Unit, onNotifications: () -> Unit, onConnections: () -> Unit, onBack: () -> Unit, onSignOut: () -> Unit) {
    val context = LocalContext.current
    var showPhotoOptions by remember { mutableStateOf(false) }
    var showSignOutConfirm by remember { mutableStateOf(false) }
    val profileBitmap = capturedBitmap ?: remember(profileUri) { profileUri?.let { context.contentResolver.openInputStream(it)?.use(BitmapFactory::decodeStream) } }
    val initials = profileName.trim().firstOrNull()?.uppercase() ?: "R"
    LazyColumn(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFF8FCFF), Color(0xFFEAF6FF)))),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = Navy, modifier = Modifier.size(25.dp)) }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, "Settings", tint = Navy, modifier = Modifier.size(24.dp)) }
            }
            Text("Profile", color = Navy, fontSize = 31.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
        }
        item {
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, Line), shadowElevation = 2.dp) {
                Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(76.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Blue, Purple))), contentAlignment = Alignment.Center) {
                        if (profileBitmap != null) Image(BitmapPainter(profileBitmap.asImageBitmap()), "Profile photo", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        else Text(initials, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(Modifier.weight(1f).padding(start = 14.dp)) {
                        Text(profileName.ifBlank { "Rekol user" }, color = Navy, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(profileEmail.ifBlank { "No email provided" }, color = Color(0xFF66738D), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("⌖  ${if (profilePhone.isBlank()) "Dubai, UAE" else profilePhone}", color = Color(0xFF66738D), fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("●  Verified", color = Color(0xFF13B77A), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 3.dp))
                    }
                    OutlinedButton(onClick = { showPhotoOptions = true }, contentPadding = PaddingValues(horizontal = 10.dp), shape = RoundedCornerShape(12.dp)) { Text("Edit profile", color = Blue, fontSize = 12.sp) }
                }
            }
        }
        item {
            if (showPhotoOptions) {
                AlertDialog(onDismissRequest = { showPhotoOptions = false }, title = { Text("Update profile photo") }, text = { Text("Choose a photo from your device or capture a new one.") }, confirmButton = { TextButton(onClick = { showPhotoOptions = false; onPick() }) { Text("Gallery") } }, dismissButton = { TextButton(onClick = { showPhotoOptions = false; onCapture() }) { Text("Camera") } })
            }
        }
        item {
            Surface(Modifier.fillMaxWidth().padding(top = 12.dp), shape = RoundedCornerShape(16.dp), color = Color(0xFFF7F1FF), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE4D9FF))) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFECE2FF)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Star, null, tint = Purple) }
                    Column(Modifier.weight(1f).padding(start = 12.dp)) { Text("Rekol Pro", color = Navy, fontWeight = FontWeight.Bold); Text("Your memories, fully unlocked", color = Color(0xFF66738D), fontSize = 12.sp); Text("Renews 23 Oct 2026", color = Color(0xFF8791A7), fontSize = 10.sp) }
                    Text("● Active", color = Color(0xFF13B77A), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        item {
            Surface(Modifier.fillMaxWidth().padding(top = 12.dp), shape = RoundedCornerShape(16.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, Line)) {
                Column(Modifier.padding(14.dp)) {
                    Text("Your Rekol", color = Navy, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        ProfileStat("128", "Memories", Icons.Default.Description, Purple)
                        ProfileStat("42", "Actions", Icons.Default.Send, Color(0xFFFFA117))
                        ProfileStat("14", "Day streak", Icons.Default.LocalFireDepartment, Coral)
                    }
                    HorizontalDivider(Modifier.padding(vertical = 12.dp), color = Line)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Storage used", color = Navy, fontSize = 12.sp); Text("2.4 GB of 10 GB", color = Color(0xFF66738D), fontSize = 12.sp) }
                    Box(Modifier.fillMaxWidth().padding(top = 7.dp).height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFE7EEF8))) { Box(Modifier.fillMaxWidth(.24f).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(Blue)) }
                }
            }
        }
        item { ProfileSection("Preferences", listOf("Notifications" to "Smart alerts on", "Connections" to "3 apps connected", "Appearance" to "System default", "Language" to "English"), listOf(Icons.Default.Notifications, Icons.Default.Link, Icons.Default.DarkMode, Icons.Default.Language), onRowClick = { index -> if (index == 0) onNotifications() else if (index == 1) onConnections() }) }
        item { ProfileSection("Privacy & security", listOf("Account security" to "Password and biometrics", "Privacy controls" to "Manage AI and data settings", "Data & storage" to "Export or delete your data", "Active sessions" to "2 devices"), listOf(Icons.Default.Security, Icons.Default.Lock, Icons.Default.Storage, Icons.Default.PhoneAndroid)) }
        item { ProfileSection("Support", listOf("Help center" to "", "Contact support" to "", "About Rekol" to "Version 1.0.0"), listOf(Icons.Default.Help, Icons.Default.HeadsetMic, Icons.Default.Info)) }
        item {
            OutlinedButton(onClick = { showSignOutConfirm = true }, Modifier.fillMaxWidth().padding(top = 14.dp).height(48.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Coral)) { Icon(Icons.Default.Logout, null); Text("  Sign out", fontWeight = FontWeight.SemiBold) }
            Text("Capture Today. Remember Tomorrow.", color = Color(0xFF7C87A0), fontSize = 12.sp, modifier = Modifier.fillMaxWidth().padding(top = 9.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Text("Crafted and designed by PrabhaTech", color = Color(0xFF7C87A0), fontSize = 11.sp, modifier = Modifier.fillMaxWidth().padding(top = 5.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
    if (showSignOutConfirm) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirm = false },
            shape = RoundedCornerShape(24.dp),
            title = { Text("Sign out?", color = Navy, fontWeight = FontWeight.Bold) },
            text = { Text("Do you want to sign out of your Rekol account?", color = Color(0xFF66738D)) },
            confirmButton = { Button(onClick = { showSignOutConfirm = false; onSignOut() }, colors = ButtonDefaults.buttonColors(containerColor = Coral), shape = RoundedCornerShape(12.dp)) { Text("Sign out") } },
            dismissButton = { TextButton(onClick = { showSignOutConfirm = false }) { Text("Cancel", color = Navy) } }
        )
    }
}

@Composable
private fun ProfileStat(value: String, label: String, icon: ImageVector, tint: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(tint.copy(alpha = .12f)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = tint, modifier = Modifier.size(21.dp)) }
        Text(value, color = Navy, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 3.dp))
        Text(label, color = Color(0xFF7C87A0), fontSize = 11.sp)
    }
}

@Composable
private fun ProfileSection(title: String, rows: List<Pair<String, String>>, icons: List<ImageVector>, onRowClick: ((Int) -> Unit)? = null) {
    Column(Modifier.padding(top = 16.dp)) {
        Text(title, color = Navy, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 7.dp))
        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, Line)) {
            Column {
                rows.forEachIndexed { index, row ->
                    Row(Modifier.fillMaxWidth().clickable(enabled = onRowClick != null) { onRowClick?.invoke(index) }.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(31.dp).clip(CircleShape).background(listOf(Color(0xFFEAF1FF), Color(0xFFE7FBF5), Color(0xFFF1E9FF), Color(0xFFEAF8FF))[index % 4]), contentAlignment = Alignment.Center) { Icon(icons[index], null, tint = listOf(Blue, Color(0xFF13B77A), Purple, Blue)[index % 4], modifier = Modifier.size(17.dp)) }
                        Text(row.first, color = Navy, fontSize = 13.sp, modifier = Modifier.padding(start = 11.dp).weight(1f))
                        if (row.second.isNotBlank()) Text(row.second, color = Color(0xFF7C87A0), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Icon(Icons.Default.ChevronRight, null, tint = Color(0xFF66738D), modifier = Modifier.padding(start = 6.dp).size(19.dp))
                    }
                    if (index < rows.lastIndex) HorizontalDivider(color = Line)
                }
            }
        }
    }
}

@Composable
private fun SettingsToggleSection(title: String, rows: List<Pair<String, Boolean>>, icons: List<ImageVector>, onToggle: (Int, Boolean) -> Unit, onHeaderClick: (() -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Text(title, color = Navy, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(enabled = onHeaderClick != null) { onHeaderClick?.invoke() })
        Surface(Modifier.fillMaxWidth().padding(top = 7.dp), shape = RoundedCornerShape(15.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, Line)) {
            Column {
                rows.forEachIndexed { index, (name, checked) ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(34.dp).clip(CircleShape).background(Color(0xFFEAF3FF)), contentAlignment = Alignment.Center) { Icon(icons.getOrElse(index) { Icons.Default.Settings }, null, tint = Blue, modifier = Modifier.size(18.dp)) }
                        Text(name, color = Navy, fontSize = 13.sp, modifier = Modifier.weight(1f).padding(start = 10.dp))
                        Switch(checked = checked, onCheckedChange = { onToggle(index, it) })
                    }
                    if (index < rows.lastIndex) HorizontalDivider(color = Line)
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(profileName: String, profileEmail: String, onBack: () -> Unit, onNotifications: () -> Unit, onConnections: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("rekol_settings", android.content.Context.MODE_PRIVATE) }
    var aiAnalysis by remember { mutableStateOf(prefs.getBoolean("ai_analysis", true)) }
    var suggestions by remember { mutableStateOf(prefs.getBoolean("personalized_suggestions", true)) }
    var autoTagging by remember { mutableStateOf(prefs.getBoolean("automatic_tagging", true)) }
    var offlineCapture by remember { mutableStateOf(prefs.getBoolean("offline_capture", true)) }
    var smartNotifications by remember { mutableStateOf(prefs.getBoolean("smart_notifications", true)) }
    var actionReminders by remember { mutableStateOf(prefs.getBoolean("action_reminders", true)) }
    var highContrast by remember { mutableStateOf(prefs.getBoolean("high_contrast", false)) }
    fun save(key: String, value: Boolean) { prefs.edit().putBoolean(key, value).apply() }
    LazyColumn(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFF8FCFF), Color(0xFFEAF6FF)))),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = Navy, modifier = Modifier.size(25.dp)) }
                Spacer(Modifier.weight(1f))
                Icon(Icons.Default.Search, "Search settings", tint = Navy, modifier = Modifier.size(24.dp))
            }
            Text("Settings", color = Navy, fontSize = 31.sp, fontWeight = FontWeight.Bold)
            Text("Make Rekol work the way you do.", color = Color(0xFF6F7B98), fontSize = 14.sp)
        }
        item {
            Surface(Modifier.fillMaxWidth().padding(top = 14.dp), shape = RoundedCornerShape(15.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, Line)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(48.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Blue, Purple))), contentAlignment = Alignment.Center) { Text(profileName.trim().firstOrNull()?.uppercase() ?: "R", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
                    Column(Modifier.padding(start = 12.dp).weight(1f)) { Text(profileName.ifBlank { "Rekol user" }, color = Navy, fontWeight = FontWeight.Bold); Text(profileEmail.ifBlank { "No email provided" }, color = Color(0xFF6F7B98), fontSize = 12.sp) }
                    Icon(Icons.Default.ChevronRight, null, tint = Color(0xFF66738D))
                }
            }
        }
        item { SettingsToggleSection("Rekol AI", listOf("AI analysis" to aiAnalysis, "Personalized suggestions" to suggestions), listOf(Icons.Default.AutoAwesome, Icons.Default.Psychology), onToggle = { index, value -> if (index == 0) { aiAnalysis = value; save("ai_analysis", value) } else { suggestions = value; save("personalized_suggestions", value) } }) }
        item { ProfileSection("Rekol AI", listOf("Analysis language" to "Auto-detect", "AI privacy controls" to "Choose what Rekol can analyze"), listOf(Icons.Default.Translate, Icons.Default.Security)) }
        item { ProfileSection("Capture & organization", listOf("Audio quality" to "High", "Default collection" to "Inbox"), listOf(Icons.Default.Mic, Icons.Default.Folder)) }
        item { SettingsToggleSection("Capture & organization", listOf("Automatic tagging" to autoTagging, "Offline capture" to offlineCapture), listOf(Icons.Default.LocalOffer, Icons.Default.CloudDownload), onToggle = { index, value -> if (index == 0) { autoTagging = value; save("automatic_tagging", value) } else { offlineCapture = value; save("offline_capture", value) } }) }
        item { SettingsToggleSection("Notifications", listOf("Smart notifications" to smartNotifications, "Action reminders" to actionReminders), listOf(Icons.Default.Notifications, Icons.Default.Event), onToggle = { index, value -> if (index == 0) { smartNotifications = value; save("smart_notifications", value) } else { actionReminders = value; save("action_reminders", value) } }, onHeaderClick = onNotifications) }
        item { SettingsToggleSection("Appearance & accessibility", listOf("High contrast" to highContrast), listOf(Icons.Default.Contrast), onToggle = { _, value -> highContrast = value; save("high_contrast", value) }) }
        item { ProfileSection("Appearance & accessibility", listOf("Theme" to "System default", "Text size" to "Default", "App language" to "English"), listOf(Icons.Default.Palette, Icons.Default.FormatSize, Icons.Default.Language)) }
        item { ProfileSection("Data & security", listOf("Connections" to "3 apps connected", "Security" to "Password and biometrics", "Storage" to "2.4 GB of 10 GB", "Export your data" to ""), listOf(Icons.Default.Link, Icons.Default.Lock, Icons.Default.Storage, Icons.Default.Download), onRowClick = { index -> if (index == 0) onConnections() }) }
        item { ProfileSection("Help & legal", listOf("Help center" to "", "Privacy policy" to "", "Terms of service" to "", "About Rekol" to "Version 1.0.0"), listOf(Icons.Default.Help, Icons.Default.Policy, Icons.Default.Description, Icons.Default.Info)) }
        item { Text("Reset all settings", color = Blue, fontSize = 13.sp, modifier = Modifier.fillMaxWidth().padding(top = 14.dp).clickable { prefs.edit().clear().apply(); aiAnalysis=true; suggestions=true; autoTagging=true; offlineCapture=true; smartNotifications=true; actionReminders=true; highContrast=false }, textAlign = androidx.compose.ui.text.style.TextAlign.Center); Text("Crafted and designed by PrabhaTech", color = Color(0xFF7C87A0), fontSize = 11.sp, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
    }
}

@Composable
private fun NotificationsScreen(memories: List<Memory>, onBack: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    var smartEnabled by remember { mutableStateOf(true) }
    val tabs = listOf("All 7", "Actions", "Memories", "AI updates", "System")
    LazyColumn(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFF8FCFF), Color(0xFFEAF6FF)))),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = Navy, modifier = Modifier.size(25.dp)) }
                Spacer(Modifier.weight(1f))
                Image(painterResource(R.drawable.rekol_logo_transparent), "Rekol", Modifier.width(92.dp).height(42.dp), contentScale = ContentScale.Fit)
                Spacer(Modifier.weight(1f))
                Icon(Icons.Default.Settings, "Notification settings", tint = Navy, modifier = Modifier.size(24.dp))
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Notifications", color = Navy, fontSize = 31.sp, fontWeight = FontWeight.Bold)
                    Text("Stay updated without the noise.", color = Color(0xFF6F7B98), fontSize = 14.sp)
                }
                Text("Mark all as read", color = Blue, fontSize = 13.sp, modifier = Modifier.clickable { })
            }
        }
        item {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                tabs.forEachIndexed { index, label ->
                    Surface(Modifier.clickable { tab = index }, shape = RoundedCornerShape(24.dp), color = if (tab == index) Blue else Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, if (tab == index) Blue else Line)) { Text(label, color = if (tab == index) Color.White else Navy, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) }
                }
            }
        }
        item {
            Surface(Modifier.fillMaxWidth().padding(top = 14.dp), shape = RoundedCornerShape(16.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, Line)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFE6F2FF)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Notifications, null, tint = Blue) }
                    Column(Modifier.weight(1f).padding(start = 12.dp)) { Text("Smart notifications are on", color = Navy, fontWeight = FontWeight.Bold); Text("Rekol only alerts you when it matters.", color = Color(0xFF6F7B98), fontSize = 12.sp) }
                    Text("Manage", color = Blue, fontSize = 13.sp)
                }
            }
        }
        item { Text("Today", color = Navy, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)) }
        item { NotificationCard(Icons.Default.Send, Color(0xFFFFA117), "Action due in 30 minutes", "Review project site photos is due at 10:30 AM.", "10 min ago", "View action") }
        item { NotificationCard(Icons.Default.AutoAwesome, Purple, "Your AI summary is ready", "Project kickoff meeting has been analyzed with 5 action items.", "28 min ago", "Open summary") }
        item { NotificationCard(Icons.Default.CheckCircle, Color(0xFF13B77A), "Action completed", "Send client meeting summary was marked complete.", "1 hr ago", null) }
        if (memories.isNotEmpty()) {
            items(memories.take(10)) { memory ->
                NotificationCard(Icons.Default.Description, memory.accent, "New memory added", "${memory.title} was saved to your library.", memory.time, null)
            }
        } else {
            item { NotificationCard(Icons.Default.Description, Purple, "No memories yet", "Capture something to start receiving memory notifications.", "Now", null) }
        }
        item { NotificationCard(Icons.Default.Sync, Blue, "Google Drive synced", "6 new files were added to your Rekol library.", "3 hrs ago", null) }
        item { Text("Yesterday", color = Navy, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)) }
        item { NotificationCard(Icons.Default.BarChart, Blue, "Your weekly insights are ready", "See your capture trends and progress.", "Yesterday, 6:00 PM", null) }
        item { NotificationCard(Icons.Default.Event, Color(0xFFFFA117), "Reminder scheduled", "Sustainability review · Tomorrow at 9:00 AM", "Yesterday, 3:42 PM", null) }
        item { NotificationCard(Icons.Default.Security, Coral, "New sign-in detected", "Signed in from Dubai, UAE on this device.", "Yesterday, 9:15 PM", "Review activity") }
        item { Text("Clear read notifications", color = Blue, fontSize = 13.sp, modifier = Modifier.fillMaxWidth().padding(top = 16.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
        item { Text("Crafted and designed by PrabhaTech", color = Color(0xFF7C87A0), fontSize = 11.sp, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
    }
}

@Composable
private fun NotificationCard(icon: ImageVector, tint: Color, title: String, body: String, time: String, action: String?) {
    Surface(Modifier.fillMaxWidth().padding(vertical = 5.dp), shape = RoundedCornerShape(16.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, Line)) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(tint.copy(alpha = .11f)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = tint, modifier = Modifier.size(24.dp)) }
            Column(Modifier.weight(1f).padding(start = 12.dp)) { Text(title, color = Navy, fontWeight = FontWeight.Bold, fontSize = 14.sp); Text(body, color = Color(0xFF6F7B98), fontSize = 12.sp, lineHeight = 16.sp); if (action != null) Text(action, color = Blue, fontSize = 12.sp, modifier = Modifier.padding(top = 5.dp)) }
            Text(time, color = Color(0xFF7C87A0), fontSize = 10.sp, modifier = Modifier.align(Alignment.Top))
        }
    }
}

@Composable
private fun ConnectionsScreen(onBack: () -> Unit) {
    val connected = remember { mutableStateListOf(true, true, true) }
    LazyColumn(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFF8FCFF), Color(0xFFEAF6FF)))),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = Navy, modifier = Modifier.size(25.dp)) }
                Spacer(Modifier.weight(1f)); Image(painterResource(R.drawable.rekol_logo_transparent), "Rekol", Modifier.width(112.dp).height(44.dp), contentScale = ContentScale.Fit); Spacer(Modifier.weight(1f)); Icon(Icons.Default.HelpOutline, "Help", tint = Navy, modifier = Modifier.size(25.dp))
            }
            Text("Connections", color = Navy, fontSize = 31.sp, fontWeight = FontWeight.Bold)
            Text("Bring your tools together and capture from anywhere.", color = Color(0xFF6F7B98), fontSize = 14.sp)
        }
        item { Surface(Modifier.fillMaxWidth().padding(top = 14.dp), shape = RoundedCornerShape(15.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, Line)) { Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Security, null, tint = Blue); Text("You control what Rekol can access. Disconnect anytime.", color = Navy, fontSize = 12.sp, modifier = Modifier.weight(1f).padding(horizontal = 10.dp)); Text("Learn more", color = Blue, fontSize = 12.sp) } } }
        item { Surface(Modifier.fillMaxWidth().padding(top = 12.dp), shape = RoundedCornerShape(16.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, Line)) { Row(Modifier.padding(horizontal = 14.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Search, null, tint = Navy); Text("Search apps and services", color = Color(0xFF7C87A0), modifier = Modifier.padding(start = 12.dp)) } } }
        item { Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("All", "Connected", "Storage", "Productivity", "Communication").forEachIndexed { i, label -> Surface(shape = RoundedCornerShape(22.dp), color = if (i == 0) Blue else Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, if (i == 0) Blue else Line)) { Text(label, color = if (i == 0) Color.White else Navy, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) } } } }
        item { Text("Connected  3", color = Navy, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 18.dp, bottom = 6.dp)) }
        item { ConnectionRow("Google Drive", "pradeep@prabhatech.com", Icons.Default.Folder, connected, 0, "Last synced 2 min ago") }
        item { ConnectionRow("Google Calendar", "2 calendars connected", Icons.Default.Event, connected, 1, "Syncing events and reminders") }
        item { ConnectionRow("Microsoft OneDrive", "Prabha Technologies", Icons.Default.Cloud, connected, 2, "Last synced today") }
        item { Surface(Modifier.fillMaxWidth().padding(top = 8.dp), shape = RoundedCornerShape(16.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, Line)) { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Sync, null, tint = Blue, modifier = Modifier.size(28.dp)); Column(Modifier.weight(1f).padding(start = 12.dp)) { Text("Sync status", color = Navy, fontWeight = FontWeight.Bold); Text("All connections are up to date", color = Color(0xFF6F7B98), fontSize = 12.sp) }; OutlinedButton(onClick = {}, shape = RoundedCornerShape(12.dp)) { Text("Sync now", color = Blue) } } } }
        item { Text("Available connections", color = Navy, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 20.dp, bottom = 7.dp)) }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { AvailableConnection("Notion", "Import pages and notes", Icons.Default.Description, Modifier.weight(1f)); AvailableConnection("Dropbox", "Save files and photos", Icons.Default.Folder, Modifier.weight(1f)) } }
        item { Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) { AvailableConnection("Slack", "Capture messages and threads", Icons.Default.Chat, Modifier.weight(1f)); AvailableConnection("Microsoft Teams", "Save chats and meetings", Icons.Default.Groups, Modifier.weight(1f)) } }
        item { Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) { AvailableConnection("Gmail", "Turn emails into memories", Icons.Default.Email, Modifier.weight(1f)); AvailableConnection("iCloud", "Sync files and photos", Icons.Default.Cloud, Modifier.weight(1f)) } }
        item { Surface(Modifier.fillMaxWidth().padding(top = 10.dp), shape = RoundedCornerShape(16.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, Line)) { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Extension, null, tint = Blue); Column(Modifier.weight(1f).padding(start = 12.dp)) { Text("Browser extension", color = Navy, fontWeight = FontWeight.Bold); Text("Capture any webpage in one click.", color = Color(0xFF6F7B98), fontSize = 12.sp) }; Button(onClick = {}, shape = RoundedCornerShape(12.dp)) { Text("Get extension") } } } }
        item { Text("Crafted by PrabhaTech", color = Color(0xFF7C87A0), fontSize = 11.sp, modifier = Modifier.fillMaxWidth().padding(top = 14.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
    }
}

@Composable
private fun ConnectionRow(name: String, detail: String, icon: ImageVector, connected: SnapshotStateList<Boolean>, index: Int, status: String) {
    Surface(Modifier.fillMaxWidth().padding(vertical = 5.dp), shape = RoundedCornerShape(16.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, Line)) { Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFE8F3FF)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Blue, modifier = Modifier.size(26.dp)) }; Column(Modifier.weight(1f).padding(start = 12.dp)) { Text(name, color = Navy, fontWeight = FontWeight.Bold); Text(detail, color = Color(0xFF6F7B98), fontSize = 12.sp); Text(status, color = Color(0xFF6F7B98), fontSize = 11.sp) }; Switch(checked = connected[index], onCheckedChange = { connected[index] = it }) } }
}

@Composable
private fun AvailableConnection(name: String, detail: String, icon: ImageVector, modifier: Modifier) {
    Surface(modifier, shape = RoundedCornerShape(16.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, Line)) { Column(Modifier.padding(12.dp)) { Icon(icon, null, tint = Blue, modifier = Modifier.size(27.dp)); Text(name, color = Navy, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp)); Text(detail, color = Color(0xFF6F7B98), fontSize = 11.sp, lineHeight = 14.sp, minLines = 2); OutlinedButton(onClick = {}, shape = RoundedCornerShape(12.dp), modifier = Modifier.padding(top = 5.dp)) { Text("Connect", color = Blue, fontSize = 11.sp) } } }
}

@Composable
private fun rememberNetworkStatus(): Boolean {
    val context = LocalContext.current
    fun isOnline(): Boolean {
        val manager = context.getSystemService(ConnectivityManager::class.java)
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
    var online by remember { mutableStateOf(isOnline()) }
    DisposableEffect(context) {
        val manager = context.getSystemService(ConnectivityManager::class.java)
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { online = isOnline() }
            override fun onLost(network: Network) { online = isOnline() }
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) { online = isOnline() }
        }
        manager.registerDefaultNetworkCallback(callback)
        onDispose { runCatching { manager.unregisterNetworkCallback(callback) } }
    }
    return online
}

@Composable
private fun RemindersScreen(reminders: List<Reminder>, onBack: () -> Unit, onCreate: (String, String, Long) -> Unit) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedAt by remember { mutableStateOf(Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 1); set(Calendar.MINUTE, 0) }) }
    var showForm by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
        item { Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.ArrowBack, "Back", tint = Navy, modifier = Modifier.size(26.dp).clickable(onClick = onBack)); Text("Reminders", color = Navy, fontSize = 25.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 16.dp)); Spacer(Modifier.weight(1f)); IconButton(onClick = { showForm = true }) { Icon(Icons.Default.Add, "Add reminder", tint = Blue) } } }
        item { Surface(Modifier.padding(horizontal = 18.dp).fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = Color(0xFFEAF4FF)) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Event, null, tint = Blue, modifier = Modifier.size(28.dp)); Column(Modifier.padding(start = 12.dp)) { Text("Never miss a follow-up", color = Navy, fontWeight = FontWeight.Bold); Text("Set a date and time and Rekol will notify you on this device.", color = Color(0xFF5B7195), fontSize = 12.sp) } } } }
        item { Text("Upcoming", color = Navy, fontSize = 19.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(20.dp, 26.dp, 20.dp, 8.dp)) }
        items(reminders.filter { it.at >= System.currentTimeMillis() }) { reminder -> Surface(Modifier.padding(horizontal = 18.dp, vertical = 5.dp).fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, Line)) { Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.NotificationsActive, null, tint = Purple); Column(Modifier.padding(start = 12.dp)) { Text(reminder.title, color = Navy, fontWeight = FontWeight.Bold); Text(java.text.SimpleDateFormat("EEE, d MMM · h:mm a", java.util.Locale.getDefault()).format(java.util.Date(reminder.at)), color = Blue, fontSize = 12.sp); if (reminder.notes.isNotBlank()) Text(reminder.notes, color = Color(0xFF6F7B98), fontSize = 12.sp, maxLines = 2) } } } }
        if (reminders.none { it.at >= System.currentTimeMillis() }) item { Text("No upcoming reminders. Tap + to schedule one.", color = Color(0xFF7C87A0), modifier = Modifier.fillMaxWidth().padding(22.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
    }
    if (showForm) {
        val formatted = java.text.SimpleDateFormat("EEE, d MMM · h:mm a", java.util.Locale.getDefault()).format(selectedAt.time)
        AlertDialog(
            onDismissRequest = { showForm = false },
            title = { Text("New reminder") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("What should Rekol remind you about?") }, singleLine = true)
                    OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes (optional)") }, minLines = 2)
                    OutlinedButton(onClick = {
                        val c = selectedAt
                        DatePickerDialog(context, { _, y, m, d ->
                            c.set(Calendar.YEAR, y); c.set(Calendar.MONTH, m); c.set(Calendar.DAY_OF_MONTH, d)
                            TimePickerDialog(context, { _, h, min -> c.set(Calendar.HOUR_OF_DAY, h); c.set(Calendar.MINUTE, min); selectedAt = c },
                                c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false).show()
                        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).also { picker ->
                            val today = Calendar.getInstance().apply {
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            picker.datePicker.minDate = today.timeInMillis
                            picker.show()
                        }
                    }) {
                        Icon(Icons.Default.Event, null)
                        Text("  $formatted")
                    }
                }
            },
            confirmButton = {
                TextButton(enabled = title.isNotBlank() && selectedAt.timeInMillis > System.currentTimeMillis(), onClick = {
                    onCreate(title.trim(), notes.trim(), selectedAt.timeInMillis)
                    title = ""; notes = ""; showForm = false
                }) { Text("Set reminder") }
            },
            dismissButton = { TextButton(onClick = { showForm = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun HomeScreen(profileName: String, memories: List<Memory>, networkOnline: Boolean, onProfile: () -> Unit, onNotifications: () -> Unit, onReminders: () -> Unit, openCapture: () -> Unit, onMemory: (Memory) -> Unit) {
    val actionCount = memories.count { !it.action.isNullOrBlank() }
    val latest = memories.firstOrNull()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 90.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.rekol_logo_transparent), "Rekol", Modifier.width(92.dp).height(42.dp).offset(x = (-16).dp), contentScale = ContentScale.Fit)
                Spacer(Modifier.weight(1f)); Icon(Icons.Default.Event, "Reminders", tint = Navy, modifier = Modifier.size(24.dp).clickable(onClick = onReminders)); Spacer(Modifier.width(14.dp)); Icon(Icons.Default.NotificationsNone, "Notifications", tint = Navy, modifier = Modifier.size(25.dp).clickable(onClick = onNotifications)); Spacer(Modifier.width(14.dp)); Box(Modifier.size(30.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Blue, Purple))).clickable(onClick = onProfile), contentAlignment = Alignment.Center) { Text(profileName.trim().firstOrNull()?.uppercase() ?: "R", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold) }
            }
            Surface(Modifier.padding(horizontal = 20.dp), shape = RoundedCornerShape(20.dp), color = if (networkOnline) Color(0xFFE9F9F0) else Color(0xFFFFE9EC)) {
                Row(Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(if (networkOnline) Color(0xFF18B66A) else Coral))
                    Text(if (networkOnline) "Online" else "Offline — captures stay on this device", color = if (networkOnline) Color(0xFF14834E) else Color(0xFFB33B50), fontSize = 11.sp, modifier = Modifier.padding(start = 6.dp))
                }
            }
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text("Let's capture what\nmatters today.", color = Navy, fontSize = 25.sp, fontWeight = FontWeight.Bold, lineHeight = 30.sp)
                Spacer(Modifier.height(18.dp))
                Surface(Modifier.fillMaxWidth().clickable(onClick = openCapture), shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 4.dp) {
                    Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Search, null, tint = Color.Gray); Text("  Ask or capture anything...", color = Color.Gray, modifier = Modifier.weight(1f)); Icon(Icons.Default.Mic, null, tint = Navy) }
                }
                Spacer(Modifier.height(18.dp)); QuickActions(openCapture)
                Spacer(Modifier.height(16.dp))
                Surface(Modifier.fillMaxWidth().clickable(onClick = onReminders), shape = RoundedCornerShape(16.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, Line), shadowElevation = 2.dp) {
                    Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFEAF4FF)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Event, "Calendar", tint = Blue) }
                        Column(Modifier.padding(start = 12.dp).weight(1f)) { Text("Calendar & reminders", color = Navy, fontWeight = FontWeight.Bold, fontSize = 14.sp); Text("Schedule follow-ups and get notified on time", color = Color(0xFF6F7B98), fontSize = 11.sp) }
                        Icon(Icons.Default.ChevronRight, "Open reminders", tint = Blue)
                    }
                }
                Spacer(Modifier.height(24.dp)); Text("Today's Snapshot", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricCard(memories.size.toString(), "items captured", Purple, Modifier.weight(1f)); MetricCard(actionCount.toString(), "smart actions", Color(0xFFFFA117), Modifier.weight(1f))
                }
                if (latest != null) {
                    Spacer(Modifier.height(16.dp))
                    Surface(Modifier.fillMaxWidth().clickable { onMemory(latest) }, shape = RoundedCornerShape(18.dp), color = Color(0xFFF4F0FF), border = androidx.compose.foundation.BorderStroke(1.dp, Purple.copy(alpha = .35f))) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(Color.White), contentAlignment = Alignment.Center) { Icon(Icons.Default.AutoAwesome, "AI insight", tint = Purple) }
                            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                                Text("Smart insight", color = Navy, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Rekol analyzed your latest capture: ${latest.title}", color = Color(0xFF536181), fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                latest.action?.let { Text("Suggested next step: $it", color = Blue, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp)) }
                            }
                            Icon(Icons.Default.ChevronRight, "Open insight", tint = Blue)
                        }
                    }
                }
                Text("Recent Captures", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(top = 26.dp, bottom = 8.dp))
            }
        }
        items(memories.take(6)) { memory -> CompactMemory(memory, onClick = { onMemory(memory) }) }
    }
}

@Composable
private fun QuickActions(onClick: () -> Unit) {
    val actions = listOf("Note" to Icons.Default.Description, "Photo" to Icons.Default.PhotoCamera, "Link" to Icons.Default.Link, "Audio" to Icons.Default.Mic, "File" to Icons.Default.InsertDriveFile, "Screenshot" to Icons.Default.ScreenshotMonitor)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        actions.forEachIndexed { index, (label, icon) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
                Box(Modifier.size(48.dp).clip(RoundedCornerShape(15.dp)).background(listOf(Color(0xFFF0E9FF), Color(0xFFE6FBF7), Color(0xFFE7F3FF), Color(0xFFFFECEF), Color(0xFFF1EDFF), Color(0xFFE7F3FF))[index]), contentAlignment = Alignment.Center) { Icon(icon, null, tint = listOf(Purple, Color(0xFF06B89A), Blue, Coral, Purple, Blue)[index]) }
                Text(label, fontSize = 11.sp, modifier = Modifier.padding(top = 5.dp))
            }
        }
    }
}

@Composable private fun MetricCard(value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(16.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, Line)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Text(value, color = color, fontSize = 25.sp, fontWeight = FontWeight.Bold); Text("  $label", color = Color(0xFF66708B), fontSize = 12.sp) }
    }
}

@Composable private fun CompactMemory(memory: Memory, onClick: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp).background(Color.White, RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(50.dp).background(memory.accent.copy(.12f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Description, null, tint = memory.accent) }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) { Text(memory.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis); Text("${memory.type} • ${memory.time}", fontSize = 11.sp, color = Color.Gray) }
        Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
    }
}

@Composable
private fun MemoryDetailScreen(memory: Memory, attachmentPath: String? = null, onOpenAttachment: (String) -> Unit = {}, onBack: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFF8FCFF), Color(0xFFEAF5FF)))), contentPadding = PaddingValues(bottom = 100.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.ArrowBack, "Back", tint = Navy, modifier = Modifier.clickable(onClick = onBack)); Spacer(Modifier.weight(1f)); Text("Memory details", color = Navy, fontWeight = FontWeight.Bold, fontSize = 20.sp); Spacer(Modifier.weight(1f)); Icon(Icons.Default.MoreHoriz, "More", tint = Navy) }
            Box(Modifier.fillMaxWidth().padding(horizontal = 18.dp).height(190.dp).clip(RoundedCornerShape(20.dp)).background(Brush.linearGradient(listOf(Blue, Cyan, Purple))), contentAlignment = Alignment.Center) { Icon(if (attachmentPath != null) Icons.Default.Attachment else Icons.Default.Description, null, tint = Color.White, modifier = Modifier.size(80.dp)) }
            Surface(Modifier.fillMaxWidth().padding(horizontal = 18.dp), shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp), color = Color.White) { Column(Modifier.padding(18.dp)) { AssistChip(onClick = {}, label = { Text(memory.type) }); Text(memory.title, color = Navy, fontSize = 25.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp)); Text("Today  •  ${memory.time}  •  6 min read", color = Color(0xFF6F7B98), fontSize = 13.sp); Text("🔗  u.ae  •  Sustainability", color = Color(0xFF6F7B98), modifier = Modifier.padding(top = 14.dp)); Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) { AssistChip({}, { Text("UAE") }); AssistChip({}, { Text("Sustainability") }); AssistChip({}, { Text("Research") }) } } }
            Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) { OutlinedButton({}, Modifier.weight(1f)) { Icon(Icons.Default.Edit, null); Text("Edit") }; OutlinedButton({}, Modifier.weight(1f)) { Icon(Icons.Default.Folder, null); Text("Move") }; OutlinedButton({}, Modifier.weight(1f)) { Icon(Icons.Default.Share, null); Text("Share") } }
        }
        if (attachmentPath != null) item { OutlinedButton(onClick = { onOpenAttachment(attachmentPath) }, modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp)) { Icon(Icons.Default.OpenInNew, null); Text("  Open attached file") } }
        item { Surface(Modifier.fillMaxWidth().padding(horizontal = 18.dp), shape = RoundedCornerShape(18.dp), color = Color(0xFFF6F0FF), border = androidx.compose.foundation.BorderStroke(1.dp, Purple.copy(.35f))) { Column(Modifier.padding(18.dp)) { Text("✨  AI summary", color = Navy, fontSize = 19.sp, fontWeight = FontWeight.Bold); Text(memory.summary, color = Color(0xFF4D5B7B), modifier = Modifier.padding(top = 10.dp)); Text("🔊  Listen", color = Blue, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 12.dp)) } } }
        item { Text("Key insights", color = Navy, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(18.dp, 22.dp, 18.dp, 8.dp)) }
        items(listOf("Clean energy investment is expanding rapidly.", "Businesses can benefit from new sustainability incentives.", "Technology and smart infrastructure remain priority sectors.")) { insight -> Surface(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 3.dp), shape = RoundedCornerShape(12.dp), color = Color.White) { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Text("✓", color = Blue, fontWeight = FontWeight.Bold); Text(insight, color = Navy, modifier = Modifier.padding(start = 12.dp).weight(1f)); Icon(Icons.Default.ChevronRight, null, tint = Navy) } } }
        item { Text("Action items", color = Navy, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(18.dp, 22.dp, 18.dp, 8.dp)) }
        items(listOf("Read the complete report", "Share findings with the team")) { action -> Surface(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 3.dp), shape = RoundedCornerShape(12.dp), color = Color.White) { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.CheckBoxOutlineBlank, null, tint = Navy); Text(action, color = Navy, modifier = Modifier.padding(start = 12.dp).weight(1f)); Text("Today", color = Color(0xFF6F7B98)); Icon(Icons.Default.ChevronRight, null, tint = Navy) } } }
    }
}

@Composable
private fun CameraPreview(modifier: Modifier = Modifier, onCaptured: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    val imageCapture = remember { ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build() }
    DisposableEffect(lifecycleOwner) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            runCatching {
                val provider = providerFuture.get()
                val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
            }
        }, ContextCompat.getMainExecutor(context))
        onDispose { runCatching { providerFuture.get().unbindAll() } }
    }
    Box(modifier.clip(RoundedCornerShape(22.dp)).background(Color.Black)) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        Surface(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp).size(70.dp).clickable {
                val target = File(context.filesDir, "captures").apply { mkdirs() }.resolve("capture_${System.currentTimeMillis()}.jpg")
                val options = ImageCapture.OutputFileOptions.Builder(target).build()
                imageCapture.takePicture(options, ContextCompat.getMainExecutor(context), object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) { onCaptured(target.absolutePath) }
                    override fun onError(exception: ImageCaptureException) { Toast.makeText(context, "Unable to capture photo", Toast.LENGTH_SHORT).show() }
                })
            }, shape = CircleShape, color = Color.White, shadowElevation = 8.dp
        ) { Box(Modifier.fillMaxSize().padding(5.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Blue, Purple))), contentAlignment = Alignment.Center) { Icon(Icons.Default.CameraAlt, "Capture photo", tint = Color.White, modifier = Modifier.size(30.dp)) } }
    }
}

@Composable
private fun CaptureSheet(sharedText: String?, cameraReady: Boolean, onDismiss: () -> Unit, onCamera: () -> Unit, onGallery: () -> Unit, onFile: () -> Unit, onPhotoCaptured: (String) -> Unit, onSave: (String, String) -> Unit) {
    var content by remember(sharedText) { mutableStateOf(sharedText.orEmpty()) }
    var reason by remember { mutableStateOf("") }
    var isPrivate by remember { mutableStateOf(false) }
    var captured by remember { mutableStateOf(false) }
    if (cameraReady) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            CameraPreview(Modifier.fillMaxSize(), onCaptured = { path ->
                captured = true
                content = "Photo captured: ${File(path).name}"
                onPhotoCaptured(path)
            })
            Box(Modifier.fillMaxWidth().height(150.dp).align(Alignment.TopCenter).background(Brush.verticalGradient(listOf(Color(0xCC000000), Color.Transparent))))
            OutlinedTextField(
                content,
                { content = it },
                Modifier.fillMaxWidth().statusBarsPadding().padding(start = 18.dp, end = 70.dp, top = 14.dp, bottom = 12.dp),
                placeholder = { Text("Add a description…", color = Color.White.copy(alpha = .8f)) },
                leadingIcon = { Icon(Icons.Default.AutoAwesome, null, tint = Color.White) },
                trailingIcon = { if (content.isNotBlank()) IconButton(onClick = { content = "" }) { Icon(Icons.Default.Clear, "Clear description", tint = Color.White) } },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color.White, unfocusedBorderColor = Color.White.copy(alpha = .75f), cursorColor = Color.White),
                shape = RoundedCornerShape(22.dp)
            )
            Box(Modifier.fillMaxWidth().height(150.dp).align(Alignment.BottomCenter).background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xE6000000)))))
            Row(Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(start = 28.dp, end = 28.dp, bottom = 28.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Surface(Modifier.size(54.dp).clickable(onClick = onGallery), shape = CircleShape, color = Color.White.copy(alpha = .92f)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.PhotoLibrary, "Gallery", tint = Navy) } }
                Spacer(Modifier.size(70.dp))
                Surface(Modifier.size(54.dp).clickable(onClick = onFile), shape = CircleShape, color = Color.White.copy(alpha = .92f)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.InsertDriveFile, "File", tint = Navy) } }
            }
            if (captured) {
                Button(onClick = { onSave(content.trim(), reason.trim()) }, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 104.dp).height(52.dp), enabled = content.isNotBlank(), shape = RoundedCornerShape(26.dp), colors = ButtonDefaults.buttonColors(containerColor = Blue)) {
                    Icon(Icons.Default.AutoAwesome, null); Text("  Analyze and save")
                }
            }
            IconButton(onClick = onDismiss, Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(8.dp)) { Icon(Icons.Default.Close, "Close camera", tint = Color.White, modifier = Modifier.size(28.dp)) }
        }
    } else {
    Box(Modifier.fillMaxSize().background(Color(0x66000000)).clickable(onClick = onDismiss), contentAlignment = Alignment.BottomCenter) {
        Surface(Modifier.fillMaxWidth().clickable(enabled = false) {}, shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp), color = Color.White) {
            Column(Modifier.padding(22.dp)) {
                Box(Modifier.width(45.dp).height(5.dp).background(Line, CircleShape).align(Alignment.CenterHorizontally)); Spacer(Modifier.height(18.dp))
                Text("Capture this", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("Capture a moment or choose something from your device.", color = Color.Gray, modifier = Modifier.padding(top = 5.dp, bottom = 14.dp))
                if (cameraReady) CameraPreview(onCaptured = onPhotoCaptured)
                else Surface(Modifier.fillMaxWidth().height(220.dp), shape = RoundedCornerShape(22.dp), color = Color(0xFFF3F7FF), border = androidx.compose.foundation.BorderStroke(1.dp, Line)) { Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize()) { Icon(Icons.Default.CameraAlt, null, tint = Blue, modifier = Modifier.size(42.dp)); Text("Allow camera access to capture", color = Navy, modifier = Modifier.padding(top = 8.dp)); TextButton(onClick = onCamera) { Text("Enable camera") } } }
                Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CaptureOption(Icons.Default.PhotoLibrary, "Gallery", onGallery, Modifier.weight(1f))
                    CaptureOption(Icons.Default.InsertDriveFile, "File", onFile, Modifier.weight(1f))
                }
                OutlinedTextField(content, { content = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Content") }, minLines = 3, shape = RoundedCornerShape(16.dp))
                OutlinedTextField(reason, { reason = it }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp), label = { Text("Why are you saving this? (optional)") }, shape = RoundedCornerShape(16.dp))
                Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Private memory", Modifier.weight(1f), fontSize = 14.sp)
                    Switch(checked = isPrivate, onCheckedChange = { isPrivate = it })
                }
                Button(onClick = { onSave(content.trim(), reason.trim()) }, enabled = content.isNotBlank(), modifier = Modifier.fillMaxWidth().padding(top = 20.dp).height(54.dp), shape = RoundedCornerShape(16.dp)) { Icon(Icons.Default.AutoAwesome, null); Text("  Save and understand") }
            }
        }
    }
    }
}

@Composable
private fun CaptureOption(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier.height(72.dp).clickable(onClick = onClick), shape = RoundedCornerShape(16.dp), color = Color(0xFFF3F7FF), border = androidx.compose.foundation.BorderStroke(1.dp, Line)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = Blue, modifier = Modifier.size(25.dp))
            Text(label, color = Navy, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun AnalysisScreen(captureLabel: String) {
    var step by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while (step < 7) { delay(650); step++ } }
    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.White, Color(0xFFF4F7FF), Color(0xFFE9FAF2)))).padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(55.dp)); Image(painterResource(R.drawable.rekol_mobile_icon), "Analyzing", Modifier.size(100.dp)); Text("Analyzing...", fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Text(captureLabel, color = Navy, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(top = 12.dp))
        CircularProgressIndicator(color = Blue, strokeWidth = 4.dp, modifier = Modifier.padding(top = 22.dp).size(42.dp))
        Text("Understanding your content", color = Color.Gray, modifier = Modifier.padding(top = 18.dp, bottom = 20.dp))
        val labels = listOf("Securing original content", "Extracting or transcribing", "Understanding context", "Identifying topics", "Creating summary", "Detecting possible actions", "Adding to your library")
        labels.forEachIndexed { index, label -> Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) { Icon(if (index < step) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, null, tint = if (index < step) Color(0xFF20C77A) else Line); Text(label, Modifier.padding(start = 12.dp)) } }
        Spacer(Modifier.weight(1f)); Surface(shape = RoundedCornerShape(18.dp), color = Color.White) { Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text("Almost done...", fontWeight = FontWeight.Bold); Text("Turning your capture into insights", color = Color.Gray, fontSize = 12.sp) } }
    }
}

@Composable
private fun JournalScreen(memories: List<Memory>, attachmentFiles: Map<String, String>, onMemory: (Memory) -> Unit) {
    var selectedFilter by remember { mutableStateOf("All") }
    var visibleCount by remember { mutableIntStateOf(20) }
    val filteredMemories = memories.sortedByDescending { it.time }.filter { selectedFilter == "All" || memoryCategory(it, attachmentFiles) == selectedFilter }
    val visibleMemories = filteredMemories.take(visibleCount)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 90.dp)) {
        item { PageHeader("Today", "Tue, 22 Sep 2026") { Icon(Icons.Default.CalendarMonth, null, tint = Navy) }; JournalFilterChips(selectedFilter) { selectedFilter = it; visibleCount = 20 } }
        if (filteredMemories.isEmpty()) item { Text("No ${selectedFilter.lowercase()} captured yet", color = Navy, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(24.dp)); Text("Capture a note, link, photo, audio, or document and it will appear here automatically.", color = Color(0xFF6F7B98), modifier = Modifier.padding(horizontal = 24.dp)) }
        items(visibleMemories) { memory ->
            Row(Modifier.padding(horizontal = 18.dp, vertical = 5.dp).clickable { onMemory(memory) }) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) { Box(Modifier.size(9.dp).background(memory.accent, CircleShape)); Box(Modifier.width(2.dp).height(180.dp).background(Line)) }
                Surface(Modifier.weight(1f).padding(start = 12.dp), shape = RoundedCornerShape(16.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, Line)) {
                    Column(Modifier.padding(14.dp)) { Text(memory.time, fontSize = 10.sp, color = Color.Gray); Row(verticalAlignment = Alignment.CenterVertically) { Text(memory.title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis); Icon(Icons.Default.ChevronRight, null, tint = Color.Gray) }; AssistChip(onClick = {}, label = { Text(memoryCategory(memory, attachmentFiles)) }); Text(memory.summary, fontSize = 13.sp, maxLines = 3, overflow = TextOverflow.Ellipsis); memory.action?.let { Text("Action: $it", color = Blue, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp).background(Color(0xFFE9F4FF), RoundedCornerShape(7.dp)).padding(6.dp)) } }
                }
            }
        }
        if (visibleMemories.size < filteredMemories.size) item { OutlinedButton(onClick = { visibleCount += 20 }, modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp)) { Text("Load more (${filteredMemories.size - visibleMemories.size} remaining)") } }
    }
}

private fun memoryCategory(memory: Memory, attachmentFiles: Map<String, String>): String {
    val content = memory.summary.lowercase()
    val path = memory.id?.let { attachmentFiles[it] }.orEmpty().lowercase()
    val extension = path.substringAfterLast('.', "")
    return when {
        extension in setOf("jpg", "jpeg", "png", "gif", "webp", "heic", "mp4", "mov", "mkv", "mp3", "wav", "m4a", "aac") ||
            content.contains("photo captured") || content.contains("attached media") -> "Media"
        extension in setOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "csv") ||
            content.contains("attached file") -> "Files"
        Regex("https?://|www\\.").containsMatchIn(content) -> "Links"
        !memory.action.isNullOrBlank() -> "Actions"
        else -> "Notes"
    }
}

@Composable
private fun JournalFilterChips(selected: String, onSelected: (String) -> Unit) {
    val labels = listOf("All", "Notes", "Media", "Links", "Files", "Actions")
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        labels.forEach { label -> FilterChip(selected = selected == label, onClick = { onSelected(label) }, label = { Text(label, fontSize = 11.sp) }) }
    }
}

@Composable
private fun FilterChips(labels: List<String>) { Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { labels.forEachIndexed { i, label -> FilterChip(i == 0, {}, { Text(label, fontSize = 11.sp) }) } } }

private fun openLocalAttachment(context: android.content.Context, path: String) {
    runCatching {
        val file = File(path)
        if (!file.exists()) error("File no longer exists")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val mime = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension.lowercase()) ?: "application/octet-stream"
        context.startActivity(Intent(Intent.ACTION_VIEW, uri).apply { setDataAndType(uri, mime); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) })
    }.onFailure { Toast.makeText(context, "Unable to open this attachment", Toast.LENGTH_SHORT).show() }
}

@Composable
private fun LibraryScreen(memories: List<Memory>, attachmentFiles: Map<String, String>, onOpenAttachment: (String) -> Unit) {
    val collections = memories.groupBy { it.type.ifBlank { "Notes" } }.entries.sortedByDescending { it.value.size }.map { (type, items) -> type to "${items.size} item${if (items.size == 1) "" else "s"}" }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 90.dp)) {
        item {
            Text("Library", color = Navy, fontSize = 30.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp)); Text("Everything you’ve captured, organized by Rekol.", color = Color(0xFF6F7B98), fontSize = 15.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp))
            OutlinedTextField("", {}, Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp), placeholder = { Text("Search your memories...") }, leadingIcon = { Icon(Icons.Default.Search, null) }, trailingIcon = { Icon(Icons.Default.Mic, null, tint = Navy) }, shape = RoundedCornerShape(18.dp)); FilterChips(listOf("All", "Notes", "Images", "Videos", "Audio", "Docs", "Links")); Spacer(Modifier.height(12.dp))
        }
        if (collections.isEmpty()) item { Text("Your library is empty", color = Navy, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(24.dp)); Text("Capture a note, link, photo, or file to create your first collection.", color = Color.Gray, modifier = Modifier.padding(horizontal = 24.dp)) }
        items(collections) { (title, subtitle) ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 7.dp).background(Color.White, RoundedCornerShape(15.dp)).padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(62.dp).background(Brush.linearGradient(listOf(Blue.copy(.75f), Cyan.copy(.55f))), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Collections, null, tint = Color.White) }
                Column(Modifier.weight(1f).padding(start = 13.dp)) { Text(title, fontWeight = FontWeight.Bold); Text(subtitle, fontSize = 11.sp, color = Color.Gray) }; Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
            }
        }
        item { Text("Saved memories", color = Navy, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 18.dp, top = 18.dp, bottom = 5.dp)) }
        items(memories) { memory ->
            val path = memory.id?.let { attachmentFiles[it] }
            Surface(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 5.dp).clickable(enabled = path != null) { path?.let(onOpenAttachment) }, shape = RoundedCornerShape(15.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, Line)) {
                Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (path != null) Icons.Default.Attachment else Icons.Default.Description, null, tint = Blue, modifier = Modifier.size(27.dp))
                    Column(Modifier.weight(1f).padding(start = 12.dp)) { Text(memory.title, color = Navy, fontWeight = FontWeight.Bold); Text(if (path != null) "Attachment available · tap to open" else memory.type, color = Color(0xFF6F7B98), fontSize = 12.sp) }
                    if (path != null) Icon(Icons.Default.OpenInNew, "Open attachment", tint = Blue)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InsightsScreen(memories: List<Memory>) {
    val total = memories.size
    val actions = memories.count { !it.action.isNullOrBlank() }
    val topics = memories.flatMap { listOf(it.type, it.action).filterNotNull() }.distinct().take(8)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)) {
        item { PageHeader("Insights") }
        item { InsightCard("Your Memories") { Box(Modifier.size(160.dp).background(Brush.sweepGradient(listOf(Blue, Cyan, Color(0xFFFFC23C), Coral, Purple, Blue)), CircleShape), contentAlignment = Alignment.Center) { Box(Modifier.size(98.dp).background(Color.White, CircleShape), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(total.toString(), fontSize = 28.sp, fontWeight = FontWeight.Bold); Text("items", fontSize = 12.sp) } } } } }
        item { InsightCard("Your Topics") { if (topics.isEmpty()) Text("Topics will appear as you capture memories.", color = Color.Gray) else FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) { topics.forEach { AssistChip({}, { Text(it) }) } } } }
        item { InsightCard("Actions Overview") { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { MiniStat(actions.toString(), "With actions", Color(0xFF11B98A), Modifier.weight(1f)); MiniStat((total - actions).coerceAtLeast(0).toString(), "Without actions", Color(0xFFFF8A22), Modifier.weight(1f)); MiniStat(total.toString(), "Total", Purple, Modifier.weight(1f)) } } }
        item { Surface(Modifier.fillMaxWidth().padding(vertical = 10.dp), shape = RoundedCornerShape(18.dp), color = Color(0xFFEDF8FF)) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Image(painterResource(R.drawable.rekol_mobile_icon), null, Modifier.size(46.dp)); Text("  “You capture ideas.\n  We help you make them happen.”", fontWeight = FontWeight.Medium) } } }
    }
}

@Composable private fun InsightCard(title: String, content: @Composable () -> Unit) { Surface(Modifier.fillMaxWidth().padding(vertical = 8.dp), shape = RoundedCornerShape(18.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, Line)) { Column(Modifier.padding(18.dp)) { Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp); Spacer(Modifier.height(16.dp)); Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { content() } } } }
@Composable private fun MiniStat(value: String, label: String, color: Color, modifier: Modifier) { Column(modifier.background(color.copy(.1f), RoundedCornerShape(13.dp)).padding(13.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 24.sp); Text(label, fontSize = 10.sp, color = Color.Gray) } }
