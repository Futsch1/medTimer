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
    compileOnly(libs.hilt.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
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
        register("androidRoom") {
            id = "medtimer.android.room"
            implementationClass = "AndroidRoomConventionPlugin"
        }
        register("androidFeature") {
            id = "medtimer.android.feature"
            implementationClass = "AndroidFeatureConventionPlugin"
        }
        register("androidHilt") {
            id = "medtimer.android.hilt"
            implementationClass = "AndroidHiltConventionPlugin"
        }
    }
}
