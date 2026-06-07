plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.futsch1.medtimer.core.datastore"
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

dependencies {
    api(project(":core:domain"))
    implementation(project(":core:common"))
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.gson)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.preference.ktx)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
}
