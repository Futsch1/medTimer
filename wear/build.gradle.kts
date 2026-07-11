plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.futsch1.medtimer.wear"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.futsch1.medtimer.wear"
        // Wear Compose Material3 targets Wear OS 3+.
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
    }
    lint {
        abortOnError = true
        warningsAsErrors = true
        disable.add("IconLocation")
        disable.add("GradleDependency")
        disable.add("AndroidGradlePluginVersion")
        disable.add("OldTargetApi")
    }
}

// Standalone remote-control app for the watch, no instrumented test suite (mirrors the
// feature modules, whose androidTest suite lives only in :app).
androidComponents {
    beforeVariants(selector().all()) {
        it.androidTest.enable = false
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.activity.compose)
    implementation(libs.wear.compose.material3)
    implementation(libs.wear.compose.foundation)
    implementation(libs.wear.tiles)
    implementation(libs.wear.tiles.material)
    implementation(libs.wear.protolayout)
    implementation(libs.wear.protolayout.material)
    implementation(libs.androidx.concurrent.futures)
    implementation(libs.play.services.wearable)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.gson)

    coreLibraryDesugaring(libs.desugar.jdk.libs)
}
