plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.androidx.navigation.safeargs)
}

android {
    namespace = "com.futsch1.medtimer.feature.ui"
    compileSdk = 36

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
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    api(project(":core:domain"))
    implementation(project(":core:common"))
    implementation(project(":core:datastore"))
    implementation(project(":core:ui"))
    implementation(project(":feature:reminders"))
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidplot)
    implementation(libs.tableview)
    implementation(libs.simply.pdf)
    implementation(libs.color.picker)
    implementation(libs.calendar)
    implementation(libs.icondialog)
    implementation(libs.flexbox)
    implementation(libs.preferencex)
    implementation(libs.preferencex.ringtone)
    implementation(libs.espresso.idling.resource)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
}
