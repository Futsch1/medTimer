// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.9.2" apply false
    id("androidx.room") version "2.7.1" apply false
    id("androidx.navigation.safeargs") version "2.9.0" apply false
    id("org.jetbrains.kotlin.android") version "2.1.20" apply false
}

buildscript {
    repositories {
        google()
        maven { url = uri("https://jitpack.io") }
    }
}
