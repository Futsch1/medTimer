// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    //noinspection AndroidGradlePluginVersion
    id("com.android.application") version "8.13.2" apply false
    id("androidx.room") version "2.8.4" apply false
    id("androidx.navigation.safeargs") version "2.9.6" apply false
    id("org.jetbrains.kotlin.android") version "2.3.0" apply false
}

buildscript {
    repositories {
        google()
        maven { url = uri("https://jitpack.io") }
    }
}
