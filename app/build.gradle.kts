plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    // FIX 1: Wajib ada di Kotlin 2.0+
    id("org.jetbrains.kotlin.plugin.compose") 
}

android {
    namespace = "vtsen.hashnode.dev.newemptycomposeapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "vtsen.hashnode.dev.newemptycomposeapp"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    buildFeatures {
        compose = true
    }
    
    // FIX 2: HAPUS block 'composeOptions' (kotlinCompilerExtensionVersion).
    // Di Kotlin 2.0, compiler compose sudah otomatis ikut versi Kotlin.
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2024.06.00")) // Update BOM ke versi baru biar aman
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    // --- LIBRARY KITA ---
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("androidx.media3:media3-exoplayer:1.2.1")
    implementation("androidx.media3:media3-ui:1.2.1")
    implementation("androidx.media3:media3-common:1.2.1")
    implementation("io.coil-kt:coil-compose:2.5.0")
}