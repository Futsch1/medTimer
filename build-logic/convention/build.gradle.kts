/*
 * Convention plugins for medTimer modules
 */

plugins {
    `kotlin-dsl`
}

group = "com.futsch1.medtimer.buildlogic"

kotlin {
    jvmToolchain(21)
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidLibrary") {
            id = "medtimer.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("composeLibrary") {
            id = "medtimer.compose.library"
            implementationClass = "ComposeLibraryConventionPlugin"
        }
    }
}
