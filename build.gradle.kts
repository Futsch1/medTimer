// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    //noinspection GradleDependency: Version 8.8.0 has an issue with JaCoCo coverage
    id("com.android.application") version "8.9.0" apply false
    id("androidx.room") version "2.6.1" apply false
    id("androidx.navigation.safeargs") version "2.8.8" apply false
    id("org.jetbrains.kotlin.android") version "2.1.10" apply false
}

buildscript {
    repositories {
        google()
        maven { url = uri("https://jitpack.io") }
    }
}
