plugins {
    id("com.android.application")
    id("androidx.room")
    id("com.github.triplet.play") version "3.10.1"
    id("androidx.navigation.safeargs")
    id("org.jetbrains.kotlin.android")
    id("jacoco")
    id("org.sonarqube") version "5.0.0.4638"
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
        versionCode = 55
        versionName = "1.8.9"
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
        debug {
            enableUnitTestCoverage = true
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
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    val roomVersion = "2.6.1"
    val androidXNavigationVersion = "2.7.7"
    val preferenceXVersion = "1.1.0"

    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.navigation:navigation-fragment-ktx:$androidXNavigationVersion")
    implementation("androidx.navigation:navigation-ui-ktx:$androidXNavigationVersion")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("androidx.lifecycle:lifecycle-extensions:2.2.0")
    implementation("androidx.work:work-runtime:2.9.0")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.2")
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.tracing:tracing:1.2.0")
    implementation("org.mockito:mockito-core:5.12.0")
    implementation("com.github.skydoves:colorpickerview:2.3.0")
    implementation("com.takisoft.preferencex:preferencex:$preferenceXVersion")
    implementation("com.github.wwdablu:SimplyPDF:2.1.1")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("com.github.evrencoskun:TableView:v0.8.9.4")
    implementation("com.androidplot:androidplot-core:1.5.10")
    implementation("com.github.AppIntro:AppIntro:6.3.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-inline:5.2.0")
    testImplementation("org.robolectric:robolectric:4.12.2")

    androidTestImplementation("androidx.test.ext:junit:1.2.0-rc01")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.0-rc01")
    androidTestImplementation("androidx.test.espresso:espresso-contrib:3.6.0-rc01")
    androidTestImplementation("androidx.test:rules:1.6.0-rc01")

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
sonar {
    properties {
        property("sonar.projectKey", "Futsch1_medTimer")
        property("sonar.organization", "futsch1")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.gradle.skipCompile", "true")
        property("sonar.android.lint.report", "build/reports/lint-results-debug.xml")
        property(
            "sonar.coverage.jacoco.xmlReportPaths",
            "build/reports/coverage/test/debug/report.xml"
        )
    }
}
tasks.withType(Test::class) {
    configure<JacocoTaskExtension> {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
}
