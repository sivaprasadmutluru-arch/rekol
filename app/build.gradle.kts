import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.rekol.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.rekol.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 5
        versionName = "0.1.0"
        buildConfigField("String", "REKOL_API_URL", "\"https://wefyx.pro/rekol-api\"")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    signingConfigs {
        create("release") {
            val localSigningProperties = Properties().apply {
                val localFile = rootProject.file("output/android/rekol-upload-keystore.properties")
                if (localFile.exists()) localFile.inputStream().use(::load)
            }
            fun signingValue(name: String): String? =
                providers.gradleProperty(name).orNull
                    ?: System.getenv(name)
                    ?: localSigningProperties.getProperty(name)

            storeFile = rootProject.file(signingValue("REKOL_KEYSTORE_FILE") ?: "rekol-release.keystore")
            storePassword = signingValue("REKOL_KEYSTORE_PASSWORD")
            keyAlias = signingValue("REKOL_KEY_ALIAS")
            keyPassword = signingValue("REKOL_KEY_PASSWORD")
        }
    }
    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.camera:camera-camera2:1.4.2")
    implementation("androidx.camera:camera-lifecycle:1.4.2")
    implementation("androidx.camera:camera-view:1.4.2")
    implementation("com.google.firebase:firebase-messaging:24.1.0")
    implementation("com.google.firebase:firebase-auth:23.1.0")
    implementation("com.google.firebase:firebase-analytics:22.1.2")
    implementation("com.google.firebase:firebase-ai:17.17.0")
    implementation("com.google.mlkit:text-recognition:16.0.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
