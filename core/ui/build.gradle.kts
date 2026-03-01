plugins {
    id("medtimer.android.library")
    id("medtimer.compose.library")
}

android {
    namespace = "com.futsch1.medtimer.core.ui"
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3)
    implementation(libs.kotlinx.collections.immutable)
}
