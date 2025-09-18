// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    //noinspection AndroidGradlePluginVersion
    id("com.android.application") version "8.11.1" apply false
    id("androidx.room") version "2.7.2" apply false
    id("androidx.navigation.safeargs") version "2.9.4" apply false
    id("org.jetbrains.kotlin.android") version "2.2.10" apply false
}

buildscript {
    repositories {
        google()
        maven { url = uri("https://jitpack.io") }
    }
}
