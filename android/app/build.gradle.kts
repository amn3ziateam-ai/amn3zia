import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Telegram API credentials (my.telegram.org) are read from local.properties so
// they never get committed to source control. Add to <repo>/android/local.properties:
//   TELEGRAM_API_ID=123456
//   TELEGRAM_API_HASH=abcdef0123456789abcdef0123456789
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "com.amn3zia.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.amn3zia.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("int", "TELEGRAM_API_ID", localProps.getProperty("TELEGRAM_API_ID", "0"))
        buildConfigField("String", "TELEGRAM_API_HASH", "\"${localProps.getProperty("TELEGRAM_API_HASH", "")}\"")

        // ABI filters: TDLib prebuilt .so must match. Build for these or supply your own.
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
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
    composeOptions {
        // Compose compiler version compatible with Kotlin 1.9.24
        // (see https://developer.android.com/jetpack/androidx/releases/compose-kotlin)
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    // TDLib ships native .so per ABI — place them under src/main/jniLibs/<abi>/libtdjni.so
    sourceSets["main"].jniLibs.srcDirs("src/main/jniLibs")

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.lifecycle:lifecycle-process:2.8.1")

    // Biometric authentication (PIN / fingerprint / face)
    implementation("androidx.biometric:biometric:1.1.0")
    // Persistent settings (DataStore Preferences)
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Secure local storage
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("net.zetetic:android-database-sqlcipher:4.5.4")
    implementation("androidx.sqlite:sqlite-ktx:2.4.0")

    // TDLib Java bindings (org.drinkless.tdlib.*) — the generated Client.java/TdApi.java
    // sources are placed directly under src/main/java by the build (see
    // .github/workflows/build-android.yml / docs/BUILD_TDLIB.md) and compiled with the app,
    // so no separate jar dependency is needed.

    debugImplementation("androidx.compose.ui:ui-tooling")
}
