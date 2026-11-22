import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

// =========================================================================
// AUTO-INCREMENT VERSI OTOMATIS SAAT BUILD RELEASE
// =========================================================================
val versionPropsFile = file("version.properties")
val versionProps = Properties()
if (versionPropsFile.exists()) {
    FileInputStream(versionPropsFile).use { versionProps.load(it) }
}

var vMajor = versionProps.getProperty("VERSION_MAJOR", "3").toInt()
var vMinor = versionProps.getProperty("VERSION_MINOR", "0").toInt()
var vPatch = versionProps.getProperty("VERSION_PATCH", "0").toInt()
var vCode = versionProps.getProperty("VERSION_CODE", "1").toInt()

val isReleaseBuild = gradle.startParameter.taskNames.any {
    it.contains("Release", ignoreCase = true)
}

if (isReleaseBuild) {
    vCode += 1
    vPatch += 1
    versionProps.setProperty("VERSION_MAJOR", vMajor.toString())
    versionProps.setProperty("VERSION_MINOR", vMinor.toString())
    versionProps.setProperty("VERSION_PATCH", vPatch.toString())
    versionProps.setProperty("VERSION_CODE", vCode.toString())
    FileOutputStream(versionPropsFile).use {
        versionProps.store(it, "Auto-incremented on release build")
    }
    println("==> Release Build Version Updated: v$vMajor.$vMinor.$vPatch (code: $vCode)")
}

val appVersionName = "$vMajor.$vMinor.$vPatch"
val appVersionCode = vCode

android {
    namespace = "com.phpnuxbill.mobile"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.phpnuxbill.mobile"
        minSdk = 24
        targetSdk = 35
        versionCode = appVersionCode
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core AndroidX & Lifecycle MVVM
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.10.1")

    // Jetpack Compose BOM & Material 3
    val composeBom = platform("androidx.compose:compose-bom:2025.02.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.8.9")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.1")

    // Networking (OkHttp & Retrofit)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.google.code.gson:gson:2.12.1")

    // DataStore Preferences (Session Token, Multi-Server, Custom Logo & Theme)
    implementation("androidx.datastore:datastore-preferences:1.1.3")

    // Coil Image Loading (Menampilkan Logo Server & Logo Galeri Lokal)
    implementation("io.coil-kt:coil-compose:2.7.0")
}
