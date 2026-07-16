plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.futsch1.medtimer.core.domain"
    compileSdk = 37

    defaultConfig {
        minSdk = 28
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildTypes {
        debug {
            enableUnitTestCoverage = true
        }
    }
}

dependencies {
    api(libs.kotlinx.coroutines.core)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
}
