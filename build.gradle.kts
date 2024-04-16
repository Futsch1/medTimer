// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.3.1" apply false
    id("androidx.room") version "2.6.1" apply false
}

buildscript {
    repositories {
        google()
    }
    dependencies {
        val androidXNavigationVersion = "2.7.7"
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:$androidXNavigationVersion")
    }
}
