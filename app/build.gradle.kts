plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.androidx.room)
    alias(libs.plugins.triplet.play)
    alias(libs.plugins.androidx.navigation.safeargs)
    id("jacoco")
    alias(libs.plugins.sonarqube)
    alias(libs.plugins.robolectric.junit5)
}

room {
    schemaDirectory("$projectDir/schemas")
}

android {
    namespace = "com.futsch1.medtimer"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.futsch1.medtimer"
        minSdk = 28
        multiDexEnabled = true
        targetSdk = 36
        versionCode = 159
        versionName = "1.22.7"
        base.archivesName = "MedTimer"
        // Use this deprecated setting because Android Lint will not pick up androidResources.localeFilters correctly
        @Suppress("DEPRECATION")
        resConfigs("en,ar,bg,cs,da,de,el,es,fi,fr,hu,it,iw,nl,pl,pt,ru,sv,ta,tr,uk,zh-rCN,zh-rTW")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments.putAll(
            mapOf(
                "clearPackageData" to "true",
                "useTestStorageService" to "true"
            )
        )
    }
    buildTypes {
        release {
            @Suppress("kotlin:S7204") // Does not make sense for open source apps
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        buildConfig = true
    }
    @Suppress("UnstableApiUsage")
    androidResources {
        generateLocaleConfig = true
        localeFilters += listOf(
            "en",
            "ar",
            "bg",
            "cs",
            "da",
            "de",
            "el",
            "es",
            "fi",
            "fr",
            "hu",
            "it",
            "iw",
            "nl",
            "pl",
            "pt-rBR",
            "ru",
            "sv",
            "ta",
            "tr",
            "uk",
            "zh-rCN",
            "zh-rTW"
        )
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
        animationsDisabled = true
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
    }
    lint {
        abortOnError = true
        warningsAsErrors = true
        disable.add("IconLocation")
        disable.addAll(elements = if (project.hasProperty("noGradleDeps")) listOf("GradleDependency", "AndroidGradlePluginVersion") else listOf())
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.lifecycle.extensions)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.color.picker)
    implementation(libs.simply.pdf)
    implementation(libs.gson)
    implementation(libs.tableview)
    implementation(libs.androidplot)
    implementation(libs.appintro)
    implementation(libs.calendar)
    implementation(libs.icondialog)
    implementation(libs.espresso.idling.resource)
    implementation(libs.espresso.idling.concurrent)
    implementation(libs.flexbox)
    implementation(libs.androidx.biometric)
    implementation(libs.preferencex.ringtone)
    implementation(libs.preferencex)

    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.inline)
    testImplementation(libs.robolectric)
    testImplementation(libs.jazzer.junit)
    testRuntimeOnly(libs.junit.jupiter.engine)

    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.espresso.contrib)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.screengrab)
    androidTestImplementation(libs.uiautomator)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.barista)
    androidTestUtil(libs.androidx.test.orchestrator)

    annotationProcessor(libs.androidx.room.compiler)

    coreLibraryDesugaring(libs.desugar.jdk.libs)
}

play {
    track.set("internal")
    defaultToAppBundles.set(true)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}
tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(listOf("-Xlint:unchecked", "-Xlint:deprecation"))
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
            "build/reports/jacoco/JacocoDebugCodeCoverage/JacocoDebugCodeCoverage.xml"
        )
    }
}

tasks.withType(Test::class) {
    configure<JacocoTaskExtension> {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
    if (System.getProperty("fuzzing") != "true")
        exclude("**/*FuzzTest.class")
    else
        include("**/*FuzzTest.class")
}

// Define task names for unit tests and Android tests
val unitTests = "testDebugUnitTest"
val androidTests = "connectedDebugAndroidTest"
val exclusions = listOf(
    "**/R.class",
    "**/R$*.class",
    "**/BuildConfig.*",
    "**/Manifest*.*",
    "**/*Test*.*",
    "**/*Args.*",
    "**/*Directions.*"
)

// Register a JacocoReport task for code coverage analysis
tasks.register<JacocoReport>("JacocoDebugCodeCoverage") {
    // Depend on unit tests and Android tests tasks
    dependsOn(listOf(unitTests, androidTests))
    // Set task grouping and description
    group = "Reporting"
    description = "Execute UI and unit tests, generate and combine Jacoco coverage report"
    // Configure reports to generate both XML and HTML formats
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
    // Set source directories to the main source directory
    sourceDirectories.setFrom(layout.projectDirectory.dir("src/main/java"))
    // Set class directories to compiled Java and Kotlin classes, excluding specified exclusions
    classDirectories.setFrom(
        files(
            fileTree(layout.buildDirectory.dir("intermediates/javac/")) {
                exclude(exclusions)
            },
            fileTree(layout.buildDirectory.dir("intermediates/built_in_kotlinc/")) {
                exclude(exclusions)
            }
        ))
    // Collect execution data from .exec and .ec files generated during test execution
    executionData.setFrom(
        files(
            fileTree(layout.buildDirectory) { include(listOf("**/*.exec", "**/*.ec")) }
        ))
}
