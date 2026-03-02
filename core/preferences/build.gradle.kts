plugins {
    id("medtimer.android.library")
    id("medtimer.android.hilt")
}

android {
    namespace = "com.futsch1.medtimer.core.preferences"
}

dependencies {
    implementation(libs.androidx.preference.ktx)
}
