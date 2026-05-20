plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.futsch1.medtimer.core.domain"
    compileSdk = 36

    defaultConfig {
        minSdk = 28
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)
}
