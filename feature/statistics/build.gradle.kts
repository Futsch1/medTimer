plugins {
    id("medtimer.android.feature")
}

android {
    namespace = "com.futsch1.medtimer.statistics"
}

dependencies {
    implementation(libs.vico.compose)
    implementation(libs.vico.compose.m3)
    implementation(libs.calendar.compose)
    implementation(libs.androidx.compose.runtime.livedata)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.preference.ktx)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)

    testImplementation(libs.junit4)
    testImplementation(libs.robolectric)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.inline)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    androidTestImplementation(project(":core:testing"))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
