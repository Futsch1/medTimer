plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.futsch1.medtimer.core.ui"
    compileSdk = 37

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
    buildTypes {
        debug {
            enableUnitTestCoverage = true
        }
    }
    buildFeatures {
        compose = true
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
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

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.kotlinx.collections.immutable)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit4)
    testImplementation(kotlin("test-junit"))
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.compose.ui.test.manifest)
}
