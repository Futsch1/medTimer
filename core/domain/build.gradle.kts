plugins {
    id("medtimer.android.library")
}

android {
    namespace = "com.futsch1.medtimer.core.domain"
}

dependencies {
    implementation(project(":core:database"))
    implementation(project(":core:preferences"))

    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
}
