import java.net.URI
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

// Firebase es opcional: solo se activa si existe google-services.json.
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

val localSettings = Properties().apply {
    rootProject.file("local.properties").takeIf { it.isFile }?.inputStream()?.use { load(it) }
}
fun sigraProperty(name: String, fallback: String = ""): String =
    providers.gradleProperty(name).orNull ?: localSettings.getProperty(name) ?: fallback

fun javaLiteral(value: String): String = "\"" + value.replace("\\", "\\\\")
    .replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r") + "\""

val sigraBaseUrl = sigraProperty("sigra.baseUrl", "https://sigra.azyrondev.com/").trimEnd('/') + "/"
val sigraApiKey = sigraProperty("sigra.apiKey")
val sigraEventsApiKey = sigraProperty("sigra.eventsApiKey", sigraApiKey)
val serverUri = URI(sigraBaseUrl)
require(serverUri.scheme in listOf("http", "https") && !serverUri.host.isNullOrBlank() && serverUri.rawQuery == null && serverUri.rawFragment == null) {
    "Configura sigra.baseUrl como una URL HTTP(S) sin consulta ni fragmento."
}

android {
    namespace = "com.sigra.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sigra.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "1.0.0.0"

        buildConfigField("String", "SIGRA_BASE_URL", javaLiteral(sigraBaseUrl))
        buildConfigField("String", "SIGRA_API_KEY", javaLiteral(sigraApiKey))
        buildConfigField("String", "SIGRA_EVENTS_API_KEY", javaLiteral(sigraEventsApiKey))
    }

    val signingStore = sigraProperty("sigra.signingStoreFile")
    val signingPassword = sigraProperty("sigra.signingStorePassword")
    val signingAlias = sigraProperty("sigra.signingKeyAlias")
    val signingKeyPassword = sigraProperty("sigra.signingKeyPassword")
    val canSignRelease = listOf(signingStore, signingPassword, signingAlias, signingKeyPassword).all { it.isNotBlank() }
    signingConfigs {
        if (canSignRelease) {
            create("sigraRelease") {
                storeFile = rootProject.file(signingStore)
                storePassword = signingPassword
                keyAlias = signingAlias
                keyPassword = signingKeyPassword
            }
        }
    }

    buildTypes {
        release {
            if (canSignRelease) signingConfig = signingConfigs.getByName("sigraRelease")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE*",
                "META-INF/NOTICE*",
            )
        }
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-service:2.8.7")
    implementation("androidx.activity:activity-compose:1.10.0")

    implementation(platform("androidx.compose:compose-bom:2025.01.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.datastore:datastore-preferences:1.1.1")

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-kotlinx-serialization:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("org.osmdroid:osmdroid-android:6.1.20")

    // Push opcional. Sin google-services.json la app funciona con polling.
    implementation("com.google.firebase:firebase-messaging:24.1.0")
}
