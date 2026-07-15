plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.futsch1.medtimer.feature.reminders"
    compileSdk = 37

    defaultConfig {
        minSdk = 28
        // Use this deprecated setting because Android Lint will not pick up androidResources.localeFilters correctly
        @Suppress("DEPRECATION")
        resConfigs("en,ar,bg,cs,da,de,el,es,fi,fr,hu,it,iw,nl,pl,pt,ru,sv,ta,tr,uk,zh-rCN,zh-rTW")
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
    lint {
        abortOnError = true
        warningsAsErrors = true
        disable.add("IconLocation")
        disable.add("GradleDependency")
        disable.add("AndroidGradlePluginVersion")
        disable.add("OldTargetApi")
    }
}

// This module has no instrumented tests (the suite lives in :app), and its
// notification/alarm/widget layouts reference :app-provided resources (theme,
// icons, app name), so a standalone androidTest APK cannot link them. Disable
// the androidTest variant rather than build a pointless, broken test APK.
androidComponents {
    beforeVariants(selector().all()) {
        it.androidTest.enable = false
    }
}

dependencies {
    api(project(":core:domain"))
    implementation(project(":core:common"))
    implementation(project(":core:datastore"))
    implementation(project(":core:ui"))
    implementation(project(":core:location"))
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.preferencex.ringtone)
    implementation(libs.espresso.idling.resource)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
}
