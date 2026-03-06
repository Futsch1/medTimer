plugins {
    id("medtimer.android.library")
    id("medtimer.compose.library")
}

android {
    namespace = "com.futsch1.medtimer.core.designsystem"
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
}
