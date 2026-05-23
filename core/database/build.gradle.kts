plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.androidx.room)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

room {
    schemaDirectory("$projectDir/schemas")
}

android {
    namespace = "com.futsch1.medtimer.core.database"
    compileSdk = 36

    defaultConfig {
        minSdk = 28
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    sourceSets {
        getByName("main") {
            assets.directories.add("$projectDir/schemas")
        }
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    api(project(":core:domain"))
    implementation(libs.androidx.room.runtime)
    implementation(libs.gson)
    implementation(libs.hilt.android)
    ksp(libs.androidx.room.compiler)
    ksp(libs.hilt.compiler)
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    testImplementation(libs.junit4)
    testImplementation(kotlin("test-junit"))
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.room.testing)
}
