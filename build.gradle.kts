// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    //noinspection AndroidGradlePluginVersion
    id("com.android.application") version "9.0.0" apply false
    id("androidx.room") version "2.8.4" apply false
    id("androidx.navigation.safeargs") version "2.9.7" apply false
    id("org.jetbrains.kotlin.android") version "2.2.21" apply false
}

buildscript {
    repositories {
        google()
        maven { url = uri("https://jitpack.io") }
    }
}
