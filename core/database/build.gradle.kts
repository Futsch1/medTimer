plugins {
    id("medtimer.android.library")
    id("medtimer.android.room")
}

android {
    namespace = "com.futsch1.medtimer.core.database"
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(libs.gson)
    implementation(libs.espresso.idling.resource)
    implementation(libs.espresso.idling.concurrent)
    implementation(libs.androidx.lifecycle.livedata.ktx)
}
