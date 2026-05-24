plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.futsch1.medtimer.core.ui"
    compileSdk = 36

    defaultConfig {
        minSdk = 28
        // Use this deprecated setting because Android Lint will not pick up androidResources.localeFilters correctly
        @Suppress("DEPRECATION")
        resConfigs("en,ar,bg,cs,da,de,el,es,fi,fr,hu,it,iw,nl,pl,pt,ru,sv,ta,tr,uk,zh-rCN,zh-rTW")
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

dependencies {
    api(project(":core:domain"))
    implementation(project(":core:common"))
    implementation(project(":core:datastore"))
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.material)
    implementation(libs.androidx.appcompat)
    implementation(libs.icondialog)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
}
