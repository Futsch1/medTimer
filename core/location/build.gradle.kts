plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.futsch1.medtimer.core.location"
    compileSdk = 37

    defaultConfig {
        minSdk = 28
    }
    flavorDimensions += "distribution"
    productFlavors {
        create("full") { dimension = "distribution" }
        create("foss") { dimension = "distribution" }
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

// No instrumented tests live in this module; disable the androidTest variant
// to avoid building a pointless test APK.
androidComponents {
    beforeVariants(selector().all()) {
        it.androidTest.enable = false
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:datastore"))
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.preference.ktx)
    "fullImplementation"(libs.play.services.location)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
}
