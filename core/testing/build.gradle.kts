plugins {
    id("medtimer.android.library")
    id("medtimer.compose.library")
}

android {
    namespace = "com.futsch1.medtimer.core.testing"
}

dependencies {
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.ui.test.junit4)
    api(libs.androidx.compose.ui.test.manifest)
    api(libs.robolectric)
    api(libs.junit4)
}
