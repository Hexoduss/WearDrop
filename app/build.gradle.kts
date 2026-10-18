plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.weardrop"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.weardrop"
        minSdk = 26
        targetSdk = 34
        versionCode = 3
        versionName = "3.0"

        // Only ship English (default) and Italian resources.
        resourceConfigurations += listOf("en", "it")
    }

    signingConfigs {
        // Fixed key committed to the repo: every build (including CI) has the same
        // signature, so new versions install over old ones without losing data.
        getByName("debug") {
            storeFile = file("weardrop-debug.keystore")
            storePassword = "android"
            keyAlias = "armadio"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.5")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.5")
    implementation("androidx.navigation:navigation-compose:2.8.0")
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // On-device image labeling (bundled model: free, works offline).
    implementation("com.google.mlkit:image-labeling:17.0.9")

    // Weather (Open-Meteo) and images.
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.2")
    implementation("io.coil-kt:coil-compose:2.7.0")
}
