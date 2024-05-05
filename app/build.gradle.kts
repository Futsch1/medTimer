plugins {
    id("com.android.application")
    id("androidx.room")
    id("com.github.triplet.play") version "3.9.1"
    id("androidx.navigation.safeargs")
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
        versionCode = 41
        versionName = "1.8.0"
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
    @Suppress("UnstableApiUsage")
    androidResources {
        generateLocaleConfig = true
    }
    @Suppress("UnstableApiUsage")
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
    sourceSets {
        getByName("main") {
            // There seems to be an issue with safeargs and Android Studio. Generated classes are not detected by the IDE and marked
            // as an error. This statement fixes it, but causes tests to fail (since they use the generated release classes). So
            // I will leave this commented here to be enabled during coding, but it may not be committed.
            //java.srcDir("build/generated/source/navigation-args/debug")
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
    implementation("androidx.core:core-ktx:1.13.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.tracing:tracing:1.2.0")
    implementation("org.mockito:mockito-core:5.11.0")
    implementation("com.github.skydoves:colorpickerview:2.3.0")
    implementation("com.takisoft.preferencex:preferencex:$preferenceXVersion")
    implementation("com.takisoft.preferencex:preferencex-ringtone:$preferenceXVersion")
    implementation("com.github.wwdablu:SimplyPDF:2.1.1")
    implementation("com.google.code.gson:gson:2.10.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-inline:5.2.0")
    testImplementation("org.robolectric:robolectric:4.12.1")

    androidTestImplementation("androidx.test.ext:junit:1.2.0-alpha04")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.0-alpha04")
    androidTestImplementation("androidx.test.espresso:espresso-contrib:3.6.0-alpha04")
    androidTestImplementation("androidx.test:rules:1.6.0-alpha04")

    annotationProcessor("androidx.room:room-compiler:$roomVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.8.0")

    implementation("com.github.evrencoskun:TableView:v0.8.9.4")

    implementation("com.androidplot:androidplot-core:1.5.10")
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
