plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.futsch1.medtimer.feature.reminders.api"
    compileSdk = 37

    defaultConfig {
        minSdk = 28
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

androidComponents {
    beforeVariants(selector().all()) {
        it.androidTest.enable = false
    }
}

dependencies {
    api(project(":core:domain"))
    implementation(project(":core:common"))
    implementation(project(":core:datastore"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.preference.ktx)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
}
