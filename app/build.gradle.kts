import org.jetbrains.kotlin.gradle.dsl.JvmTarget

/**
 * An unset GitHub Actions secret arrives as an empty string rather than as an
 * absent variable, so `?:` alone would accept "" as a real value and then fail on
 * an empty keystore path. Blank is treated as absent here.
 */
fun envOrNull(name: String): String? = System.getenv(name)?.takeIf { it.isNotBlank() }

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.ymopuri.qsactions"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ymopuri.qsactions"
        // requestAddTileService() and the intent-based Shizuku fork both require 33.
        minSdk = 33
        targetSdk = 35
        versionCode = 2
        versionName = "0.2.0"
    }

    // The checked-in key signs releases so anyone can build an identical, installable
    // APK — no secrets to configure. It is deliberately not a secret: see
    // signing/README.md for what that does and doesn't cost.
    //
    // Every value can be overridden by environment variable, so moving to a private
    // key later means setting four secrets in CI and changing nothing here.
    val defaultKeystore = rootProject.file("signing/release.jks")
    val keystorePath: String? = envOrNull("KEYSTORE_FILE")
        ?: defaultKeystore.takeIf { it.exists() }?.absolutePath

    signingConfigs {
        if (keystorePath != null) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = envOrNull("KEYSTORE_PASSWORD") ?: "qsactions"
                keyAlias = envOrNull("KEY_ALIAS") ?: "qsactions"
                keyPassword = envOrNull("KEY_PASSWORD") ?: "qsactions"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        aidl = true
        buildConfig = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}
