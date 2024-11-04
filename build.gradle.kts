// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.7.2" apply false
    id("androidx.room") version "2.6.1" apply false
    id("androidx.navigation.safeargs") version "2.8.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
}

buildscript {
    repositories {
        google()
        maven { url = uri("https://jitpack.io") }
    }
}
