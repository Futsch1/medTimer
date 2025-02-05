plugins {
    id("com.android.application")
    id("androidx.room")
    id("com.github.triplet.play") version "3.12.1"
    id("androidx.navigation.safeargs")
    id("org.jetbrains.kotlin.android")
    id("jacoco")
    //noinspection GradleDependency: Version 6 crashes with an error in apache.commons.compress
    id("org.sonarqube") version "5.1.0.4882"
    id("tech.apter.junit5.jupiter.robolectric-extension-gradle-plugin") version "0.9.0"
}

room {
    schemaDirectory("$projectDir/schemas")
}

android {
    namespace = "com.futsch1.medtimer"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.futsch1.medtimer"
        minSdk = 28
        multiDexEnabled = true
        targetSdk = 35
        versionCode = 98
        versionName = "1.15.3"
        base.archivesName = "MedTimer"

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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
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
        animationsDisabled = true
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    val appcompatVersion = "1.7.0"
    val materialVersion = "1.12.0"
    val constraintLayoutVersion = "2.2.0"
    val androidXNavigationVersion = "2.8.6"
    val preferenceKtxVersion = "1.2.1"
    val lifecycleExtensionsVersion = "2.2.0"
    val workRuntimeVersion = "2.10.0"
    val coreKtxVersion = "1.15.0"
    val lifecycleViewmodelKtxVersion = "2.8.7"
    val roomVersion = "2.6.1"
    val colorPickerViewVersion = "2.3.0"
    val simplyPDFVersion = "2.1.1"
    val gsonVersion = "2.12.1"
    val tableViewVersion = "0.8.9.4"
    val androidPlotVersion = "1.5.11"
    val appIntroVersion = "6.3.1"
    val calendarVersion = "2.6.2"
    val iconDialogVersion = "3.3.0"

    val junitVersion = "5.11.4"
    val mockitoCoreVersion = "5.15.2"
    val mockitoInlineVersion = "5.2.0"
    val robolectricVersion = "4.14.1"
    val jazzerVersion = "0.24.0"

    val androidTestJunitVersion = "1.2.1"
    val androidTestEspressoVersion = "3.6.1"
    val androidTestRulesVersion = "1.6.1"
    val screengrabVersion = "2.1.1"
    val uiautomatorVersion = "2.3.0"
    val androidTestRunnerVersion = "1.6.2"
    val androidTestOrchestratorVersion = "1.5.1"
    val baristaVersion = "4.3.0"

    val desugarJdkVersion = "2.1.4"

    implementation("androidx.appcompat:appcompat:$appcompatVersion")
    implementation("com.google.android.material:material:$materialVersion")
    implementation("androidx.constraintlayout:constraintlayout:$constraintLayoutVersion")
    implementation("androidx.navigation:navigation-fragment-ktx:$androidXNavigationVersion")
    implementation("androidx.navigation:navigation-ui-ktx:$androidXNavigationVersion")
    implementation("androidx.preference:preference-ktx:$preferenceKtxVersion")
    implementation("androidx.lifecycle:lifecycle-extensions:$lifecycleExtensionsVersion")
    implementation("androidx.work:work-runtime:$workRuntimeVersion")
    implementation("androidx.core:core-ktx:$coreKtxVersion")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleViewmodelKtxVersion")
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("com.github.skydoves:colorpickerview:$colorPickerViewVersion")
    implementation("com.github.wwdablu:SimplyPDF:$simplyPDFVersion")
    implementation("com.google.code.gson:gson:$gsonVersion")
    implementation("com.github.evrencoskun:TableView:v$tableViewVersion")
    implementation("com.androidplot:androidplot-core:$androidPlotVersion")
    implementation("com.github.AppIntro:AppIntro:$appIntroVersion")
    implementation("com.kizitonwose.calendar:view:$calendarVersion")
    implementation("com.maltaisn:icondialog:$iconDialogVersion")
    implementation("androidx.test.espresso:espresso-idling-resource:$androidTestEspressoVersion")
    implementation("androidx.test.espresso.idling:idling-concurrent:$androidTestEspressoVersion")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testImplementation("org.mockito:mockito-core:$mockitoCoreVersion")
    testImplementation("org.mockito:mockito-inline:$mockitoInlineVersion")
    testImplementation("org.robolectric:robolectric:$robolectricVersion")
    testImplementation("com.code-intelligence:jazzer-junit:$jazzerVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")

    androidTestImplementation("androidx.test.ext:junit:$androidTestJunitVersion")
    androidTestImplementation("androidx.test.espresso:espresso-core:$androidTestEspressoVersion")
    androidTestImplementation("androidx.test.espresso:espresso-contrib:$androidTestEspressoVersion")
    androidTestImplementation("androidx.test:rules:$androidTestRulesVersion")
    androidTestImplementation("tools.fastlane:screengrab:$screengrabVersion")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:$uiautomatorVersion")
    androidTestImplementation("androidx.test:runner:$androidTestRunnerVersion")
    androidTestImplementation("com.adevinta.android:barista:$baristaVersion")
    androidTestUtil("androidx.test:orchestrator:$androidTestOrchestratorVersion")

    annotationProcessor("androidx.room:room-compiler:$roomVersion")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:$desugarJdkVersion")
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

android {
    // Define task names for unit tests and Android tests
    val unitTests = "testDebugUnitTest"
    val androidTests = "connectedDebugAndroidTest"
    val exclusions = listOf(
        "**/R.class",
        "**/R\$*.class",
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
        classDirectories.setFrom(files(
            fileTree(layout.buildDirectory.dir("intermediates/javac/")) {
                exclude(exclusions)
            },
            fileTree(layout.buildDirectory.dir("tmp/kotlin-classes/")) {
                exclude(exclusions)
            }
        ))
        // Collect execution data from .exec and .ec files generated during test execution
        executionData.setFrom(files(
            fileTree(layout.buildDirectory) { include(listOf("**/*.exec", "**/*.ec")) }
        ))
    }
}
