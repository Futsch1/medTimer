plugins {
    id("com.android.application")
    id("androidx.room")
    id("com.github.triplet.play") version "3.9.1"
}

room {
    schemaDirectory("$projectDir/schemas")
}

android {
    namespace = "com.futsch1.medtimer"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.futsch1.medtimer"
        minSdk = 33
        targetSdk = 34
        versionCode = 29
        versionName = "1.7.0-rc.1"
        setProperty("archivesBaseName", "MedTimer")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    androidResources {
        generateLocaleConfig = true
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    val roomVersion = "2.6.1"
    val androidXNavigationVersion = "2.7.7"
    val preferenceXVersion = "1.1.0"

    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.navigation:navigation-fragment:$androidXNavigationVersion")
    implementation("androidx.navigation:navigation-ui:$androidXNavigationVersion")
    implementation("androidx.preference:preference:1.2.1")
    implementation("androidx.lifecycle:lifecycle-extensions:2.2.0")
    implementation("androidx.work:work-runtime:2.9.0")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("org.mockito:mockito-core:5.11.0")
    implementation("com.github.skydoves:colorpickerview:2.3.0")
    implementation("com.takisoft.preferencex:preferencex:$preferenceXVersion")
    implementation("com.takisoft.preferencex:preferencex-ringtone:$preferenceXVersion")
    implementation("com.github.wwdablu:SimplyPDF:2.1.1")
    implementation("com.google.code.gson:gson:2.10.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-inline:5.2.0")
    testImplementation("org.robolectric:robolectric:4.12.1")

    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    annotationProcessor("androidx.room:room-compiler:$roomVersion")
}

play {
    track.set("internal")
    defaultToAppBundles.set(true)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}
